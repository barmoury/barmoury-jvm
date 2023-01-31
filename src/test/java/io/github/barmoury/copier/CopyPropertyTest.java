package io.github.barmoury.copier;

import io.github.barmoury.util.FieldUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    
}
