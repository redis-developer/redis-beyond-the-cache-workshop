package com.redis.workshop.platform.controlplane.persistence.repository;

import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionMode;
import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionRecord;
import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionState;
import com.redis.workshop.platform.controlplane.persistence.model.WorkspaceCleanupState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisPlatformSessionRecordRepositoryTest {

    private static final PlatformSessionRecordRedisProperties PROPERTIES =
        new PlatformSessionRecordRedisProperties("test:sessions:", "test:sessions:index");

    @Mock
    private RedisTemplate<String, PlatformSessionRecord> redisTemplate;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, PlatformSessionRecord> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    private RedisPlatformSessionRecordRepository repository;

    @BeforeEach
    void setUp() {
        repository = new RedisPlatformSessionRecordRepository(redisTemplate, stringRedisTemplate, PROPERTIES);
    }

    @Test
    void savesRecordAndIndexesSessionId() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        PlatformSessionRecord record = sessionRecord(
            "sess-001",
            "learner@example.com",
            PlatformSessionState.READY,
            Instant.parse("2026-04-21T10:00:00Z")
        );

        repository.save(record);

        verify(valueOperations).set("test:sessions:sess-001", record);
        verify(setOperations).add("test:sessions:index", "sess-001");
    }

    @Test
    void filtersAndSortsRecordsFromRedisIndex() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        PlatformSessionRecord requested = sessionRecord(
            "sess-001",
            "learner@example.com",
            PlatformSessionState.REQUESTED,
            Instant.parse("2026-04-21T10:00:00Z")
        );
        PlatformSessionRecord ready = sessionRecord(
            "sess-002",
            "learner@example.com",
            PlatformSessionState.READY,
            Instant.parse("2026-04-21T10:05:00Z")
        );
        ready.setWorkspaceCleanupState(WorkspaceCleanupState.PENDING);
        ready.setInternalRuntimeRef("runtime-002");
        PlatformSessionRecord otherOwner = sessionRecord(
            "sess-003",
            "other@example.com",
            PlatformSessionState.READY,
            Instant.parse("2026-04-21T10:10:00Z")
        );

        when(setOperations.members("test:sessions:index")).thenReturn(Set.of("sess-001", "sess-002", "sess-003"));
        when(valueOperations.get("test:sessions:sess-001")).thenReturn(requested);
        when(valueOperations.get("test:sessions:sess-002")).thenReturn(ready);
        when(valueOperations.get("test:sessions:sess-003")).thenReturn(otherOwner);

        assertThat(repository.findAllByOwnerUserIdOrderByCreatedAtDesc("learner@example.com"))
            .extracting(PlatformSessionRecord::getSessionId)
            .containsExactly("sess-002", "sess-001");
        assertThat(repository.findAllByOwnerUserIdAndStateInOrderByCreatedAtDesc(
            "learner@example.com",
            List.of(PlatformSessionState.READY)
        )).extracting(PlatformSessionRecord::getSessionId).containsExactly("sess-002");
        assertThat(repository.countByWorkshopIdAndModeAndState(
            "1_session_management",
            PlatformSessionMode.LAB,
            PlatformSessionState.READY
        )).isEqualTo(2);
        assertThat(repository.findAllByWorkspaceCleanupStateInOrderByLastActivityAtAsc(List.of(
            WorkspaceCleanupState.PENDING
        ))).extracting(PlatformSessionRecord::getSessionId).containsExactly("sess-002");
        assertThat(repository.findAllByStateInAndInternalRuntimeRefIsNotNullOrderByLastActivityAtAsc(List.of(
            PlatformSessionState.READY
        ))).extracting(PlatformSessionRecord::getSessionId).containsExactly("sess-002");
    }

    private PlatformSessionRecord sessionRecord(
        String sessionId,
        String ownerUserId,
        PlatformSessionState state,
        Instant createdAt
    ) {
        PlatformSessionRecord record = new PlatformSessionRecord();
        record.setSessionId(sessionId);
        record.setOwnerUserId(ownerUserId);
        record.setWorkshopId("1_session_management");
        record.setReleaseVersion("2026.04.1");
        record.setMode(PlatformSessionMode.LAB);
        record.setState(state);
        record.setQuotaClass("standard");
        record.setResourceClass("small");
        record.setCreatedAt(createdAt);
        record.setExpiresAt(createdAt.plusSeconds(3600));
        record.setLastActivityAt(createdAt.plusSeconds(60));
        record.setWorkspaceCleanupState(WorkspaceCleanupState.ACTIVE);
        return record;
    }
}
