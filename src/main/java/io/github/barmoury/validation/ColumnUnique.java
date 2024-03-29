package io.github.barmoury.validation;

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
@Constraint(validatedBy = ColumnUnique.Validator.class)
public @interface ColumnUnique {

    String message();
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    String table() default "";
    String whereClause() default "";
    String column();
    boolean nullable() default false;

    class Validator implements ConstraintValidator<ColumnUnique, Object> {

        boolean nullable;
        String table;
        String column;
        String whereClause;
        Class<?>[] groups;

        @Autowired
        EntityManager entityManager;

        @Override
        public void initialize(ColumnUnique constraintAnnotation) {
            ConstraintValidator.super.initialize(constraintAnnotation);
            this.table = constraintAnnotation.table();
            this.column = constraintAnnotation.column();
            this.groups = constraintAnnotation.groups();
            this.nullable = constraintAnnotation.nullable();
            this.whereClause = constraintAnnotation.whereClause();
        }

        @Override
        public boolean isValid(Object o, ConstraintValidatorContext constraintValidatorContext) {
            // TODO, auto fetch column
            if (this.nullable && o == null) return true;
            Long rowId = null;
            String queryString = String.format("SELECT count(*) FROM %s WHERE %s = :self %s", table, column,
                    (whereClause.isBlank() ? "" : " AND " + whereClause));
            if (constraintValidatorContext instanceof HibernateConstraintValidatorContext) {
                rowId = constraintValidatorContext
                        .unwrap(HibernateConstraintValidatorContext.class)
                        .getConstraintValidatorPayload(Long.class);
                if (rowId != null) { queryString += " AND id != :id "; }
            }
            Query countQuery = entityManager.createNativeQuery(queryString);
            countQuery.setParameter("self", o);
            if (rowId != null) { countQuery.setParameter("id", rowId); }
            int countValue = ((Number) countQuery.getSingleResult()).intValue();
            if (countValue > 0) {
                if (constraintValidatorContext instanceof ConstraintValidatorContextImpl) {
                    ((ConstraintValidatorContextImpl) constraintValidatorContext)
                            .addMessageParameter("value", String.valueOf(o));
                }
                return false;
            }
            return true;
        }
    }

}
