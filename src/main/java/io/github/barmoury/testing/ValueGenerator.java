package io.github.barmoury.testing;

import io.github.barmoury.util.FieldUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class ValueGenerator {

    // https://stackoverflow.com/a/20536597
    public static String generateRandomString(String saltChars, int length) {
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < length) { // length of the random string.
            int index = (int) (rnd.nextFloat() * saltChars.length());
            salt.append(saltChars.charAt(index));
        }
        return salt.toString();
    }

    public static String generateRandomString(int length) {
        return ValueGenerator.generateRandomString(
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890", length);
    }

    public static String generateRandomString(String saltChars) {
        return ValueGenerator.generateRandomString(saltChars, 5);
    }

    public static String generateRandomString() {
        return ValueGenerator.generateRandomString(5);
    }

    public static int generateRandomInteger(int min, int max) {
        return (new Random().nextInt(max - min) + min);
    }

    public static char generateRandomChar() {
        return (char) generateRandomInteger(1, 99);
    }

    public static byte generateRandomByte() {
        return (byte) generateRandomInteger(1, 99);
    }

    public static short generateRandomShort() {
        return (short) generateRandomInteger(1, 99);
    }

    public static int generateRandomInteger() {
        return generateRandomInteger(999, 99999);
    }

    public static long generateRandomLong() {
        return generateRandomInteger(999, 99999);
    }

    public static double generateRandomDouble() {
        return generateRandomInteger(999, 99999);
    }

    public static float generateRandomFloat() {
        return generateRandomInteger(999, 99999);
    }

    public static Date generateRandomDate() {
        return new Date();
    }

    public static boolean anyBooleanValue() {
        return generateRandomInteger(1, 3) > 1;
    }

    public static <T> T populate(T obj) {
        return populate(obj, true);
    }

    public static <T> T populate(T obj, boolean onlyNullFields) {
        List<Field> fields = FieldUtil.getAllFields(obj.getClass());
        for (Field field : fields) {
            try {
                boolean fieldIsAccessible = field.canAccess(obj);
                if (!fieldIsAccessible) field.setAccessible(true);
                if (Modifier.isStatic(field.getModifiers()) || (onlyNullFields && field.get(obj) != null)) {
                    if (!fieldIsAccessible) field.setAccessible(false);
                    continue;
                }
                if (FieldUtil.objectsHasAnyType(field.getType(), int.class, Integer.class))
                { field.set(obj, generateRandomInteger(1, 999)); }
                else if (FieldUtil.objectsHasAnyType(field.getType(), long.class, Long.class))
                { field.set(obj, generateRandomLong()); }
                else if (FieldUtil.objectsHasAnyType(field.getType(), float.class, Float.class))
                { field.set(obj, generateRandomFloat()); }
                else if (FieldUtil.objectsHasAnyType(field.getType(), double.class, Double.class))
                { field.set(obj, generateRandomDouble()); }
                else if (FieldUtil.objectsHasAnyType(field.getType(), short.class, Short.class))
                { field.set(obj, generateRandomShort()); }
                else if (FieldUtil.objectsHasAnyType(field.getType(), byte.class, Byte.class))
                { field.set(obj, generateRandomByte()); }
                else if (FieldUtil.objectsHasAnyType(field.getType(), char.class, Character.class))
                { field.set(obj, generateRandomChar()); }
                else if (FieldUtil.objectsHasAnyType(field.getType(), boolean.class, Boolean.class))
                { field.set(obj, !((boolean) field.get(obj))); }
                else if (FieldUtil.objectsHasAnyType(field.getType(), Date.class))
                { field.set(obj, generateRandomDate()); }
                else if (FieldUtil.objectsHasAnyType(field.getType(), String.class))
                { field.set(obj, generateRandomString(10)); }
                else if (FieldUtil.objectsHasAnyType(field.getType(), List.class))
                { field.set(obj, new ArrayList<>()); }
                else if (FieldUtil.objectsHasAnyType(field.getType(), Map.class))
                { field.set(obj, new HashMap<>()); }
                if (!fieldIsAccessible) field.setAccessible(false);
            } catch (Exception e) {
                throw new ValueGeneratorException(e.getMessage());
            }
        }
        return obj;
    }

}
