package com.surense.supporthub.auth;

import com.surense.supporthub.security.AppUserPrincipal;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    @Value("${app.jwt.access-token-expiry-seconds}")
    private long accessTokenExpirySeconds;

    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;

    @PostConstruct
    void init() throws Exception {
        String privateKeyPem = System.getenv("JWT_PRIVATE_KEY");
        String publicKeyPem = System.getenv("JWT_PUBLIC_KEY");

        if (privateKeyPem == null || privateKeyPem.isBlank()) {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            KeyPair pair = gen.generateKeyPair();
            privateKey = (RSAPrivateKey) pair.getPrivate();
            publicKey = (RSAPublicKey) pair.getPublic();
            log.info("JWT keypair auto-generated (dev mode). Fingerprint: {}", fingerprint(publicKey));
        } else {
            if (publicKeyPem == null || publicKeyPem.isBlank()) {
                throw new IllegalStateException("JWT_PUBLIC_KEY must be set when JWT_PRIVATE_KEY is set");
            }
            privateKey = parsePrivateKey(privateKeyPem);
            publicKey = parsePublicKey(publicKeyPem);
            log.info("JWT keypair loaded from environment. Fingerprint: {}", fingerprint(publicKey));
        }
    }

    public String generateAccessToken(AppUserPrincipal principal) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(principal.userId().toString())
                .claim("username", principal.username())
                .claim("role", principal.role().name())
                .claim("agentId", principal.agentId() != null ? principal.agentId().toString() : null)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenExpirySeconds)))
                .signWith(privateKey)
                .compact();
    }

    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    public long getAccessTokenExpirySeconds() {
        return accessTokenExpirySeconds;
    }

    private RSAPrivateKey parsePrivateKey(String pem) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(stripPem(pem));
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }

    private RSAPublicKey parsePublicKey(String pem) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(stripPem(pem));
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
    }

    private String stripPem(String pem) {
        return pem.replaceAll("-+[^-]+-+", "").replaceAll("\\s", "");
    }

    private String fingerprint(RSAPublicKey key) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(key.getEncoded());
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return "unknown";
        }
    }
}
