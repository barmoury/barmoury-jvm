package io.github.barmoury.crypto.pgp;

import io.github.barmoury.api.config.PgpConfig;
import io.github.barmoury.util.FileUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Setter;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.bc.BcPGPObjectFactory;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.PublicKeyDataDecryptorFactory;
import org.bouncycastle.openpgp.operator.bc.BcPublicKeyDataDecryptorFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;

import java.io.*;
import java.nio.charset.Charset;
import java.security.Security;
import java.util.*;

@Builder
@AllArgsConstructor
public class PgpDecryption {

    static {
        // Add Bouncy castle to JVM
        if (Objects.isNull(Security.getProvider(BouncyCastleProvider.PROVIDER_NAME))) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    boolean signed;
    @Setter String passCode;
    @Setter String[] passCodes;
    @Setter PgpManager pgpManager;
    @Setter String[] privateKeyLocations;
    @Builder.Default int bufferSize = 1 << 16;
    @Setter PGPSecretKeyRingCollection pgpSecretKeyRingCollection;

    static {
        // Add Bouncy castle to JVM
        if (Objects.isNull(Security.getProvider(BouncyCastleProvider.PROVIDER_NAME))) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    // I currently have no idea how to initialize multiple key at once
    // so I would just loop through until I find matching PGPSecretKey
    // should not be too CPU task if 1 secret key location is set
    PGPPrivateKey findSecretKey(long keyID) throws PGPException, IOException {
        if (pgpSecretKeyRingCollection == null && privateKeyLocations != null) {
            for (int index = 0; index < privateKeyLocations.length; index++) {
                PGPSecretKey pgpSecretKey = new PGPSecretKeyRingCollection(
                        PGPUtil.getDecoderStream(FileUtil.fileStream(PgpConfig.getApplicationClass(),
                                privateKeyLocations[index])),
                        new JcaKeyFingerprintCalculator()).getSecretKey(keyID);
                if (pgpSecretKey != null) {
                    return pgpSecretKey.extractPrivateKey(new JcePBESecretKeyDecryptorBuilder()
                            .setProvider(BouncyCastleProvider.PROVIDER_NAME).build(passCodes[index].toCharArray()));
                }
            }
            return null;
        }
        if (pgpSecretKeyRingCollection == null) return null;
        PGPSecretKey pgpSecretKey = pgpSecretKeyRingCollection.getSecretKey(keyID);
        return pgpSecretKey == null ? null : pgpSecretKey.extractPrivateKey(new JcePBESecretKeyDecryptorBuilder()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME).build(passCode.toCharArray()));
    }

    public void decrypt(InputStream encryptedIn, OutputStream clearOut) throws PGPException, IOException {
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
        if (this.signed) {
            decryptSigned(encryptedIn, clearOut);
        } else {
            decrypt(encryptedIn, clearOut);
        }
        return clearOut.toByteArray();
    }

    public byte[] decrypt(String encrypted) throws PGPException, IOException {
        return decrypt(encrypted.getBytes(Charset.defaultCharset()));
    }

    void decryptSigned(InputStream cipherStream, OutputStream plainStream) {
        List<PGPPublicKey> verifyingKeys = this.pgpManager.getPublicKeys();
        List<PGPSecretKey> decryptionKeys = this.pgpManager.getSecretKeys();
        try {
            InputStream decodedInputStream = PGPUtil.getDecoderStream(cipherStream);
            PGPObjectFactory pgpObjectFactory = new BcPGPObjectFactory(decodedInputStream);
            Optional<PGPOnePassSignature> possibleOnePassSignature = Optional.empty();
            PGPPublicKeyEncryptedData publicKeyEncryptedData = null;
            boolean signatureVerified = false;

            Object pgpObject;

            while ((pgpObject = pgpObjectFactory.nextObject()) != null) {
                if (pgpObject instanceof PGPEncryptedDataList pgpEncryptedDataList) {
                    publicKeyEncryptedData =
                            this.pgpManager.getDecryptablePublicKeyData(pgpEncryptedDataList, decryptionKeys);
                    PGPPrivateKey decryptionKey =
                            this.pgpManager.findDecryptionKey(publicKeyEncryptedData, decryptionKeys).get();
                    InputStream decryptedDataStream = publicKeyEncryptedData
                            .getDataStream(new BcPublicKeyDataDecryptorFactory(decryptionKey));
                    pgpObjectFactory = new BcPGPObjectFactory(decryptedDataStream);
                } else if (pgpObject instanceof PGPCompressedData pgpCompressedData) {
                    pgpObjectFactory = new BcPGPObjectFactory(pgpCompressedData.getDataStream());
                } else if (pgpObject instanceof PGPOnePassSignatureList pgpOnePassSignatureList) {
                    possibleOnePassSignature =
                            this.pgpManager.getVerifiableOnePassSignature(pgpOnePassSignatureList, verifyingKeys);
                } else if (pgpObject instanceof PGPLiteralData) {
                    PGPOnePassSignature onePassSignature = possibleOnePassSignature
                            .orElseThrow(() -> new PgpSignatureException("No one pass signature present."));
                    readLiteralData(plainStream, (PGPLiteralData) pgpObject, onePassSignature);
                } else if (pgpObject instanceof PGPSignatureList pgpSignatureList) {
                    PGPOnePassSignature onePassSignature = possibleOnePassSignature
                            .orElseThrow(() -> new PgpSignatureException("No one pass signature present."));
                    signatureVerified = this.pgpManager.verifyAnySignature(pgpSignatureList, onePassSignature);
                }
            }

            if (publicKeyEncryptedData == null) {
                throw new PgpDecryptionException("Failed to decrypt the message");
            }
            if (publicKeyEncryptedData.isIntegrityProtected() && !publicKeyEncryptedData.verify()) {
                throw new PgpDecryptionException("Message failed integrity check");
            }
            if (!signatureVerified) {
                throw new PgpSignatureException("Signature not verified");
            }
        } catch (IOException | PGPException exception) {
            throw new PgpDecryptionException("Cipher text reading error", exception);
        }
    }

    void decrypt(OutputStream clearOut, PGPPrivateKey pgpPrivateKey, PGPPublicKeyEncryptedData publicKeyEncryptedData)
            throws IOException, PGPException {
        PublicKeyDataDecryptorFactory decryptorFactory = new JcePublicKeyDataDecryptorFactoryBuilder()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME).build(pgpPrivateKey);
        InputStream decryptedCompressedIn = publicKeyEncryptedData.getDataStream(decryptorFactory);

        PGPObjectFactory decCompObjFac = new JcaPGPObjectFactory(decryptedCompressedIn);
        PGPCompressedData pgpCompressedData = (PGPCompressedData) decCompObjFac.nextObject();

        InputStream compressedDataStream = new BufferedInputStream(pgpCompressedData.getDataStream());
        PGPObjectFactory pgpCompObjFac = new JcaPGPObjectFactory(compressedDataStream);
        Optional<PGPOnePassSignature> possibleOnePassSignature = Optional.empty();

        boolean signatureVerified = !signed;
        Object message = pgpCompObjFac.nextObject();
        do {
            /*if (message instanceof PGPEncryptedDataList pgpEncryptedDataList) {
                publicKeyEncryptedData =
                        getDecryptablePublicKeyData(
                                (PGPEncryptedDataList) pgpObject, decryptionKeys
                        );
                PGPPrivateKey decryptionKey =
                        findDecryptionKey(publicKeyEncryptedData, decryptionKeys).get();
                InputStream decryptedDataStream =
                        publicKeyEncryptedData.getDataStream(new BcPublicKeyDataDecryptorFactory(decryptionKey));
                pgpCompObjFac = new BcPGPObjectFactory(decryptedDataStream);
            } else */if (message instanceof PGPCompressedData pgpCompressedData1) {
                pgpCompObjFac = new BcPGPObjectFactory(pgpCompressedData1.getDataStream());
            } else if (message instanceof PGPLiteralData pgpLiteralData) {
                PGPOnePassSignature onePassSignature;
                if (signed) {
                    onePassSignature = possibleOnePassSignature.orElseThrow(
                            () -> new PgpSignatureException("No one pass signature present.")
                    );
                    readLiteralData(clearOut, (PGPLiteralData) message, onePassSignature);
                    continue;
                }
                InputStream decDataStream = pgpLiteralData.getInputStream();
                IOUtils.copy(decDataStream, clearOut);
                clearOut.close();
            } else if (message instanceof PGPOnePassSignatureList) {
                if (pgpManager == null || !signed) {
                    throw new PGPException("Encrypted message contains a signed message not literal data");
                }
                possibleOnePassSignature = pgpManager.getVerifiableOnePassSignature(
                        (PGPOnePassSignatureList) message, pgpManager.getPublicKeys());
            } else if (message instanceof PGPSignatureList pgpSignatureList) {
                if (pgpManager == null || !signed) {
                    throw new PGPException("Encrypted message contains a signed message not literal data");
                }
                PGPOnePassSignature onePassSignature = possibleOnePassSignature
                        .orElseThrow(() -> new PgpSignatureException("No one pass signature present."));
                signatureVerified = pgpManager.verifyAnySignature(pgpSignatureList, onePassSignature);
            } else {
                throw new PGPException("Message is not a simple encrypted file - Type Unknown");
            }
        } while ((message = pgpCompObjFac.nextObject()) != null);

        // Performing Integrity check
        if (publicKeyEncryptedData.isIntegrityProtected()) {
            if (!publicKeyEncryptedData.verify()) {
                throw new PGPException("Message failed integrity check");
            }
        }

        // validate signature verification
        if (!signatureVerified) {
            throw new PgpSignatureException("Signature not verified");
        }
    }

    void readLiteralData(OutputStream plainMessage, PGPLiteralData literalData, PGPOnePassSignature onePassSignature)
            throws IOException {
        InputStream dataStream = literalData.getDataStream();
        byte[] buffer = new byte[bufferSize];
        int readBytes;
        while ((readBytes = dataStream.read(buffer)) >= 0) {
            onePassSignature.update(buffer, 0, readBytes);
            plainMessage.write(buffer, 0, readBytes);
        }
    }

}
