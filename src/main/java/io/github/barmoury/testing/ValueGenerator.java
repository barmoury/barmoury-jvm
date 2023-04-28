package io.github.barmoury.testing;

import java.util.Random;

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

    public static int generateRandomInteger() {
        return generateRandomInteger(999, 99999);
    }

    public static long generateRandomLong() {
        return generateRandomInteger(999, 99999);
    }

    public static double generateRandomDouble() {
        return generateRandomInteger(999, 99999);
    }

    public static boolean anyBooleanValue() {
        return generateRandomInteger(1, 3) > 1;
    }

}
