package com.saas.audit.api.mapper;

import org.springframework.stereotype.Component;

import com.saas.audit.api.dto.AuditEventResponse;
import com.saas.audit.domain.AuditEvent;

@Component
public class AuditEventResponseMapper implements Mapper<AuditEvent, AuditEventResponse> {

    @Override
    public AuditEventResponse map(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.type(),
                event.getProjectId(),
                event.getOccurredAt() != null ? event.getOccurredAt().toString() : null,
                event.describe());
    }
}
