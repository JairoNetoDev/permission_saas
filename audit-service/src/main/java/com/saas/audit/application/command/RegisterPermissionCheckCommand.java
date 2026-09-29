package com.saas.audit.application.command;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RegisterPermissionCheckCommand(
        UUID projectId,
        OffsetDateTime occurredAt,
        String routePath,
        String httpMethod,
        String roleName,
        boolean granted,
        String reason,
        double durationMs) {
}
