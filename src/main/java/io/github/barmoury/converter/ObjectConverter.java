package io.github.barmoury.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;

@Converter
public abstract class ObjectConverter<T> implements AttributeConverter<T, String> {

    @Autowired
    public ObjectMapper objectMapper;

    @SneakyThrows
    @Override
    public String convertToDatabaseColumn(T o) {
        return objectMapper.writeValueAsString(o);
    }

}
