package com.tradebridge.security.jwt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradebridge.security.config.SecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Supplies RSA public keys for JWT verification.
 *
 * <p>Two modes:
 * <ul>
 *   <li><b>JWKS URI</b> — fetches the JWKS document (identity-service's
 *       {@code /.well-known/jwks.json}) and caches keys by {@code kid}.
 *       Supports rotation: unknown {@code kid} triggers an immediate refresh.</li>
 *   <li><b>Static file</b> — loads a single PEM public key
 *       ({@code tradebridge.security.jwt.public-key-location}).</li>
 * </ul>
 *
 * <p>Thread-safe. The lock guards the fetch-refresh cycle so that a burst of
 * requests on an unknown {@code kid} does not fan out to N network calls.
 */
public class JwksProvider {

    private static final Logger log = LoggerFactory.getLogger(JwksProvider.class);
    private static final Duration FETCH_TIMEOUT = Duration.ofSeconds(5);

    private final SecurityProperties.Jwt props;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private final Map<String, RSAPublicKey> keysByKid = new ConcurrentHashMap<>();
    private final ReentrantLock refreshLock = new ReentrantLock();
    private volatile Instant lastRefreshAt = Instant.EPOCH;

    // Static-file mode
    private volatile RSAPublicKey staticKey;
    private volatile String staticKid;

    public JwksProvider(SecurityProperties.Jwt props,
                        ResourceLoader resourceLoader,
                        ObjectMapper objectMapper) {
        this.props = props;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(FETCH_TIMEOUT)
                .build();
    }

    /**
     * Returns the public key for the given {@code kid}. If the kid is not known
     * and a JWKS URI is configured, attempts one refresh.
     */
    public RSAPublicKey getKey(String kid) {
        if (props.getPublicKeyLocation() != null) {
            return loadStaticKey();
        }
        if (kid == null) {
            throw new IllegalStateException("JWT header missing 'kid' and no static public key is configured");
        }
        RSAPublicKey key = keysByKid.get(kid);
        if (key != null) {
            return key;
        }
        // Unknown kid — refresh once
        refresh();
        key = keysByKid.get(kid);
        if (key == null) {
            throw new IllegalStateException("Unknown JWT key id: " + kid);
        }
        return key;
    }

    /**
     * Force a JWKS refresh. Guards against concurrent refreshes.
     */
    public void refresh() {
        if (props.getJwksUri() == null || props.getJwksUri().isBlank()) {
            return;
        }
        refreshLock.lock();
        try {
            if (Instant.now().isBefore(lastRefreshAt.plus(props.getRefreshJwksBeforeExpiry()))) {
                // Another thread just refreshed
                return;
            }
            doFetch();
            lastRefreshAt = Instant.now();
        } finally {
            refreshLock.unlock();
        }
    }

    private void doFetch() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(props.getJwksUri()))
                    .timeout(FETCH_TIMEOUT)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("JWKS fetch returned HTTP " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode keys = root.path("keys");
            if (!keys.isArray()) {
                throw new IllegalStateException("JWKS payload missing 'keys' array");
            }
            Map<String, RSAPublicKey> fresh = new ConcurrentHashMap<>();
            for (JsonNode key : keys) {
                String kid = key.path("kid").asText(null);
                String kty = key.path("kty").asText(null);
                if (kid == null || !"RSA".equals(kty)) {
                    continue;
                }
                RSAPublicKey parsed = parseRsaKey(key.path("n").asText(), key.path("e").asText());
                fresh.put(kid, parsed);
            }
            keysByKid.clear();
            keysByKid.putAll(fresh);
            log.info("JWKS refreshed: {} keys registered", fresh.size());
        } catch (Exception ex) {
            log.warn("JWKS refresh failed ({}), keeping previous keys", ex.getMessage());
            // Do NOT clear keys on transient failure — previous keys still valid
        }
    }

    private RSAPublicKey loadStaticKey() {
        if (staticKey != null) {
            return staticKey;
        }
        synchronized (this) {
            if (staticKey != null) {
                return staticKey;
            }
            try {
                Resource resource = resourceLoader.getResource(props.getPublicKeyLocation());
                try (InputStream in = resource.getInputStream()) {
                    String pem = new String(in.readAllBytes())
                            .replace("-----BEGIN PUBLIC KEY-----", "")
                            .replace("-----END PUBLIC KEY-----", "")
                            .replaceAll("\\s", "");
                    byte[] der = Base64.getDecoder().decode(pem);
                    // PEM RSA public key is SubjectPublicKeyInfo; use X509 parser
                    var keySpec = new java.security.spec.X509EncodedKeySpec(der);
                    this.staticKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(keySpec);
                    this.staticKid = props.getKeyId();
                    log.info("Loaded static public key from {}", props.getPublicKeyLocation());
                    return staticKey;
                }
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load public key from " + props.getPublicKeyLocation(), e);
            }
        }
    }

    private static RSAPublicKey parseRsaKey(String nB64, String eB64) throws Exception {
        BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(nB64));
        BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(eB64));
        return (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(new RSAPublicKeySpec(modulus, exponent));
    }
}
