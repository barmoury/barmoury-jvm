package io.github.barmoury.api.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

@Getter
public class HttpStatusException extends RuntimeException {

    HttpStatus httpStatus;
    List<String> errors = new ArrayList<>();

    public HttpStatusException(HttpStatus httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public HttpStatusException(HttpStatus httpStatus, String message, List<String> errors) {
        super(message);
        this.errors = errors;
        this.httpStatus = httpStatus;
    }

}
