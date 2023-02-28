package io.github.barmoury.api.persistence;

import lombok.Getter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RequestParamFilter {

    // field for resolve sub entity with _id and .id and .* for inner join
    String column() default "";
    String[] aliases() default {};
    boolean alwaysQuery() default false;
    boolean acceptSnakeCase() default true;
    Operator operator() default Operator.EQ;
    boolean columnIsSnakeCase() default true;
    boolean columnObjectFieldsIsSnakeCase() default true;

    enum Operator {
        EQ,
        GT,
        LT,
        NE,
        IN,
        LIKE,
        GT_EQ,
        LT_EQ,
        BETWEEN,
        NOT_LIKE,
        CONTAINS,
        OBJECT_EQ,
        OBJECT_NE,
        ENDS_WITH,
        STARTS_WITH,
        OBJECT_LIKE,
        NOT_CONTAINS,
        OBJECT_STR_EQ,
        OBJECT_STR_NE,
        OBJECT_NOT_LIKE,
        OBJECT_CONTAINS,
        OBJECT_ENDS_WITH,
        OBJECT_STARTS_WITH,
        OBJECT_NOT_CONTAINS,
        OBJECT_STR_ENDS_WITH,
        OBJECT_STR_STARTS_WITH
    }

}
