package io.github.barmoury.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class FileUtil {

    public static InputStream fileStreamFromResource(Class<?> clazz, String path) throws FileNotFoundException {
        if (clazz == null) clazz = FileUtil.class;
        InputStream in = clazz.getClassLoader().getResourceAsStream(path);
        if (in == null) throw new FileNotFoundException("file not found in resource: " + path);
        return in;
    }

    public static InputStream fileStreamFromResource(String path) {
        return FileUtil.class.getClassLoader().getResourceAsStream(path);
    }

    public static InputStream fileStream(Class<?> clazz, String path) throws FileNotFoundException {
        if (path.startsWith(":classpath:")) {
            return fileStreamFromResource(clazz, path.substring(11));
        }
        return new FileInputStream(path);
    }

    public static InputStream fileStream(String path) throws FileNotFoundException {
        return fileStream(FileUtil.class, path);
    }

}
