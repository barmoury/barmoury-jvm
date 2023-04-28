package io.github.barmoury.api.config;

import io.github.barmoury.api.model.UserDetails;
import io.github.barmoury.crypto.Encryptor;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.logging.log4j.Logger;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.function.Function;

public abstract class JwtTokenUtil {

    static final String BARMOURY_DATA = "BARMOURY_DATA";
    static final String BARMOURY_AUTHORITIES = "BARMOURY_AUTHORITIES";

    public abstract String getSecret();

    public abstract Logger getLogger();

    public Encryptor<Object> getEncryptor() {
        return null;
    };

    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parser().setSigningKey(getSecret()).parseClaimsJws(token).getBody();
    }

    public String getIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.getSubject();
    }

    public List<String> getAuthoritiesFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return (List<String>) claims.get(BARMOURY_AUTHORITIES);
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    public boolean validate(String token, UserDetails<?> userDetails) {
        final String id = getIdFromToken(token);
        return (id.equals(userDetails.getId()) && !isTokenExpired(token));
    }

    @SuppressWarnings("unchecked")
    public <T> UserDetails<?> validate(String token) {
        Claims claims = getAllClaimsFromToken(token);
        Encryptor<Object> encryptor = getEncryptor();
        T data = (T) ((encryptor != null)
                ? getEncryptor().decrypt((String)claims.get(BARMOURY_DATA))
                : claims.get(BARMOURY_DATA));
        List<String> authorities = (List<String>) ((encryptor != null)
                ? getEncryptor().decrypt((String)claims.get(BARMOURY_AUTHORITIES))
                : claims.get(BARMOURY_AUTHORITIES));
        String subject = ((encryptor != null)
                ? (String) getEncryptor().decrypt(claims.getSubject())
                : claims.getSubject());
        return new UserDetails<>(subject, authorities, data);
    }

    public String generateToken(UserDetails<?> userDetails, long tokenExpiryInSeconds) {
        Map<String, Object> claims = new HashMap<>();
        Encryptor<Object> encryptor = getEncryptor();
        Date expiryDate = new Date(System.currentTimeMillis() + (tokenExpiryInSeconds) * 1000);
        claims.put(BARMOURY_DATA, (encryptor != null
                ? encryptor.encrypt(userDetails.getData())
                : userDetails.getData()));
        claims.put(BARMOURY_AUTHORITIES, (encryptor != null
                ? encryptor.encrypt(userDetails.getAuthoritiesValues())
                : userDetails.getAuthoritiesValues()));
        String subject = (encryptor != null
                ? encryptor.encrypt(userDetails.getUsername())
                : userDetails.getUsername());
        return doGenerateToken(claims, subject, expiryDate);
    }

    public String doGenerateToken(Map<String, Object> claims, String subject, Date expiryDate) {
        return Jwts.builder().setClaims(claims).setSubject(subject).setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, getSecret()).compact();
    }

    public Optional<RSAPublicKey> getParsedPublicKey() {
        String publicKey = "";
        try {
            X509EncodedKeySpec keySpecX509 = new X509EncodedKeySpec(publicKey.getBytes());
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPublicKey pubKey = (RSAPublicKey) keyFactory.generatePublic(keySpecX509);
            return Optional.of(pubKey);

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            getLogger().error("Exception block | Public key parsing error ", e);
            return Optional.empty();
        }
    }

}
