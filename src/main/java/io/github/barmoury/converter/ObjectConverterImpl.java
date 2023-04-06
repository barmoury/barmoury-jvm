package io.github.barmoury.converter;

import io.github.barmoury.converter.ObjectConverter;
import jakarta.persistence.Converter;
import lombok.SneakyThrows;

@Converter
public class ObjectConverterImpl extends ObjectConverter<Object> {

    @Override
    @SneakyThrows
    public Object convertToEntityAttribute(String s) {
        if (s == null) return null;
        return objectMapper.readValue(s, Object.class);
    }

}
