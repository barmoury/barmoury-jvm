package io.github.barmoury.crypto.pgp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.barmoury.api.config.PgpConfig;
import io.github.barmoury.api.exception.ConstraintViolationException;
import io.github.barmoury.util.FieldUtil;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.SneakyThrows;
import org.hibernate.validator.HibernateValidatorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class PgpTranslateHttpMessageConverter extends AbstractHttpMessageConverter<PgpTranslate> {

    @Autowired LocalValidatorFactoryBean localValidatorFactoryBean;

    public PgpTranslateHttpMessageConverter() {
        super(new MediaType("*", "*"));
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return FieldUtil.isSubclassOf(clazz, PgpTranslate.class);
    }

    @Override
    @SneakyThrows
    protected PgpTranslate readInternal(Class<? extends PgpTranslate> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        ObjectMapper objectMapper = PgpConfig.getObjectMapper();
        PgpTranslate requestBody = objectMapper
                .readValue(PgpConfig.decodeEncryptedString(new String(inputMessage.getBody()
                        .readAllBytes(), PgpConfig.getCharset())), clazz);
        Valid valid = requestBody.getClass().getAnnotation(Valid.class);
        Validated validated = requestBody.getClass().getAnnotation(Validated.class);
        if (valid == null && validated == null) return requestBody;
        Validator validator = localValidatorFactoryBean.unwrap(HibernateValidatorFactory.class )
                .usingContext()
                .constraintValidatorPayload(requestBody)
                .getValidator();
        Set<? extends ConstraintViolation<?>> errors = validator.validate(requestBody);
        if (!errors.isEmpty()) {
            throw new ConstraintViolationException(clazz, errors);
        }
        return requestBody;
    }

    @Override
    protected void writeInternal(PgpTranslate t, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        outputMessage.getBody().write(t.toEncryptedString().getBytes(PgpConfig.getCharset()));
    }

}
