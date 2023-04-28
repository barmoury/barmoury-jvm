package io.github.barmoury.validation;

import io.github.barmoury.eloquent.RequestParamFilter;
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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Constraint(validatedBy = ValidateFields.Validator.class)
@Repeatable(ValidateFields.Container.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidateFields {

    String message();
    Class<?>[] groups() default {};
    String[] expressions() default {};
    Class<? extends Payload>[] payload() default {};

    @Target({ ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @interface Container {
        ValidateFields[] value();
    }

    class Validator implements ConstraintValidator<ValidateFields, Object> {

        Class<?>[] groups;
        String[] expressions;

        @Override
        public void initialize(ValidateFields constraintAnnotation) {
            ConstraintValidator.super.initialize(constraintAnnotation);
            this.groups = constraintAnnotation.groups();
            this.expressions = constraintAnnotation.expressions();
        }

        @Override
        public boolean isValid(Object o, ConstraintValidatorContext constraintValidatorContext) {
            if (o == null) return false;
            boolean evaluationValue = true;
            Map<String, Object> fieldValueCache = new HashMap<>();
            for (String expression : this.expressions) {
                expression = expression.trim();
                if (!expression.contains(":")) continue;
                String field = expression.substring(expression.indexOf(":")+1, expression.indexOf(" "));
                Object value;
                if (!fieldValueCache.containsKey(field)) {
                    value = FieldUtil.getFieldValue(o, field);
                    fieldValueCache.computeIfAbsent(field, v -> value);
                } else {
                    value = fieldValueCache.get(field);
                }
                expression = expression.substring(expression.indexOf(field)+field.length()).trim();
                String operator = expression.substring(0, expression.indexOf(" "));
                expression = expression.substring(expression.indexOf(operator)+operator.length()).trim();
                String nextToken = expression.substring(0, 1);
                String valueType = switch(nextToken) {
                    case "'" -> "char";
                    case "\"" -> "string";
                    default -> "object";
                };
                String rightOperand = switch(valueType) {
                    case "char" , "string" -> expression
                            .substring(expression.indexOf(nextToken)+1, expression.lastIndexOf(nextToken));
                    default -> expression;
                };
                evaluationValue = evaluationValue && switch(operator) {
                    case "LLIKE", "STARTS_WITH" -> String.valueOf(value).startsWith(rightOperand);
                    case "RLIKE", "ENDS_WITH" -> String.valueOf(value).endsWith(rightOperand);
                    case "LIKE" -> String.valueOf(value).contains(rightOperand);
                    case "!=" -> !rightOperand.equals(String.valueOf(value));
                    default -> rightOperand.equals(String.valueOf(value));
                };
            }
            return !evaluationValue;
        }
    }

}
