package com.payment.system.domain.pixkeys;

import java.util.regex.Pattern;

public record EmailPixKey(String value) implements PixKeyValue {

    private static final String EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+){0,5}\\.[a-zA-Z]{2,5}$")
            .pattern();

    public EmailPixKey {
        this.assertArgumentNotEmpty(value, "value", "Email pix key value should not be empty");
        this.assertArgumentPattern(value, EMAIL_PATTERN, "value", "Email pix key value is not valid");
    }

    @Override
    public PixKeyType type() {
        return PixKeyType.EMAIL;
    }
}
