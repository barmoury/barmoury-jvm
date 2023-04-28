package io.github.barmoury.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.SneakyThrows;

@Converter
public abstract class EnumStringConverter implements AttributeConverter<Enum<?>, String> {

    public abstract Class getEnumClass();

    @Override
    public String convertToDatabaseColumn(Enum o) {
        return o.name();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Enum<?> convertToEntityAttribute(String s) {
        return Enum.valueOf(getEnumClass(), s);
    }

}
