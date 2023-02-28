package io.github.barmoury.api.config;

import io.github.barmoury.api.exception.InvalidBactuatorQueryException;
import io.github.barmoury.api.exception.InvalidLoginException;
import io.github.barmoury.api.exception.SubModelResolveException;
import io.github.barmoury.copier.CopierException;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.persistence.EntityNotFoundException;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.validation.BindException;
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

public abstract class ExceptionAdviser extends DefaultResponseErrorHandler {

    public abstract Logger getLogger();
    public abstract <T> T processResponse(String message, boolean success);

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public <T> T handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = (!ex.getBindingResult().getAllErrors().isEmpty() ?
                ex.getBindingResult().getAllErrors().get(0).getDefaultMessage():
                "");
        //getLogger().error(ex.getMessage(), ex);
        return processResponse(errorMessage, false);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BindException.class)
    public <T> T handleValidationExceptions(BindException ex) {
        String errorMessage = (!ex.getBindingResult().getAllErrors().isEmpty() ?
                ex.getBindingResult().getAllErrors().get(0).getDefaultMessage():
                "");
        //getLogger().error(ex.getMessage(), ex);
        return processResponse(errorMessage, false);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public <T> T handleException(IllegalArgumentException ex) {
        getLogger().error(ex.getMessage(), ex);
        return processResponse(ex.getMessage(), false);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public <T> T handleException(HttpRequestMethodNotSupportedException ex) {
        return processResponse(ex.getMessage(), false);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(AccessDeniedException.class)
    public <T> T handleException(AccessDeniedException ex) {
        getLogger().error(ex.getMessage(), ex);
        return processResponse("Access denied. You do not have access to this file", false);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public <T> T handleException(org.springframework.security.access.AccessDeniedException ex) {
        getLogger().error(ex.getMessage(), ex);
        return processResponse("Access denied. You do not have the required access", false);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public <T> T handleException(MissingServletRequestParameterException ex) {
        getLogger().error(ex.getMessage(), ex);
        return processResponse(ex.getMessage(), false);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public <T> T handleException(Exception ex) {
        getLogger().error(ex.getMessage(), ex);
        return processResponse("Internal server error occur", false);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public <T> T handleException(HttpMediaTypeNotSupportedException ex) {
        getLogger().error(ex.getMessage(), ex);
        return processResponse(ex.getMessage(), false);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public <T> T handleException(HttpMessageNotReadableException ex) {
        getLogger().error(ex.getMessage(), ex);
        return processResponse("The request body is missing or is invalid", false);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidDataAccessResourceUsageException.class)
    public <T> T handleException(InvalidDataAccessResourceUsageException ex) {
        getLogger().error(ex.getMessage(), ex);
        return processResponse("Invalid parameter, especially in the value of sort parameter", false);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentConversionNotSupportedException.class)
    public <T> T handleException(MethodArgumentConversionNotSupportedException ex) {
        getLogger().error(ex.getMessage(), ex);
        return processResponse("Invalid parameter, check the values and try again", false);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(EntityNotFoundException.class)
    public <T> T handleException(EntityNotFoundException ex) {
        getLogger().error(ex.getMessage(), ex);
        return processResponse(ex.getMessage(), false);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(UnsupportedOperationException.class)
    public <T> T handleException(UnsupportedOperationException ex) {
        getLogger().error(ex.getMessage(), ex);
        return processResponse(ex.getMessage(), false);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public <T> T handleException(DataIntegrityViolationException ex) {
        getLogger().error(ex.getMessage(), ex);
        return processResponse("Duplicate or constraint error occur while saving the resource. " +
                "Check your request payload", false);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(JpaSystemException.class)
    public <T> T handleException(JpaSystemException ex) {
        getLogger().error(ex.getMessage(), ex);
        return processResponse("Error occur during persistence. Contact support", false);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(CopierException.class)
    public <T> T handleException(CopierException ex) {
        getLogger().error(ex.getMessage(), ex);
        return processResponse(ex.getMessage(), false);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(UnexpectedRollbackException.class)
    public <T> T handleException(UnexpectedRollbackException ex) {
        return processResponse(ex.getMessage(), false);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidBactuatorQueryException.class)
    public <T> T handleException(InvalidBactuatorQueryException ex) {
        getLogger().error(ex.getMessage(), ex);
        return processResponse(ex.getMessage(), false);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(InvalidLoginException.class)
    public <T> T handleException(InvalidLoginException ex) {
        return processResponse(ex.getMessage(), false);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(SubModelResolveException.class)
    public <T> T handleException(SubModelResolveException ex) {
        getLogger().error(ex.getMessage(), ex);
        return processResponse("Invalid field specified when updating " + ex.getEntity(), false);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public <T> T handleException(MethodArgumentTypeMismatchException ex) {
        getLogger().error(ex.getMessage(), ex);
        return processResponse(String.format("Unable to convert the parameter '%s' to the required type '%s' " +
                "", ex.getValue(),
                ex.getParameter().getGenericParameterType().getTypeName()), false);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(ExpiredJwtException.class)
    public <T> T handleException(ExpiredJwtException ex) {
        getLogger().error(ex.getMessage(), ex);
        return processResponse("The authorization token has expired", false);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NoHandlerFoundException.class)
    public <T> T handleException(NoHandlerFoundException ex) {
        getLogger().error(ex.getMessage(), ex);
        return processResponse(ex.getMessage(), false);
    }

}
