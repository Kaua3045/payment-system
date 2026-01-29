package com.kaua.template.domain.validation;

import com.kaua.template.domain.exceptions.ValidationException;

import java.util.List;
import java.util.Set;

public interface AssertionConcern {

    default <T> T assertArgumentNotNull(final T val, final String property, final String message) {
        if (val == null) {
            throw ValidationException.with(new Error(property, message));
        }
        return val;
    }

    default String assertArgumentNotEmpty(final String val, final String property, final String message) {
        if (val == null || val.isBlank()) {
            throw ValidationException.with(new Error(property, message));
        }
        return val;
    }

    default String assertArgumentMinLength(final String val, int minLength,final String property, final String message) {
        if (val != null && val.length() < minLength) {
            throw ValidationException.with(new Error(property, message));
        }
        return val;
    }

    default String assertArgumentMaxLength(final String val, int maxLength, final String property, final String message) {
        if (val != null && val.length() > maxLength) {
            throw ValidationException.with(new Error(property, message));
        }
        return val;
    }

    // Used for fixed length strings
    default String assertArgumentFixedLength(final String val,  int length, final String property, final String message) {
        if (val == null || val.length() != length) {
            throw ValidationException.with(new Error(property, message));
        }
        return val;
    }

    default void assertArgumentTrue(final Boolean val, final String property, final String message) {
        if (Boolean.FALSE.equals(val)) {
            throw ValidationException.with(new Error(property, message));
        }
    }

    default String assertArgumentPattern(final String val, String pattern, final String property, final String message) {
        if (val != null && !val.matches(pattern)) {
            throw ValidationException.with(new Error(property, message));
        }
        return val;
    }

    default <T> Set<T> assertArgumentNotEmpty(final Set<T> val, final String property, final String message) {
        if (val == null || val.isEmpty()) {
            throw ValidationException.with(new Error(property, message));
        }
        return val;
    }

    default <T> List<T> assertArgumentNotEmpty(final List<T> val, final String property, final String message) {
        if (val == null || val.isEmpty()) {
            throw ValidationException.with(new Error(property, message));
        }
        return val;
    }

    default int assertArgumentGreaterThan(final int val, final int compare, final String property, final String message) {
        if (val <= compare) {
            throw ValidationException.with(new Error(property, message));
        }
        return val;
    }

    default int assertArgumentGreaterOrEquals(final int val, final int compare, final String property, final String message) {
        if (val < compare) {
            throw ValidationException.with(new Error(property, message));
        }
        return val;
    }
}
