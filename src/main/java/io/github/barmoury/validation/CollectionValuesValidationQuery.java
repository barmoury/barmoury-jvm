package io.github.barmoury.validation;

import jakarta.persistence.EntityManager;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Constraint(validatedBy = CollectionValuesValidationQuery.Validator.class)
public @interface CollectionValuesValidationQuery {

    String message();
    String table() default "";
    Class<?>[] groups() default {};
    String[] orClauses() default {};
    String[] andClauses() default {};
    boolean checkIsZero() default false;
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<CollectionValuesValidationQuery, Object> {

        ValidationQuery.Validator validator;

        @Autowired
        EntityManager entityManager;

        @Override
        public void initialize(CollectionValuesValidationQuery constraintAnnotation) {
            ConstraintValidator.super.initialize(constraintAnnotation);
            this.validator = new ValidationQuery.Validator();
            this.validator.entityManager = this.entityManager;
            this.validator.table = constraintAnnotation.table();
            this.validator.groups = constraintAnnotation.groups();
            this.validator.orClauses = constraintAnnotation.orClauses();
            this.validator.andClauses = constraintAnnotation.andClauses();
            this.validator.checkIsZero = constraintAnnotation.checkIsZero();
        }

        @Override
        public boolean isValid(Object o, ConstraintValidatorContext constraintValidatorContext) {
            if (o == null) {
                ((ConstraintValidatorContextImpl) constraintValidatorContext)
                        .addMessageParameter("value", "(null)");
                return false;
            }
            Collection<?> values = (Collection<?>) o;
            for (Object value : values) {
                if (!validator.isValid(value, constraintValidatorContext)) {
                    return false;
                }
            }
            return true;
        }
    }

}
