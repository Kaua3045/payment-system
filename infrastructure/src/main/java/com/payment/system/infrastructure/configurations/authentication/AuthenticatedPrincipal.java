package com.payment.system.infrastructure.configurations.authentication;

public sealed interface AuthenticatedPrincipal permits AuthenticatedService, AuthenticatedUser {

    String id();
}
