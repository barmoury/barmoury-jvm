package io.github.barmoury.crypto.pgp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.barmoury.api.config.PgpConfig;
import io.github.barmoury.util.FieldUtil;
import lombok.SneakyThrows;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.IOException;

public class PgpTranslateHttpMessageConverter extends AbstractHttpMessageConverter<PgpTranslate> {

    public PgpTranslateHttpMessageConverter() {
        super(new MediaType("text", "plain"));
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
        return objectMapper
                .readValue(PgpConfig.decodeEncryptedString(new String(inputMessage.getBody()
                        .readAllBytes(), PgpConfig.getCharset())), clazz);
    }

    @Override
    protected void writeInternal(PgpTranslate t, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        outputMessage.getBody().write(t.toEncryptedString().getBytes(PgpConfig.getCharset()));
    }

}
