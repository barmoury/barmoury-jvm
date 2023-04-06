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
import java.util.Collection;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Constraint(validatedBy = CollectionValuesShouldMatchAny.ValueMatchAnyValidator.class)
public @interface CollectionValuesShouldMatchAny {

    String[] values();
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    String message() default "The value {value} in the collection does not match any of these {values}";

    class ValueMatchAnyValidator implements ConstraintValidator<CollectionValuesShouldMatchAny, Object> {

        ValueShouldMatchAny.Validator validator;

        @Override
        public void initialize(CollectionValuesShouldMatchAny constraintAnnotation) {
            ConstraintValidator.super.initialize(constraintAnnotation);
            this.validator = new ValueShouldMatchAny.Validator();
            this.validator.values = constraintAnnotation.values();
            this.validator.groups = constraintAnnotation.groups();
        }

        @Override
        public boolean isValid(Object o, ConstraintValidatorContext constraintValidatorContext) {
            if (o instanceof Collection) {
                Collection<?> oValues = (Collection<?>) o;
                for (Object oValue : oValues) {
                    if (this.validator.isValid(oValue, constraintValidatorContext)) {
                        return true;
                    }
                }
            }
            ((ConstraintValidatorContextImpl) constraintValidatorContext).addMessageParameter("value", "none");
            return false;
        }
    }

}
