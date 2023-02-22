package io.github.barmoury.api.persistence;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Constraint(validatedBy = ColumnUnique.ColumnUniqueValidator.class)
public @interface ColumnUnique {

    String message();
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    String table() default "";
    String where() default "";
    String column();
    boolean nullable() default false;

    class ColumnUniqueValidator implements ConstraintValidator<ColumnUnique, Object> {

        boolean nullable;
        String table;
        String where;
        String column;
        Class<?>[] groups;

        @Autowired
        EntityManager entityManager;

        @Override
        public void initialize(ColumnUnique constraintAnnotation) {
            ConstraintValidator.super.initialize(constraintAnnotation);
            this.table = constraintAnnotation.table();
            this.where = constraintAnnotation.where();
            this.column = constraintAnnotation.column();
            this.groups = constraintAnnotation.groups();
            this.nullable = constraintAnnotation.nullable();
        }

        @Override
        public boolean isValid(Object o, ConstraintValidatorContext constraintValidatorContext) {
            // TODO, auto fetch column
            if (this.nullable && o == null) return true;
            Long rowId = null;
            String queryString = String.format("SELECT count(*) FROM %s WHERE %s = :value", table, column);
            if (constraintValidatorContext instanceof HibernateConstraintValidatorContext) {
                rowId = constraintValidatorContext
                        .unwrap(HibernateConstraintValidatorContext.class)
                        .getConstraintValidatorPayload(Long.class);
                if (rowId != null) { queryString += " AND id != :id "; }
            }
            queryString = String.format("%s %s ", queryString, this.where);
            Query countQuery = entityManager.createNativeQuery(queryString);
            countQuery.setParameter("value", o);
            if (rowId != null) { countQuery.setParameter("id", rowId); }
            int countValue = ((Number) countQuery.getSingleResult()).intValue();
            if (countValue > 0) {
                if (constraintValidatorContext instanceof ConstraintValidatorContextImpl) {
                    ((ConstraintValidatorContextImpl) constraintValidatorContext)
                            .addMessageParameter("value", o);
                }
                return false;
            }
            return true;
        }
    }

}
