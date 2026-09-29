package com.saas.audit.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record RegisterPermissionCheckRequest(

        @NotNull UUID projectId,

        @NotNull OffsetDateTime occurredAt,

        @NotBlank @Size(max = 255) @Pattern(regexp = "^/.*", message = "must start with /") String routePath,

        @NotBlank @Pattern(regexp = "(?i)GET|POST|PUT|PATCH|DELETE", message = "must be a valid HTTP method") String httpMethod,

        @NotBlank @Size(max = 80) String roleName,

        @NotNull Boolean granted,

        @Size(max = 255) String reason,

        @NotNull @PositiveOrZero Double durationMs) {
}
