package io.github.barmoury.validation;

import io.github.barmoury.api.model.Model;
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

import java.lang.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ValidationQuery.Container.class)
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Constraint(validatedBy = ValidationQuery.Validator.class)
public @interface ValidationQuery {

    String table();
    String message();
    Class<?>[] groups() default {};
    String[] orClauses() default {};
    String[] andClauses() default {};
    boolean checkIsZero() default false;
    Class<? extends Payload>[] payload() default {};

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
    @interface Container {
        ValidationQuery[] value();
    }

    class Validator implements ConstraintValidator<ValidationQuery, Object> {

        String table;
        Class<?>[] groups;
        String[] orClauses;
        String[] andClauses;
        boolean checkIsZero;

        @Autowired
        EntityManager entityManager;

        @Override
        public void initialize(ValidationQuery constraintAnnotation) {
            ConstraintValidator.super.initialize(constraintAnnotation);
            this.table = constraintAnnotation.table();
            this.groups = constraintAnnotation.groups();
            this.orClauses = constraintAnnotation.orClauses();
            this.andClauses = constraintAnnotation.andClauses();
            this.checkIsZero = constraintAnnotation.checkIsZero();
        }

        // TODO: accept paremeters instead of just assuming id for queries see -ts
        @Override
        public boolean isValid(Object o, ConstraintValidatorContext constraintValidatorContext) {
            if (constraintValidatorContext instanceof HibernateConstraintValidatorContext && o == null) {
                Long rowId = constraintValidatorContext
                        .unwrap(HibernateConstraintValidatorContext.class)
                        .getConstraintValidatorPayload(Long.class);
                if (rowId != null) return true;
            }
            Map<String, Object> fieldValues = new HashMap<>();
            StringBuilder queryString = new StringBuilder("SELECT count(*) FROM ");
            queryString.append(table);
            if (this.orClauses.length > 0 || this.andClauses.length > 0) {
                queryString.append(" WHERE (");
            }
            for (int index = 0; index < this.orClauses.length; index++) {
                String clause = this.orClauses[index].trim();
                queryString.append(clause);
                while (clause.contains(":")) {
                    int endIndex = clause.indexOf("\\s+");
                    if (endIndex == -1) endIndex = clause.length();
                    String fieldName = clause.substring(clause.indexOf(":")+1, endIndex);
                    clause = clause.substring(clause.indexOf(":")+1).trim();
                    Object value = fieldName.equals(Constants.SELF) ? o : FieldUtil.getFieldValue(o, fieldName);
                    fieldValues.putIfAbsent(fieldName, value);
                }
                if (index < this.orClauses.length-1) {
                    queryString.append(" OR ");
                }
            }
            if (this.orClauses.length > 0 && this.andClauses.length > 0) queryString.append(") AND (");
            for (int index = 0; index < this.andClauses.length; index++) {
                String clause = this.andClauses[index].trim();
                queryString.append(clause);
                while (clause.contains(":")) {
                    int endIndex = clause.indexOf("\\s+");
                    if (endIndex == -1) endIndex = clause.length();
                    String fieldName = clause.substring(clause.indexOf(":")+1, endIndex);
                    clause = clause.substring(clause.indexOf(":")+1).trim();
                    Object value = fieldName.equals(Constants.SELF) ? o : FieldUtil.getFieldValue(o, fieldName);
                    fieldValues.putIfAbsent(fieldName, value);
                }
                if (index < this.andClauses.length-1) {
                    queryString.append(" AND ");
                }
            }
            queryString.append(" )");
            String queryStringValue = queryString.toString();
            Query countQuery = entityManager.createNativeQuery(queryStringValue);
            for (Map.Entry<String, Object> fieldValue : fieldValues.entrySet()) {
                if (queryStringValue.contains(":" + fieldValue.getKey())) {
                    countQuery.setParameter(fieldValue.getKey(), fieldValue.getValue());
                }
            }
            int countValue = ((Number) countQuery.getSingleResult()).intValue();
            boolean passed = this.checkIsZero == (countValue == 0);
            if (!passed && (constraintValidatorContext instanceof ConstraintValidatorContextImpl)) {
                    ((ConstraintValidatorContextImpl) constraintValidatorContext)
                            .addMessageParameter(Constants.VALUE, String.valueOf(o));
            }
            return passed;
        }
    }

}
