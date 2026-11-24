package com.tradebridge.notification.service;

import com.tradebridge.common.enums.Channel;
import com.tradebridge.common.exception.BusinessException;
import com.tradebridge.common.exception.ConflictException;
import com.tradebridge.common.exception.ErrorCode;
import com.tradebridge.common.exception.ResourceNotFoundException;
import com.tradebridge.common.id.UuidV7Generator;
import com.tradebridge.common.pagination.PageMetadata;
import com.tradebridge.common.response.PageResponse;
import com.tradebridge.notification.api.dto.request.CreateTemplateRequest;
import com.tradebridge.notification.api.dto.request.PreviewTemplateRequest;
import com.tradebridge.notification.api.dto.request.UpdateTemplateRequest;
import com.tradebridge.notification.api.dto.response.PreviewResponse;
import com.tradebridge.notification.api.dto.response.TemplateResponse;
import com.tradebridge.notification.domain.NotificationTemplate;
import com.tradebridge.notification.repository.NotificationTemplateRepository;
import com.tradebridge.notification.template.TemplateRenderer;
import com.tradebridge.notification.template.VariableSchemaValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Template CRUD + preview (spec §5.1D).
 *
 * <p>Updates create a new version — the old version is disabled rather
 * than deleted, preserving history (spec §5.4 "Template rendering must
 * escape untrusted input" is enforced by {@link TemplateRenderer}).
 */
@Service
public class TemplateService {

    private final NotificationTemplateRepository repository;
    private final TemplateRenderer renderer;
    private final VariableSchemaValidator schemaValidator;

    public TemplateService(NotificationTemplateRepository repository,
                            TemplateRenderer renderer,
                            VariableSchemaValidator schemaValidator) {
        this.repository = repository;
        this.renderer = renderer;
        this.schemaValidator = schemaValidator;
    }

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public PageResponse<TemplateResponse> list(int page, int size) {
        Page<NotificationTemplate> result = repository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<TemplateResponse> content = result.getContent().stream()
                .map(TemplateService::toResponse)
                .toList();
        PageMetadata meta = PageMetadata.of(result.getNumber(), result.getSize(), result.getTotalElements());
        return PageResponse.of(content, meta);
    }

    @Transactional(readOnly = true)
    public TemplateResponse get(UUID templateId) {
        return toResponse(require(templateId));
    }

    // ------------------------------------------------------------------ writes

    @Transactional
    public TemplateResponse create(CreateTemplateRequest request, UUID actorId) {
        if (repository.existsByTemplateCodeAndChannelAndLanguageCode(
                request.templateCode(), request.channel(),
                request.languageCode().toLowerCase())) {
            throw new ConflictException(ErrorCode.DUPLICATE_RESOURCE,
                    "Template already exists for " + request.templateCode()
                    + "/" + request.channel() + "/" + request.languageCode());
        }
        if (request.channel() == Channel.EMAIL
                && (request.subjectTemplate() == null || request.subjectTemplate().isBlank())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Email templates require a subject");
        }

        NotificationTemplate template = new NotificationTemplate(
                UuidV7Generator.next(),
                request.templateCode(),
                request.channel(),
                request.languageCode(),
                request.subjectTemplate(),
                request.bodyTemplate(),
                request.variablesSchema(),
                request.enabled());
        repository.save(template);
        return toResponse(template);
    }

    @Transactional
    public TemplateResponse update(UUID templateId, UpdateTemplateRequest request, UUID actorId) {
        NotificationTemplate current = require(templateId);

        // Versioning: disable the current row and create a new version
        current.setEnabled(false);
        repository.save(current);

        NotificationTemplate next = new NotificationTemplate(
                UuidV7Generator.next(),
                current.getTemplateCode(),
                current.getChannel(),
                current.getLanguageCode(),
                request.subjectTemplate() != null
                        ? request.subjectTemplate() : current.getSubjectTemplate(),
                request.bodyTemplate(),
                request.variablesSchema() != null
                        ? request.variablesSchema() : current.getVariablesSchema(),
                true);
        next.bumpVersion(current.getVersion() + 1);
        repository.save(next);
        return toResponse(next);
    }

    @Transactional
    public TemplateResponse changeStatus(UUID templateId, boolean enabled, UUID actorId) {
        NotificationTemplate template = require(templateId);
        template.setEnabled(enabled);
        repository.save(template);
        return toResponse(template);
    }

    @Transactional(readOnly = true)
    public PreviewResponse preview(UUID templateId, PreviewTemplateRequest request) {
        NotificationTemplate template = require(templateId);
        schemaValidator.validate(template, request.variables());
        String subject = template.getSubjectTemplate() == null
                ? null : renderer.render(template.getSubjectTemplate(), request.variables());
        String body = renderer.render(template.getBodyTemplate(), request.variables());
        return new PreviewResponse(subject, body);
    }

    // ------------------------------------------------------------------ helpers

    private NotificationTemplate require(UUID templateId) {
        return repository.findById(templateId)
                .orElseThrow(() -> ResourceNotFoundException.of("NotificationTemplate", templateId));
    }

    static TemplateResponse toResponse(NotificationTemplate t) {
        return new TemplateResponse(
                t.getId(), t.getTemplateCode(), t.getChannel().name(), t.getLanguageCode(),
                t.getSubjectTemplate(), t.getBodyTemplate(), t.getVariablesSchema(),
                t.getVersion(), t.isEnabled(), t.getCreatedAt(), t.getUpdatedAt());
    }
}