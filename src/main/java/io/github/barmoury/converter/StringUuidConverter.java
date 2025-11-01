package io.github.barmoury.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.UUID;

@Converter
public class StringUuidConverter implements AttributeConverter<String, UUID> {

    @Override
    public UUID convertToDatabaseColumn(String o) {
        return (o == null || o.isBlank() ? null : UUID.fromString(o));
    }

    @Override
    public String convertToEntityAttribute(UUID s) {
        return (s == null ? "" : s.toString());
    }

}
