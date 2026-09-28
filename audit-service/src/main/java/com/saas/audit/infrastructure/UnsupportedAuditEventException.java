package com.saas.audit.infrastructure;

public class UnsupportedAuditEventException extends RuntimeException {

    public UnsupportedAuditEventException(Class<?> type) {
        super("Tipo de evento de auditoria nao mapeado para persistencia: " + type.getSimpleName());
    }
}
