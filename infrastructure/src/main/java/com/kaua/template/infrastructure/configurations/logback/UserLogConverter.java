package com.kaua.template.infrastructure.configurations.logback;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.kaua.template.infrastructure.configurations.authentication.AuthenticatedService;
import com.kaua.template.infrastructure.configurations.authentication.AuthenticatedUser;
import com.kaua.template.infrastructure.configurations.authentication.ServiceAuthentication;
import com.kaua.template.infrastructure.configurations.authentication.UserAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class UserLogConverter extends ClassicConverter {

    private static final Logger log = LoggerFactory.getLogger(UserLogConverter.class);

    @Override
    public String convert(final ILoggingEvent event) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return "anonymous"; // anonymous user
            }

            return switch (authentication) {
                case ServiceAuthentication serviceAuthentication when serviceAuthentication.getPrincipal() instanceof AuthenticatedService authenticatedService ->
                        authenticatedService.id();
                case UserAuthentication userAuthentication when userAuthentication.getPrincipal() instanceof AuthenticatedUser aUser ->
                        aUser.id();
                default -> "anonymous";
            };
        } catch (Exception ex) {
            log.warn("Error on get authenticated user/system to set in log");
            return "error";
        }
    }
}
