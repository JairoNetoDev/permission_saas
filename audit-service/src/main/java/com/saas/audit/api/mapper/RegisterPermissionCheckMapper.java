package com.saas.audit.api.mapper;

import org.springframework.stereotype.Component;

import com.saas.audit.api.dto.RegisterPermissionCheckRequest;
import com.saas.audit.application.command.RegisterPermissionCheckCommand;

@Component
public class RegisterPermissionCheckMapper
        implements Mapper<RegisterPermissionCheckRequest, RegisterPermissionCheckCommand> {

    @Override
    public RegisterPermissionCheckCommand map(RegisterPermissionCheckRequest request) {
        return new RegisterPermissionCheckCommand(
                request.projectId(),
                request.occurredAt(),
                request.routePath(),
                request.httpMethod(),
                request.roleName(),
                request.granted(),
                request.reason(),
                request.durationMs());
    }
}
