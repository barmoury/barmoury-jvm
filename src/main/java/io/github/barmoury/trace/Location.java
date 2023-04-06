package io.github.barmoury.trace;

import io.github.barmoury.converter.ObjectConverter;
import jakarta.persistence.Converter;
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

    @Converter
    public static class LocationConverter extends ObjectConverter<Location> {

        @SneakyThrows
        @Override
        public Location convertToEntityAttribute(String s) {
            return objectMapper.readValue(s, Location.class);
        }

    }

}
