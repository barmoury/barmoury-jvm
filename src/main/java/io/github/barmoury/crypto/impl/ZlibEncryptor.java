package io.github.barmoury.crypto.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.barmoury.crypto.IEncryptor;
import lombok.SneakyThrows;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Component
public class ZlibEncryptor implements IEncryptor<Object> {

    @Autowired
    public ObjectMapper objectMapper;

    @Override
    @SneakyThrows
    public String encrypt(Object obj) {
        if (obj == null) return "";
        String str = objectMapper.writeValueAsString(obj);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(byteArrayOutputStream);
        gzip.write(str.getBytes(StandardCharsets.UTF_8));
        gzip.flush(); gzip.close();
        return Base64.encodeBase64String(byteArrayOutputStream.toByteArray());
    }

    @Override
    @SneakyThrows
    public Object decrypt(String encrypted) {
        if (encrypted == null || encrypted.length() == 0) return "";
        byte[] compressed = Base64.decodeBase64(encrypted);
        final StringBuilder result = new StringBuilder();
        if (isCompressed(compressed)) {
            GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(compressed));
            BufferedReader bufferedReader =
                    new BufferedReader(new InputStreamReader(gis, StandardCharsets.UTF_8));
            int readCount;
            char[] buffer = new char[256];
            while((readCount = bufferedReader.read(buffer)) > 0){
                result.append(buffer, 0, readCount);
            }
        } else {
            result.append(new String(compressed));
        }
        return objectMapper.readValue(result.toString(), Object.class);
    }

    public boolean isCompressed(final byte[] compressed) {
        return (compressed[0] == (byte) (GZIPInputStream.GZIP_MAGIC)) &&
                (compressed[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8));
    }

}
