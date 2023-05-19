package io.github.barmoury.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.barmoury.api.exception.ConstraintViolationException;
import io.github.barmoury.api.exception.PgpConstraintViolationException;
import io.github.barmoury.crypto.pgp.PgpRequestHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.extern.log4j.Log4j2;
import org.apache.tomcat.util.codec.binary.Base64;
import org.hibernate.validator.HibernateValidatorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
public class PgpRequestBodyTranslator implements HandlerInterceptor {

    @Autowired
    LocalValidatorFactoryBean localValidatorFactoryBean;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        PgpRequestHandler pgpRequestHandler = ((HandlerMethod) handler).getMethodAnnotation(PgpRequestHandler.class);
        if (pgpRequestHandler != null) {
            String payload = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            if (payload.isBlank()) throw new IllegalArgumentException("the request body is empty, cannot" +
                    " continue with pgp translation");
            ObjectMapper objectMapper = PgpConfig.getObjectMapper();
            Object requestBody = objectMapper
                    .readValue(PgpConfig.decodeEncryptedString(payload), pgpRequestHandler.value());
            request.setAttribute(PgpConfig.REQUEST_MODEL_ATTRIBUTE, requestBody);
            Valid valid = requestBody.getClass().getAnnotation(Valid.class);
            Validated validated = requestBody.getClass().getAnnotation(Validated.class);
            if (valid == null && validated == null) return true;
            Validator validator = localValidatorFactoryBean
                    .unwrap(HibernateValidatorFactory.class )
                    .usingContext()
                    .constraintValidatorPayload(requestBody)
                    .getValidator();
            Set<? extends ConstraintViolation<?>> errors = validator.validate(requestBody);
            if (!errors.isEmpty()) {
                throw new PgpConstraintViolationException(pgpRequestHandler.value(), errors);
            }
        }
        return true;
    }

}