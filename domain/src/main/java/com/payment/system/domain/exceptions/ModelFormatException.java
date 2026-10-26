package com.payment.system.domain.exceptions;

public class ModelFormatException extends NoStackTraceException {

    public ModelFormatException(int line, String message) {
        super("Model invalid (line " + line + "): " + message, "FRAUD_MODEL_FORMAT_INVALID");
    }
}
