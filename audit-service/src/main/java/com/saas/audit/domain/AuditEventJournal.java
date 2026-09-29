package com.saas.audit.domain;

public interface AuditEventJournal {

    void record(AuditEvent event);
}
