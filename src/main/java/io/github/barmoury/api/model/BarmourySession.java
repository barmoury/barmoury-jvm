package io.github.barmoury.api.model;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.barmoury.api.converters.ObjectConverter;
import io.github.barmoury.api.converters.ObjectConverterImpl;
import io.github.barmoury.api.persistence.RequestParamFilter;
import jakarta.persistence.*;
import lombok.Data;
import lombok.SneakyThrows;

import java.io.Serializable;
import java.util.Date;

@Data
@MappedSuperclass
public class BarmourySession<T> extends BarmouryModel {

    @RequestParamFilter long refreshCount;
    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE) String actorId;
    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE) String actorType;
    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE) String ipAddress;
    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE) String sessionId;
    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE) String sessionToken;
    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE) String lastAuthToken;
    @RequestParamFilter(operator = RequestParamFilter.Operator.EQ) String status = "ACTIVE";
    @RequestParamFilter(operator = RequestParamFilter.Operator.BETWEEN) Date expirationDate;
    @Transient T actor;

    @RequestParamFilter(operator = RequestParamFilter.Operator.OBJECT_STR_LIKE)
    @Column(columnDefinition = "TEXT") @Convert(converter = DeviceConverter.class)
    Device device;

    @RequestParamFilter(operator = RequestParamFilter.Operator.OBJECT_STR_LIKE)
    @Column(columnDefinition = "TEXT") @Convert(converter = LocationConverter.class)
    Location location;

    @RequestParamFilter(operator = RequestParamFilter.Operator.OBJECT_STR_LIKE)
    @Column(columnDefinition = "TEXT") @Convert(converter = ObjectConverterImpl.class)
    Object extraData;

    @Data
    public static class Device implements Serializable {

        String osName;
        String osVersion;
        String engineName;
        String deviceName;
        String deviceType;
        String deviceClass;
        String browserName;
        String engineVersion;
        String browserVersion;

    }

    @Converter
    static class DeviceConverter extends ObjectConverter<Device> {

        @SneakyThrows
        @Override
        public Device convertToEntityAttribute(String s) {
            return objectMapper.readValue(s, Device.class);
        }

    }

    @Data
    public static class Location implements Serializable {

        long latitude;
        long longitude;
        String state;
        String country;
        String address;

    }

    @Converter
    static class LocationConverter extends ObjectConverter<Location> {

        @SneakyThrows
        @Override
        public Location convertToEntityAttribute(String s) {
            return objectMapper.readValue(s, Location.class);
        }

    }

}
