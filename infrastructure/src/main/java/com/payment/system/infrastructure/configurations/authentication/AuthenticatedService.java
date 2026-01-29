package com.payment.system.infrastructure.configurations.authentication;

public record AuthenticatedService(String id) implements AuthenticatedPrincipal {
}
