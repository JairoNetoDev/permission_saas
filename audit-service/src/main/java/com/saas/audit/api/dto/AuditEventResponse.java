package com.saas.audit.api.dto;

import java.util.UUID;

public record AuditEventResponse(
        UUID id,
        String type,
        UUID projectId,
        String occurredAt,
        String description) {

}
