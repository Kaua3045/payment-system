package com.kaua.template.infrastructure.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaua.template.IntegrationTest;
import com.kaua.template.application.wrapper.TracerWrapper;
import com.kaua.template.domain.utils.IdentifierUtils;
import com.kaua.template.infrastructure.configurations.SecurityConfig;
import com.kaua.template.infrastructure.idempotency.gateways.IdempotencyKeyGateway;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;

import static com.kaua.template.ApiTest.admin;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@IntegrationTest
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
public class IdempotencyKeyFilterTest {

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private IdempotencyKeyGateway idempotencyKeyGateway;

    @Autowired
    private TracerWrapper tracerWrapper;

    @Test
    void givenAValidPostMethodWithValidNonExistsIdempotencyKey_whenCallEndpoint_thenReturnSuccess() throws Exception {
        final var aId = IdentifierUtils.generateNewIdWithoutHyphen();

        final var aBody = new IdempotencyKeyBodyTest(aId);

        final var request = MockMvcRequestBuilders.post("/test/idempotency-key-helper/success")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-idempotency-key", IdentifierUtils.generateNewIdWithoutHyphen())
                .content(this.mapper.writeValueAsString(aBody))
                .with(admin());

        this.mvc.perform(request)
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.header().string("Location", "/test/idempotency-key-helper/" + aId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(aId));
    }

    @Test
    void givenAValidPostMethodWithValidNonExistsIdempotencyKeyButWithXSS_whenCallEndpoint_thenReturnSuccess() throws Exception {
        final var aBody = new IdempotencyKeyBodyTest("<script>alert('xss')</script>");

        final var request = MockMvcRequestBuilders.post("/test/idempotency-key-helper/success")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-idempotency-key", IdentifierUtils.generateNewIdWithoutHyphen())
                .content(this.mapper.writeValueAsString(aBody))
                .with(admin());

        this.mvc.perform(request)
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.header().string("Location", "/test/idempotency-key-helper/" + "&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;"));
    }

    @Test
    void givenAValidPostMethodWithValidExistsIdempotencyKey_whenCallEndpoint_thenReturnSuccess() throws Exception {
        final var aId = IdentifierUtils.generateNewIdWithoutHyphen();
        final var aKey = IdentifierUtils.generateNewIdWithoutHyphen();

        final var aBody = new IdempotencyKeyBodyTest(aId);

        final var aFirstRequest = MockMvcRequestBuilders.post("/test/idempotency-key-helper/success")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-idempotency-key", aKey)
                .content(this.mapper.writeValueAsString(aBody))
                .with(admin());

        this.mvc.perform(aFirstRequest)
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.header().string("Location", "/test/idempotency-key-helper/" + aId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(aId));

        final var request = MockMvcRequestBuilders.post("/test/idempotency-key-helper/success")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-idempotency-key", aKey)
                .content(this.mapper.writeValueAsString(aBody))
                .with(admin());

        this.mvc.perform(request)
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.header().string("Location", "/test/idempotency-key-helper/" + aId))
                .andExpect(MockMvcResultMatchers.header().string("x-idempotency-response", "true"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(aId));
    }

    @Test
    void givenAValidGetMethodWithoutIdempotencyKeyAnnotation_whenCallEndpoint_thenReturnSuccess() throws Exception {
        final var aId = IdentifierUtils.generateNewIdWithoutHyphen();

        final var request = MockMvcRequestBuilders.get("/test/idempotency-key-helper/" + aId)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .with(admin());

        this.mvc.perform(request)
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(aId));
    }

    @Test
    void givenAValidPatchMethodWithValidNonExistsIdempotencyKey_whenCallEndpoint_thenReturnSuccess() throws Exception {
        final var aId = IdentifierUtils.generateNewIdWithoutHyphen();

        final var aBody = new IdempotencyKeyBodyTest(aId);

        final var request = MockMvcRequestBuilders.patch("/test/idempotency-key-helper/patch/" + aId)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-idempotency-key", IdentifierUtils.generateNewIdWithoutHyphen())
                .content(this.mapper.writeValueAsString(aBody))
                .with(admin());

        this.mvc.perform(request)
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(aId));
    }

    @Test
    void givenAValidPutMethodWithValidNonExistsIdempotencyKey_whenCallEndpoint_thenReturnSuccess() throws Exception {
        final var aId = IdentifierUtils.generateNewIdWithoutHyphen();

        final var aBody = new IdempotencyKeyBodyTest(aId);

        final var request = MockMvcRequestBuilders.put("/test/idempotency-key-helper/put/idempotency/" + aId)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-idempotency-key", IdentifierUtils.generateNewIdWithoutHyphen())
                .content(this.mapper.writeValueAsString(aBody))
                .with(admin());

        this.mvc.perform(request)
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(aId));
    }

    @Test
    void givenASupportedPutMethodButWithoutIdempotencyKeyAnnotation_whenCallEndpoint_thenReturnSuccess() throws Exception {
        final var aId = IdentifierUtils.generateNewIdWithoutHyphen();

        final var aBody = new IdempotencyKeyBodyTest(aId);

        final var request = MockMvcRequestBuilders.put("/test/idempotency-key-helper/put/" + aId)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(this.mapper.writeValueAsString(aBody))
                .with(admin());

        this.mvc.perform(request)
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(aId));
    }

    @Test
    void givenAValidPutMethodButWithoutIdempotencyHeader_whenCallEndpoint_thenReturnError() throws Exception {
        final var aId = IdentifierUtils.generateNewIdWithoutHyphen();

        final var aBody = new IdempotencyKeyBodyTest(aId);

        final var request = MockMvcRequestBuilders.put("/test/idempotency-key-helper/put/idempotency/" + aId)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(this.mapper.writeValueAsString(aBody))
                .with(admin());

        this.mvc.perform(request)
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isUnprocessableEntity())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Idempotency key required and the required header is 'x-idempotency-key'"));
    }

    @Test
    void givenAnInvalidGetMethodWithIdempotencyKeyAnnotation_whenCallEndpoint_thenReturnError() throws Exception {
        final var aId = IdentifierUtils.generateNewIdWithoutHyphen();

        final var request = MockMvcRequestBuilders.get("/test/idempotency-key-helper/get/" + aId)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .with(admin());

        this.mvc.perform(request)
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isMethodNotAllowed())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Idempotency key is not supported for this method: GET"));
    }

    @Test
    void testOnHandlerMethodThrows() throws Exception {
        final var aRequest = Mockito.mock(HttpServletRequest.class);
        final var aResponse = Mockito.mock(HttpServletResponse.class);
        final var aFilterChain = Mockito.mock(FilterChain.class);

        final var aHandlerExceptionResolver = Mockito.spy(HandlerExceptionResolver.class);
        final var aRequestMappingHandlerMapping = Mockito.mock(RequestMappingHandlerMapping.class);

        final var aIdempotencyKeyFilter = new IdempotencyKeyFilter(
                idempotencyKeyGateway,
                aRequestMappingHandlerMapping,
                aHandlerExceptionResolver,
                tracerWrapper
        );

        Mockito.when(aRequestMappingHandlerMapping.getHandler(Mockito.any()))
                .thenThrow(new RuntimeException("test"));

        aIdempotencyKeyFilter.doFilterInternal(aRequest, aResponse, aFilterChain);

        Mockito.verify(aHandlerExceptionResolver, Mockito.times(1))
                .resolveException(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void testOnHandlerMethodReturnsNull() throws Exception {
        final var aRequest = Mockito.mock(HttpServletRequest.class);
        final var aResponse = Mockito.mock(HttpServletResponse.class);
        final var aFilterChain = Mockito.mock(FilterChain.class);

        final var aHandlerExceptionResolver = Mockito.spy(HandlerExceptionResolver.class);
        final var aRequestMappingHandlerMapping = Mockito.mock(RequestMappingHandlerMapping.class);

        final var aIdempotencyKeyFilter = new IdempotencyKeyFilter(
                idempotencyKeyGateway,
                aRequestMappingHandlerMapping,
                aHandlerExceptionResolver,
                tracerWrapper
        );

        Mockito.when(aRequestMappingHandlerMapping.getHandler(aRequest))
                .thenReturn(null);

        Assertions.assertDoesNotThrow(() -> aIdempotencyKeyFilter.doFilterInternal(aRequest, aResponse, aFilterChain));
    }

    @Test
    void testOnHandlerMethodIsNotHandlerMethod() throws Exception {
        final var aRequest = Mockito.mock(HttpServletRequest.class);
        final var aResponse = Mockito.mock(HttpServletResponse.class);
        final var aFilterChain = Mockito.mock(FilterChain.class);

        final var aHandlerExceptionResolver = Mockito.spy(HandlerExceptionResolver.class);
        final var aRequestMappingHandlerMapping = Mockito.mock(RequestMappingHandlerMapping.class);

        final var aIdempotencyKeyFilter = new IdempotencyKeyFilter(
                idempotencyKeyGateway,
                aRequestMappingHandlerMapping,
                aHandlerExceptionResolver,
                tracerWrapper
        );

        final var aHandlerExecutionChain = Mockito.mock(HandlerExecutionChain.class);

        Mockito.when(aRequestMappingHandlerMapping.getHandler(aRequest))
                .thenReturn(aHandlerExecutionChain);

        Assertions.assertDoesNotThrow(() -> aIdempotencyKeyFilter.doFilterInternal(aRequest, aResponse, aFilterChain));
    }

    @Test
    void testOnHandlerMethodReturnsValidHandlerMethod() throws Exception {
        final var aRequest = mock(HttpServletRequest.class);
        final var aHandlerExceptionResolver = mock(HandlerExceptionResolver.class);
        final var aRequestMappingHandlerMapping = mock(RequestMappingHandlerMapping.class);
        final var aHandlerMethod = mock(HandlerMethod.class);

        final var aHandlerChain = new HandlerExecutionChain(aHandlerMethod);

        when(aRequestMappingHandlerMapping.getHandler(Mockito.any()))
                .thenReturn(aHandlerChain);

        final var aIdempotencyKeyFilter = new IdempotencyKeyFilter(
                idempotencyKeyGateway,
                aRequestMappingHandlerMapping,
                aHandlerExceptionResolver,
                tracerWrapper
        );

        var method = IdempotencyKeyFilter.class.getDeclaredMethod("getHandlerMethod", HttpServletRequest.class);
        method.setAccessible(true);
        var result = method.invoke(aIdempotencyKeyFilter, aRequest);

        Assertions.assertEquals(aHandlerMethod, result);
    }

    @Test
    void testOnHandlerMethodReturnsHandlerChainButNotHandlerMethod() throws Exception {
        final var aRequest = mock(HttpServletRequest.class);
        final var aHandlerExceptionResolver = mock(HandlerExceptionResolver.class);
        final var aRequestMappingHandlerMapping = mock(RequestMappingHandlerMapping.class);

        final var someOtherHandler = new Object();
        final var aHandlerChain = new HandlerExecutionChain(someOtherHandler);

        when(aRequestMappingHandlerMapping.getHandler(Mockito.any()))
                .thenReturn(aHandlerChain);

        final var aIdempotencyKeyFilter = new IdempotencyKeyFilter(
                idempotencyKeyGateway,
                aRequestMappingHandlerMapping,
                aHandlerExceptionResolver,
                tracerWrapper
        );

        var method = IdempotencyKeyFilter.class.getDeclaredMethod("getHandlerMethod", HttpServletRequest.class);
        method.setAccessible(true);
        var result = method.invoke(aIdempotencyKeyFilter, aRequest);

        Assertions.assertNull(result);
    }

    @Test
    void shouldReturnTrueWhenMethodAndClassAreAnnotated() throws NoSuchMethodException {
        @RestController
        class TestController {
            @IdempotencyKey
            public void testMethod() {
            }
        }

        Method method = TestController.class.getMethod("testMethod");
        HandlerMethod handlerMethod = new HandlerMethod(new TestController(), method);

        boolean result = invokeIsIdempotencyKeyAnnotated(handlerMethod);
        Assertions.assertTrue(result);
    }

    @Test
    void shouldReturnFalseWhenMethodIsAnnotatedButClassIsNot() throws NoSuchMethodException {
        class TestController {
            @IdempotencyKey
            public void testMethod() {
            }
        }

        Method method = TestController.class.getMethod("testMethod");
        HandlerMethod handlerMethod = new HandlerMethod(new TestController(), method);

        boolean result = invokeIsIdempotencyKeyAnnotated(handlerMethod);
        Assertions.assertFalse(result);
    }

    @Test
    void shouldReturnFalseWhenMethodIsNotAnnotatedButClassIs() throws NoSuchMethodException {
        @RestController
        class TestController {
            public void testMethod() {
            }
        }

        Method method = TestController.class.getMethod("testMethod");
        HandlerMethod handlerMethod = new HandlerMethod(new TestController(), method);

        boolean result = invokeIsIdempotencyKeyAnnotated(handlerMethod);
        Assertions.assertFalse(result);
    }

    @Test
    void shouldReturnFalseWhenNeitherMethodNorClassAreAnnotated() throws NoSuchMethodException {
        class TestController {
            public void testMethod() {
            }
        }

        Method method = TestController.class.getMethod("testMethod");
        HandlerMethod handlerMethod = new HandlerMethod(new TestController(), method);

        boolean result = invokeIsIdempotencyKeyAnnotated(handlerMethod);
        Assertions.assertFalse(result);
    }

    private boolean invokeIsIdempotencyKeyAnnotated(HandlerMethod handlerMethod) {
        final var filter = new IdempotencyKeyFilter(
                idempotencyKeyGateway,
                mock(RequestMappingHandlerMapping.class),
                mock(HandlerExceptionResolver.class),
                tracerWrapper
        );
        return Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(filter, "isIdempotencyKeyAnnotated", handlerMethod));
    }
}
