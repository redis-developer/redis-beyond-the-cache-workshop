package com.redis.workshop.platform.controlplane.audit;

import com.redis.workshop.platform.controlplane.persistence.model.AuditActionType;
import com.redis.workshop.platform.controlplane.persistence.model.AuditEventRecord;
import com.redis.workshop.platform.controlplane.persistence.model.AuditResult;
import com.redis.workshop.platform.controlplane.persistence.model.AuditTargetType;
import com.redis.workshop.platform.controlplane.persistence.repository.AuditEventRecordRepository;
import com.redis.workshop.platform.controlplane.security.CurrentActorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditTrailServiceTest {

    @Mock
    private AuditEventRecordRepository auditEventRecordRepository;

    private AuditTrailService auditTrailService;

    @BeforeEach
    void setUp() {
        auditTrailService = new AuditTrailService(auditEventRecordRepository);
    }

    @Test
    void persistsAuditPayloadFields() {
        when(auditEventRecordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AuditEventResponse response = auditTrailService.record(new AuditEventPayload(
            "learner-1",
            CurrentActorType.LEARNER,
            AuditActionType.SESSION_CREATED,
            AuditTargetType.SESSION,
            "sess-001",
            AuditResult.SUCCESS,
            null,
            "req-001",
            "sess-001",
            "1_session_management",
            "current",
            "runtime-001",
            Instant.parse("2026-04-21T10:00:00Z")
        ));

        assertThat(response.actorId()).isEqualTo("learner-1");
        assertThat(response.actionType()).isEqualTo("SESSION_CREATED");
        assertThat(response.runtimeRef()).isEqualTo("runtime-001");
    }
}
