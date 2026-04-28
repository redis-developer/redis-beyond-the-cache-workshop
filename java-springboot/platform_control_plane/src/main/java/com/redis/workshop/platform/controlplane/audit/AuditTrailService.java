package com.redis.workshop.platform.controlplane.audit;

import com.redis.workshop.platform.controlplane.persistence.model.AuditActorType;
import com.redis.workshop.platform.controlplane.persistence.model.AuditEventRecord;
import com.redis.workshop.platform.controlplane.persistence.repository.AuditEventRecordRepository;
import com.redis.workshop.platform.controlplane.security.CurrentActorType;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditTrailService {

    private final AuditEventRecordRepository auditEventRecordRepository;

    public AuditTrailService(AuditEventRecordRepository auditEventRecordRepository) {
        this.auditEventRecordRepository = auditEventRecordRepository;
    }

    public AuditEventResponse record(AuditEventPayload payload) {
        AuditEventRecord record = new AuditEventRecord();
        record.setActorId(payload.actorId());
        record.setActorType(toAuditActorType(payload.actorType()));
        record.setActionType(payload.actionType());
        record.setTargetResourceType(payload.targetResourceType());
        record.setTargetResourceId(payload.targetResourceId());
        record.setResult(payload.result());
        record.setReason(payload.reason());
        record.setRequestId(payload.requestId());
        record.setSessionId(payload.sessionId());
        record.setWorkshopId(payload.workshopId());
        record.setReleaseVersion(payload.releaseVersion());
        record.setRuntimeRef(payload.runtimeRef());

        if (payload.createdAt() != null) {
            record.setCreatedAt(payload.createdAt());
        }

        return AuditEventResponse.fromRecord(auditEventRecordRepository.save(record));
    }

    public List<AuditEventResponse> getRecentEvents(int limit) {
        return auditEventRecordRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
            .limit(limit)
            .map(AuditEventResponse::fromRecord)
            .toList();
    }

    public List<AuditEventResponse> getRecentEventsByTargetPrefix(String targetPrefix, int limit) {
        return auditEventRecordRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
            .filter(record -> record.getTargetResourceId() != null && record.getTargetResourceId().startsWith(targetPrefix))
            .limit(limit)
            .map(AuditEventResponse::fromRecord)
            .toList();
    }

    private AuditActorType toAuditActorType(CurrentActorType currentActorType) {
        if (currentActorType == null) {
            return AuditActorType.INTERNAL_SERVICE;
        }

        return switch (currentActorType) {
            case LEARNER -> AuditActorType.LEARNER;
            case ADMIN -> AuditActorType.ADMIN;
            case INTERNAL_SERVICE -> AuditActorType.INTERNAL_SERVICE;
        };
    }
}
