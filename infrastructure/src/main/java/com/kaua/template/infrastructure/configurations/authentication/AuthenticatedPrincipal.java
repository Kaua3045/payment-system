package com.kaua.template.infrastructure.configurations.authentication;

public sealed interface AuthenticatedPrincipal permits AuthenticatedService, AuthenticatedUser {

    String id();
}
