package com.tradebridge.notification.service;

import com.tradebridge.common.context.CorrelationContext;
import com.tradebridge.common.enums.Channel;
import com.tradebridge.common.enums.Priority;
import com.tradebridge.common.exception.BusinessException;
import com.tradebridge.common.exception.ErrorCode;
import com.tradebridge.common.exception.ValidationException;
import com.tradebridge.common.id.UuidV7Generator;
import com.tradebridge.common.response.FieldError;
import com.tradebridge.common.util.StringUtils;
import com.tradebridge.notification.api.dto.request.BulkSubmitRequest;
import com.tradebridge.notification.api.dto.request.SubmitEmailRequest;
import com.tradebridge.notification.api.dto.request.SubmitInAppRequest;
import com.tradebridge.notification.api.dto.request.SubmitNotificationRequest;
import com.tradebridge.notification.api.dto.request.SubmitSmsRequest;
import com.tradebridge.notification.api.dto.response.NotificationStatusResponse;
import com.tradebridge.notification.config.NotificationProperties;
import com.tradebridge.notification.domain.Notification;
import com.tradebridge.notification.domain.NotificationLog;
import com.tradebridge.notification.domain.NotificationRecipient;
import com.tradebridge.notification.domain.NotificationTemplate;
import com.tradebridge.notification.domain.enums.DestinationType;
import com.tradebridge.notification.domain.enums.NotificationStatus;
import com.tradebridge.notification.repository.NotificationLogRepository;
import com.tradebridge.notification.repository.NotificationRecipientRepository;
import com.tradebridge.notification.repository.NotificationRepository;
import com.tradebridge.notification.repository.NotificationTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Submission + scheduling entrypoint (spec §5.1A, §5.4).
 *
 * <p>Submission persists the {@link Notification} + {@link NotificationRecipient}
 * + an initial {@link NotificationLog} audit row, and hands off to
 * {@link DeliveryService} which actually sends. Idempotency keys short-circuit
 * duplicate submissions (spec §5.4 "Idempotency keys must be unique within
 * their defined scope").
 *
 * <p><b>OTP templates</b> (identity.mfa.otp, identity.verification,
 * identity.password.reset) never persist the message body — the body is
 * only forwarded to the provider once. This is enforced here (no
 * {@code body_reference} is set for those templates).
 */
@Service
public class NotificationSubmissionService {

    private static final Logger log = LoggerFactory.getLogger(NotificationSubmissionService.class);

    private static final List<String> OTP_SENSITIVE_TEMPLATES = List.of(
            "identity.verification",
            "identity.password.reset",
            "identity.mfa.otp"
    );

    private final NotificationRepository notificationRepository;
    private final NotificationRecipientRepository recipientRepository;
    private final NotificationLogRepository logRepository;
    private final NotificationTemplateRepository templateRepository;
    private final DeliveryService deliveryService;
    private final MandatoryNotificationClassifier classifier;
    private final NotificationProperties properties;

    public NotificationSubmissionService(NotificationRepository notificationRepository,
                                          NotificationRecipientRepository recipientRepository,
                                          NotificationLogRepository logRepository,
                                          NotificationTemplateRepository templateRepository,
                                          DeliveryService deliveryService,
                                          MandatoryNotificationClassifier classifier,
                                          NotificationProperties properties) {
        this.notificationRepository = notificationRepository;
        this.recipientRepository = recipientRepository;
        this.logRepository = logRepository;
        this.templateRepository = templateRepository;
        this.deliveryService = deliveryService;
        this.classifier = classifier;
        this.properties = properties;
    }

    // ------------------------------------------------------------------ single submissions

    @Transactional
    public NotificationStatusResponse submit(SubmitNotificationRequest request) {
        return submitInternal(
                request.channel(), request.templateCode(), request.languageCode(),
                request.recipient(), request.variables(), request.priority(),
                request.recipientUserId(), request.sourceServiceId(),
                request.idempotencyKey(), request.scheduledAt(), request.expiresAt());
    }

    @Transactional
    public NotificationStatusResponse submitEmail(SubmitEmailRequest request) {
        return submitInternal(
                Channel.EMAIL, request.templateCode(), request.languageCode(),
                request.recipient(), request.variables(), request.priority(),
                request.recipientUserId(), null,
                request.idempotencyKey(), request.scheduledAt(), request.expiresAt());
    }

    @Transactional
    public NotificationStatusResponse submitSms(SubmitSmsRequest request) {
        return submitInternal(
                Channel.SMS, request.templateCode(), request.languageCode(),
                request.recipient(), request.variables(), request.priority(),
                request.recipientUserId(), null,
                request.idempotencyKey(), request.scheduledAt(), request.expiresAt());
    }

    @Transactional
    public NotificationStatusResponse submitInApp(SubmitInAppRequest request) {
        return submitInternal(
                Channel.IN_APP, request.templateCode(), request.languageCode(),
                request.recipientUserId().toString(), request.variables(), request.priority(),
                request.recipientUserId(), null,
                request.idempotencyKey(), null, request.expiresAt());
    }

    // ------------------------------------------------------------------ bulk

    @Transactional
    public List<NotificationStatusResponse> submitBulk(BulkSubmitRequest request) {
        if (request.recipients().size() > properties.getBulk().getMaxBatchSize()) {
            throw new ValidationException(
                    "Bulk batch exceeds maximum of " + properties.getBulk().getMaxBatchSize(),
                    List.of(new FieldError("recipients",
                            "Maximum batch size is " + properties.getBulk().getMaxBatchSize())));
        }
        List<NotificationStatusResponse> responses = new ArrayList<>(request.recipients().size());
        for (BulkSubmitRequest.Recipient r : request.recipients()) {
            responses.add(submitInternal(
                    request.channel(), request.templateCode(), request.languageCode(),
                    r.destination(), r.variables(), Priority.NORMAL,
                    r.recipientUserId(), null, r.idempotencyKey(), null, null));
        }
        return responses;
    }

    // ------------------------------------------------------------------ query

    @Transactional(readOnly = true)
    public NotificationStatusResponse getStatus(UUID notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> com.tradebridge.common.exception.ResourceNotFoundException
                        .of("Notification", notificationId));
        return new NotificationStatusResponse(
                n.getId(), n.getChannel().name(), n.getStatus().name(),
                n.getTemplateCode(), n.getCreatedAt(), n.getScheduledAt());
    }

    // ------------------------------------------------------------------ internal

    private NotificationStatusResponse submitInternal(Channel channel,
                                                       String templateCode,
                                                       String languageCode,
                                                       String destination,
                                                       Map<String, Object> variables,
                                                       Priority priority,
                                                       UUID recipientUserId,
                                                       UUID sourceServiceId,
                                                       String idempotencyKey,
                                                       Instant scheduledAt,
                                                       Instant expiresAt) {
        // Idempotency: return the existing record
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = notificationRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                Notification n = existing.get();
                return new NotificationStatusResponse(n.getId(), n.getChannel().name(),
                        n.getStatus().name(), n.getTemplateCode(),
                        n.getCreatedAt(), n.getScheduledAt());
            }
        }

        // Resolve template (latest enabled, correct language with fallback)
        NotificationTemplate template = resolveTemplate(templateCode, channel, languageCode);

        boolean otpSensitive = OTP_SENSITIVE_TEMPLATES.contains(templateCode);
        if (otpSensitive && channel == Channel.IN_APP) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "OTP templates cannot be sent in-app");
        }

        UUID notificationId = UuidV7Generator.next();
        NotificationStatus initialStatus = scheduledAt != null && scheduledAt.isAfter(Instant.now())
                ? NotificationStatus.SCHEDULED
                : NotificationStatus.PENDING;

        Notification notification = new Notification(
                notificationId, recipientUserId, destination,
                templateCode, channel, languageCode,
                template.getSubjectTemplate(),
                initialStatus,
                priority == null ? Priority.NORMAL : priority,
                deriveSourceService(templateCode),
                UUID.fromString(CorrelationContext.currentOrGenerate()),
                idempotencyKey,
                scheduledAt, expiresAt);
        notificationRepository.save(notification);

        // Recipient — masked + hashed
        recipientRepository.save(new NotificationRecipient(
                UuidV7Generator.next(), notificationId, recipientUserId,
                destinationTypeFor(channel),
                maskDestination(channel, destination),
                hashDestination(destination)));

        // Audit
        logRepository.save(new NotificationLog(
                UuidV7Generator.next(), notificationId, null,
                "SUBMITTED", "OK",
                Map.of(
                        "channel", channel.name(),
                        "template", templateCode,
                        "mandatory", classifier.isMandatory(templateCode),
                        "otpSensitive", otpSensitive)));

        // Hand off to delivery — either immediate or scheduled
        if (initialStatus == NotificationStatus.PENDING) {
            deliveryService.deliver(notification, template, variables, destination);
        } else {
            log.debug("Notification {} scheduled for {}", notificationId, scheduledAt);
        }

        return new NotificationStatusResponse(
                notificationId, channel.name(), notification.getStatus().name(),
                templateCode, notification.getCreatedAt(), scheduledAt);
    }

    private NotificationTemplate resolveTemplate(String templateCode, Channel channel,
                                                 String languageCode) {
        String lang = languageCode == null ? "en" : languageCode.toLowerCase();
        var exact = templateRepository.findActiveByCodeAndChannelAndLanguage(
                templateCode, channel, lang);
        if (!exact.isEmpty()) {
            return exact.get(0); // latest version, sorted desc
        }
        // Fall back to base language (first 2 chars)
        String baseLang = lang.length() > 2 ? lang.substring(0, 2) : lang;
        var fallback = templateRepository.findActiveByCodeAndChannelAndLanguage(
                templateCode, channel, baseLang);
        if (!fallback.isEmpty()) {
            return fallback.get(0);
        }
        throw new BusinessException(ErrorCode.NOT_FOUND,
                "No enabled template for " + templateCode + "/" + channel + "/" + languageCode);
    }

    private static String deriveSourceService(String templateCode) {
        int dot = templateCode.indexOf('.');
        return dot < 0 ? "SYSTEM" : templateCode.substring(0, dot).toUpperCase();
    }

    private static DestinationType destinationTypeFor(Channel channel) {
        return switch (channel) {
            case EMAIL -> DestinationType.EMAIL;
            case SMS -> DestinationType.SMS;
            case IN_APP -> DestinationType.IN_APP;
        };
    }

    private static String maskDestination(Channel channel, String destination) {
        return switch (channel) {
            case EMAIL -> StringUtils.maskEmail(destination);
            case SMS -> StringUtils.maskPhone(destination);
            case IN_APP -> "user:" + (destination == null || destination.isBlank() ? "unknown" : "***");
        };
    }

    private static String hashDestination(String destination) {
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(
                    md.digest(destination.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}