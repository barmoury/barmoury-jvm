package io.github.barmoury.util;

import io.github.barmoury.copier.CopierException;

import java.lang.reflect.Field;

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

}
