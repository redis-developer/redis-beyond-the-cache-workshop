package com.redis.workshop.platform.controlplane.persistence.repository;

import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionMode;
import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionRecord;
import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionState;
import com.redis.workshop.platform.controlplane.persistence.model.WorkspaceCleanupState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PlatformSessionRecordRepository extends JpaRepository<PlatformSessionRecord, String> {

    Optional<PlatformSessionRecord> findBySessionIdAndOwnerUserId(String sessionId, String ownerUserId);

    List<PlatformSessionRecord> findAllByOwnerUserIdOrderByCreatedAtDesc(String ownerUserId);

    Optional<PlatformSessionRecord> findFirstByOwnerUserIdAndWorkshopIdAndStateInOrderByCreatedAtDesc(
        String ownerUserId,
        String workshopId,
        Collection<PlatformSessionState> states
    );

    long countByWorkshopIdAndModeAndState(
        String workshopId,
        PlatformSessionMode mode,
        PlatformSessionState state
    );

    List<PlatformSessionRecord> findAllByExpiresAtBeforeAndStateInOrderByExpiresAtAsc(
        Instant cutoff,
        Collection<PlatformSessionState> states
    );

    List<PlatformSessionRecord> findAllByWorkspaceCleanupStateInOrderByLastActivityAtAsc(
        Collection<WorkspaceCleanupState> states
    );

    List<PlatformSessionRecord> findAllByStateInAndInternalRuntimeRefIsNotNullOrderByLastActivityAtAsc(
        Collection<PlatformSessionState> states
    );
}
