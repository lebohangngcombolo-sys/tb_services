package com.tradebridge.customer.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignReviewerRequest(@NotNull UUID reviewerUserId) { }