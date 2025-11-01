package io.github.barmoury.crypto.pgp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import io.github.barmoury.copier.Copier;
import io.jsonwebtoken.impl.TextCodec;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.util.encoders.Base32;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;

@Log4j2
public class PgpUtil {

    @Getter @Setter static Gson gson;
    @Setter @Getter static Charset charset;
    @Setter @Getter static String encoding;
    @Setter @Getter static String namingStrategy;
    @Setter @Getter static ObjectMapper objectMapper;
    @Getter @Setter static PgpEncryption pgpEncryptor;
    @Getter @Setter static PgpDecryption pgpDecryptor;

    public static String objectToString(Object object) {
        return gson.toJson(object);
    }

    public static <T> T fromEncryptedString(String pgpEncrypted, Class<T> tClass)
            throws PGPException, IOException, NoSuchMethodException, InvocationTargetException,
            InstantiationException, IllegalAccessException {
        T body = tClass.getConstructor().newInstance();
        Copier.copy(body, objectMapper
                .readValue(decodeEncryptedString(pgpEncrypted), tClass));
        return body;
    }

    public static byte[] decodeEncrypted(byte[] pgpEncrypted) throws PGPException, IOException {
        return pgpDecryptor.decrypt(switch (encoding) {
            case "BASE64" -> TextCodec.BASE64.decode(new String(pgpEncrypted, charset));
            case "BASE64_URL" -> TextCodec.BASE64URL.decode(new String(pgpEncrypted, charset));
            case "BASE32" -> Base32.decode(pgpEncrypted);
            default -> pgpEncrypted;
        });
    }

    public static byte[] decodeEncryptedString(String pgpEncrypted) throws PGPException, IOException {
        return decodeEncrypted(pgpEncrypted.getBytes(charset));
    }

    public static void warn(String message) {
        if (log == null) return;
        log.warn(message);
    }

}
