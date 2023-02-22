package io.github.barmoury.api.persistence;

import lombok.Getter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RequestParamFilter {

    String column() default "";
    String[] aliases() default {};
    boolean alwaysQuery() default false;
    boolean acceptSnakeCase() default true;
    Operator operator() default Operator.EQ;
    boolean columnIsSnakeCase() default true;

    enum Operator {
        EQ,
        GT,
        LT,
        NE,
        GT_EQ,
        LT_EQ,
        LIKE,
        CONTAINS,
        BETWEEN,
        ENDS_WITH,
        STARTS_WITH,
        OBJECT_STR_EQ,
        OBJECT_STR_EN,
        OBJECT_STR_LIKE,
        OBJECT_STR_CONTAINS,
        OBJECT_STR_ENDS_WITH,
        OBJECT_STR_STARTS_WITH;

    }

}
