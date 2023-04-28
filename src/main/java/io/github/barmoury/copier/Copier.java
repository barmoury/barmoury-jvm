package io.github.barmoury.copier;

import io.github.barmoury.util.FieldUtil;

import java.lang.reflect.Field;

public class Copier {

    @SafeVarargs
    public static <T> void copy(T target, T... sources) {
        if (sources.length == 0) return;
        Field[] sourceFields = target.getClass().getDeclaredFields();
        for (Field field : sourceFields) {
            String fieldName = field.getName();
            CopyProperty copyProperty = field.getAnnotation(CopyProperty.class);
            if (copyProperty != null && copyProperty.ignore()) continue;
            try {
                Object[][] fields = new Object[sources.length][2];
                for (int counter = 0; counter < fields.length; ++counter) {
                    Field subField = sources[counter].getClass().getDeclaredField(fieldName);
                    fields[counter][0] = sources[counter];
                    fields[counter][1] = subField;
                }
                boolean fieldIsAccessible = field.canAccess(target);
                if (!fieldIsAccessible) field.setAccessible(true);
                Object value = findUsableValue(fields);
                if (value != null) {
                    if (copyProperty != null && copyProperty.useNonZeroValue() && FieldUtil.isPrimitiveNumber(field)) {
                        if (FieldUtil.objectsHasAnyType(field.getType(), int.class, Integer.class) && ((int) value > 0))
                        { field.set(target, value); }
                        else if (FieldUtil.objectsHasAnyType(field.getType(), long.class, Long.class) && ((Long) value > 0))
                        { field.set(target, value); }
                        else if (FieldUtil.objectsHasAnyType(field.getType(), float.class, Float.class) && ((Float) value > 0))
                        { field.set(target, value); }
                        else if (FieldUtil.objectsHasAnyType(field.getType(), double.class, Double.class) && ((Double) value > 0))
                        { field.set(target, value); }
                        else if (FieldUtil.objectsHasAnyType(field.getType(), short.class, Short.class) && ((Short) value > 0))
                        { field.set(target, value); }
                        else if (FieldUtil.objectsHasAnyType(field.getType(), byte.class, Byte.class) && ((Byte) value > 0))
                        { field.set(target, value); } // maybe check type matches
                        else if (FieldUtil.objectsHasAnyType(field.getType(), char.class, Character.class) && ((Character) value > 0))
                        { field.set(target, value); }
                    } else { field.set(target, value); }
                } else if (copyProperty != null && copyProperty.priority() == CopyProperty.CopyValuePriority.SOURCE &&
                    !FieldUtil.isPrimitive(field)) {
                    field.set(target, null);
                }
                if (!fieldIsAccessible) field.setAccessible(false);
            } catch (NoSuchFieldException ex) {
                // should not occur, tho can happen
                // we need not throw exception, so
                // we just skip the one not present
            } catch (IllegalAccessException ex) {
                throw new CopierException(String.format("Could not get the value of the property '%s' from all the " +
                        "provided sources", fieldName));
            }
        }
    }

    private static Object findUsableValue(Object[][] fields) throws IllegalAccessException {
        boolean allValueIsNull = true;
        Class<?>[] types = new Class[fields.length];
        Object[] values = new Object[fields.length];
        boolean[] fieldIsAccessible = new boolean[fields.length];
        for (int index = 0; index < fields.length; index++) {
            Object[] objects = fields[index];
            Object source = objects[0];
            Field field = (Field) objects[1];
            types[index] = field.getType();
            fieldIsAccessible[index] = field.canAccess(source);
            if (!fieldIsAccessible[index]) field.setAccessible(true);
            values[index] = field.get(source);
            if (allValueIsNull && values[index] != null) allValueIsNull = false;
            if (!fieldIsAccessible[index]) field.setAccessible(false);
        }
        if (allValueIsNull) return null;
        for (int index = 0;; index++) {
            Object value = values[index];
            if (value != null && FieldUtil.objectsHasAnyType(types[index], int.class, Integer.class)) {
                if (((Integer) value) >= 0 || index == values.length-1) return value;
            } else if (value != null && FieldUtil.objectsHasAnyType(types[index], long.class, Long.class)) {
                if (((Long) value) > 0 || index == values.length-1) return value;
            } else if (value != null && FieldUtil.objectsHasAnyType(types[index], float.class, Float.class)) {
                if (((Float) value) > 0 || index == values.length-1) return value;
            } else if (value != null && FieldUtil.objectsHasAnyType(types[index], double.class, Double.class)) {
                if (((Double) value) > 0 || index == values.length-1) return value;
            } else if (value != null && FieldUtil.objectsHasAnyType(types[index], boolean.class, Boolean.class)) {
                if (((Boolean) value) || index == values.length-1) return value;
            } else if (value != null && FieldUtil.objectsHasAnyType(types[index], boolean.class, Short.class)) {
                if (((Short) value) > 0 || index == values.length-1) return value;
            } else if (value != null && FieldUtil.objectsHasAnyType(types[index], boolean.class, Byte.class)) {
                if (((Byte) value) > 0 || index == values.length-1) return value;
            } else if (value != null && FieldUtil.objectsHasAnyType(types[index], boolean.class, Character.class)) {
                if (((Character) value) > 0 || index == values.length-1) return value;
            } else if (value != null) return value;
        }
    }

}
