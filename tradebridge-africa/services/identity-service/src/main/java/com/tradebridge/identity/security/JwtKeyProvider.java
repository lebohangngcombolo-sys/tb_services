package com.tradebridge.identity.security;

import com.tradebridge.security.config.SecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

/**
 * Loads the platform's JWT signing keys (spec §11 decision 1).
 *
 * <p>Identity owns signing keys; other services verify via the JWKS
 * endpoint exposed by {@link JwksController}. RSA 2048+ is required.
 *
 * <p>Rotation: keys are referenced by {@code kid}. New keys are added by
 * updating the file(s) configured at
 * {@code tradebridge.security.jwt.private-key-location} (and public-key),
 * bumping {@code key-id}, and letting the JWKS cache expire.
 */
@Component
public class JwtKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtKeyProvider.class);

    private final SecurityProperties.Jwt props;
    private final ResourceLoader resourceLoader;

    private volatile PrivateKey privateKey;
    private volatile PublicKey publicKey;

    public JwtKeyProvider(SecurityProperties properties, ResourceLoader resourceLoader) {
        this.props = properties.getJwt();
        this.resourceLoader = resourceLoader;
    }

    public PrivateKey privateKey() {
        PrivateKey local = this.privateKey;
        if (local == null) {
            synchronized (this) {
                if (this.privateKey == null) {
                    this.privateKey = loadPrivateKey();
                }
                local = this.privateKey;
            }
        }
        return local;
    }

    public PublicKey publicKey() {
        PublicKey local = this.publicKey;
        if (local == null) {
            synchronized (this) {
                if (this.publicKey == null) {
                    this.publicKey = loadPublicKey();
                }
                local = this.publicKey;
            }
        }
        return local;
    }

    public String keyId() {
        return props.getKeyId();
    }

    /**
     * Public key as a JWK set for {@code /.well-known/jwks.json}.
     */
    public Map<String, Object> jwks() {
        java.security.interfaces.RSAPublicKey rsaKey =
                (java.security.interfaces.RSAPublicKey) publicKey();
        String n = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(unsignedBytes(rsaKey.getModulus()));
        String e = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(unsignedBytes(rsaKey.getPublicExponent()));
        Map<String, Object> jwk = Map.of(
                "kty", "RSA",
                "use", "sig",
                "alg", props.getSigningAlgorithm(),
                "kid", keyId(),
                "n", n,
                "e", e);
        return Map.of("keys", java.util.List.of(jwk));
    }

    private PrivateKey loadPrivateKey() {
        String location = props.getPrivateKeyLocation();
        if (location == null || location.isBlank()) {
            throw new IllegalStateException(
                    "tradebridge.security.jwt.private-key-location is required by identity-service");
        }
        try {
            Resource resource = resourceLoader.getResource(location);
            try (InputStream in = resource.getInputStream()) {
                String pem = new String(in.readAllBytes())
                        .replace("-----BEGIN PRIVATE KEY-----", "")
                        .replace("-----END PRIVATE KEY-----", "")
                        .replaceAll("\\s", "");
                byte[] der = Base64.getDecoder().decode(pem);
                PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
                return KeyFactory.getInstance("RSA").generatePrivate(spec);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load private key from " + location, e);
        }
    }

    private PublicKey loadPublicKey() {
        String location = props.getPublicKeyLocation();
        if (location == null || location.isBlank()) {
            // Derive from private key material if no public key file is provided
            try {
                PrivateKey priv = privateKey();
                return derivePublicKey(priv);
            } catch (Exception e) {
                throw new IllegalStateException(
                        "tradebridge.security.jwt.public-key-location is required (or derivable)", e);
            }
        }
        try {
            Resource resource = resourceLoader.getResource(location);
            try (InputStream in = resource.getInputStream()) {
                String pem = new String(in.readAllBytes())
                        .replace("-----BEGIN PUBLIC KEY-----", "")
                        .replace("-----END PUBLIC KEY-----", "")
                        .replaceAll("\\s", "");
                byte[] der = Base64.getDecoder().decode(pem);
                X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
                return KeyFactory.getInstance("RSA").generatePublic(spec);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load public key from " + location, e);
        }
    }

    /**
     * Derives the public key from an RSA private key (works because
     * {@code RSAPrivateCrtKey} exposes the modulus and public exponent).
     */
    private static PublicKey derivePublicKey(PrivateKey priv) throws Exception {
        if (!(priv instanceof java.security.interfaces.RSAPrivateCrtKey rsaPriv)) {
            throw new IllegalStateException("Private key is not an RSA CRT key; cannot derive public key");
        }
        return KeyFactory.getInstance("RSA").generatePublic(
                new java.security.spec.RSAPublicKeySpec(rsaPriv.getModulus(), rsaPriv.getPublicExponent()));
    }

    private static byte[] unsignedBytes(java.math.BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            byte[] trimmed = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, trimmed, 0, trimmed.length);
            return trimmed;
        }
        return bytes;
    }
}
