package com.tradebridge.identity.service;

import com.tradebridge.common.exception.UnauthorizedException;
import com.tradebridge.identity.api.dto.response.TokenResponse;
import com.tradebridge.identity.config.IdentityProperties;
import com.tradebridge.identity.domain.RefreshToken;
import com.tradebridge.identity.domain.User;
import com.tradebridge.identity.domain.UserSession;
import com.tradebridge.identity.repository.RefreshTokenRepository;
import com.tradebridge.identity.repository.RoleRepository;
import com.tradebridge.identity.repository.UserRepository;
import com.tradebridge.identity.repository.UserRoleRepository;
import com.tradebridge.identity.repository.UserSessionRepository;
import com.tradebridge.security.jwt.JwtEncoderFactory;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class TokenServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private UserSessionRepository sessionRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private RoleRepository roleRepository;

    @TempDir Path tempDir;

    private TokenService service;
    private User user;
    private UserSession session;
    private RSAPublicKey publicKey;

    @BeforeEach
    void setup() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Generate RSA key pair on the fly
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        publicKey = (RSAPublicKey) kp.getPublic();
        Path privPath = tempDir.resolve("jwt-private.pem");
        Path pubPath = tempDir.resolve("jwt-public.pem");
        writePem(privPath, "PRIVATE KEY", kp.getPrivate().getEncoded());
        writePem(pubPath, "PUBLIC KEY", kp.getPublic().getEncoded());

        IdentityProperties props = new IdentityProperties();
        props.getAccessToken().setTtl(Duration.ofMinutes(15));
        props.getRefreshToken().setTtl(Duration.ofDays(30));

        com.tradebridge.security.config.SecurityProperties secProps =
                new com.tradebridge.security.config.SecurityProperties();
        secProps.getJwt().setPrivateKeyLocation("file:" + privPath);
        secProps.getJwt().setPublicKeyLocation("file:" + pubPath);
        secProps.getJwt().setKeyId("test-kid");
        secProps.getJwt().setIssuer("http://localhost:8081");
        secProps.getJwt().setAudience("tradebridge-clients");

        JwtEncoderFactory encoderFactory = new JwtEncoderFactory(secProps, new DefaultResourceLoader());

        service = new TokenService(encoderFactory, refreshTokenRepository, sessionRepository,
                userRepository, userRoleRepository, roleRepository, props, secProps);

        user = new User(UUID.randomUUID(), "T", "U", "t@e.com", "+27123456789", "ZA", "EN");
        user.markEmailVerified();
        user.activate();

        session = new UserSession(UUID.randomUUID(), user.getId(), "device", "1.2.3.4", "agent",
                Instant.now().plus(Duration.ofDays(30)));

        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRoleRepository.findActiveByUserId(any(), any())).thenReturn(List.of());
    }

    @Test
    void issueTokens_buildsAccessAndRefreshTokens() {
        TokenResponse response = service.issueTokens(user, session);

        assertThat(response.accessToken()).isNotBlank().contains(".");
        var claims = Jwts.parser().verifyWith(publicKey).build()
                .parseSignedClaims(response.accessToken()).getPayload();
        assertThat(claims.getIssuer()).isEqualTo("http://localhost:8081");
        assertThat(claims.getAudience()).contains("tradebridge-clients");
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.expiresInSeconds()).isEqualTo(900);
        assertThat(response.refreshExpiresInSeconds()).isEqualTo(2592000);
        assertThat(response.mfaRequired()).isFalse();
        assertThat(response.user().id()).isEqualTo(user.getId().toString());
    }

    @Test
    void refresh_withValidToken_rotatesAndIssuesNewTokens() {
        TokenResponse original = service.issueTokens(user, session);

        // Simulate the stored refresh row by capturing it via save()
        RefreshToken stored = new RefreshToken(
                user.getId(),
                // must match what service computed; use the captured instance from the save() call
                "", // dummy — we will capture from save() mock
                session.getId(),
                Instant.now().plus(Duration.ofDays(30)));
        // Reset the mock to return our stored entity by token hash
        // Hash of the raw refresh token used by service is not directly accessible; use a spy approach.
        // Simpler: recreate via the service's own mechanism.
        // For unit-test simplicity, we run a full flow.

        // Retrieve the token by replaying issueTokens and intercepting the save
        org.mockito.ArgumentCaptor<RefreshToken> captor =
                org.mockito.ArgumentCaptor.forClass(RefreshToken.class);
        org.mockito.Mockito.verify(refreshTokenRepository).save(captor.capture());
        RefreshToken realStored = captor.getValue();

        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(realStored));
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        TokenResponse refreshed = service.refresh(original.refreshToken());

        assertThat(refreshed.accessToken()).isNotBlank();
        assertThat(refreshed.refreshToken()).isNotBlank().isNotEqualTo(original.refreshToken());
    }

    @Test
    void refresh_reusedTokenRevokesAllSessions() {
        RefreshToken revoked = new RefreshToken(user.getId(), "hash", session.getId(),
                Instant.now().plus(Duration.ofDays(30)));
        revoked.revoke();
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> service.refresh("any-token"))
                .isInstanceOf(UnauthorizedException.class);

        org.mockito.Mockito.verify(sessionRepository)
                .revokeAllByUserId(org.mockito.ArgumentMatchers.eq(user.getId()), any());
    }

    @Test
    void refresh_unknownTokenRejected() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.refresh("bogus"))
                .isInstanceOf(UnauthorizedException.class);
    }

    private static void writePem(Path path, String type, byte[] der) throws Exception {
        String pem = "-----BEGIN " + type + "-----\n"
                + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(der)
                + "\n-----END " + type + "-----\n";
        Files.writeString(path, pem);
    }
}
