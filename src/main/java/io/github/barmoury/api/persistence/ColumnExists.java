package io.github.barmoury.api.persistence;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
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

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Constraint(validatedBy = ColumnExists.ColumnUniqueValidator.class)
public @interface ColumnExists {

    String message();
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    String table() default "";
    String where() default "";
    String column();

    class ColumnUniqueValidator implements ConstraintValidator<ColumnExists, Object> {

        String table;
        String where;
        String column;
        Class<?>[] groups;

        @Autowired
        EntityManager entityManager;

        @Override
        public void initialize(ColumnExists constraintAnnotation) {
            ConstraintValidator.super.initialize(constraintAnnotation);
            this.table = constraintAnnotation.table();
            this.where = constraintAnnotation.where();
            this.column = constraintAnnotation.column();
            this.groups = constraintAnnotation.groups();
        }

        @Override
        public boolean isValid(Object o, ConstraintValidatorContext constraintValidatorContext) {
            if (constraintValidatorContext instanceof HibernateConstraintValidatorContext && o == null) {
                Long rowId = constraintValidatorContext
                        .unwrap(HibernateConstraintValidatorContext.class)
                        .getConstraintValidatorPayload(Long.class);
                if (rowId != null) return true;
            }
            String queryString = String.format("SELECT count(*) FROM %s WHERE %s = :value", table, column);
            queryString = String.format("%s %s ", queryString, this.where);
            Query countQuery = entityManager.createNativeQuery(queryString);
            countQuery.setParameter("value", o);
            int countValue = ((Number) countQuery.getSingleResult()).intValue();
            if (countValue == 0) {
                if (constraintValidatorContext instanceof ConstraintValidatorContextImpl) {
                    ((ConstraintValidatorContextImpl) constraintValidatorContext)
                            .addMessageParameter("value", o);
                }
            }
            return false;
        }
    }

}
