package io.github.barmoury.trace;

import io.github.barmoury.converter.ObjectConverter;
import lombok.Data;
import lombok.SneakyThrows;

import java.io.Serializable;

@Data
public class Isp implements Serializable {

    String name;
    String carrier;

    @jakarta.persistence.Converter
    public static class Converter extends ObjectConverter<Isp> {

        @SneakyThrows
        @Override
        public Isp convertToEntityAttribute(String s) {
            return objectMapper.readValue(s, Isp.class);
        }

    }

}
