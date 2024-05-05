package io.github.barmoury.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import lombok.SneakyThrows;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;

import java.lang.annotation.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Pattern;

@SuppressWarnings("unchecked")
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidateEnum.Validator.class)
@Target({ ElementType.FIELD, ElementType.ANNOTATION_TYPE })
public @interface ValidateEnum {

    Class<?> value();
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    String message() default "The value {value} does not match any of these {values}";

    class Validator implements ConstraintValidator<ValidateEnum, Object> {

        Class<?>[] groups;
        ValueShouldMatchAny.Validator validator;

        public static String EnumValueAsString(Object entry) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            return String.valueOf(entry.getClass().getMethod("name").invoke(entry));
        }

        public static List<String> EnumValuesAsList(Class enumerator) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            List<String> values = new ArrayList<>();
            if (enumerator != null) for (Object entry : EnumSet.allOf(enumerator)) {
                values.add(EnumValueAsString(entry));
            }
            return values;
        }

        @Override
        @SneakyThrows
        public void initialize(ValidateEnum constraintAnnotation) {
            ConstraintValidator.super.initialize(constraintAnnotation);
            this.groups = constraintAnnotation.groups();
            this.validator = new ValueShouldMatchAny.Validator();
            this.validator.groups = constraintAnnotation.groups();
            List<String> values = EnumValuesAsList(constraintAnnotation.value());
            this.validator.values = values.toArray(new String[0]);
        }

        @Override
        public boolean isValid(Object o, ConstraintValidatorContext constraintValidatorContext) {
            return this.validator.isValid(o, constraintValidatorContext);
        }
    }

}
