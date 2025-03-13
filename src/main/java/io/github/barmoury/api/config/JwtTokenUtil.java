package io.github.barmoury.api.config;

import io.github.barmoury.api.model.UserDetails;
import io.github.barmoury.crypto.IEncryptor;
import io.github.barmoury.testing.ValueGenerator;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public abstract class JwtTokenUtil {

    String secret;
    static final String BARMOURY_DATA = "BARMOURY_DATA";
    static final String BARMOURY_LANGUAGE = "BARMOURY_LANGUAGE";
    static final String BARMOURY_AUTHORITIES = "BARMOURY_AUTHORITIES";

    public String getSecret() {
        if (secret == null) secret = ValueGenerator.generateRandomString(98);
        return secret;
    }

    public String getSecret(String $) {
        return getSecret();
    }

    public abstract void log(String message, Exception exception);

    public IEncryptor<Object> getEncryptor() {
        return null;
    };

    public Claims getAllClaimsFromToken(String key, String token) {
        String secret = getSecret(key);
        return Jwts.parser().setSigningKey(secret.getBytes(StandardCharsets.UTF_8))
                .parseClaimsJws(token).getBody();
    }

    public String getIdFromToken(String key, String token) {
        Claims claims = getAllClaimsFromToken(key, token);
        return claims.getSubject();
    }

    public List<String> getAuthoritiesFromToken(String key, String token) {
        Claims claims = getAllClaimsFromToken(key, token);
        return (List<String>) claims.get(BARMOURY_AUTHORITIES);
    }

    public <T> T getClaimFromToken(String key, String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(key, token);
        return claimsResolver.apply(claims);
    }

    public Date getExpirationDateFromToken(String key, String token) {
        return getClaimFromToken(key, token, Claims::getExpiration);
    }

    private Boolean isTokenExpired(String key, String token) {
        final Date expiration = getExpirationDateFromToken(key, token);
        return expiration.before(new Date());
    }

    public boolean validate(String key, String token, UserDetails<?> userDetails) {
        final String id = getIdFromToken(key, token);
        return (id.equals(userDetails.getId()) && !isTokenExpired(key, token));
    }

    public boolean validate(String token, UserDetails<?> userDetails) {
        final String id = getIdFromToken(null, token);
        return validate(null, token, userDetails);
    }

    public <T> UserDetails<?> validate(String key, String token) {
        Claims claims = getAllClaimsFromToken(key, token);
        IEncryptor<Object> encryptor = getEncryptor();
        T data = (T) ((encryptor != null)
                ? encryptor.decrypt((String)claims.get(BARMOURY_DATA))
                : claims.get(BARMOURY_DATA));
        List<String> authorities = (List<String>) ((encryptor != null)
                ? encryptor.decrypt((String)claims.get(BARMOURY_AUTHORITIES))
                : claims.get(BARMOURY_AUTHORITIES));
        String language = (String) ((encryptor != null)
                ? encryptor.decrypt((String)claims.get(BARMOURY_LANGUAGE))
                : claims.get(BARMOURY_LANGUAGE));
        String subject = ((encryptor != null)
                ? (String) encryptor.decrypt(claims.getSubject())
                : claims.getSubject());
        return new UserDetails<>(subject, authorities, data, language);
    }

    public <T> UserDetails<?> validate(String token) {
        return validate(null, token);
    }

    public String generateToken(String key, UserDetails<?> userDetails, long tokenExpiryInSeconds) {
        Map<String, Object> claims = new HashMap<>();
        IEncryptor<Object> encryptor = getEncryptor();
        Date expiryDate = new Date(System.currentTimeMillis() + (tokenExpiryInSeconds) * 1000);
        claims.put(BARMOURY_DATA, (encryptor != null
                ? encryptor.encrypt(userDetails.getData())
                : userDetails.getData()));
        claims.put(BARMOURY_AUTHORITIES, (encryptor != null
                ? encryptor.encrypt(userDetails.getAuthoritiesValues())
                : userDetails.getAuthoritiesValues()));
        claims.put(BARMOURY_LANGUAGE, (encryptor != null
                ? encryptor.encrypt(userDetails.getLanguage())
                : userDetails.getLanguage()));
        String subject = (encryptor != null
                ? encryptor.encrypt(userDetails.getUsername())
                : userDetails.getUsername());
        return doGenerateToken(key, claims, subject, expiryDate);
    }

    public String generateToken(UserDetails<?> userDetails, long tokenExpiryInSeconds) {
        return generateToken(null, userDetails, tokenExpiryInSeconds);
    }

    public String doGenerateToken(String key, Map<String, Object> claims, String subject, Date expiryDate) {
        return Jwts.builder().setClaims(claims).setSubject(subject).setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, getSecret(key).getBytes(StandardCharsets.UTF_8))
                .compact();
    }

}
