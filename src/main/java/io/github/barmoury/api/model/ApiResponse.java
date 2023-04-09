package io.github.barmoury.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    T data;
    String message;
    boolean success;
    List<Object> errors;

    public ApiResponse(T data, String message, boolean success) {
        this.data = data;
        this.message = message;
        this.success = success;
    }

    public ApiResponse(List<Object> errors, String message) {
        this.errors = errors;
        this.success = false;
        this.message = message;
    }

    public ApiResponse(List<Object> errors) {
        this.errors = errors;
        this.success = false;
        this.message = errors.get(0).toString();
    }

    public ApiResponse(T data, String message) {
        this(data, message, true);
    }

    public ApiResponse(T data, boolean success) {
        this(data, null, success);
    }

    public ApiResponse(T data) {
        this(data, true);
    }

    public static <T> ResponseEntity<ApiResponse<T>> build(HttpStatus status, T data, String message, boolean success) {
        return new ResponseEntity<>(new ApiResponse<>(data, message, success), status);
    }

    public static <T> ResponseEntity<ApiResponse<T>> build(HttpStatus status, T data, String message) {
        return new ResponseEntity<>(new ApiResponse<>(data, message), status);
    }

    public static <T> ResponseEntity<ApiResponse<T>> build(HttpStatus status, T data, boolean success) {
        return new ResponseEntity<>(new ApiResponse<>(data, success), status);
    }

    public static <T> ResponseEntity<ApiResponse<T>> build(HttpStatus status, T data) {
        return new ResponseEntity<>(new ApiResponse<>(data), status);
    }

    public static <T> ResponseEntity<ApiResponse<T>> buildError(HttpStatus status, List<Object> errors, String message) {
        return new ResponseEntity<>(new ApiResponse<>(errors, message), status);
    }

    public static <T> ResponseEntity<ApiResponse<T>> buildError(HttpStatus status, String message) {
        List<Object> errors = new ArrayList<>(); errors.add(message);
        return new ResponseEntity<>(new ApiResponse<>(errors, message), status);
    }

    public static <T> ResponseEntity<ApiResponse<T>> buildError(HttpStatus status, List<Object> errors) {
        return new ResponseEntity<>(new ApiResponse<>(errors, errors.get(0).toString()), status);
    }

    public static ResponseEntity<Void> noContent() {
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
