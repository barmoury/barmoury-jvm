package io.github.barmoury.validation;

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

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Constraint(validatedBy = ColumnExists.Validator.class)
public @interface ColumnExists {

    String message();
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    String table() default "";
    String whereClause() default "";
    String column();

    class Validator implements ConstraintValidator<ColumnExists, Object> {

        String table;
        String column;
        Class<?>[] groups;
        String whereClause;

        @Autowired
        EntityManager entityManager;

        @Override
        public void initialize(ColumnExists constraintAnnotation) {
            ConstraintValidator.super.initialize(constraintAnnotation);
            this.table = constraintAnnotation.table();
            this.column = constraintAnnotation.column();
            this.groups = constraintAnnotation.groups();
            this.whereClause = constraintAnnotation.whereClause();
        }

        @Override
        public boolean isValid(Object o, ConstraintValidatorContext constraintValidatorContext) {
            if (constraintValidatorContext instanceof HibernateConstraintValidatorContext && o == null) {
                Long rowId = constraintValidatorContext
                        .unwrap(HibernateConstraintValidatorContext.class)
                        .getConstraintValidatorPayload(Long.class);
                if (rowId != null) return true;
            }
            String queryString = String.format("SELECT count(*) FROM %s WHERE %s = :self %s", table, column,
                    (whereClause.isBlank() ? "" : " AND " + whereClause));
            Query countQuery = entityManager.createNativeQuery(queryString);
            countQuery.setParameter("self", o);
            int countValue = ((Number) countQuery.getSingleResult()).intValue();
            if (countValue == 0) {
                if (constraintValidatorContext instanceof ConstraintValidatorContextImpl) {
                    ((ConstraintValidatorContextImpl) constraintValidatorContext)
                            .addMessageParameter("value", String.valueOf(o));
                }
            }
            return countValue > 0;
        }
    }

}
