package com.kaua.template.infrastructure.idempotency;

import com.kaua.template.application.wrapper.TracerWrapper;
import com.kaua.template.infrastructure.configurations.xss.XSSRequestWrapper;
import com.kaua.template.infrastructure.exceptions.IdempotencyKeyRequiredException;
import com.kaua.template.infrastructure.exceptions.IdempotencyKeyUnsupportedMethodException;
import com.kaua.template.infrastructure.idempotency.gateways.IdempotencyKeyGateway;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class IdempotencyKeyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyKeyFilter.class);

    private final IdempotencyKeyGateway idempotencyKeyGateway;
    private final RequestMappingHandlerMapping requestMappingHandlerMapping;
    private final HandlerExceptionResolver resolver;
    private final TracerWrapper tracerWrapper;

    public IdempotencyKeyFilter(
            final IdempotencyKeyGateway idempotencyKeyGateway,
            final RequestMappingHandlerMapping requestMappingHandlerMapping,
            final @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver,
            final TracerWrapper tracerWrapper
    ) {
        this.idempotencyKeyGateway = Objects.requireNonNull(idempotencyKeyGateway);
        this.requestMappingHandlerMapping = Objects.requireNonNull(requestMappingHandlerMapping);
        this.resolver = Objects.requireNonNull(resolver);
        this.tracerWrapper = Objects.requireNonNull(tracerWrapper);
    }

    @Override
    protected void doFilterInternal(
            @NonNull final HttpServletRequest request,
            @NonNull final HttpServletResponse response,
            @NonNull final FilterChain filterChain
    ) {
        log.debug("Processing the idempotency key filter");
        XSSRequestWrapper sanitizedRequest = new XSSRequestWrapper(request);

        this.tracerWrapper.trace(
                "http.filter.idempotency_key_filter",
                span -> {
                    span.setAttribute("http.method", request.getMethod());
                    span.setAttribute("http.path", request.getRequestURI());
                    span.setAttribute("http.query", request.getQueryString());
                    span.setAttribute("http.remote_address", request.getRemoteAddr());
                    span.setAttribute("http.user_agent", request.getHeader("User-Agent"));

                    try {
                        final var aHandlerMethod = getHandlerMethod(sanitizedRequest);

                        if (aHandlerMethod != null && isIdempotencyKeyAnnotated(aHandlerMethod)) {
                            if (!isSupportedMethod(sanitizedRequest)) {
                                throw new IdempotencyKeyUnsupportedMethodException(sanitizedRequest.getMethod());
                            }

                            final var aIdempotencyKey = sanitizedRequest.getHeader(IdempotencyKey.IDEMPOTENCY_KEY_HEADER);

                            if (!StringUtils.hasText(aIdempotencyKey)) {
                                throw new IdempotencyKeyRequiredException();
                            }

                            span.setAttribute("idempotency_key", aIdempotencyKey);
                            final var aExistsIdempotencyKey = this.idempotencyKeyGateway.find(aIdempotencyKey);

                            if (aExistsIdempotencyKey.isPresent()) {
                                response.setStatus(aExistsIdempotencyKey.get().statusCode());
                                aExistsIdempotencyKey.get().headers().forEach(response::addHeader);
                                response.getWriter().write(aExistsIdempotencyKey.get().body());
                                response.addHeader(IdempotencyKey.IDEMPOTENCY_RESPONSE_HEADER, "true");
                                span.setAttribute("idempotency_key_found", true);
                                log.debug("Idempotency key found, returning the previous response {}", aExistsIdempotencyKey.get());
                                return;
                            }

                            final var aIdempotencyKeyValues = getIdempotencyKeyValues(aHandlerMethod);
                            final var aTTL = aIdempotencyKeyValues.ttl();
                            final var aTimeUnit = aIdempotencyKeyValues.timeUnit();

                            log.debug("Idempotency key not found, saving before processing the request [key:{}] [ttl:{}] [timeUnit:{}]",
                                    aIdempotencyKey, aTTL, aTimeUnit);

                            this.idempotencyKeyGateway.save(aIdempotencyKey, aTTL, aTimeUnit);

                            final var aResponseWrapper = new ContentCachingResponseWrapper(response);
                            filterChain.doFilter(sanitizedRequest, aResponseWrapper);

                            final var aBodyArray = aResponseWrapper.getContentAsByteArray();
                            aResponseWrapper.copyBodyToResponse();

                            final var aBody = new String(aBodyArray, aResponseWrapper.getCharacterEncoding());
                            final var aHeaders = aResponseWrapper.getHeaderNames().stream()
                                    .collect(Collectors.toMap(
                                            headerName -> headerName,
                                            aResponseWrapper::getHeader
                                    ));

                            final var aInput = new IdempotencyKeyInput(
                                    aResponseWrapper.getStatus(),
                                    aBody,
                                    aHeaders
                            );

                            this.idempotencyKeyGateway.save(
                                    aIdempotencyKey,
                                    aInput,
                                    aTTL,
                                    aTimeUnit
                            );

                            log.debug("Idempotency key not found, saving the response for future requests [key:{}] [ttl:{}] [timeUnit:{}], result: {}",
                                    aIdempotencyKey, aTTL, aTimeUnit, aInput);
                        } else {
                            filterChain.doFilter(sanitizedRequest, response);
                        }
                    } catch (final Exception e) {
                        resolver.resolveException(sanitizedRequest, response, null, e);
                    }
                });
    }

    private HandlerMethod getHandlerMethod(final HttpServletRequest request) {
        // Obtain the handler method from the request, if available
        final HandlerExecutionChain handlerChain;
        try {
            handlerChain = this.requestMappingHandlerMapping.getHandler(request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (handlerChain != null && handlerChain.getHandler() instanceof HandlerMethod) {
            return (HandlerMethod) handlerChain.getHandler();
        }
        return null;
    }

    private boolean isIdempotencyKeyAnnotated(final HandlerMethod handlerMethod) {
        Method method = handlerMethod.getMethod();
        return method.isAnnotationPresent(IdempotencyKey.class) && handlerMethod.getBeanType().isAnnotationPresent(RestController.class);
    }

    private IdempotencyKey getIdempotencyKeyValues(final HandlerMethod handlerMethod) {
        return handlerMethod.getMethodAnnotation(IdempotencyKey.class);
    }

    private boolean isSupportedMethod(final HttpServletRequest request) {
        return request.getMethod().equals("POST") ||
                request.getMethod().equals("PATCH") ||
                request.getMethod().equals("PUT");
    }
}
