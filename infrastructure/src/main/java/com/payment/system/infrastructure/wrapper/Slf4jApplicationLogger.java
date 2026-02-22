package com.payment.system.infrastructure.wrapper;

import com.payment.system.application.wrapper.ApplicationLogger;
import com.payment.system.domain.utils.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Generated
public class Slf4jApplicationLogger implements ApplicationLogger {

    private final Logger logger;

    public Slf4jApplicationLogger(final Class<?> aClazz) {
        this.logger = LoggerFactory.getLogger(aClazz);
    }

    @Override
    public void info(String message, Object... args) {
        logger.info(message, args);
    }

    @Override
    public void warn(String message, Object... args) {
        logger.warn(message, args);
    }

    @Override
    public void error(String message, Object... args) {
        logger.error(message, args);
    }
}
