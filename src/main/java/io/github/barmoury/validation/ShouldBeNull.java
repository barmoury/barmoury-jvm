package io.github.barmoury.validation;

import io.github.barmoury.util.FieldUtil;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import lombok.SneakyThrows;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ShouldBeNull.Validator.class)
@Target({ ElementType.FIELD, ElementType.ANNOTATION_TYPE })
public @interface ShouldBeNull {

    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    String message() default "The field {field} must be null";

    class Validator implements ConstraintValidator<ShouldBeNull, Object> {

        String[] values;
        Class<?>[] groups;

        @Override
        public void initialize(ShouldBeNull constraintAnnotation) {
            ConstraintValidator.super.initialize(constraintAnnotation);
            this.groups = constraintAnnotation.groups();
        }

        @Override
        @SneakyThrows
        public boolean isValid(Object o, ConstraintValidatorContext constraintValidatorContext) {
            if (o == null) return true;
            String fieldName = FieldUtil.getVariableName(o);
            ((ConstraintValidatorContextImpl) constraintValidatorContext)
                    .addMessageParameter("field", fieldName);
            return false;
        }
    }

}
