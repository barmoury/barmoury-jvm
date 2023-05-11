package io.github.barmoury.api.exception;

import jakarta.validation.ConstraintViolation;

import java.util.Set;

public class ConstraintViolationException extends jakarta.validation.ConstraintViolationException {

    Class<?> type;

    public ConstraintViolationException(Class<?> type, Set<? extends ConstraintViolation<?>> constraintViolations) {
        super(constraintViolations);
        this.type = type;
    }

}
