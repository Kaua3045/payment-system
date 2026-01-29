package com.kaua.template.infrastructure.configurations.interceptors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
public class InterceptorsAdapterConfig implements WebMvcConfigurer {

    @Autowired
    private RequestMdcInterceptor requestMdcInterceptor;

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(requestMdcInterceptor);
        WebMvcConfigurer.super.addInterceptors(registry);
    }
}
