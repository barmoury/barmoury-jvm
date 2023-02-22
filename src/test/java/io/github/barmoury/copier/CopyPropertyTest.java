package io.github.barmoury.copier;

import io.github.barmoury.util.FieldUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public class CopyPropertyTest {

    static class TestIgnoreClass {
        @CopyProperty(ignore = true)
        long id;
        String name;
        @CopyProperty(ignore = true)
        String permanent;
        String description;
    }

    TestIgnoreClass testIgnoreClass1;
    TestIgnoreClass testIgnoreClass2;
    TestIgnoreClass testIgnoreClass3;

    @BeforeEach
    public void setup() {
        testIgnoreClass1 = new TestIgnoreClass();
        testIgnoreClass1.id = 2;
        testIgnoreClass1.name = "Thecarisma";
        testIgnoreClass1.permanent = "Glue";
        testIgnoreClass1.description = "test ignore annotation class 1";

        testIgnoreClass2 = new TestIgnoreClass();
        testIgnoreClass2.id = 4;
        testIgnoreClass2.name = "hackers Deck";
        testIgnoreClass2.permanent = "Marker";
        testIgnoreClass2.description = "test ignore annotation class 2";

        testIgnoreClass3 = new TestIgnoreClass();
        testIgnoreClass3.id = 6;
        testIgnoreClass3.name = "Quick Utils";
        testIgnoreClass3.permanent = "Adhesive";
        testIgnoreClass3.description = "test ignore annotation class 3";
    }

    @Test
    public void testIgnoreDuringCopy() throws CopierException {
        Copier.copy(testIgnoreClass1, testIgnoreClass2);

        Assertions.assertNotEquals(testIgnoreClass1.id, testIgnoreClass2.id);
        Assertions.assertEquals(testIgnoreClass1.name, testIgnoreClass2.name);
        Assertions.assertNotEquals(testIgnoreClass1.permanent, testIgnoreClass2.permanent);
        Assertions.assertEquals(testIgnoreClass1.description, testIgnoreClass2.description);
        Assertions.assertFalse(FieldUtil.deepCompare(testIgnoreClass1, testIgnoreClass2));
    }

    public String encrypt(String plainText, String ivKey, String secretKey) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashedIV = digest.digest(ivKey.getBytes(StandardCharsets.UTF_8));
        byte[] hashedSecret = digest.digest(secretKey.getBytes(StandardCharsets.UTF_8));

        IvParameterSpec IV = new IvParameterSpec(
                bytesToHex(hashedIV).substring(0, 16).getBytes()
        );

        SecretKeySpec secret = new SecretKeySpec(
                bytesToHex(hashedSecret).substring(0, 32).getBytes(),
                "AES"
        );

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secret, IV);
        byte[] encrypted = cipher.doFinal(plainText.getBytes());

        String params = Base64.getEncoder().encodeToString(encrypted);
        return Base64.getEncoder().encodeToString(params.getBytes());
    }

    /**
     * bytesToHex - a function to convert encrypted byte to a hexadecimal String
     *
     * @param data byte[]
     * @return string String
     */
    public static String bytesToHex(byte[] data) {
        if (data == null) {
            return null;
        }

        int len = data.length;
        String string = "";
        for (int i = 0; i < len; i++) {
            if ((data[i] & 0xFF) < 16) {
                string = string + "0" + java.lang.Integer.toHexString(data[i] & 0xFF);
            } else {
                string = string + java.lang.Integer.toHexString(data[i] & 0xFF);
            }
        }
        return string;
    }

    @Test
    void testEncryption() throws Exception {
        String params = encrypt("{\"msisdn\":\"+226112018639\",\"accountNumber\":\"0027363929\",\"countryCode\":\"BF\",\"currencyCode\":\"XOF\",\"customerEmail\":\"test123tingg@getnada.com\",\"customerFirstName\":\"John\",\"customerLastName\":\"Smith\",\"dueDate\":\"2023-02-15 04:40:56\",\"failRedirectUrl\":\"https://null/gateway/base/payTinggResultpage/1416010331032883200\",\"languageCode\":\"en\",\"merchantTransactionID\":\"20230210TEST1\",\"paymentWebhookUrl\":\"https://6b4e-113-87-233-191.ap.ngrok.io/gateway/base/paytinggback\",\"pendingRedirectUrl\":\"\",\"requestAmount\":\"100\",\"requestDescription\":\"your pro simple desc\",\"serviceCode\":\"AXI\",\"successRedirectUrl\":\"https://null/gateway/base/payTinggResultpage/1416010331032883200\"}",
                "XDEgFkWgXZuRUJ7h",
                "kPtve7Iargur8aPk");
        System.out.printf("https://online.tingg.africa/v2/express/?params=%s&accessKey=%s&countryCode=BF%n",
                params, "4TmDwY5kBhF8Yffq5NGTPVQqWBSZCugYxDwecDW94dZ2mdcGIq3KSmpsu9jtn");
    }
    
}
