package io.github.barmoury.api.exception;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class PreConditionException extends RuntimeException {

    List<String> errors = new ArrayList<>();

    public PreConditionException(String message) {
        super(message);
    }

    public PreConditionException(String message, List<String> errors) {
        super(message);
        this.errors = errors;
    }

}
