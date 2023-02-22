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
@Constraint(validatedBy = ListValuesExists.ListValuesExistsValidator.class)
public @interface ListValuesExists {

    String message();
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    String table() default "";
    String where() default "";
    String column();

    class ListValuesExistsValidator implements ConstraintValidator<ListValuesExists, Object> {

        String table;
        String where;
        String column;
        Class<?>[] groups;

        @Autowired
        EntityManager entityManager;

        @Override
        public void initialize(ListValuesExists constraintAnnotation) {
            ConstraintValidator.super.initialize(constraintAnnotation);
            this.table = constraintAnnotation.table();
            this.where = constraintAnnotation.where();
            this.column = constraintAnnotation.column();
            this.groups = constraintAnnotation.groups();
        }

        @Override
        public boolean isValid(Object o, ConstraintValidatorContext constraintValidatorContext) {
            List<?> values = (List<?>) o;
            for (Object value : values) {
                String queryString = String.format("SELECT count(*) FROM %s WHERE %s = :value", table, column);
                queryString = String.format("%s %s ", queryString, this.where);
                Query countQuery = entityManager.createNativeQuery(queryString);
                countQuery.setParameter("value", value);
                if (((Number) countQuery.getSingleResult()).intValue() == 0) {
                    ((ConstraintValidatorContextImpl) constraintValidatorContext)
                            .addMessageParameter("value", value);
                    return false;
                }
            }
            return true;
        }
    }

}
