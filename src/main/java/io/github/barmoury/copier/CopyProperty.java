package io.github.barmoury.copier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CopyProperty {
    boolean ignore() default false;
    boolean useNonZeroValue()  default false;
    CopyValuePriority priority() default CopyValuePriority.SOURCE;

    enum CopyValuePriority {
        TARGET,
        SOURCE,
    }
}
