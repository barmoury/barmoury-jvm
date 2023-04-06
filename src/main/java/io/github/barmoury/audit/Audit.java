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

    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE)
    @RequestParamFilter(operator = RequestParamFilter.Operator.EQ)
    String type;

    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE)
    @RequestParamFilter(operator = RequestParamFilter.Operator.EQ)
    String group;

    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE)
    @RequestParamFilter(operator = RequestParamFilter.Operator.EQ)
    String status;

    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE)
    @RequestParamFilter(operator = RequestParamFilter.Operator.EQ)
    String source;

    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE)
    @RequestParamFilter(operator = RequestParamFilter.Operator.EQ)
    String action;

    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE)
    @RequestParamFilter(operator = RequestParamFilter.Operator.EQ)
    String actorId;

    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE)
    @RequestParamFilter(operator = RequestParamFilter.Operator.EQ)
    String actorType;

    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE)
    @RequestParamFilter(operator = RequestParamFilter.Operator.EQ)
    String ipAddress;

    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE)
    @RequestParamFilter(operator = RequestParamFilter.Operator.EQ)
    String environment;

    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE)
    @Column(columnDefinition = "VARCHAR")
    UUID auditId = UUID.randomUUID();

    @Transient T actor;

    @Column(columnDefinition = "TEXT") @Convert(converter = Device.DeviceConverter.class)
    @RequestParamFilter(operator = RequestParamFilter.Operator.OBJECT_LIKE)
    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE)
    Device device;

    @Column(columnDefinition = "TEXT") @Convert(converter = Location.LocationConverter.class)
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
