package io.github.barmoury.trace;

import io.github.barmoury.converter.ObjectConverter;
import lombok.Data;
import lombok.SneakyThrows;

import java.io.Serializable;

@Data
public class Location implements Serializable {

    long latitude;
    long longitude;
    String state;
    String country;
    String address;

    @jakarta.persistence.Converter
    public static class Converter extends ObjectConverter<Location> {

        @SneakyThrows
        @Override
        public Location convertToEntityAttribute(String s) {
            return objectMapper.readValue(s, Location.class);
        }

    }

}
