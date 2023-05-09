package io.github.barmoury.eloquent;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(RequestParamFilter.Container.class)
public @interface RequestParamFilter {

    String column() default "";
    String[] aliases() default {};
    boolean alwaysQuery() default false;
    boolean booleanToInt() default false;
    boolean acceptSnakeCase() default true;
    Operator operator() default Operator.EQ;
    boolean columnIsSnakeCase() default true;
    String multiFilterSeparator() default "_";
    boolean columnObjectFieldsIsSnakeCase() default true;

    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD)
    @interface Container {
        RequestParamFilter[] value();
    }

    enum Operator {
        EQ,
        GT,
        LT,
        NE,
        IN,
        LIKE,
        ILIKE,
        GT_EQ,
        LT_EQ,
        RANGE,
        ENTITY,
        NOT_IN,
        BETWEEN,
        NOT_LIKE,
        CONTAINS,
        NOT_ILIKE,
        OBJECT_EQ,
        OBJECT_NE,
        ENDS_WITH,
        NOT_BETWEEN,
        STARTS_WITH,
        OBJECT_LIKE,
        NOT_CONTAINS,
        OBJECT_STR_EQ,
        OBJECT_STR_NE,
        SENSITIVE_LIKE,
        OBJECT_NOT_LIKE,
        OBJECT_CONTAINS,
        OBJECT_ENDS_WITH,
        SENSITIVE_NOT_LIKE,
        OBJECT_STARTS_WITH,
        OBJECT_NOT_CONTAINS,
        OBJECT_STR_ENDS_WITH,
        SENSITIVE_OBJECT_LIKE,
        OBJECT_STR_STARTS_WITH
    }

}
