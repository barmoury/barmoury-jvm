package io.github.barmoury.api.model;

import io.github.barmoury.converter.ObjectConverterImpl;
import io.github.barmoury.eloquent.RequestParamFilter;
import io.github.barmoury.eloquent.StatQuery;
import io.github.barmoury.trace.Device;
import io.github.barmoury.trace.Location;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@MappedSuperclass
@EqualsAndHashCode(callSuper = true)
@StatQuery(fetchPrevious = true, fetchHourly = true, fetchMonthly = true,
        fetchYearly = true, fetchWeekDays = true, fetchMonthDays = true,
        enableClientQuery = true)
public class Session<T> extends Model {

    @RequestParamFilter
    @StatQuery.PercentageChangeQuery
    @StatQuery.AverageQuery @StatQuery.MedianQuery
    @StatQuery.ColumnQuery(name = "%s_sum", sqlFunction = "SUM")
    @RequestParamFilter(operator = RequestParamFilter.Operator.BETWEEN)
    @StatQuery.PercentageChangeQuery(sqlFunction = "SUM", name = "%s_sum_percentage_change")
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
    @Column(columnDefinition = "TEXT") @Convert(converter = Device.DeviceConverter.class)
    Device device;

    @RequestParamFilter(operator = RequestParamFilter.Operator.OBJECT_LIKE)
    @Column(columnDefinition = "TEXT") @Convert(converter = Location.LocationConverter.class)
    Location location;

    @Column(columnDefinition = "TEXT") @Convert(converter = ObjectConverterImpl.class)
    @RequestParamFilter(operator = RequestParamFilter.Operator.OBJECT_LIKE, columnObjectFieldsIsSnakeCase = false)
    Object extraData;

}