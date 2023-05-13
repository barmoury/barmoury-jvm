package io.github.barmoury.logger;

import io.github.barmoury.api.model.Model;
import io.github.barmoury.eloquent.RequestParamFilter;
import io.github.barmoury.eloquent.StatQuery;
import jakarta.persistence.MappedSuperclass;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@MappedSuperclass
@EqualsAndHashCode(callSuper = true)
@StatQuery(fetchPrevious = true, fetchHourly = true, fetchMonthly = true,
        fetchYearly = true, fetchWeekDays = true, fetchMonthDays = true, enableClientQuery = true)
public class Log extends Model {

    @RequestParamFilter(operator = RequestParamFilter.Operator.STARTS_WITH)
    @RequestParamFilter(operator = RequestParamFilter.Operator.ENDS_WITH)
    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE)
    @RequestParamFilter(operator = RequestParamFilter.Operator.EQ)
    @Builder.Default
    Level level = Level.INFO;

    @StatQuery.OccurrenceQuery(type = StatQuery.OccurrenceQuery.Type.PERCENTAGE)
    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE)
    @RequestParamFilter(operator = RequestParamFilter.Operator.EQ)
    @StatQuery.OccurrenceQuery
    String group;

    @StatQuery.OccurrenceQuery(type = StatQuery.OccurrenceQuery.Type.PERCENTAGE)
    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE)
    @RequestParamFilter(operator = RequestParamFilter.Operator.EQ)
    @StatQuery.OccurrenceQuery
    String source;

    @RequestParamFilter(operator = RequestParamFilter.Operator.STARTS_WITH)
    @RequestParamFilter(operator = RequestParamFilter.Operator.ENDS_WITH)
    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE)
    @RequestParamFilter(operator = RequestParamFilter.Operator.EQ)
    String content;

    @RequestParamFilter(operator = RequestParamFilter.Operator.STARTS_WITH)
    @RequestParamFilter(operator = RequestParamFilter.Operator.ENDS_WITH)
    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE)
    @RequestParamFilter(operator = RequestParamFilter.Operator.EQ)
    String traceId;

    @RequestParamFilter(operator = RequestParamFilter.Operator.STARTS_WITH)
    @RequestParamFilter(operator = RequestParamFilter.Operator.ENDS_WITH)
    @RequestParamFilter(operator = RequestParamFilter.Operator.LIKE)
    @RequestParamFilter(operator = RequestParamFilter.Operator.EQ)
    String spanId;


    public enum Level {
        INFO,
        WARN,
        ERROR,
        TRACE,
        FATAL
    }


}
