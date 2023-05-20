package io.github.barmoury.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.barmoury.crypto.IEncryptor;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;

@Converter
public abstract class EncryptorObjectConverter<T> implements AttributeConverter<T, Object> {

    @Autowired
    public ObjectMapper objectMapper;

    public abstract IEncryptor<Object> getEncryptor();

    @Override
    @SneakyThrows
    public String convertToDatabaseColumn(T o) {
        return getEncryptor().encrypt(objectMapper.writeValueAsString(o));
    }

    @Override
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public T convertToEntityAttribute(Object s) {
        return (T) objectMapper.readValue(getEncryptor().decrypt(s.toString()).toString(), Object.class);
    }

}
