package io.github.barmoury.api.exception;

import jakarta.validation.ConstraintViolation;

import java.util.Set;

public class PgpConstraintViolationException extends jakarta.validation.ConstraintViolationException {

    Class<?> type;

    public PgpConstraintViolationException(Class<?> type, Set<? extends ConstraintViolation<?>> constraintViolations) {
        super(constraintViolations);
        this.type = type;
    }

}
