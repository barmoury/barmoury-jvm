package io.github.barmoury.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.gson.Gson;
import io.github.barmoury.copier.Copier;
import io.github.barmoury.crypto.pgp.PgpDecryption;
import io.github.barmoury.crypto.pgp.PgpEncryption;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.tomcat.util.codec.binary.Base64;
import org.bouncycastle.bcpg.CompressionAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.openpgp.PGPException;
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

@Component
public class PgpConfig {

    @Getter static Gson gson;
    @Getter static String namingStrategy;
    @Getter static ObjectMapper objectMapper;
    @Getter static PgpEncryption pgpEncryptor;
    @Getter static PgpDecryption pgpDecryptor;
    public static final String REQUEST_MODEL_ATTRIBUTE = "barmoury.pgp.model.attr";
    public static final String RMA = REQUEST_MODEL_ATTRIBUTE;
    public static final String REQUEST_ATTRIBUTE_CLASS_KEY = "barmoury.pgp.request.class";
    public static final String REQUEST_ATTRIBUTE_NAMING_STRATEGY_KEY = "barmoury.property.naming.strategy";

    @Value("${barmoury.crypto.pgp.password:#{null}}") String pgpPassword;
    @Value("${spring.jackson.property-naming-strategy:}") String _namingStrategy;
    @Value("${barmoury.crypto.pgp.key.path.public:#{null}}") String pgpPublicKeyPath;
    @Value("${barmoury.crypto.pgp.key.path.private:#{null}}") String pgpPrivateKeyPath;

    @SneakyThrows
    @PostConstruct
    void init() {
        gson = new Gson();
        namingStrategy = _namingStrategy;
        objectMapper = new ObjectMapper();
        if (pgpPublicKeyPath != null) {
            pgpEncryptor = PgpEncryption.builder()
                    .armor(true)
                    .withIntegrityCheck(true)
                    .compressionAlgorithm(CompressionAlgorithmTags.ZIP)
                    .symmetricKeyAlgorithm(SymmetricKeyAlgorithmTags.AES_128)
                    .publicKeyLocation(pgpPublicKeyPath)
                    .build();
        }
        if (pgpPassword != null && pgpPrivateKeyPath != null) {
            pgpDecryptor = PgpDecryption.builder()
                    .passCode(pgpPassword.toCharArray())
                    .pgpSecretKeyRingCollection(new PGPSecretKeyRingCollection(
                            PGPUtil.getDecoderStream(new FileInputStream(pgpPrivateKeyPath)),
                            new JcaKeyFingerprintCalculator()))
                    .build();
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
