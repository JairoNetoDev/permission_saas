package com.saas.audit.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.saas.audit.api.dto.AuditEventResponse;
import com.saas.audit.api.dto.RegisterPermissionCheckRequest;
import com.saas.audit.api.dto.SearchAuditEventsRequest;
import com.saas.audit.api.mapper.AuditEventResponseMapper;
import com.saas.audit.api.mapper.RegisterPermissionCheckMapper;
import com.saas.audit.api.mapper.SearchAuditEventsMapper;
import com.saas.audit.application.RegisterPermissionCheckUseCase;
import com.saas.audit.application.SearchAuditEventsUseCase;
import com.saas.audit.domain.AuditEvent;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/audit-events")
@RequiredArgsConstructor
@Tag(name = "Audit", description = "Trilha de auditoria das validacoes de permissao")
public class AuditEventController {

    private final SearchAuditEventsUseCase searchAuditEventsUseCase;

    private final SearchAuditEventsMapper searchAuditEventsMapper;

    private final AuditEventResponseMapper auditEventResponseMapper;

    private final RegisterPermissionCheckUseCase registerPermissionCheckUseCase;

    private final RegisterPermissionCheckMapper registerPermissionCheckMapper;

    @PostMapping("/permission-checks")
    @Operation(summary = "Registra uma validacao de permissao na trilha de auditoria")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Evento registrado"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos ou corpo malformado")
    })
    public ResponseEntity<AuditEventResponse> registerPermissionCheck(
            @RequestBody @Valid RegisterPermissionCheckRequest request) {

        AuditEvent event = registerPermissionCheckUseCase.execute(registerPermissionCheckMapper.map(request));

        return ResponseEntity.status(HttpStatus.CREATED).body(auditEventResponseMapper.map(event));
    }

    @GetMapping
    @Operation(summary = "Lista a trilha de auditoria, do evento mais recente para o mais antigo")
    @ApiResponse(responseCode = "200", description = "Lista devolvida com sucesso")
    public ResponseEntity<List<AuditEventResponse>> searchAuditEvents(@Valid SearchAuditEventsRequest request) {

        List<AuditEvent> events = searchAuditEventsUseCase.execute(searchAuditEventsMapper.map(request));

        return ResponseEntity.ok(events.stream().map(auditEventResponseMapper::map).toList());
    }

}
