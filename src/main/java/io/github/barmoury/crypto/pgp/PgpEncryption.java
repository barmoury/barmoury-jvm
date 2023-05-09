package io.github.barmoury.crypto.pgp;

import io.github.barmoury.api.config.PgpConfig;
import io.github.barmoury.util.FileUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.bcpg.CompressionAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.operator.KeyFingerPrintCalculator;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.PGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.PGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.bc.*;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;

import java.io.*;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.SecureRandom;
import java.security.Security;
import java.sql.Date;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Builder
@AllArgsConstructor
public class PgpEncryption {

    static {
        // Add Bouncy castle to JVM
        if (Objects.isNull(Security.getProvider(BouncyCastleProvider.PROVIDER_NAME))) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Builder.Default boolean sign = false;
    @Builder.Default boolean armor = true;
    @Builder.Default int bufferSize = 1 << 16;
    @Builder.Default boolean withIntegrityCheck = true;
    @Builder.Default int compressionAlgorithm = CompressionAlgorithmTags.ZIP;
    @Builder.Default int symmetricKeyAlgorithm = SymmetricKeyAlgorithmTags.AES_256; // TODO make configurable
    @Setter PGPSecretKeyRingCollection pgpSecretKeyRingCollection;
    PgpManager pgpManager;
    String[] publicKeyLocations;
    String[] signingKeyLocations;

    static {
        // Add Bouncy castle to JVM
        if (Objects.isNull(Security.getProvider(BouncyCastleProvider.PROVIDER_NAME))) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    // TODO allow config for Jce and Bce

    public void encrypt(OutputStream encryptOut, InputStream clearIn, long length, List<InputStream> publicKeyIns)
            throws IOException, PGPException {
        PGPDataEncryptorBuilder dataEncryptorBuilder =
                new BcPGPDataEncryptorBuilder(symmetricKeyAlgorithm)
                        .setSecureRandom(new SecureRandom())
                        .setWithIntegrityPacket(withIntegrityCheck);
        PGPEncryptedDataGenerator pgpEncryptedDataGenerator = new PGPEncryptedDataGenerator(dataEncryptorBuilder);
        // Adding public key
        if (pgpManager != null) {
            for (PGPPublicKey publicKey : pgpManager.getPublicKeys()) {
                pgpEncryptedDataGenerator.addMethod(new BcPublicKeyKeyEncryptionMethodGenerator(publicKey));
            }
        } else {
            for (InputStream publicKeyIn : publicKeyIns) {
                pgpEncryptedDataGenerator.addMethod(new BcPublicKeyKeyEncryptionMethodGenerator(
                        getPublicKey(publicKeyIn)));
            }
        }
        if (armor) {
            encryptOut = new ArmoredOutputStream(encryptOut);
        }
        OutputStream cipherOutStream = pgpEncryptedDataGenerator.open(encryptOut, new byte[bufferSize]);
        PGPCompressedDataGenerator compressedDataGenerator =
                new PGPCompressedDataGenerator(compressionAlgorithm);
        try (BCPGOutputStream bcpgOutputStream =
                     new BCPGOutputStream(compressedDataGenerator.open(cipherOutStream))) {
            if (this.sign && pgpManager != null) {
                List<PGPSignatureGenerator> signatureGenerators =
                        pgpManager.createSignatureGenerators(pgpManager.getSecretKeys());
                for (PGPSignatureGenerator signatureGenerator : signatureGenerators) {
                    PGPOnePassSignature onePassSignature =
                            signatureGenerator.generateOnePassVersion(false);
                    onePassSignature.encode(bcpgOutputStream);
                }
                writeLiteralData(clearIn, bcpgOutputStream, signatureGenerators);
                for (PGPSignatureGenerator signatureGenerator : signatureGenerators) {
                    PGPSignature signature = signatureGenerator.generate();
                    signature.encode(bcpgOutputStream);
                }
            }
        }
        // Closing all output streams in sequence
        compressedDataGenerator.close();
        cipherOutStream.close();
        encryptOut.close();
    }

    private void writeLiteralData(InputStream inputStream,
                                  OutputStream outputStream,
                                  List<PGPSignatureGenerator> signatureGenerators) throws IOException {
        // Create literal data generator
        PGPLiteralDataGenerator literalDataGenerator = new PGPLiteralDataGenerator();
        // Write literal data and encode signatures
        try (OutputStream literalOutputStream =
                     literalDataGenerator.open(
                             outputStream,
                             PGPLiteralData.BINARY,
                             PGPLiteralData.CONSOLE,
                             PGPLiteralData.NOW,
                             new byte[bufferSize])) {
            // Read the input stream and encrypt
            byte[] readingBuffer = new byte[bufferSize];
            int readBytes;

            while ((readBytes = inputStream.read(readingBuffer)) >= 0) {
                literalOutputStream.write(readingBuffer, 0, readBytes);
                for (PGPSignatureGenerator signatureGenerator : signatureGenerators) {
                    signatureGenerator.update(readingBuffer, 0, readBytes);
                }
            }
        }
        literalDataGenerator.close();
    }

    public byte[] encrypt(byte[] clearData) throws PGPException, IOException {
        List<InputStream> publicKeyStreams = new ArrayList<>();
        for (String publicKeyLocation : publicKeyLocations) {
            publicKeyStreams.add(FileUtil.fileStream(PgpConfig.getApplicationClass(), publicKeyLocation));
        }
        return encrypt(clearData, publicKeyStreams);
    }

    public byte[] encrypt(byte[] clearData, List<InputStream> publicKeyStreams) throws PGPException, IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(clearData);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        encrypt(outputStream, inputStream, clearData.length, publicKeyStreams);
        return outputStream.toByteArray();
    }

    public byte[] encrypt(String data) throws PGPException, IOException {
        return encrypt(data.getBytes(Charset.defaultCharset()));
    }

    public byte[] encrypt(String data, List<InputStream> publicKeyStreams) throws PGPException, IOException {
        return encrypt(data.getBytes(Charset.defaultCharset()), publicKeyStreams);
    }

    PGPPublicKey getPublicKey(InputStream keyInputStream) throws IOException, PGPException {
        PGPPublicKeyRingCollection pgpPublicKeyRings = new PGPPublicKeyRingCollection(
                PGPUtil.getDecoderStream(keyInputStream), new JcaKeyFingerprintCalculator());
        Iterator<PGPPublicKeyRing> keyRingIterator = pgpPublicKeyRings.getKeyRings();
        while (keyRingIterator.hasNext()) {
            PGPPublicKeyRing pgpPublicKeyRing = keyRingIterator.next();
            Optional<PGPPublicKey> pgpPublicKey = extractPGPKeyFromRing(pgpPublicKeyRing);
            if (pgpPublicKey.isPresent()) {
                return pgpPublicKey.get();
            }
        }
        throw new PGPException("Invalid public key");
    }

    Optional<PGPPublicKey> extractPGPKeyFromRing(PGPPublicKeyRing pgpPublicKeyRing) {
        for (PGPPublicKey publicKey : pgpPublicKeyRing) {
            if (publicKey.isEncryptionKey()) {
                return Optional.of(publicKey);
            }
        }
        return Optional.empty();
    }

    void copyAsLiteralData(BCPGOutputStream outputStream, InputStream in, long length, int bufferSize) throws IOException {
        PGPLiteralDataGenerator lData = new PGPLiteralDataGenerator();
        OutputStream pOut = lData.open(outputStream, PGPLiteralData.BINARY, PGPLiteralData.CONSOLE,
                Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC)), new byte[bufferSize]);
        byte[] buff = new byte[bufferSize];
        try {
            int len;
            long totalBytesWritten = 0L;
            while (totalBytesWritten <= length && (len = in.read(buff)) > 0) {
                pOut.write(buff, 0, len);
                totalBytesWritten += len;
            }
            pOut.close();
        } finally {
            Arrays.fill(buff, (byte) 0);
            in.close();
        }
    }

}
