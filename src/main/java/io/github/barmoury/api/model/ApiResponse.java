package io.github.barmoury.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.barmoury.api.config.TranslationConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Data
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    T data;
    String message;
    boolean success;
    List<Object> errors;
    Object secondaryData;

    @Builder.Default
    @JsonIgnore Locale locale = TranslationConfig.getSessionLocale();

    public ApiResponse(T data, String message, boolean success) {
        this.data = data;
        this.success = success;
        setMessage(message);
    }

    public ApiResponse(T data, String message, boolean success, Locale locale) {
        this.data = data;
        this.locale = locale;
        this.success = success;
        setMessage(message);
    }

    public ApiResponse(T data, String message, boolean success, Object secondaryData) {
        this.data = data;
        this.success = success;
        this.secondaryData = secondaryData;
        setMessage(message);
    }

    public ApiResponse(T data, String message, boolean success, Object secondaryData, Locale locale) {
        this.data = data;
        this.locale = locale;
        this.success = success;
        this.secondaryData = secondaryData;
        setMessage(message);
    }

    public ApiResponse(List<Object> errors, String message) {
        this.success = false;
        setErrors(errors);
        setMessage(message);
    }

    public ApiResponse(List<Object> errors, String message, Locale locale) {
        this.success = false;
        this.locale = locale;
        setErrors(errors);
        setMessage(message);
    }

    public ApiResponse(List<Object> errors, String message, Object secondaryData) {
        this.success = false;
        this.secondaryData = secondaryData;
        setErrors(errors);
        setMessage(message);
    }

    public ApiResponse(List<Object> errors, String message, Object secondaryData, Locale locale) {
        this.success = false;
        this.locale = locale;
        this.secondaryData = secondaryData;
        setErrors(errors);
        setMessage(message);
    }

    public ApiResponse(List<Object> errors) {
        this.success = false;
        setErrors(errors);
        setMessage(errors.get(0).toString());
    }

    public ApiResponse(List<Object> errors, Locale locale) {
        this.success = false;
        this.locale = locale;
        setErrors(errors);
        setMessage(errors.get(0).toString());
    }

    public void setMessage(String message) {
        if (locale == null) locale = TranslationConfig.getSessionLocale();
        if (message.startsWith("{") && message.endsWith("}")) {
            this.message = TranslationConfig.getTranslation().t(message.substring(1, message.length()-1), locale);
            return;
        }
        this.message = message;
    }

    public void setErrors(List<Object> errors) {
        this.errors = new ArrayList<>();
        if (locale == null) locale = TranslationConfig.getSessionLocale();
        for (Object error : errors) {
            if (error instanceof String errorMessage) {
                if (errorMessage.startsWith("{") && errorMessage.endsWith("}")) {
                    error = TranslationConfig.getTranslation().t(errorMessage.substring(1, errorMessage.length()-1), locale);
                }
            }
            this.errors.add(error);
        }
    }

    public ApiResponse(T data, String message) {
        this(data, message, true);
    }

    public ApiResponse(T data, String message, Locale locale) {
        this(data, message, true, locale);
    }

    public ApiResponse(T data, String message, Object secondaryData) {
        this(data, message, true, secondaryData);
    }

    public ApiResponse(T data, String message, Object secondaryData, Locale locale) {
        this(data, message, true, secondaryData, locale);
    }

    public ApiResponse(T data, boolean success) {
        this(data, null, success);
    }

    public ApiResponse(T data, boolean success, Locale locale) {
        this(data, null, success, locale);
    }

    public ApiResponse(T data, boolean success, Object secondaryData) {
        this(data, null, success, secondaryData);
    }

    public ApiResponse(T data, boolean success, Object secondaryData, Locale locale) {
        this(data, null, success, secondaryData, locale);
    }

    public ApiResponse(T data) {
        this(data, true);
    }

    public ApiResponse(T data, Locale locale) {
        this(data, true, locale);
    }

    public ApiResponse(T data, Object secondaryData) {
        this(data, true, secondaryData);
    }

    public ApiResponse(T data, Object secondaryData, Locale locale) {
        this(data, true, secondaryData, locale);
    }

    public static <T> ResponseEntity<ApiResponse<T>> build(HttpStatus status, T data, String message,
                                                           boolean success) {
        return new ResponseEntity<>(new ApiResponse<>(data, message, success), status);
    }

    public static <T> ResponseEntity<ApiResponse<T>> build(HttpStatus status, T data, String message,
                                                           boolean success, Locale locale) {
        return new ResponseEntity<>(new ApiResponse<>(data, message, success, locale), status);
    }

    public static <T> ResponseEntity<ApiResponse<T>> build(HttpStatus status, T data, String message,
                                                           boolean success, Object secondaryData) {
        return new ResponseEntity<>(new ApiResponse<>(data, message, success, secondaryData), status);
    }

    public static <T> ResponseEntity<ApiResponse<T>> build(HttpStatus status, T data, String message,
                                                           boolean success, Object secondaryData, Locale locale) {
        return new ResponseEntity<>(new ApiResponse<>(data, message, success, secondaryData, locale), status);
    }

    public static <T> ResponseEntity<ApiResponse<T>> build(HttpStatus status, T data, String message) {
        return new ResponseEntity<>(new ApiResponse<>(data, message), status);
    }

    public static <T> ResponseEntity<ApiResponse<T>> build(HttpStatus status, T data, String message, Locale locale) {
        return new ResponseEntity<>(new ApiResponse<>(data, message, locale), status);
    }

    public static <T> ResponseEntity<ApiResponse<T>> build(HttpStatus status, T data, String message,
                                                           Object secondaryData) {
        return new ResponseEntity<>(new ApiResponse<>(data, message, secondaryData), status);
    }

    public static <T> ResponseEntity<ApiResponse<T>> build(HttpStatus status, T data, String message,
                                                           Object secondaryData, Locale locale) {
        return new ResponseEntity<>(new ApiResponse<>(data, message, secondaryData, locale), status);
    }

    public static <T> ResponseEntity<ApiResponse<T>> build(HttpStatus status, T data, boolean success) {
        return new ResponseEntity<>(new ApiResponse<>(data, success), status);
    }

    public static <T> ResponseEntity<ApiResponse<T>> build(HttpStatus status, T data, boolean success, Locale locale) {
        return new ResponseEntity<>(new ApiResponse<>(data, success, locale), status);
    }

    public static <T> ResponseEntity<ApiResponse<T>> build(HttpStatus status, T data,
                                                           boolean success, Object secondaryData) {
        return new ResponseEntity<>(new ApiResponse<>(data, success, secondaryData), status);
    }

    public static <T> ResponseEntity<ApiResponse<T>> build(HttpStatus status, T data,
                                                           boolean success, Object secondaryData, Locale locale) {
        return new ResponseEntity<>(new ApiResponse<>(data, success, secondaryData, locale), status);
    }

    public static <T> ResponseEntity<ApiResponse<T>> build(HttpStatus status, T data) {
        return new ResponseEntity<>(new ApiResponse<>(data), status);
    }

    public static <T> ResponseEntity<ApiResponse<T>> build(HttpStatus status, T data, Locale locale) {
        return new ResponseEntity<>(new ApiResponse<>(data, locale), status);
    }

    public static <T> ResponseEntity<ApiResponse<T>> build(HttpStatus status, ApiResponse<T> apiResponse) {
        return new ResponseEntity<>(apiResponse, status);
    }

    public static <T> ResponseEntity<ApiResponse<T>> build(HttpStatus status, T data, Object secondaryData) {
        return new ResponseEntity<>(new ApiResponse<>(data, secondaryData), status);
    }

    public static <T> ResponseEntity<ApiResponse<T>> build(HttpStatus status, T data, Object secondaryData, Locale locale) {
        return new ResponseEntity<>(new ApiResponse<>(data, secondaryData, locale), status);
    }

    public static <T> ResponseEntity<ApiResponse<T>> buildError(HttpStatus status, List<Object> errors,
                                                                String message) {
        return new ResponseEntity<>(new ApiResponse<>(errors, message), status);
    }

    public static <T> ResponseEntity<ApiResponse<T>> buildError(HttpStatus status, List<Object> errors,
                                                                String message, Locale locale) {
        return new ResponseEntity<>(new ApiResponse<>(errors, message, locale), status);
    }

    public static <T> ResponseEntity<ApiResponse<T>> buildError(HttpStatus status, String message) {
        List<Object> errors = new ArrayList<>(); errors.add(message);
        return new ResponseEntity<>(new ApiResponse<>(errors, message), status);
    }

    public static <T> ResponseEntity<ApiResponse<T>> buildError(HttpStatus status, String message, Locale locale) {
        List<Object> errors = new ArrayList<>(); errors.add(message);
        return new ResponseEntity<>(new ApiResponse<>(errors, message, locale), status);
    }

    public static <T> ResponseEntity<ApiResponse<T>> buildError(HttpStatus status, List<Object> errors) {
        return new ResponseEntity<>(new ApiResponse<>(errors, errors.get(0).toString()), status);
    }

    public static <T> ResponseEntity<ApiResponse<T>> buildError(HttpStatus status, List<Object> errors, Locale locale) {
        return new ResponseEntity<>(new ApiResponse<>(errors, errors.get(0).toString(), locale), status);
    }

    public static ResponseEntity<Void> noContent() {
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
