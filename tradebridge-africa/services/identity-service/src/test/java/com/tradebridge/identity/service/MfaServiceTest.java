package com.tradebridge.identity.service;

import com.tradebridge.common.exception.BusinessException;
import com.tradebridge.identity.api.dto.request.MfaSetupRequest;
import com.tradebridge.identity.api.dto.response.MfaSetupResponse;
import com.tradebridge.identity.config.IdentityProperties;
import com.tradebridge.identity.domain.MfaFactor;
import com.tradebridge.identity.domain.User;
import com.tradebridge.identity.domain.UserCredential;
import com.tradebridge.identity.domain.enums.MfaFactorType;
import com.tradebridge.identity.event.IdentityEventPublisher;
import com.tradebridge.identity.repository.IdentityAuditRecordRepository;
import com.tradebridge.identity.repository.MfaFactorRepository;
import com.tradebridge.identity.repository.RecoveryCodeRepository;
import com.tradebridge.identity.repository.UserCredentialRepository;
import com.tradebridge.identity.repository.UserRepository;
import com.tradebridge.identity.repository.VerificationChallengeRepository;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MfaServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserCredentialRepository credentialRepository;
    @Mock private MfaFactorRepository mfaRepository;
    @Mock private RecoveryCodeRepository recoveryCodeRepository;
    @Mock private VerificationChallengeRepository challengeRepository;
    @Mock private IdentityAuditRecordRepository auditRepository;
    @Mock private IdentityEventPublisher eventPublisher;
    @Mock private SecurityNotificationHandoff notificationHandoff;

    private PasswordEncoder passwordEncoder;
    private MfaService service;
    private IdentityProperties props;
    private User user;
    private final String password = "Aa123456789!";

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        passwordEncoder = new BCryptPasswordEncoder(4);
        props = new IdentityProperties();
        service = new MfaService(userRepository, credentialRepository, mfaRepository,
                recoveryCodeRepository, challengeRepository, auditRepository,
                passwordEncoder, eventPublisher, notificationHandoff, props);

        user = new User(UUID.randomUUID(), "T", "U", "t@e.com", "+27123456789", "ZA", "EN");
        user.markEmailVerified();
        user.activate();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    }

    @Test
    void beginSetup_requiresCorrectPassword() {
        UserCredential cred = new UserCredential(user.getId(), passwordEncoder.encode(password));
        when(credentialRepository.findByUserId(user.getId())).thenReturn(Optional.of(cred));
        when(mfaRepository.findByUserIdAndFactorType(user.getId(), MfaFactorType.TOTP))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.beginTotpSetup(user.getId(),
                new MfaSetupRequest("wrong")))
                .isInstanceOf(com.tradebridge.common.exception.ValidationException.class);
    }

    @Test
    void beginSetup_returnsProvisioningUri() {
        UserCredential cred = new UserCredential(user.getId(), passwordEncoder.encode(password));
        when(credentialRepository.findByUserId(user.getId())).thenReturn(Optional.of(cred));
        when(mfaRepository.findByUserIdAndFactorType(eq(user.getId()), eq(MfaFactorType.TOTP)))
                .thenReturn(Optional.empty());
        when(mfaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        MfaSetupResponse response = service.beginTotpSetup(user.getId(),
                new MfaSetupRequest(password));

        assertThat(response.provisioningUri()).startsWith("otpauth://totp/");
        assertThat(response.provisioningUri()).contains("secret=");
        assertThat(response.issuer()).isEqualTo("TradeBridge Africa");
    }

    @Test
    void verifySetup_acceptsValidCode() throws Exception {
        UserCredential cred = new UserCredential(user.getId(), passwordEncoder.encode(password));
        when(credentialRepository.findByUserId(user.getId())).thenReturn(Optional.of(cred));

        // Begin setup to obtain the secret
        when(mfaRepository.findByUserIdAndFactorType(user.getId(), MfaFactorType.TOTP))
                .thenReturn(Optional.empty());
        when(mfaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        MfaSetupResponse response = service.beginTotpSetup(user.getId(),
                new MfaSetupRequest(password));
        String secret = extractSecret(response.provisioningUri());

        // Generate a valid current code
        DefaultCodeGenerator gen = new DefaultCodeGenerator();
        String code = gen.generate(secret, System.currentTimeMillis() / 30_000L);

        // Make the persisted factor readable again
        MfaFactor factor = new MfaFactor(user.getId(), MfaFactorType.TOTP,
                "ENC:" + java.util.Base64.getEncoder().encodeToString(secret.getBytes()));
        when(mfaRepository.findByUserIdAndFactorType(user.getId(), MfaFactorType.TOTP))
                .thenReturn(Optional.of(factor));
        when(mfaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(recoveryCodeRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

        java.util.List<String> codes = service.verifyTotpSetup(user.getId(), code);

        assertThat(codes).hasSize(10);
        verify(userRepository).save(user);
    }

    @Test
    void restartingPendingSetup_replacesThePersistedSecret() throws Exception {
        UserCredential cred = new UserCredential(user.getId(), passwordEncoder.encode(password));
        MfaFactor pendingFactor = new MfaFactor(user.getId(), MfaFactorType.TOTP,
                "ENC:" + java.util.Base64.getEncoder().encodeToString(
                        "OLDSECRETOLDSECRET".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        when(credentialRepository.findByUserId(user.getId())).thenReturn(Optional.of(cred));
        when(mfaRepository.findByUserIdAndFactorType(user.getId(), MfaFactorType.TOTP))
                .thenReturn(Optional.of(pendingFactor));
        when(mfaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(recoveryCodeRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

        MfaSetupResponse response = service.beginTotpSetup(user.getId(),
                new MfaSetupRequest(password));
        String secret = extractSecret(response.provisioningUri());
        String code = new DefaultCodeGenerator().generate(
                secret, System.currentTimeMillis() / 30_000L);

        assertThat(pendingFactor.getSecretEncrypted())
                .isEqualTo("ENC:" + java.util.Base64.getEncoder().encodeToString(
                        secret.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertThat(service.verifyTotpSetup(user.getId(), code)).hasSize(10);
    }

    @Test
    void disable_requiresValidCode() {
        MfaFactor factor = new MfaFactor(user.getId(), MfaFactorType.TOTP,
                "ENC:" + java.util.Base64.getEncoder().encodeToString("SECRETSECRETSECRET".getBytes()));
        factor.markVerifiedAndEnabled(factor.getSecretEncrypted());
        when(mfaRepository.findByUserIdAndFactorTypeAndEnabledTrue(user.getId(), MfaFactorType.TOTP))
                .thenReturn(Optional.of(factor));
        when(recoveryCodeRepository.findUnusedByUserId(user.getId())).thenReturn(java.util.List.of());

        assertThatThrownBy(() -> service.disable(user.getId(), "000000"))
                .isInstanceOf(com.tradebridge.common.exception.ValidationException.class);
    }

    private static String extractSecret(String uri) {
        int idx = uri.indexOf("secret=");
        int end = uri.indexOf('&', idx);
        return end < 0 ? uri.substring(idx + 7) : uri.substring(idx + 7, end);
    }
}
