package com.tradebridge.notification.domain;

import com.tradebridge.common.enums.Channel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Notification template (spec §5.2 {@code notification_templates}).
 *
 * <p>Every update creates a new version — existing notifications keep
 * referencing the version that produced them, so a template change cannot
 * retroactively alter sent history (spec §5.4 "Template rendering must
 * escape untrusted input").
 */
@Entity
@Table(name = "notification_templates",
        uniqueConstraints = @UniqueConstraint(name = "uk_notification_templates",
                columnNames = {"template_code", "channel", "language_code", "version"}))
public class NotificationTemplate {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "template_code", nullable = false, length = 100)
    private String templateCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private Channel channel;

    @Column(name = "language_code", nullable = false, length = 5)
    private String languageCode;

    @Column(name = "subject_template", columnDefinition = "text")
    private String subjectTemplate;

    @Column(name = "body_template", nullable = false, columnDefinition = "text")
    private String bodyTemplate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "variables_schema", columnDefinition = "jsonb")
    private Map<String, Object> variablesSchema;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    protected NotificationTemplate() { }

    public NotificationTemplate(UUID id, String templateCode, Channel channel,
                                String languageCode, String subjectTemplate,
                                String bodyTemplate, Map<String, Object> variablesSchema,
                                boolean enabled) {
        this.id = Objects.requireNonNull(id, "id");
        this.templateCode = Objects.requireNonNull(templateCode, "templateCode");
        this.channel = Objects.requireNonNull(channel, "channel");
        this.languageCode = Objects.requireNonNull(languageCode, "languageCode").toLowerCase();
        this.subjectTemplate = subjectTemplate;
        this.bodyTemplate = Objects.requireNonNull(bodyTemplate, "bodyTemplate");
        this.variablesSchema = variablesSchema;
        this.version = 1;
        this.enabled = enabled;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void updateBody(String subjectTemplate, String bodyTemplate,
                           Map<String, Object> variablesSchema) {
        this.subjectTemplate = subjectTemplate;
        this.bodyTemplate = Objects.requireNonNull(bodyTemplate);
        this.variablesSchema = variablesSchema;
    }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void bumpVersion(int newVersion) { this.version = newVersion; }

    public UUID getId() { return id; }
    public String getTemplateCode() { return templateCode; }
    public Channel getChannel() { return channel; }
    public String getLanguageCode() { return languageCode; }
    public String getSubjectTemplate() { return subjectTemplate; }
    public String getBodyTemplate() { return bodyTemplate; }
    public Map<String, Object> getVariablesSchema() { return variablesSchema; }
    public int getVersion() { return version; }
    public boolean isEnabled() { return enabled; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}