package io.github.barmoury.crypto.pgp;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.IteratorUtils;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.bc.BcPGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.operator.KeyFingerPrintCalculator;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.PGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.PGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.bc.*;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class PgpManager {

    PGPSecretKeyRingCollection secretKeyRings;
    PGPPublicKeyRingCollection publicKeyRings;
    @Getter int hashAlgorithmCode = PGPUtil.SHA256;
    Map<Long, PGPPrivateKey> privateKeyMap = new ConcurrentHashMap<>();

    public synchronized Map<Long, Set<String>> addPublicKeys(InputStream inputStream) throws KeyManagementException,
            PGPException, IOException {
        Map<Long, Set<String>> keyUsers = new HashMap<>();
        if (publicKeyRings == null) publicKeyRings = new PGPPublicKeyRingCollection(new ArrayList<>());
        try {
            InputStream decodedStream = PGPUtil.getDecoderStream(inputStream);
            PGPPublicKeyRingCollection keyRingCollection =
                    new BcPGPPublicKeyRingCollection(decodedStream);
            for (Iterator<PGPPublicKeyRing> it = keyRingCollection.getKeyRings(); it.hasNext(); ) {
                PGPPublicKeyRing pgpPublicKeyRing = it.next();
                for (Iterator<PGPPublicKey> iter = pgpPublicKeyRing.getPublicKeys(); iter.hasNext(); ) {
                    PGPPublicKey pgpPublicKey = iter.next();
                    log.debug(String.format("Adding public key %s. Fingerprint: %s.",
                            Long.toHexString(pgpPublicKey.getKeyID()), Arrays.toString(pgpPublicKey.getFingerprint())));
                    Set<String> set = new HashSet<>();
                    pgpPublicKey.getUserIDs().forEachRemaining(set::add);
                    keyUsers.putIfAbsent(pgpPublicKey.getKeyID(), set);
                }
                publicKeyRings = PGPPublicKeyRingCollection.addPublicKeyRing(publicKeyRings, pgpPublicKeyRing);
            }
        } catch (IllegalArgumentException | IOException | PGPException exception) {
            throw new KeyManagementException("Problem adding PGP public key", exception);
        }

        return keyUsers;
    }

    public synchronized Map<Long, Set<String>> addSecretKeys(InputStream inputStream, char[]... passphrases)
            throws KeyManagementException, PGPException, IOException {
        Map<Long, Set<String>> keyUsers = new HashMap<>();
        KeyFingerPrintCalculator keyFingerPrintCalculator = new BcKeyFingerprintCalculator();
        if (secretKeyRings == null) secretKeyRings = new PGPSecretKeyRingCollection(new ArrayList<>());
        try {
            InputStream decodedStream = PGPUtil.getDecoderStream(inputStream);
            PGPSecretKeyRingCollection keyRingCollection = new PGPSecretKeyRingCollection(decodedStream,
                    keyFingerPrintCalculator);
            for (Iterator<PGPSecretKeyRing> it = keyRingCollection.getKeyRings(); it.hasNext(); ) {
                PGPSecretKeyRing pgpSecretKeyRing = it.next();
                Map<PGPSecretKey, PGPPrivateKey> privateKeys = extractPrivateKeys(pgpSecretKeyRing, passphrases);
                for (PGPSecretKey secretKey : privateKeys.keySet()) {
                    Set<String> set = new HashSet<>();
                    secretKey.getUserIDs().forEachRemaining(set::add);
                    keyUsers.putIfAbsent(secretKey.getKeyID(), set);
                    PGPPrivateKey privateKey = privateKeys.get(secretKey);
                    privateKeyMap.put(secretKey.getKeyID(), privateKey);
                    if (!privateKeys.isEmpty() && set.size() > 0) {
                        secretKeyRings = PGPSecretKeyRingCollection.addSecretKeyRing(secretKeyRings, pgpSecretKeyRing);
                    }
                }
            }
        } catch (IOException | PGPException exception) {
            throw new KeyManagementException("Problem adding PGP secret key", exception);
        }
        return keyUsers;
    }

    Map<PGPSecretKey, PGPPrivateKey> extractPrivateKeys(PGPSecretKeyRing secretKeyRing, char[]... passphrases) {
        PGPDigestCalculatorProvider digestCalculatorProvider = new BcPGPDigestCalculatorProvider();
        Map<PGPSecretKey, PGPPrivateKey> privateKeys = new HashMap<>();

        for (char[] passphrase : passphrases) {
            PBESecretKeyDecryptor decryptor =
                    new BcPBESecretKeyDecryptorBuilder(digestCalculatorProvider)
                            .build(passphrase);
            for (Iterator<PGPSecretKey> it = secretKeyRing.getSecretKeys(); it.hasNext(); ) {
                PGPSecretKey secretKey = it.next();
                try {
                    PGPPrivateKey privateKey = secretKey.extractPrivateKey(decryptor);
                    log.debug(
                            String.format("Extracted private key %s from secret key %s",
                                    Long.toHexString(privateKey.getKeyID()),
                                    Long.toHexString(secretKey.getKeyID()))
                    );
                    privateKeys.put(secretKey, privateKey);
                } catch (PGPException exception) {
                    log.debug(
                            String.format("Private key extraction failed for key: %s. Cause: %s",
                                    Long.toHexString(secretKey.getKeyID()),
                                    exception.getMessage())
                    );
                }
            }
        }

        if (privateKeys.isEmpty()) {
            log.warn("Tried to extract private key from secret key ring but failed");
        }

        return privateKeys;
    }

    public Optional<PGPPrivateKey> getPrivateKey(long keyId) {
        return Optional.ofNullable(privateKeyMap.get(keyId));
    }

    public List<PGPSecretKey> getSecretKeys(String... userIds) {
        List<PGPSecretKey> pgpSecretKeys = new ArrayList<>();
        for (String userId : userIds) {
            for (Iterator<PGPSecretKeyRing> it = secretKeyRings.getKeyRings(); it.hasNext();) {
                for (Iterator<PGPSecretKey> it2 = it.next().getSecretKeys(); it2.hasNext();) {
                    PGPSecretKey pgpSecretKey = it2.next();
                    for (Iterator<String> it3 = pgpSecretKey.getUserIDs(); it3.hasNext();) {
                        if (it3.next().equals(userId)) pgpSecretKeys.add(pgpSecretKey);
                    }
                }
            }
        }
        if (pgpSecretKeys.size() != userIds.length) {
            String ids = String.join(",", userIds);
            throw new KeySearchException("Not all public keys were retrieved. User IDs: " + ids);
        }
        return pgpSecretKeys;
    }

    public List<PGPSecretKey> getSecretKeys() {
        List<PGPSecretKey> pgpSecretKeys = new ArrayList<>();
        for (Iterator<PGPSecretKeyRing> it = secretKeyRings.getKeyRings(); it.hasNext();) {
            for (Iterator<PGPSecretKey> it2 = it.next().getSecretKeys(); it2.hasNext();) {
                pgpSecretKeys.add(it2.next());
            }
        }
        return pgpSecretKeys;
    }

    public List<PGPPublicKey> getPublicKeys(String... userIds) {
        List<PGPPublicKey> pgpPublicKeys = new ArrayList<>();
        for (String userId : userIds) {
            for (Iterator<PGPPublicKeyRing> it = publicKeyRings.getKeyRings(); it.hasNext();) {
                for (Iterator<PGPPublicKey> it2 = it.next().getPublicKeys(); it2.hasNext();) {
                    PGPPublicKey pgpPublicKey = it2.next();
                    for (Iterator<String> it3 = pgpPublicKey.getUserIDs(); it3.hasNext();) {
                        if (it3.next().equals(userId)) pgpPublicKeys.add(pgpPublicKey);
                    }
                }
            }
        }
        if (pgpPublicKeys.size() != userIds.length) {
            String ids = String.join(",", userIds);
            throw new KeySearchException("Not all public keys were retrieved. User IDs: " + ids);
        }
        return pgpPublicKeys;
    }

    public List<PGPPublicKey> getPublicKeys() {
        List<PGPPublicKey> pgpPublicKeys = new ArrayList<>();
        for (Iterator<PGPPublicKeyRing> it = publicKeyRings.getKeyRings(); it.hasNext();) {
            for (Iterator<PGPPublicKey> it2 = it.next().getPublicKeys(); it2.hasNext();) {
                pgpPublicKeys.add(it2.next());
            }
        }
        return pgpPublicKeys;
    }

    List<PGPSignatureGenerator> createSignatureGenerators(Collection<PGPSecretKey> signingKeys)
            throws PGPException {
        List<PGPSignatureGenerator> signatureGenerators = new ArrayList<>();
        for (PGPSecretKey signingKey : signingKeys) {
            PGPPublicKey publicKey = signingKey.getPublicKey();
            PGPContentSignerBuilder contentSignerBuilder =
                    new BcPGPContentSignerBuilder(publicKey.getAlgorithm(), hashAlgorithmCode);
            PGPSignatureGenerator signatureGenerator =
                    new PGPSignatureGenerator(contentSignerBuilder);

            long signingKeyId = signingKey.getKeyID();
            PGPPrivateKey privateKey = getPrivateKey(signingKeyId)
                    .orElseThrow(() -> new PgpEncryptionException("Unknown key: " + signingKeyId));

            signatureGenerator.init(PGPSignature.BINARY_DOCUMENT, privateKey);
            for (Iterator<String> it = publicKey.getUserIDs(); it.hasNext(); ) {
                String userId = it.next();
                PGPSignatureSubpacketGenerator subpacketGenerator =
                        new PGPSignatureSubpacketGenerator();
                subpacketGenerator.setSignerUserID(false, userId);
                signatureGenerator.setHashedSubpackets(subpacketGenerator.generate());
            }
            signatureGenerators.add(signatureGenerator);
        }

        return signatureGenerators;
    }

    public void setHashAlgorithmCode(String hashAlgorithm) {
        if (!(hashAlgorithm.equalsIgnoreCase("MD5")
                || hashAlgorithm.equalsIgnoreCase("SHA256")
                || hashAlgorithm.equalsIgnoreCase("SHA512")
                || hashAlgorithm.equalsIgnoreCase("TIGER192")
                || hashAlgorithm.equalsIgnoreCase("RIPEMD160"))) {
            throw new IllegalArgumentException("The hash algorithm (barmoury.crypto.pgp.encryption.sign.hash-algorithm)" +
                    " must be any of MD5, SHA256, SHA512, TIGER192, RIPEMD160");
        }
        this.hashAlgorithmCode = getHashAlgorithmCode(hashAlgorithm);
    }

    public static int getHashAlgorithmCode(String hashAlgorithm) {
        return switch (hashAlgorithm) {
            case "MD5" -> PGPUtil.MD5;
            case "SHA256" -> PGPUtil.SHA256;
            case "SHA512" -> PGPUtil.SHA512;
            case "TIGER192" -> PGPUtil.TIGER_192;
            case "RIPEMD160" -> PGPUtil.RIPEMD160;
            default -> throw new IllegalArgumentException("Unsupported hash algorithm: " + hashAlgorithm);
        };
    }

    public Optional<PGPOnePassSignature> getVerifiableOnePassSignature(PGPOnePassSignatureList onePassSignatures,
                                                                List<PGPPublicKey> signatureVerifyingKeys)
            throws PGPException {
        for (PGPOnePassSignature onePassSignature : onePassSignatures) {
            long signatureKeyId = onePassSignature.getKeyID();

            Optional<PGPPublicKey> possibleVerifyingKey = signatureVerifyingKeys.stream()
                    .filter(key -> key.getKeyID() == signatureKeyId)
                    .peek(key -> log.debug("One-pass signature matches key: " + Long.toHexString(key.getKeyID())))
                    .findAny();

            if (possibleVerifyingKey.isPresent()) {
                PGPPublicKey verifyingKey = possibleVerifyingKey.get();
                onePassSignature.init(new BcPGPContentVerifierBuilderProvider(), verifyingKey);
                return Optional.of(onePassSignature);
            }
        }

        return Optional.empty();
    }

    public boolean verifyAnySignature(PGPSignatureList signatures, PGPOnePassSignature onePassSignature)
            throws PGPException {
        PGPSignature possibleSignature = null;
        for (PGPSignature pgpSignature : signatures) {
            if (pgpSignature.getKeyID() != onePassSignature.getKeyID()) {
                continue;
            }
            log.debug("One-pass matched signature of key: " + Long.toHexString(pgpSignature.getKeyID()));
            possibleSignature = pgpSignature;
            break;
        }
        if (possibleSignature == null) {
            throw new PgpDecryptionException("No matching signature present");
        }
        return onePassSignature.verify(possibleSignature);
    }

    public PGPPublicKeyEncryptedData getDecryptablePublicKeyData(PGPEncryptedDataList encryptedDataList,
                                                                 List<PGPSecretKey> decryptionKeys)
            throws PgpDecryptionException {
        return IteratorUtils.toList(encryptedDataList.getEncryptedDataObjects())
                .stream()
                .filter(data -> data instanceof PGPPublicKeyEncryptedData)
                .map(data -> (PGPPublicKeyEncryptedData) data)
                .peek(data -> log.debug("Data encrypted with key: " + Long.toHexString(data.getKeyID())))
                .filter(data -> decryptionKeyExists(data, decryptionKeys))
                .peek(data -> log.debug(
                                "Decryptable data found. Encrypted with public key: " +  Long.toHexString(data.getKeyID())
                        )
                )
                .findAny()
                .orElseThrow(() -> new PgpDecryptionException("Data stream is not decryptable"));
    }

    private boolean decryptionKeyExists(PGPPublicKeyEncryptedData publicKeyEncryptedData,
                                        List<PGPSecretKey> decryptionKeys) {
        return decryptionKeys.stream()
                .anyMatch(key -> {
                    PGPPublicKey publicKey = key.getPublicKey();
                    return publicKey.getKeyID() == publicKeyEncryptedData.getKeyID();
                });
    }

    public Optional<PGPPrivateKey> findDecryptionKey(PGPPublicKeyEncryptedData publicKeyEncryptedData, 
                                                     List<PGPSecretKey> decryptionKeys) {

        return decryptionKeys.stream()
                .filter(key -> {
                    PGPPublicKey publicKey = key.getPublicKey();
                    boolean privateKeyExists = getPrivateKey(key.getKeyID())
                            .isPresent();
                    return publicKey.getKeyID() == publicKeyEncryptedData.getKeyID() && privateKeyExists;
                })
                .map(key -> getPrivateKey(key.getKeyID()).get())
                .peek(key -> log.debug("Found decryption key: " + Long.toHexString(key.getKeyID())))
                .findAny();
    }

}
