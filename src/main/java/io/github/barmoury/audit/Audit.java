package io.github.barmoury.audit;

import io.github.barmoury.api.model.Model;
import io.github.barmoury.converter.ObjectConverterImpl;
import io.github.barmoury.eloquent.RequestParamFilter;
import io.github.barmoury.eloquent.StatQuery;
import io.github.barmoury.trace.Device;
import io.github.barmoury.trace.Location;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@Builder
@MappedSuperclass
@EqualsAndHashCode(callSuper = true)
@StatQuery(fetchPrevious = true, fetchHourly = true, fetchMonthly = true,
        fetchYearly = true, fetchWeekDays = true, fetchMonthDays = true, enableClientQuery = true)
public class Audit<T> extends Model {

    @StatQuery.OccurrenceQuery(type = StatQuery.OccurrenceQuery.Type.PERCENTAGE)
    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE)
    @RequestParamFilter(operator = RequestParamFilter.Operator.EQ)
    @StatQuery.OccurrenceQuery
    String type;

    @StatQuery.OccurrenceQuery(type = StatQuery.OccurrenceQuery.Type.PERCENTAGE)
    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE)
    @RequestParamFilter(operator = RequestParamFilter.Operator.EQ)
    @StatQuery.OccurrenceQuery
    String group;

    @StatQuery.OccurrenceQuery(type = StatQuery.OccurrenceQuery.Type.PERCENTAGE)
    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE)
    @RequestParamFilter(operator = RequestParamFilter.Operator.EQ)
    @StatQuery.OccurrenceQuery
    String status;

    @StatQuery.OccurrenceQuery(type = StatQuery.OccurrenceQuery.Type.PERCENTAGE)
    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE)
    @RequestParamFilter(operator = RequestParamFilter.Operator.EQ)
    @StatQuery.OccurrenceQuery
    String source;

    @StatQuery.OccurrenceQuery(type = StatQuery.OccurrenceQuery.Type.PERCENTAGE)
    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE)
    @RequestParamFilter(operator = RequestParamFilter.Operator.EQ)
    @StatQuery.OccurrenceQuery
    String action;

    @StatQuery.OccurrenceQuery(type = StatQuery.OccurrenceQuery.Type.PERCENTAGE)
    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE)
    @RequestParamFilter(operator = RequestParamFilter.Operator.EQ)
    @StatQuery.OccurrenceQuery
    String actorId;

    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE)
    @RequestParamFilter(operator = RequestParamFilter.Operator.EQ)
    String actorType;

    @StatQuery.OccurrenceQuery(type = StatQuery.OccurrenceQuery.Type.PERCENTAGE)
    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE)
    @RequestParamFilter(operator = RequestParamFilter.Operator.EQ)
    @StatQuery.OccurrenceQuery
    String ipAddress;

    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE)
    @RequestParamFilter(operator = RequestParamFilter.Operator.EQ)
    String environment;

    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE)
    @Column(columnDefinition = "VARCHAR")
    @Builder.Default
    UUID auditId = UUID.randomUUID();

    @Transient T actor;

    @Column(columnDefinition = "TEXT") @Convert(converter = Device.Converter.class)
    @RequestParamFilter(operator = RequestParamFilter.Operator.OBJECT_LIKE)
    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE)
    Device device;

    @Column(columnDefinition = "TEXT") @Convert(converter = Location.Converter.class)
    @RequestParamFilter(operator = RequestParamFilter.Operator.OBJECT_LIKE)
    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE)
    Location location;

    @RequestParamFilter(operator = RequestParamFilter.Operator.OBJECT_LIKE, columnObjectFieldsIsSnakeCase = false)
    @Column(columnDefinition = "TEXT") @Convert(converter = ObjectConverterImpl.class)
    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE)
    Object auditable;

    @RequestParamFilter(operator = RequestParamFilter.Operator.OBJECT_LIKE, columnObjectFieldsIsSnakeCase = false)
    @Column(columnDefinition = "TEXT") @Convert(converter = ObjectConverterImpl.class)
    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE)
    Object extraData;

}
