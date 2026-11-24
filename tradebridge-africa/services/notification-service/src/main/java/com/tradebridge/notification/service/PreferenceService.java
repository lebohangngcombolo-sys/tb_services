package com.tradebridge.notification.service;

import com.tradebridge.common.enums.Channel;
import com.tradebridge.common.id.UuidV7Generator;
import com.tradebridge.notification.api.dto.request.ChannelPreferenceRequest;
import com.tradebridge.notification.api.dto.request.UpdatePreferencesRequest;
import com.tradebridge.notification.api.dto.response.PreferenceResponse;
import com.tradebridge.notification.domain.NotificationPreference;
import com.tradebridge.notification.repository.NotificationPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Per-user notification preferences (spec §5.1C).
 *
 * <p>Returned preference rows always include all three channels — missing
 * rows default to enabled for EMAIL/IN_APP, disabled for SMS (opt-in).
 * Security-critical channels are marked {@code mandatory=true} in the
 * response (spec §5.4), but preferences can still be updated for
 * non-mandatory channels.
 */
@Service
public class PreferenceService {

    private final NotificationPreferenceRepository repository;
    private final MandatoryNotificationClassifier classifier;

    public PreferenceService(NotificationPreferenceRepository repository,
                              MandatoryNotificationClassifier classifier) {
        this.repository = repository;
        this.classifier = classifier;
    }

    @Transactional(readOnly = true)
    public List<PreferenceResponse> get(UUID userId) {
        Map<Channel, NotificationPreference> byChannel = repository.findByUserId(userId).stream()
                .collect(Collectors.toMap(NotificationPreference::getChannel, p -> p));

        return List.of(
                response(Channel.EMAIL, byChannel.get(Channel.EMAIL), true),
                response(Channel.SMS, byChannel.get(Channel.SMS), false),
                response(Channel.IN_APP, byChannel.get(Channel.IN_APP), true));
    }

    @Transactional
    public List<PreferenceResponse> update(UUID userId, UpdatePreferencesRequest request) {
        for (ChannelPreferenceRequest pref : request.preferences()) {
            upsert(userId, pref);
        }
        return get(userId);
    }

    @Transactional
    public PreferenceResponse updateChannel(UUID userId, String channelName,
                                             ChannelPreferenceRequest request) {
        Channel channel = Channel.valueOf(channelName.toUpperCase());
        if (request.channel() != channel) {
            throw new com.tradebridge.common.exception.ValidationException(
                    "Channel mismatch",
                    List.of(new com.tradebridge.common.response.FieldError(
                            "channel", "Path channel does not match body channel")));
        }
        NotificationPreference stored = upsert(userId, request);
        return new PreferenceResponse(stored.getChannel().name(), stored.isEnabled(),
                stored.getLanguageCode(), false);
    }

    /**
     * Effective preference used by the delivery pipeline. Returns true if
     * the notification should be sent on this channel regardless of the
     * user's marketing preference (mandatory) OR the user has enabled it.
     */
    @Transactional(readOnly = true)
    public boolean shouldSend(UUID userId, Channel channel, String templateCode) {
        if (classifier.isMandatory(templateCode)) {
            return true;
        }
        return repository.findByUserIdAndChannel(userId, channel)
                .map(NotificationPreference::isEnabled)
                .orElse(defaultEnabled(channel));
    }

    private boolean defaultEnabled(Channel channel) {
        return channel == Channel.EMAIL || channel == Channel.IN_APP;
    }

    private NotificationPreference upsert(UUID userId, ChannelPreferenceRequest pref) {
        NotificationPreference stored = repository.findByUserIdAndChannel(userId, pref.channel())
                .orElseGet(() -> new NotificationPreference(
                        UuidV7Generator.next(), userId, pref.channel(),
                        pref.enabled(), pref.languageCode()));
        stored.update(pref.enabled(), pref.languageCode());
        return repository.save(stored);
    }

    private static PreferenceResponse response(Channel channel, NotificationPreference stored,
                                                boolean defaultEnabled) {
        if (stored == null) {
            return new PreferenceResponse(channel.name(), defaultEnabled, null, false);
        }
        return new PreferenceResponse(channel.name(), stored.isEnabled(),
                stored.getLanguageCode(), false);
    }
}