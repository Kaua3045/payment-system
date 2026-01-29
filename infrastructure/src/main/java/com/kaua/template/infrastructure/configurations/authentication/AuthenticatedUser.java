package com.kaua.template.infrastructure.configurations.authentication;

public record AuthenticatedUser(String id) implements AuthenticatedPrincipal {
}
