package com.tradebridge.identity.service;

import com.tradebridge.common.exception.BusinessException;
import com.tradebridge.common.exception.ErrorCode;
import com.tradebridge.common.exception.UnauthorizedException;
import com.tradebridge.identity.api.dto.request.LoginRequest;
import com.tradebridge.identity.api.dto.response.TokenResponse;
import com.tradebridge.identity.config.IdentityProperties;
import com.tradebridge.identity.domain.User;
import com.tradebridge.identity.domain.UserCredential;
import com.tradebridge.identity.domain.UserSession;
import com.tradebridge.identity.domain.enums.UserStatus;
import com.tradebridge.identity.mapper.UserMapper;
import com.tradebridge.identity.repository.IdentityAuditRecordRepository;
import com.tradebridge.identity.repository.RoleRepository;
import com.tradebridge.identity.repository.UserCredentialRepository;
import com.tradebridge.identity.repository.UserRepository;
import com.tradebridge.identity.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthenticationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserCredentialRepository credentialRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private IdentityAuditRecordRepository auditRepository;
    @Mock private SessionService sessionService;
    @Mock private TokenService tokenService;

    private PasswordEncoder passwordEncoder;
    private AuthenticationService service;

    private final String email = "user@example.com";
    private final String password = "Aa123456789!";
    private User user;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        passwordEncoder = new BCryptPasswordEncoder(4); // fast hash for tests
        IdentityProperties props = new IdentityProperties();
        props.getLockout().setThreshold(5);
        props.getLockout().setLockDuration(Duration.ofMinutes(15));

        service = new AuthenticationService(userRepository, credentialRepository,
                userRoleRepository, roleRepository, auditRepository,
                passwordEncoder, sessionService, tokenService,
                new UserMapper(), props);

        user = new User(UUID.randomUUID(), "Test", "User", email, "+27123456789", "ZA", "EN");
        user.markEmailVerified();
        user.activate();
    }

    @Test
    void successfulLogin_issuesTokensAndResetsLockout() {
        UserCredential cred = new UserCredential(user.getId(), passwordEncoder.encode(password));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(credentialRepository.findByUserId(user.getId())).thenReturn(Optional.of(cred));

        UserSession session = new UserSession(UUID.randomUUID(), user.getId(), "device", "1.2.3.4", "agent", java.time.Instant.now().plusSeconds(60));
        when(sessionService.createSession(any(), any(), any(), any())).thenReturn(session);

        TokenResponse expected = new TokenResponse("access", "Bearer", 900, "refresh", 2592000, false, null,
                new TokenResponse.UserSummary(user.getId().toString(), email, "Test", "User", java.util.List.of("CUSTOMER")));
        when(tokenService.issueTokens(user, session)).thenReturn(expected);

        TokenResponse response = service.login(new LoginRequest(email, password, "device", "agent"));

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(cred.getFailedAttempts()).isZero();
        verify(credentialRepository).save(cred);
    }

    @Test
    void failedPassword_incrementsCounterAndThrows() {
        UserCredential cred = new UserCredential(user.getId(), passwordEncoder.encode(password));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(credentialRepository.findByUserId(user.getId())).thenReturn(Optional.of(cred));

        assertThatThrownBy(() -> service.login(new LoginRequest(email, "Wrong!", null, null)))
                .isInstanceOf(UnauthorizedException.class);

        assertThat(cred.getFailedAttempts()).isEqualTo(1);
        verify(credentialRepository).save(cred);
        verify(sessionService, never()).createSession(any(), any(), any(), any());
    }

    @Test
    void lockedAccount_rejectedBeforePasswordCheck() {
        UserCredential cred = new UserCredential(user.getId(), passwordEncoder.encode(password));
        cred.recordFailedAttempt(5, Duration.ofMinutes(15));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(credentialRepository.findByUserId(user.getId())).thenReturn(Optional.of(cred));

        assertThatThrownBy(() -> service.login(new LoginRequest(email, password, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_LOCKED);
    }

    @Test
    void suspendedAccount_rejected() {
        user.suspend();
        UserCredential cred = new UserCredential(user.getId(), passwordEncoder.encode(password));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(credentialRepository.findByUserId(user.getId())).thenReturn(Optional.of(cred));

        assertThatThrownBy(() -> service.login(new LoginRequest(email, password, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_SUSPENDED);
    }

    @Test
    void pendingVerificationAccount_rejected() {
        user = new User(UUID.randomUUID(), "Test", "User", email, "+27123456789", "ZA", "EN");
        // remains PENDING_VERIFICATION
        UserCredential cred = new UserCredential(user.getId(), passwordEncoder.encode(password));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(credentialRepository.findByUserId(user.getId())).thenReturn(Optional.of(cred));

        assertThatThrownBy(() -> service.login(new LoginRequest(email, password, null, null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void loginByPhone_works() {
        UserCredential cred = new UserCredential(user.getId(), passwordEncoder.encode(password));
        when(userRepository.findByPhoneNumber("+27123456789")).thenReturn(Optional.of(user));
        when(credentialRepository.findByUserId(user.getId())).thenReturn(Optional.of(cred));

        UserSession session = new UserSession(UUID.randomUUID(), user.getId(), null, null, null, java.time.Instant.now().plusSeconds(60));
        when(sessionService.createSession(any(), any(), any(), any())).thenReturn(session);
        when(tokenService.issueTokens(eq(user), eq(session)))
                .thenReturn(new TokenResponse("a", "Bearer", 900, "r", 60, false, null,
                        new TokenResponse.UserSummary(user.getId().toString(), email, "T", "U", java.util.List.of())));

        TokenResponse response = service.login(new LoginRequest("+27123456789", password, null, null));
        assertThat(response.accessToken()).isEqualTo("a");
    }
}
