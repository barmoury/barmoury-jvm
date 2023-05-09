package io.github.barmoury.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.gson.Gson;
import io.github.barmoury.copier.Copier;
import io.github.barmoury.crypto.pgp.PgpDecryption;
import io.github.barmoury.crypto.pgp.PgpEncryption;
import io.github.barmoury.crypto.pgp.PgpManager;
import io.github.barmoury.util.FileUtil;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.tomcat.util.codec.binary.Base64;
import org.bouncycastle.bcpg.CompressionAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.GsonJsonParser;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.GsonBuilderUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@Component
public class PgpConfig {

    @Getter @Setter static Gson gson;
    @Getter static String namingStrategy;
    @Getter static PgpManager pgpManager;
    @Getter static PgpEncryption pgpEncryptor;
    @Getter static PgpDecryption pgpDecryptor;
    @Setter @Getter static ObjectMapper objectMapper;
    @Setter @Getter static Class<?> applicationClass = PgpConfig.class;
    public static final String REQUEST_MODEL_ATTRIBUTE = "barmoury.pgp.model.attr";
    public static final String RMA = REQUEST_MODEL_ATTRIBUTE;
    public static final String REQUEST_ATTRIBUTE_CLASS_KEY = "barmoury.pgp.request.class";
    public static final String REQUEST_ATTRIBUTE_NAMING_STRATEGY_KEY = "barmoury.property.naming.strategy";


    @Value("${barmoury.crypto.pgp.passwords:#{null}}") String[] pgpPasswords;
    @Value("${spring.jackson.property-naming-strategy:}") String _namingStrategy;
    @Value("${barmoury.crypto.pgp.encryption.sign:false}") boolean signEncryptedPayload;
    @Value("${barmoury.crypto.pgp.key.path.public:#{null}}") String[] pgpPublicKeyPaths;
    @Value("${barmoury.crypto.pgp.key.path.private:#{null}}") String[] pgpPrivateKeyPaths;
    @Value("${barmoury.crypto.pgp.encryption.sign.hash-algorithm:SHA256}") String hashAlgorithm;

    @SneakyThrows
    @PostConstruct
    void init() {
        gson = new Gson();
        namingStrategy = _namingStrategy;
        objectMapper = new ObjectMapper();
        pgpManager = new PgpManager();
        pgpManager.setHashAlgorithmCode(hashAlgorithm);
        for (int index = 0; index < pgpPrivateKeyPaths.length; index++) {
            pgpManager.addSecretKeys(FileUtil.fileStream(pgpPrivateKeyPaths[index]),
                    pgpPasswords[index].toCharArray());
        }
        if (pgpPublicKeyPaths != null && pgpPublicKeyPaths.length > 0) {
            for (String pgpPublicKeyPath : pgpPublicKeyPaths) {
                pgpManager.addPublicKeys(FileUtil.fileStream(pgpPublicKeyPath));
            }
        }
        if (pgpPublicKeyPaths != null && pgpPublicKeyPaths.length > 0) {
            PgpEncryption.PgpEncryptionBuilder pgpEncryptorBuilder = PgpEncryption.builder()
                    .armor(true)
                    .withIntegrityCheck(true)
                    .sign(signEncryptedPayload)
                    .compressionAlgorithm(CompressionAlgorithmTags.ZIP)
                    .symmetricKeyAlgorithm(SymmetricKeyAlgorithmTags.AES_128)
                    .publicKeyLocations(pgpPublicKeyPaths);
            if (pgpManager != null) pgpEncryptorBuilder.pgpManager(pgpManager);
            pgpEncryptor = pgpEncryptorBuilder.build();
        }

        if (pgpPasswords != null && pgpPasswords.length > 0 && pgpPrivateKeyPaths != null && pgpPrivateKeyPaths.length > 0) {
            pgpDecryptor = PgpDecryption.builder().signed(signEncryptedPayload).build();
            if (pgpManager != null) pgpDecryptor.setPgpManager(pgpManager);
            if (pgpPrivateKeyPaths.length == 1) {
                pgpDecryptor.setPassCode(pgpPasswords[0]);
                pgpDecryptor.setPgpSecretKeyRingCollection(new PGPSecretKeyRingCollection(
                        PGPUtil.getDecoderStream(FileUtil.fileStream(applicationClass, pgpPrivateKeyPaths[0])),
                        new JcaKeyFingerprintCalculator()));
            } else {
                pgpDecryptor.setPassCodes(pgpPasswords);
                pgpDecryptor.setPrivateKeyLocations(pgpPrivateKeyPaths);
            }
        }

    }

    public static String objectToString(Object object) {
        return gson.toJson(object);
    }

    public <T> T fromEncryptedString(Class<T> tClass, String pgpEncrypted)
            throws PGPException, IOException, NoSuchMethodException, InvocationTargetException,
            InstantiationException, IllegalAccessException {
        T body = tClass.getConstructor().newInstance();
        Copier.copy(body, getObjectMapper()
                .readValue(getPgpDecryptor().decrypt(Base64.decodeBase64(pgpEncrypted)), tClass));
        return body;
    }

}
