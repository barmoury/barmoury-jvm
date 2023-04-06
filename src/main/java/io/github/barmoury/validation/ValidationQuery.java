package io.github.barmoury.validation;

import io.github.barmoury.util.Constants;
import io.github.barmoury.util.FieldUtil;
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
@Constraint(validatedBy = ValidationQuery.Validator.class)
public @interface ValidationQuery {

    String table();
    String message();
    Class<?>[] groups() default {};
    String[] orClauses() default {};
    String[] andClauses() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidationQuery, Object> {

        String table;
        Class<?>[] groups;
        String[] orClauses;
        String[] andClauses;

        @Autowired
        EntityManager entityManager;

        @Override
        public void initialize(ValidationQuery constraintAnnotation) {
            ConstraintValidator.super.initialize(constraintAnnotation);
            this.table = constraintAnnotation.table();
            this.groups = constraintAnnotation.groups();
            this.orClauses = constraintAnnotation.orClauses();
            this.andClauses = constraintAnnotation.andClauses();
        }

        @Override
        public boolean isValid(Object o, ConstraintValidatorContext constraintValidatorContext) {
            if (constraintValidatorContext instanceof HibernateConstraintValidatorContext && o == null) {
                Long rowId = constraintValidatorContext
                        .unwrap(HibernateConstraintValidatorContext.class)
                        .getConstraintValidatorPayload(Long.class);
                if (rowId != null) return true;
            }
            StringBuilder queryString = new StringBuilder("SELECT count(*) FROM ");
            queryString.append(table);
            if (this.orClauses.length > 0 || this.andClauses.length > 0) {
                queryString.append(" WHERE (");
            }
            for (int index = 0; index < this.orClauses.length; index++) {
                String orClause = this.orClauses[index];
                queryString.append(String.format(orClause, o));
                if (index < this.orClauses.length-1) {
                    queryString.append(" OR ");
                }
            }
            if (this.orClauses.length > 0 && this.andClauses.length > 0) queryString.append(") AND (");
            for (int index = 0; index < this.andClauses.length; index++) {
                String orClause = this.andClauses[index];
                queryString.append(String.format(orClause, o));
                if (index < this.andClauses.length-1) {
                    queryString.append(" AND ");
                }
            }
            queryString.append(" )");
            Query countQuery = entityManager.createNativeQuery(queryString.toString());
            if (queryString.toString().contains(Constants.VALUE)) countQuery.setParameter(Constants.VALUE, o);
            if (o != null && queryString.toString().contains(Constants.UPDATE_ENTITY_ID)) {
                countQuery.setParameter(Constants.UPDATE_ENTITY_ID,
                        FieldUtil.getFieldValue(o.getClass(), "updateEntityId"));
            }
            int countValue = ((Number) countQuery.getSingleResult()).intValue();
            if (countValue == 0 && (constraintValidatorContext instanceof ConstraintValidatorContextImpl)) {
                    ((ConstraintValidatorContextImpl) constraintValidatorContext)
                            .addMessageParameter(Constants.VALUE, String.valueOf(o));
            }
            return countValue > 0;
        }
    }

}
