package io.github.barmoury.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.barmoury.crypto.pgp.PgpRequestHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import java.util.stream.Collectors;

@Log4j2
public class PgpRequestBodyTranslator implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        PgpRequestHandler pgpRequestHandler = ((HandlerMethod) handler).getMethodAnnotation(PgpRequestHandler.class);
        if (pgpRequestHandler != null) {
            String payload = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            if (payload.isBlank()) throw new IllegalArgumentException("the request body is empty, cannot" +
                    " continue with pgp translation");
            ObjectMapper objectMapper = PgpConfig.getObjectMapper();
            request.setAttribute(PgpConfig.REQUEST_MODEL_ATTRIBUTE, objectMapper
                    .readValue(PgpConfig.decodeEncryptedString(payload), pgpRequestHandler.value()));
        }
        return true;
    }

}