package io.github.barmoury.eloquent;

import lombok.With;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface StatQuery {

    boolean fetchHourly() default false;
    boolean fetchYearly() default false;
    boolean fetchMonthly() default false;
    boolean fetchWeekDays() default false;
    boolean fetchPrevious() default false;
    boolean fetchMonthDays() default false;
    boolean enableClientQuery() default false;
    boolean columnsAreSnakeCase() default true;
    String intervalColumn() default "updated_at";

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD)
    @Repeatable(ColumnQuery.QueryContainer.class)
    @interface ColumnQuery {

        String whereClause() default "";
        String sqlFunction() default "";
        String name() default "%s_count";

        @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD)
        @interface QueryContainer {
            ColumnQuery[] value();
        }

    }

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD)
    @Repeatable(OccurrenceQuery.QueryContainer.class)
    @interface OccurrenceQuery {

        long fetchCount() default 10;
        Type type() default Type.COUNT;
        String whereClause() default "";
        String name() default "top_%s_%s";

        enum Type {
            COUNT,
            PERCENTAGE
        }

        @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD)
        @interface QueryContainer {
            OccurrenceQuery[] value();
        }

    }

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD)
    @Repeatable(AverageQuery.QueryContainer.class)
    @interface AverageQuery {

        String name() default "average_%s";
        String whereClause() default "";

        @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD)
        @interface QueryContainer {
            AverageQuery[] value();
        }

    }

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD)
    @Repeatable(MedianQuery.QueryContainer.class)
    @interface MedianQuery {

        String name() default "median_%s";
        String whereClause() default "";

        @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD)
        @interface QueryContainer {
            MedianQuery[] value();
        }

    }

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD)
    @Repeatable(PercentageChangeQuery.QueryContainer.class)
    @interface PercentageChangeQuery {

        String whereClause() default "";
        String sqlFunction() default "COUNT";
        String name() default "%s_percentage_change";

        @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD)
        @interface QueryContainer {
            PercentageChangeQuery[] value();
        }

    }

}
