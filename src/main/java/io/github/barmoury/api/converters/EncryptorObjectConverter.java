package io.github.barmoury.api.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.barmoury.crypto.BarmouryEncryptor;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;

@Converter
public abstract class EncryptorObjectConverter<T> implements AttributeConverter<T, String> {

    @Autowired
    public ObjectMapper objectMapper;

    public abstract BarmouryEncryptor<String> getEncryptor();

    @Override
    @SneakyThrows
    public String convertToDatabaseColumn(T o) {
        return getEncryptor().encrypt(objectMapper.writeValueAsString(o));
    }

    @Override
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public T convertToEntityAttribute(String s) {
        return (T) objectMapper.readValue(getEncryptor().encrypt(s), Object.class);
    }

}
