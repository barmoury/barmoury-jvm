package io.github.barmoury.api.config;

import io.github.barmoury.api.exception.*;
import io.github.barmoury.copier.CopierException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ValidationException;
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
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public abstract class ExceptionAdviser extends DefaultResponseErrorHandler {

    public abstract Object processResponse(Exception ex, List<Object> errors);

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<Object> errors = new ArrayList<>();
        for (ObjectError objectError : ex.getBindingResult().getAllErrors()) {
            errors.add(objectError.getDefaultMessage());
        }
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    public Object handleValidationExceptions(ConstraintViolationException ex) {
        List<Object> errors = new ArrayList<>();
        for (ConstraintViolation<?> constraintViolation : ex.getConstraintViolations()) {
            errors.add(constraintViolation.getMessage());
        }
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BindException.class)
    public Object handleValidationExceptions(BindException ex) {
        List<Object> errors = new ArrayList<>();
        for (ObjectError objectError : ex.getBindingResult().getAllErrors()) {
            errors.add(objectError.getDefaultMessage());
        }
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleException(IllegalArgumentException ex) {
        List<Object> errors = new ArrayList<>();
        errors.add(ex.getMessage());
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Object handleException(HttpRequestMethodNotSupportedException ex) {
        List<Object> errors = new ArrayList<>();
        errors.add(ex.getMessage());
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(AccessDeniedException.class)
    public Object handleException(AccessDeniedException ex) {
        List<Object> errors = new ArrayList<>();
        errors.add("Access denied. You do not have access to this file");
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(SignatureException.class)
    public Object handleException(SignatureException ex) {
        List<Object> errors = new ArrayList<>();
        errors.add("Access denied. You do not have access to this resource. Suspicious request.");
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public Object handleException(org.springframework.security.access.AccessDeniedException ex) {
        List<Object> errors = new ArrayList<>();
        errors.add("Access denied. You do not have the required access");
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Object handleException(MissingServletRequestParameterException ex) {
        List<Object> errors = new ArrayList<>();
        errors.add(ex.getMessage());
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public Object handleException(Exception ex) {
        List<Object> errors = new ArrayList<>();
        errors.add("Internal server error occur");
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(HttpMessageConversionException.class)
    public Object handleException(HttpMessageConversionException ex) {
        List<Object> errors = new ArrayList<>();
        errors.add("Unable to convert the object for response");
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(NullPointerException.class)
    public Object handleException(NullPointerException ex) {
        List<Object> errors = new ArrayList<>();
        errors.add("Internal server error occur");
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Object handleException(HttpMediaTypeNotSupportedException ex) {
        List<Object> errors = new ArrayList<>();
        errors.add(ex.getMessage());
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Object handleException(HttpMessageNotReadableException ex) {
        List<Object> errors = new ArrayList<>();
        errors.add("The request body is missing or is invalid");
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidDataAccessResourceUsageException.class)
    public Object handleException(InvalidDataAccessResourceUsageException ex) {
        List<Object> errors = new ArrayList<>();
        errors.add("Invalid parameter, especially when querying an invalid column");
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentConversionNotSupportedException.class)
    public Object handleException(MethodArgumentConversionNotSupportedException ex) {
        List<Object> errors = new ArrayList<>();
        errors.add("Invalid parameter, check the values and try again");
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({EntityNotFoundException.class})
    public Object handleException(EntityNotFoundException ex) {
        List<Object> errors = new ArrayList<>();
        errors.add(ex.getMessage());
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({NoSuchElementException.class})
    public Object handleException(NoSuchElementException ex) {
        List<Object> errors = new ArrayList<>();
        errors.add(ex.getMessage());
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.PRECONDITION_FAILED)
    @ExceptionHandler({PreConditionException.class})
    public Object handleException(PreConditionException ex) {
        List<Object> errors = new ArrayList<>(); errors.add(ex.getMessage());
        return this.processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(UnsupportedOperationException.class)
    public Object handleException(UnsupportedOperationException ex) {
        List<Object> errors = new ArrayList<>();
        errors.add(ex.getMessage());
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Object handleException(DataIntegrityViolationException ex) {
        List<Object> errors = new ArrayList<>();
        errors.add("Duplicate or constraint error occur while saving the resource. Check your request payload");
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ParseException.class)
    public Object handleException(ParseException ex) {
        List<Object> errors = new ArrayList<>();
        errors.add("An error occur while trying to parse an input, request query params. " + ex.getMessage());
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(JpaSystemException.class)
    public Object handleException(JpaSystemException ex) {
        List<Object> errors = new ArrayList<>();
        errors.add("Error occur during persistence. Contact support");
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(CopierException.class)
    public Object handleException(CopierException ex) {
        List<Object> errors = new ArrayList<>();
        errors.add(ex.getMessage());
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(UnexpectedRollbackException.class)
    public Object handleException(UnexpectedRollbackException ex) {
        List<Object> errors = new ArrayList<>();
        errors.add(ex.getMessage());
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidBactuatorQueryException.class)
    public Object handleException(InvalidBactuatorQueryException ex) {
        List<Object> errors = new ArrayList<>();
        errors.add(ex.getMessage());
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(InvalidLoginException.class)
    public Object handleException(InvalidLoginException ex) {
        List<Object> errors = new ArrayList<>();
        errors.add(ex.getMessage());
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(SubModelResolveException.class)
    public Object handleException(SubModelResolveException ex) {
        List<Object> errors = new ArrayList<>();
        errors.add("Invalid field specified when updating " + ex.getEntity());
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Object handleException(MethodArgumentTypeMismatchException ex) {
        List<Object> errors = new ArrayList<>();
        errors.add(String.format("Unable to convert the parameter '%s' to the required type '%s' " +
                        "", ex.getValue(),
                ex.getParameter().getGenericParameterType().getTypeName()));
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(ExpiredJwtException.class)
    public Object handleException(ExpiredJwtException ex) {
        List<Object> errors = new ArrayList<>();
        errors.add("The authorization token has expired");
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(MalformedJwtException.class)
    public Object handleException(MalformedJwtException ex) {
        List<Object> errors = new ArrayList<>();
        errors.add("The authorization token is malformed");
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NoHandlerFoundException.class)
    public Object handleException(NoHandlerFoundException ex) {
        List<Object> errors = new ArrayList<>();
        errors.add("No handler found for route");
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(RouteMethodNotSupportedException.class)
    public Object handleException(RouteMethodNotSupportedException ex) {
        List<Object> errors = new ArrayList<>();
        errors.add(ex.getMessage());
        return processResponse(ex, errors);
    }

    @ResponseBody
    @ExceptionHandler(ValidationException.class)
    public Object handleException(ValidationException ex, HttpServletResponse response) {
        List<Object> errors = new ArrayList<>();
        errors.add("An error occur while trying to validate the payload body");
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return processResponse(ex, errors);
    }


}
