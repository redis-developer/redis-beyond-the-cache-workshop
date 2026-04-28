package com.redis.workshop.platform.controlplane.persistence.repository;

import com.redis.workshop.platform.controlplane.persistence.model.AuditEventRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditEventRecordRepository extends JpaRepository<AuditEventRecord, UUID> {

    List<AuditEventRecord> findAllBySessionIdOrderByCreatedAtDesc(String sessionId);

    List<AuditEventRecord> findAllByTargetResourceIdOrderByCreatedAtDesc(String targetResourceId);
}
