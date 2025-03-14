package io.github.barmoury.api.config;

import io.github.barmoury.api.exception.*;
import io.github.barmoury.copier.CopierException;
import io.github.barmoury.translation.Translation;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ValidationException;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.method.annotation.MethodArgumentConversionNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.nio.file.AccessDeniedException;
import java.text.ParseException;
import java.util.*;

public abstract class ExceptionAdviser extends DefaultResponseErrorHandler {

    public abstract Object processResponse(Exception ex, List<Object> errors);
    
    public Translation getTranslation() {
        return TranslationConfig.getTranslation();
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleValidationExceptions(MethodArgumentNotValidException ex, Locale locale) {
        List<Object> errors = new ArrayList<>();
        for (ObjectError objectError : ex.getBindingResult().getAllErrors()) {
            errors.add(objectError.getDefaultMessage());
        }
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    public Object handleValidationExceptions(ConstraintViolationException ex, Locale locale) {
        List<Object> errors = new ArrayList<>();
        for (ConstraintViolation<?> constraintViolation : ex.getConstraintViolations()) {
            errors.add(constraintViolation.getMessage());
        }
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BindException.class)
    public Object handleValidationExceptions(BindException ex, Locale locale) {
        List<Object> errors = new ArrayList<>();
        for (ObjectError objectError : ex.getBindingResult().getAllErrors()) {
            errors.add(objectError.getDefaultMessage());
        }
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleException(IllegalArgumentException ex, Locale locale) {
        List<Object> errors = new ArrayList<>();
        errors.add(ex.getMessage());
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Object handleException(HttpRequestMethodNotSupportedException ex, Locale locale) {
        List<Object> errors = new ArrayList<>();
        errors.add(ex.getMessage());
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(AccessDeniedException.class)
    public Object handleException(AccessDeniedException ex, Locale locale) {
        List<Object> errors = new ArrayList<>();
        errors.add(getTranslation().t("barmoury.exception.file.access.denied",
                "Access denied. You do not have access to this file", locale));
        errors.add(ex.getMessage());
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(SignatureException.class)
    public Object handleException(SignatureException ex, Locale locale) {
        List<Object> errors = new ArrayList<>();
        errors.add(getTranslation().t("barmoury.exception.access.denied.suspicious",
                "Access denied. You do not have access to this resource. Suspicious request.",
                locale));
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public Object handleException(org.springframework.security.access.AccessDeniedException ex, Locale locale) {
        List<Object> errors = new ArrayList<>();
        errors.add(getTranslation().t("barmoury.exception.access.denied",
                "Access denied. You do not have the required access", locale));
        errors.add(ex.getMessage());
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Object handleException(MissingServletRequestParameterException ex, Locale locale) {
        List<Object> errors = new ArrayList<>();
        errors.add(ex.getMessage());
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public Object handleException(Exception ex, Locale locale) {
        List<Object> errors = new ArrayList<>();
        errors.add(getTranslation().t("barmoury.exception.internal.server.error",
                "Internal server error occur", locale));
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(RuntimeException.class)
    public Object handleException(RuntimeException ex, Locale locale) {
        List<Object> errors = new ArrayList<>();
        errors.add(getTranslation().t("barmoury.exception.internal.server.error",
                "Internal server error occur", locale));
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(HttpMessageConversionException.class)
    public Object handleException(HttpMessageConversionException ex, Locale locale) {
        List<Object> errors = new ArrayList<>();
        errors.add(getTranslation().t("barmoury.exception.cannot.convert.object",
                "Unable to convert the object for response", locale));
        errors.add(ex.getMessage());
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(NullPointerException.class)
    public Object handleException(NullPointerException ex, Locale locale) {
        List<Object> errors = new ArrayList<>();
        errors.add(getTranslation().t("barmoury.exception.internal.server.error",
                "Internal server error occur", locale));
        addSubExceptionsMessages(ex, errors);
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Object handleException(HttpMediaTypeNotSupportedException ex, Locale locale) {
        List<Object> errors = new ArrayList<>();
        errors.add(ex.getMessage());
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Object handleException(HttpMessageNotReadableException ex, Locale locale) {
        List<Object> errors = new ArrayList<>();
        errors.add(getTranslation().t("barmoury.exception.request.body.missing",
                "The request body is missing or is invalid", locale));
        errors.add(ex.getMessage());
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidDataAccessResourceUsageException.class)
    public Object handleException(InvalidDataAccessResourceUsageException ex, Locale locale) {
        List<Object> errors = new ArrayList<>();
        errors.add(getTranslation().t("barmoury.exception.invalid.column.parameter",
                "Invalid parameter, especially when querying an invalid column", locale));
        errors.add(ex.getMessage());
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentConversionNotSupportedException.class)
    public Object handleException(MethodArgumentConversionNotSupportedException ex, Locale locale) {
        List<Object> errors = new ArrayList<>();
        errors.add(getTranslation().t("barmoury.exception.invalid.parameter",
                "Invalid parameter, check the values and try again", locale));
        errors.add(ex.getMessage());
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({EntityNotFoundException.class})
    public Object handleException(EntityNotFoundException ex, Locale locale) {
        List<Object> errors = new ArrayList<>();
        errors.add(ex.getMessage());
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({NoSuchElementException.class})
    public Object handleException(NoSuchElementException ex, Locale locale) {
        List<Object> errors = new ArrayList<>();
        errors.add(ex.getMessage());
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.PRECONDITION_FAILED)
    @ExceptionHandler({PreConditionException.class})
    public Object handleException(PreConditionException ex, Locale locale) {
        List<Object> errors = new ArrayList<>(); errors.add(ex.getMessage());
        errors.addAll(ex.getErrors());
        return this.processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(UnsupportedOperationException.class)
    public Object handleException(UnsupportedOperationException ex, Locale locale) {
        List<Object> errors = new ArrayList<>();
        errors.add(ex.getMessage());
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Object handleException(DataIntegrityViolationException ex, Locale locale) {
        List<Object> errors = new ArrayList<>();
        errors.add(getTranslation().t("barmoury.exception.duplicate.constraint.error",
                "Duplicate or constraint error occur while saving the resource. Check your request payload",
                locale));
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ParseException.class)
    public Object handleException(ParseException ex, Locale locale) {
        List<Object> errors = new ArrayList<>();
        errors.add(getTranslation().t("barmoury.exception.parse.input.error",
                "An error occur while trying to parse an input, request query params",
                locale));
        errors.add(ex.getMessage());
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(JpaSystemException.class)
    public Object handleException(JpaSystemException ex, Locale locale) {
        List<Object> errors = new ArrayList<>();
        errors.add(getTranslation().t("barmoury.exception.persistence.error",
                "Error occur during persistence. Contact support",
                locale));
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(CopierException.class)
    public Object handleException(CopierException ex, Locale locale) {
        List<Object> errors = new ArrayList<>();
        errors.add(ex.getMessage());
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(UnexpectedRollbackException.class)
    public Object handleException(UnexpectedRollbackException ex, Locale locale) {
        List<Object> errors = new ArrayList<>();
        errors.add(ex.getMessage());
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidBactuatorQueryException.class)
    public Object handleException(InvalidBactuatorQueryException ex, Locale locale) {
        List<Object> errors = new ArrayList<>();
        errors.add(ex.getMessage());
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(InvalidLoginException.class)
    public Object handleException(InvalidLoginException ex, Locale locale) {
        List<Object> errors = new ArrayList<>();
        errors.add(ex.getMessage());
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(SubModelResolveException.class)
    public Object handleException(SubModelResolveException ex, Locale locale) {
        List<Object> errors = new ArrayList<>();
        errors.add(getTranslation().t("barmoury.exception.invalid.update.field",
                "Invalid field specified when updating",
                locale) + " " + ex.getEntity());
        errors.add(ex.getMessage());
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Object handleException(MethodArgumentTypeMismatchException ex, Locale locale) {
        List<Object> errors = new ArrayList<>();
        errors.add(String.format(Objects.requireNonNull(getTranslation().t("barmoury.exception.cannot.convert.parameter",
                "Unable to convert the parameter '%s' to the required type '%s'",
                locale)), ex.getValue(), ex.getParameter().getGenericParameterType().getTypeName()));
        errors.add(ex.getMessage());
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(ExpiredJwtException.class)
    public Object handleException(ExpiredJwtException ex, Locale locale) {
        List<Object> errors = new ArrayList<>();
        errors.add(getTranslation().t("barmoury.exception.authorization.token.expired",
                "The authorization token has expired", locale));
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(MalformedJwtException.class)
    public Object handleException(MalformedJwtException ex, Locale locale) {
        List<Object> errors = new ArrayList<>();
        errors.add(getTranslation().t("barmoury.exception.authorization.token.malformed",
                "The authorization token is malformed", locale));
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NoHandlerFoundException.class)
    public Object handleException(NoHandlerFoundException ex, Locale locale) {
        List<Object> errors = new ArrayList<>();
        errors.add(getTranslation().t("barmoury.exception.no.handler",
                "No handler found for route", locale));
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(RouteMethodNotSupportedException.class)
    public Object handleException(RouteMethodNotSupportedException ex, Locale locale) {
        List<Object> errors = new ArrayList<>();
        errors.add(ex.getMessage());
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ExceptionHandler(HttpStatusException.class)
    public Object handleHttpStatusException(HttpStatusException ex, HttpServletResponse response, Locale locale) {
        List<Object> errors = new ArrayList<>();
        errors.add(ex.getMessage());
        response.setStatus(ex.getHttpStatus().value());
        return processResponse(ex, errors);
    }

    @SneakyThrows
    @ResponseBody
    @ExceptionHandler(ValidationException.class)
    public Object handleException(ValidationException ex, HttpServletResponse response, Locale locale) {
        List<Object> errors = new ArrayList<>();
        errors.add(getTranslation().t("barmoury.exception.payload.validation.error",
                "An error occur while trying to validate the payload body", locale));
        addSubExceptionsMessages(ex, errors);
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return processResponse(ex, errors);
    }

    public void addSubExceptionsMessages(Throwable cause, List<Object> errors) {
        while ((cause = cause.getCause()) != null) {
            errors.add(cause.getMessage());
        }
    }


}
