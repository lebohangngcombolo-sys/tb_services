package com.tradebridge.notification.repository;

import com.tradebridge.common.enums.Channel;
import com.tradebridge.notification.domain.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    /**
     * The latest enabled version of a template for the given channel and
     * language. Falls back to base language (2-letter) if the specific
     * variant does not exist — handled by the caller via
     * {@link #findActiveByCodeAndChannelAndLanguage}.
     */
    @Query("""
            SELECT t FROM NotificationTemplate t
            WHERE t.templateCode = :code
              AND t.channel = :channel
              AND t.languageCode = :language
              AND t.enabled = TRUE
            ORDER BY t.version DESC
            """)
    List<NotificationTemplate> findActiveByCodeAndChannelAndLanguage(
            @Param("code") String templateCode,
            @Param("channel") Channel channel,
            @Param("language") String languageCode);

    @Query("""
            SELECT t FROM NotificationTemplate t
            WHERE t.templateCode = :code AND t.channel = :channel AND t.enabled = TRUE
            ORDER BY t.languageCode ASC, t.version DESC
            """)
    List<NotificationTemplate> findActiveByCodeAndChannel(
            @Param("code") String templateCode, @Param("channel") Channel channel);

    Optional<NotificationTemplate> findFirstByTemplateCodeAndChannelAndLanguageCodeOrderByVersionDesc(
            String templateCode, Channel channel, String languageCode);

    boolean existsByTemplateCodeAndChannelAndLanguageCode(String templateCode,
                                                          Channel channel, String languageCode);
}