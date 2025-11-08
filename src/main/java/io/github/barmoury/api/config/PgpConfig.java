package io.github.barmoury.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import io.github.barmoury.crypto.pgp.PgpDecryption;
import io.github.barmoury.crypto.pgp.PgpEncryption;
import io.github.barmoury.crypto.pgp.PgpManager;
import io.github.barmoury.crypto.pgp.PgpUtil;
import io.github.barmoury.util.FileUtil;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.ArrayUtils;
import org.bouncycastle.bcpg.CompressionAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Component
public class PgpConfig {

    @Getter static PgpManager pgpManager;
    @Getter static PgpEncryption pgpEncryptor;
    @Getter static PgpDecryption pgpDecryptor;
    @Setter @Getter static Class<?> applicationClass = PgpConfig.class;
    public static final String REQUEST_MODEL_ATTRIBUTE = "barmoury.pgp.model.attr";
    public static final String RMA = REQUEST_MODEL_ATTRIBUTE;
    public static final String REQUEST_ATTRIBUTE_CLASS_KEY = "barmoury.pgp.request.class";
    public static final String REQUEST_ATTRIBUTE_NAMING_STRATEGY_KEY = "barmoury.property.naming.strategy";


    @Value("${barmoury.crypto.pgp.encryption.charset:UTF-8}") String _charset;
    @Value("${spring.jackson.property-naming-strategy:}") String _namingStrategy;
    @Value("${barmoury.crypto.pgp.encryption.encoding:BASE64}") String _encoding;
    @Value("${barmoury.crypto.pgp.path.passwords:#{null}}") String[] pgpPathPasswords;
    @Value("${barmoury.crypto.pgp.encryption.sign:false}") boolean signEncryptedPayload;
    @Value("${barmoury.crypto.pgp.key.path.public:#{null}}") String[] pgpPublicKeyPaths;
    @Value("${barmoury.crypto.pgp.key.path.private:#{null}}") String[] pgpPrivateKeyPaths;
    @Value("${barmoury.crypto.pgp.string.passwords:#{null}}") String[] pgpStringPasswords;
    @Value("${barmoury.crypto.pgp.key.string.public:#{null}}") String[] pgpPublicKeyStrings;
    @Value("${barmoury.crypto.pgp.key.string.private:#{null}}") String[] pgpPrivateKeyStrings;
    @Value("${barmoury.crypto.pgp.encryption.sign.hash-algorithm:SHA256}") String hashAlgorithm;

    @SneakyThrows
    @PostConstruct
    void init() {
        PgpUtil.setGson(new Gson());
        PgpUtil.setNamingStrategy(_namingStrategy);
        PgpUtil.setObjectMapper(new ObjectMapper());
        PgpUtil.setCharset(resolveCharset(_charset));
        validateEncoding(_encoding); PgpUtil.setEncoding(_encoding);
        pgpManager = new PgpManager();
        pgpManager.setHashAlgorithmCode(hashAlgorithm);
        if (pgpPrivateKeyPaths != null && pgpPrivateKeyPaths.length > 0) {
            for (int index = 0;  index < pgpPrivateKeyPaths.length; index++) {
                pgpManager.addSecretKeys(FileUtil.fileStream(pgpPrivateKeyPaths[index]),
                        pgpPathPasswords[index].toCharArray());
            }
        }
        if (pgpPrivateKeyStrings != null && pgpPrivateKeyStrings.length > 0) {
            for (int index = 0;  index < pgpPrivateKeyStrings.length; index++) {
                pgpManager.addSecretKeys(new ByteArrayInputStream(pgpPrivateKeyStrings[index].getBytes(StandardCharsets.UTF_8)),
                        pgpStringPasswords[index].toCharArray());
            }
        }
        if (pgpPublicKeyPaths != null && pgpPublicKeyPaths.length > 0) {
            for (String pgpPublicKeyPath : pgpPublicKeyPaths) {
                pgpManager.addPublicKeys(FileUtil.fileStream(pgpPublicKeyPath));
            }
        }
        if (pgpPublicKeyStrings != null && pgpPublicKeyStrings.length > 0) {
            for (String pgpPublicKeyString : pgpPublicKeyStrings) {
                pgpManager.addPublicKeys(new ByteArrayInputStream(pgpPublicKeyString.getBytes(StandardCharsets.UTF_8)));
            }
        }
        if ((pgpPublicKeyPaths != null && pgpPublicKeyPaths.length > 0) || (pgpPublicKeyStrings != null && pgpPublicKeyStrings.length > 0)) {
            PgpEncryption.PgpEncryptionBuilder pgpEncryptorBuilder = PgpEncryption.builder()
                    .armor(true)
                    .withIntegrityCheck(true)
                    .sign(signEncryptedPayload)
                    .compressionAlgorithm(CompressionAlgorithmTags.ZIP)
                    .symmetricKeyAlgorithm(SymmetricKeyAlgorithmTags.AES_128);
            if (pgpPublicKeyPaths != null && pgpPublicKeyPaths.length > 0) {
                pgpEncryptorBuilder = pgpEncryptorBuilder.publicKeyLocations(pgpPublicKeyPaths);
            }
            if (pgpPublicKeyStrings != null && pgpPublicKeyStrings.length > 0) {
                pgpEncryptorBuilder = pgpEncryptorBuilder.publicKeyStrings(pgpPublicKeyStrings);
            }
            if (pgpManager != null) pgpEncryptorBuilder.pgpManager(pgpManager);
            pgpEncryptor = pgpEncryptorBuilder.build();
            PgpUtil.setPgpEncryptor(pgpEncryptor);
        }

        if ((pgpPathPasswords != null && pgpPathPasswords.length > 0 && pgpPrivateKeyPaths != null && pgpPrivateKeyPaths.length > 0)
                || (pgpStringPasswords != null && pgpStringPasswords.length > 0 && pgpPrivateKeyStrings != null && pgpPrivateKeyStrings.length > 0)) {
            pgpDecryptor = PgpDecryption.builder().signed(signEncryptedPayload).build();
            if (pgpManager != null) pgpDecryptor.setPgpManager(pgpManager);
            if ((pgpPrivateKeyPaths != null && pgpPrivateKeyPaths.length == 1) && (pgpPrivateKeyStrings == null || pgpPrivateKeyStrings.length == 0)) {
                pgpDecryptor.setPassCode(pgpPathPasswords[0]);
                pgpDecryptor.setPgpSecretKeyRingCollection(new PGPSecretKeyRingCollection(
                        PGPUtil.getDecoderStream(FileUtil.fileStream(applicationClass, pgpPrivateKeyPaths[0])),
                        new JcaKeyFingerprintCalculator()));
            } else if ((pgpPrivateKeyStrings != null && pgpPrivateKeyStrings.length == 1) && (pgpPrivateKeyPaths == null || pgpPrivateKeyPaths.length == 0)) {
                pgpDecryptor.setPassCode(pgpStringPasswords[0]);
                pgpDecryptor.setPgpSecretKeyRingCollection(new PGPSecretKeyRingCollection(
                        PGPUtil.getDecoderStream(new ByteArrayInputStream(pgpPrivateKeyStrings[0].getBytes(StandardCharsets.UTF_8))),
                        new JcaKeyFingerprintCalculator()));
            } else {
                if (pgpPathPasswords == null) pgpPathPasswords = new String[]{};
                if (pgpStringPasswords == null) pgpStringPasswords = new String[]{};
                pgpDecryptor.setPassCodes(ArrayUtils.addAll(pgpPathPasswords, pgpStringPasswords));
                if (pgpPrivateKeyPaths != null && pgpPrivateKeyPaths.length == 1) {
                    pgpDecryptor.setPrivateKeyLocations(pgpPrivateKeyPaths);
                }
                if (pgpPrivateKeyStrings != null && pgpPrivateKeyStrings.length == 1) {
                    pgpDecryptor.setPrivateStrings(pgpPrivateKeyPaths);
                }
            }
            PgpUtil.setPgpDecryptor(pgpDecryptor);
        }

    }

    @Deprecated
    public static String objectToString(Object object) {
        return PgpUtil.objectToString(object);
    }

    @Deprecated
    public static <T> T fromEncryptedString(Class<T> tClass, String pgpEncrypted)
            throws PGPException, IOException, NoSuchMethodException, InvocationTargetException,
            InstantiationException, IllegalAccessException {
        return PgpUtil.fromEncryptedString(pgpEncrypted, tClass);
    }

    @Deprecated
    public static byte[] decodeEncrypted(byte[] pgpEncrypted) throws PGPException, IOException {
        return PgpUtil.decodeEncrypted(pgpEncrypted);
    }

    @Deprecated
    public static byte[] decodeEncryptedString(String pgpEncrypted) throws PGPException, IOException {
        return PgpUtil.decodeEncryptedString(pgpEncrypted);
    }

    void validateEncoding(String encoding) {
        if (!(encoding.equalsIgnoreCase("NONE")
                || encoding.equalsIgnoreCase("BASE32")
                || encoding.equalsIgnoreCase("BASE64")
                || encoding.equalsIgnoreCase("BASE64_URL"))) {
            throw new IllegalArgumentException("The barmoury.crypto.pgp.encryption.encoding must be any of" +
                    " NONE, BASE32, BASE64, BASE64_URL");
        }
    }

    Charset resolveCharset(String charsetName) {
        if (!Charset.isSupported(charsetName)) {
            throw new IllegalArgumentException("The barmoury.crypto.pgp.encryption.charset must be any of" +
                    " " + Charset.availableCharsets());
        }
        return Charset.forName(charsetName);
    }

}
