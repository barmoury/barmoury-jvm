package io.github.barmoury.api.model;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.barmoury.api.converters.ObjectConverter;
import io.github.barmoury.api.converters.ObjectConverterImpl;
import io.github.barmoury.api.persistence.RequestParamFilter;
import io.github.barmoury.api.persistence.StatQuery;
import jakarta.persistence.*;
import lombok.Data;
import lombok.SneakyThrows;

import java.io.Serializable;
import java.util.Date;

@Data
@MappedSuperclass
@StatQuery(fetchPrevious = true)
public class BarmourySession<T> extends BarmouryModel {

    @RequestParamFilter
    @StatQuery.PercentageChangeQuery
    @StatQuery.AverageQuery @StatQuery.MedianQuery
    @StatQuery.ColumnQuery(name = "%s_sum", sqlFunction = "SUM")
    //@RequestParamFilter(operator = RequestParamFilter.Operator.BETWEEN) // support multiple RequestParamFilter
    long refreshCount;

    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE)
    String actorId;

    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE)
    String actorType;

    @StatQuery.OccurrenceQuery
    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE)
    @StatQuery.OccurrenceQuery(type = StatQuery.OccurrenceQuery.Type.PERCENTAGE)
    String sessionId;

    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE)
    String sessionToken;

    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE)
    String lastAuthToken;

    @RequestParamFilter(operator = RequestParamFilter.Operator.BETWEEN)
    Date expirationDate;

    @StatQuery.OccurrenceQuery
    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE)
    @StatQuery.OccurrenceQuery(type = StatQuery.OccurrenceQuery.Type.PERCENTAGE)
    String ipAddress;

    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE)
    @StatQuery.ColumnQuery(name = "total_active", sqlFunction = "COUNT", whereClause = "%s = 'ACTIVE'")
    @StatQuery.ColumnQuery(name = "total_inactive", sqlFunction = "COUNT", whereClause = "%s = 'INACTIVE'")
    String status = "ACTIVE";

    @Transient T actor;

    @RequestParamFilter(operator = RequestParamFilter.Operator.OBJECT_LIKE)
    @Column(columnDefinition = "TEXT") @Convert(converter = DeviceConverter.class)
    Device device;

    @RequestParamFilter(operator = RequestParamFilter.Operator.OBJECT_LIKE)
    @Column(columnDefinition = "TEXT") @Convert(converter = LocationConverter.class)
    Location location;

    @Column(columnDefinition = "TEXT") @Convert(converter = ObjectConverterImpl.class)
    @RequestParamFilter(operator = RequestParamFilter.Operator.OBJECT_LIKE, columnObjectFieldsIsSnakeCase = false)
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
