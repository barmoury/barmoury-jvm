package io.github.barmoury.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.regex.Pattern;

@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValueShouldMatchAny.Validator.class)
@Target({ ElementType.FIELD, ElementType.ANNOTATION_TYPE })
public @interface ValueShouldMatchAny {

    String[] value() default {};
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    String message() default "The value {value} does not match any of these {values}";

    class Validator implements ConstraintValidator<ValueShouldMatchAny, Object> {

        String[] values;
        Class<?>[] groups;

        @Override
        public void initialize(ValueShouldMatchAny constraintAnnotation) {
            ConstraintValidator.super.initialize(constraintAnnotation);
            this.values = constraintAnnotation.value();
            this.groups = constraintAnnotation.groups();
        }

        @Override
        public boolean isValid(Object o, ConstraintValidatorContext constraintValidatorContext) {
            if (o == null) {
                o = "null";
            } else {
                o = String.valueOf(o);
                for (String value : this.values) {
                    if (value.equals(o) || Pattern.compile(value).matcher((String) o).find()) {
                        return true;
                    }
                }
            }
            ((ConstraintValidatorContextImpl) constraintValidatorContext).addMessageParameter("value", o);
            ((ConstraintValidatorContextImpl) constraintValidatorContext)
                    .addMessageParameter("values", Arrays.toString(this.values));
            return false;
        }
    }

}
