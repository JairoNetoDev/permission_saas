package com.saas.audit.infrastructure;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface JpaAuditEventRepository extends JpaRepository<AuditEventJpaEntity, UUID> {

    @Query("""
            SELECT event FROM AuditEventJpaEntity event
            WHERE (:type IS NULL OR event.eventType = :type)
                AND (:projectId IS NULL OR event.projectId = :projectId)
                AND (cast(:from as OffsetDateTime) IS NULL OR event.occurredAt >= :from)
                AND (cast(:to as OffsetDateTime) IS NULL OR event.occurredAt <= :to)
            ORDER BY event.occurredAt DESC
            """)
    List<AuditEventJpaEntity> search(String type, UUID projectId, OffsetDateTime from, OffsetDateTime to);

}
