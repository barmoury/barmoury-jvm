package io.github.barmoury.api.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Constraint(validatedBy = ListValuesValidateQuery.Validator.class)
public @interface ListValuesValidateQuery {

    String message();
    String table() default "";
    Class<?>[] groups() default {};
    String[] orClauses() default {};
    String[] andClauses() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ListValuesValidateQuery, Object> {

        ValidateQuery.Validator validator;

        @Autowired
        EntityManager entityManager;

        @Override
        public void initialize(ListValuesValidateQuery constraintAnnotation) {
            ConstraintValidator.super.initialize(constraintAnnotation);
            this.validator = new ValidateQuery.Validator();
            this.validator.entityManager = this.entityManager;
            this.validator.table = constraintAnnotation.table();
            this.validator.groups = constraintAnnotation.groups();
            this.validator.orClauses = constraintAnnotation.orClauses();
            this.validator.andClauses = constraintAnnotation.andClauses();
        }

        @Override
        public boolean isValid(Object o, ConstraintValidatorContext constraintValidatorContext) {
            if (o == null) {
                ((ConstraintValidatorContextImpl) constraintValidatorContext)
                        .addMessageParameter("value", "none");
                return false;
            }
            List<?> values = (List<?>) o;
            for (Object value : values) {
                if (!validator.isValid(value, constraintValidatorContext)) {
                    return false;
                }
            }
            return true;
        }
    }

}
