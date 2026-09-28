package com.saas.audit.domain.exception;

import java.time.OffsetDateTime;

public class InvalidAuditPeriodException extends InvalidDataException {

    public InvalidAuditPeriodException(OffsetDateTime from, OffsetDateTime to) {
        super("Invalid audit period: 'from' (" + from + ") is after 'to' (" + to + ")");
    }
}
