package io.github.barmoury.copier;

import io.github.barmoury.util.FieldUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class CopierTest {

    static class AnObject {
        long id;
        float fl;
        double dob;
        String name;
        String email;
        List<String> socialProfiles;
        boolean banned;
        int deleted;
    }

    static class AnObject2 {
        @CopyProperty(ignore = true) long id;
        float fl;
        double dob;
        String name;
        String email;
        @CopyProperty(ignore = true) List<String> socialProfiles;
        @CopyProperty(priority = CopyProperty.CopyValuePriority.TARGET) Boolean banned = true;
        int deleted;
    }

    static class AnObject3 {
        @CopyProperty(useNonZeroValue = true) long id = 30;
        float fl;
        double dob;
        String name;
        String email;
        List<String> socialProfiles;
        boolean banned;
        int deleted;
    }

    AnObject obj1;
    AnObject obj2;
    AnObject obj3;
    AnObject obj4;
    AnObject obj5;
    AnObject obj6;
    AnObject obj7;
    AnObject2 obj8;

    @BeforeEach
    public void setup() {
        obj1 = new AnObject();
        obj2 = new AnObject();
        obj3 = new AnObject();
        obj4 = new AnObject();
        obj5 = new AnObject();
        obj6 = new AnObject();
        obj7 = new AnObject();
        obj8 = new AnObject2();

        obj1.id = 1;
        obj1.banned = true;
        obj1.name = "Thecarisma";

        obj5.id = 20;
        obj5.deleted = 1;

        obj2.email = "whatisthis@gmail.com";
        obj2.socialProfiles = new ArrayList<String>();
        obj2.deleted = 1;
        obj2.socialProfiles.add("https://github.com/Thecarisma");
        obj2.socialProfiles.add("https://github.com/Thecarisma");

        obj3.id = 2;
        obj3.banned = false;
        obj3.socialProfiles = new ArrayList<String>();
        obj3.socialProfiles.add("https://dev.to/iamthecarisma");
        obj3.socialProfiles.add("https://twitter.com/iamthecarisma");
        obj3.deleted = 7;

        obj6.id = 0;
        obj6.banned = false;
        obj6.name = "Thecarisma";
        obj6.email = "whatisthis@gmail.com";
        obj6.socialProfiles = new ArrayList<String>();
        obj6.deleted = 1;
        obj6.socialProfiles.add("https://github.com/Thecarisma");
        obj6.socialProfiles.add("https://github.com/Thecarisma");

        obj7.id = 1;
        obj7.banned = true;
        obj7.name = "Thecarisma";
        obj7.email = "whatisthis@gmail.com";
        obj7.socialProfiles = new ArrayList<String>();
        obj7.deleted = 1;
        obj7.socialProfiles.add("https://github.com/Thecarisma");
        obj7.socialProfiles.add("https://github.com/Thecarisma");

        obj4.id = 20;
        obj4.banned = true;
        obj4.name = "Thecarisma";
        obj4.email = "whatisthis@gmail.com";
        obj4.socialProfiles = new ArrayList<String>();
        obj4.deleted = 7;
        obj4.socialProfiles.add("https://github.com/Thecarisma");
        obj4.socialProfiles.add("https://github.com/Thecarisma");

        obj8.id = 20;
        obj8.banned = null;
        obj8.name = "Thecarisma";
        obj8.email = "whatisthis@gmail.com";
        obj8.socialProfiles = new ArrayList<String>();
        obj8.deleted = 7;
        obj8.socialProfiles.add("https://github.com/Thecarisma");
        obj8.socialProfiles.add("https://github.com/Thecarisma");
    }

    @Test
    public void testCopyFromOneSource() throws CopierException {
        AnObject obj = new AnObject();
        Copier.copy(obj, obj2);
        Assertions.assertTrue(FieldUtil.deepCompare(obj, obj2));
        Assertions.assertFalse(FieldUtil.deepCompare(obj, obj6));
    }

    @Test
    public void testCopyFromTwoSource() throws CopierException {
        AnObject obj = new AnObject();
        Copier.copy(obj, obj2, obj1);
        Assertions.assertTrue(FieldUtil.deepCompare(obj, obj7));
    }

    @Test
    public void testCopyFromThreeSource() throws CopierException {
        AnObject obj = new AnObject();
        Copier.copy(obj, obj2, obj1);
        Assertions.assertTrue(FieldUtil.deepCompare(obj, obj7));
    }

    @Test
    public void testCopyFromFourSource() throws CopierException {
        AnObject obj = new AnObject();
        Copier.copy(obj, obj2, obj1, obj5);
        Assertions.assertTrue(FieldUtil.deepCompare(obj, obj7));
    }

    @Test
    public void testCopyWithCopyProperty() throws CopierException {
        AnObject2 obj = new AnObject2();
        Copier.copy(obj, obj2);
        Assertions.assertEquals(obj.id, obj2.id);
    }

    @Test
    public void testCopyWithCopyPropertyPrioroty() throws CopierException {
        AnObject obj = new AnObject();
        obj.banned = true;
        Copier.copy(obj, obj8);
        Assertions.assertTrue(obj.banned);
        Assertions.assertEquals(obj.id, obj8.id);
        Assertions.assertNotEquals(obj.banned, obj8.banned);
    }

    @Test
    public void testCopyWithCopyPropertyNonZero() throws CopierException {
        AnObject3 obj = new AnObject3();
        Copier.copy(obj, obj6);
        Assertions.assertNotEquals(obj.id, obj2.id);
    }

}
