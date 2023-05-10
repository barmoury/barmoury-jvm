package io.github.barmoury.crypto.pgp;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.barmoury.api.config.PgpConfig;
import io.jsonwebtoken.impl.TextCodec;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.bouncycastle.util.encoders.Base32;
import org.springframework.beans.factory.annotation.Value;

@Log4j2
@NoArgsConstructor
@JsonDeserialize(using = PgpTranslateDeserializer.class)
public class PgpTranslate {

    @JsonValue
    @SneakyThrows
    public String toEncryptedString() {
        byte[] encrypted = getPgpEncryptor()
                .encrypt(PgpConfig.objectToString(this));
        return switch (PgpConfig.getEncoding()) {
            case "BASE64" -> TextCodec.BASE64.encode(encrypted);
            case "BASE64_URL" -> TextCodec.BASE64URL.encode(encrypted);
            case "BASE32" -> new String(Base32.encode(encrypted), PgpConfig.getCharset());
            default -> new String(encrypted, PgpConfig.getCharset());
        };
    }

    public PgpEncryption getPgpEncryptor() {
        PgpEncryption pgpEncryption = PgpConfig.getPgpEncryptor();
        if (pgpEncryption == null) {
            log.warn(String.format("%s: the PgpEncryption is null, check if barmoury.crypto.pgp.*" +
                    " values are defined in your properties file, or add 'io.github.barmoury' in your component scan," +
                    " or override the method getPgpEncryptor in the class that extends this PgpTranslate",
                    this.getClass().getCanonicalName()));
        }
        return pgpEncryption;
    }

}
