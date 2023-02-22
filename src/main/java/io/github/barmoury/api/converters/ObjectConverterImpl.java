package io.github.barmoury.api.converters;

import jakarta.persistence.Converter;
import lombok.SneakyThrows;

@Converter
public class ObjectConverterImpl extends ObjectConverter<Object> {

    @Override
    @SneakyThrows
    public Object convertToEntityAttribute(String s) {
        return objectMapper.readValue(s, Object.class);
    }

}
