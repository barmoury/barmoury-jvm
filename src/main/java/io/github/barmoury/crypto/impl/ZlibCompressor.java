package io.github.barmoury.crypto.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.barmoury.crypto.IEncryptor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

@Component
public class ZlibCompressor implements IEncryptor<Object> {

    @Autowired
    public ObjectMapper objectMapper;

    @Override
    @SneakyThrows
    public String encrypt(Object obj) {
        if (obj == null) return "";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DeflaterOutputStream dos = new DeflaterOutputStream(baos);
        dos.write(objectMapper.writeValueAsBytes(obj));
        dos.flush(); dos.close();
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    @Override
    @SneakyThrows
    public Object decrypt(String encrypted) {
        if (encrypted == null || encrypted.length() == 0) return "";
        ByteArrayInputStream bais =
                new ByteArrayInputStream(org.apache.tomcat.util.codec.binary.Base64.decodeBase64(encrypted));
        InflaterInputStream iis = new InflaterInputStream(bais);
        int length;
        StringBuilder result = new StringBuilder();
        byte[] buf = new byte[5];
        while ((length = iis.read(buf)) != -1) result.append(new String(Arrays.copyOf(buf, length)));
        return objectMapper.readValue(result.toString(), Object.class);
    }

}
