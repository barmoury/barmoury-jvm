package io.github.barmoury.util;

import io.github.barmoury.copier.CopierException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;

public class FieldUtil {

    public static boolean objectsAreSameType(Class<?> objectTypeCheck, Class<?>... objectTypes) {
        for (Class<?> objectType : objectTypes) {
            if (objectType != objectTypeCheck) {
                return false;
            }
        }
        return true;
    }

    public static boolean objectsHasAnyType(Class<?> objectTypeCheck, Class<?>... objectTypes) {
        for (Class<?> objectType : objectTypes) {
            if (objectType == objectTypeCheck) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPrimitive(Field field) {
        return FieldUtil.isPrimitiveNumber(field) || FieldUtil.objectsHasAnyType(field.getType(), boolean.class);
    }

    public static boolean isPrimitiveNumber(Field field) {
        return FieldUtil.objectsHasAnyType(field.getType(),
                byte.class, char.class, short.class,
                int.class, long.class, float.class, double.class);
    }

    public static <T> boolean deepCompare(T object1, T object2) throws CopierException {
        Field[] object1Fields = object1.getClass().getDeclaredFields();
        Field[] object2Fields = object2.getClass().getDeclaredFields();

        if (object1Fields.length != object2Fields.length) {
            return false;
        }
        for (int index = 0; index < object1Fields.length; ++index) {
            Field field = object1Fields[index];
            Field field2 = object2Fields[index];
            boolean accessible = field.isAccessible();
            boolean accessible2 = field2.isAccessible();
            field.setAccessible(true);
            field2.setAccessible(true);

            Object value1 = null;
            Object value2 = null;
            try {
                value1 = field.get(object1);
                value2 = field2.get(object2);
            } catch (IllegalAccessException e) {
                throw new CopierException(String.format("Could not get the value of the property '%s' from the two " +
                        "object for compare", field.getName()));
            }
            if ((value1 == null && value2 != null) || (value1 != null && !value1.equals(value2))) {
                return false;
            }

            field.setAccessible(accessible);
            field2.setAccessible(accessible2);
        }
        return true;
    }

    public static List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            fields.addAll(Arrays.asList(c.getDeclaredFields()));
        }
        return fields;
    }

    public static Field getDeclaredField(Class<?> type, String name) {
        Class<?> ctype = type;
        Field field = null;
        do {
            try { field = ctype.getDeclaredField(name); } catch (NoSuchFieldException ignored) {}
            ctype = ctype.getSuperclass();
        } while (field == null && ctype != null);
        return field;
    }

    public static <A extends Annotation> A getAnnotation(Class<?> classType, final Class<A> annotationClass) {
        while (!classType.isAssignableFrom(Object.class)) {
            if (classType.isAnnotationPresent(annotationClass)) {
                return classType.getAnnotation(annotationClass);
            }
            classType = classType.getSuperclass();
        }
        return null;
    }

    public static String getFieldColumnName(Field field) {
        JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
        if (joinColumn != null && !joinColumn.name().isEmpty()) return joinColumn.name();
        Column column = field.getAnnotation(Column.class);
        if (column != null && !column.name().isEmpty()) return column.name();
        return field.getName();
    }

    public static Map<String, Object[]> findJoinColumnFields(Class<?> type) {
        Map<String, Object[]> result = new HashMap<>();
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            Field[] fields = c.getDeclaredFields();
            for (Field field : fields) {
                JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
                if (joinColumn != null) {
                    Class<?> fieldType = field.getType();
                    result.put(joinColumn.name(),
                            new Object[]{
                                    field,
                                    fieldType,
                                    joinColumn
                            });
                }
            }
        }
        return result;
    }

}
