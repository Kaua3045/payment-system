package com.payment.system.infrastructure.configurations.authentication;

public record AuthenticatedUser(String id) implements AuthenticatedPrincipal {
}
