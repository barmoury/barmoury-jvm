package io.github.barmoury.crypto.pgp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.PublicKeyDataDecryptorFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;

import java.io.*;
import java.nio.charset.Charset;
import java.security.Security;
import java.util.Iterator;
import java.util.Objects;

@Builder
@AllArgsConstructor
public class PgpDecryption {

    static {
        // Add Bouncy castle to JVM
        if (Objects.isNull(Security.getProvider(BouncyCastleProvider.PROVIDER_NAME))) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private char[] passCode;
    final PGPSecretKeyRingCollection pgpSecretKeyRingCollection;

    static {
        // Add Bouncy castle to JVM
        if (Objects.isNull(Security.getProvider(BouncyCastleProvider.PROVIDER_NAME))) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    PGPPrivateKey findSecretKey(long keyID) throws PGPException {
        PGPSecretKey pgpSecretKey = pgpSecretKeyRingCollection.getSecretKey(keyID);
        return pgpSecretKey == null ? null : pgpSecretKey.extractPrivateKey(new JcePBESecretKeyDecryptorBuilder()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME).build(passCode));
    }

    public void decrypt(InputStream encryptedIn, OutputStream clearOut)
            throws PGPException, IOException {
        // Removing armour and returning the underlying binary encrypted stream
        encryptedIn = PGPUtil.getDecoderStream(encryptedIn);
        JcaPGPObjectFactory pgpObjectFactory = new JcaPGPObjectFactory(encryptedIn);

        Object obj = pgpObjectFactory.nextObject();
        //The first object might be a marker packet
        PGPEncryptedDataList pgpEncryptedDataList = (obj instanceof PGPEncryptedDataList)
                ? (PGPEncryptedDataList) obj : (PGPEncryptedDataList) pgpObjectFactory.nextObject();

        PGPPrivateKey pgpPrivateKey = null;
        PGPPublicKeyEncryptedData publicKeyEncryptedData = null;

        Iterator<PGPEncryptedData> encryptedDataItr = pgpEncryptedDataList.getEncryptedDataObjects();
        while (pgpPrivateKey == null && encryptedDataItr.hasNext()) {
            publicKeyEncryptedData = (PGPPublicKeyEncryptedData) encryptedDataItr.next();
            pgpPrivateKey = findSecretKey(publicKeyEncryptedData.getKeyID());
        }

        if (Objects.isNull(publicKeyEncryptedData)) {
            throw new PGPException("Could not generate PGPPublicKeyEncryptedData object");
        }

        if (pgpPrivateKey == null) {
            throw new PGPException("Could Not Extract private key");
        }
        decrypt(clearOut, pgpPrivateKey, publicKeyEncryptedData);
    }

    public byte[] decrypt(byte[] encryptedBytes) throws PGPException, IOException {
        ByteArrayInputStream encryptedIn = new ByteArrayInputStream(encryptedBytes);
        ByteArrayOutputStream clearOut = new ByteArrayOutputStream();
        decrypt(encryptedIn, clearOut);
        return clearOut.toByteArray();
    }

    public byte[] decrypt(String encrypted) throws PGPException, IOException {
        return decrypt(encrypted.getBytes(Charset.defaultCharset()));
    }

    void decrypt(OutputStream clearOut, PGPPrivateKey pgpPrivateKey, PGPPublicKeyEncryptedData publicKeyEncryptedData) throws IOException, PGPException {
        PublicKeyDataDecryptorFactory decryptorFactory = new JcePublicKeyDataDecryptorFactoryBuilder()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME).build(pgpPrivateKey);
        InputStream decryptedCompressedIn = publicKeyEncryptedData.getDataStream(decryptorFactory);

        JcaPGPObjectFactory decCompObjFac = new JcaPGPObjectFactory(decryptedCompressedIn);
        PGPCompressedData pgpCompressedData = (PGPCompressedData) decCompObjFac.nextObject();

        InputStream compressedDataStream = new BufferedInputStream(pgpCompressedData.getDataStream());
        JcaPGPObjectFactory pgpCompObjFac = new JcaPGPObjectFactory(compressedDataStream);

        Object message = pgpCompObjFac.nextObject();

        if (message instanceof PGPLiteralData pgpLiteralData) {
            InputStream decDataStream = pgpLiteralData.getInputStream();
            IOUtils.copy(decDataStream, clearOut);
            clearOut.close();
        } else if (message instanceof PGPOnePassSignatureList) {
            throw new PGPException("Encrypted message contains a signed message not literal data");
        } else {
            throw new PGPException("Message is not a simple encrypted file - Type Unknown");
        }
        // Performing Integrity check
        if (publicKeyEncryptedData.isIntegrityProtected()) {
            if (!publicKeyEncryptedData.verify()) {
                throw new PGPException("Message failed integrity check");
            }
        }
    }

}
