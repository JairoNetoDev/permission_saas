package com.saas.audit.application;

import org.springframework.stereotype.Service;

import com.saas.audit.application.command.RegisterPermissionCheckCommand;
import com.saas.audit.domain.AuditEvent;
import com.saas.audit.domain.AuditEventJournal;
import com.saas.audit.domain.AuditEventRepository;
import com.saas.audit.domain.PermissionCheckEvent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegisterPermissionCheckUseCase {

    private final AuditEventRepository auditEventRepository;
    private final AuditEventJournal auditEventJournal;

    public AuditEvent execute(RegisterPermissionCheckCommand command) {
        AuditEvent saved = auditEventRepository.save(PermissionCheckEvent.builder()
                .projectId(command.projectId())
                .occurredAt(command.occurredAt())
                .routePath(command.routePath())
                .httpMethod(command.httpMethod().toUpperCase())
                .roleName(command.roleName())
                .granted(command.granted())
                .reason(command.reason())
                .durationMs(command.durationMs())
                .build());

        auditEventJournal.record(saved);

        return saved;
    }
}