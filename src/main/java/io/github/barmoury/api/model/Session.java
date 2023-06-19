package io.github.barmoury.api.model;

import io.github.barmoury.converter.ObjectConverterImpl;
import io.github.barmoury.copier.CopyProperty;
import io.github.barmoury.eloquent.RequestParamFilter;
import io.github.barmoury.eloquent.StatQuery;
import io.github.barmoury.trace.Device;
import io.github.barmoury.trace.Isp;
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
    @RequestParamFilter(operator = RequestParamFilter.Operator.RANGE)
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

    @RequestParamFilter(operator = RequestParamFilter.Operator.RANGE)
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

    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE)
    @RequestParamFilter(operator = RequestParamFilter.Operator.OBJECT_LIKE)
    @Column(columnDefinition = "TEXT") @Convert(converter = Isp.Converter.class)
    Isp isp;

    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE)
    @RequestParamFilter(operator = RequestParamFilter.Operator.OBJECT_LIKE)
    @Column(columnDefinition = "TEXT") @Convert(converter = Device.Converter.class)
    Device device;

    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE)
    @RequestParamFilter(operator = RequestParamFilter.Operator.OBJECT_LIKE)
    @Column(columnDefinition = "TEXT") @Convert(converter = Location.Converter.class)
    Location location;

    @Column(columnDefinition = "TEXT") @Convert(converter = ObjectConverterImpl.class)
    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE, columnObjectFieldsIsSnakeCase = false)
    @RequestParamFilter(operator = RequestParamFilter.Operator.OBJECT_LIKE, columnObjectFieldsIsSnakeCase = false)
    Object extraData;

    @CopyProperty(ignore = true) @Temporal(TemporalType.TIMESTAMP)
    @RequestParamFilter(operator = RequestParamFilter.Operator.RANGE)
    Date deletedAt = new Date();

}
