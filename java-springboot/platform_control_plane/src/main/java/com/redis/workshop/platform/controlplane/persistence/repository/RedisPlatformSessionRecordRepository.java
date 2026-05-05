package com.redis.workshop.platform.controlplane.persistence.repository;

import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionMode;
import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionRecord;
import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionState;
import com.redis.workshop.platform.controlplane.persistence.model.WorkspaceCleanupState;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

@Repository
@Profile("cloudrun")
class RedisPlatformSessionRecordRepository implements PlatformSessionRecordRepository {

    private final RedisTemplate<String, PlatformSessionRecord> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final PlatformSessionRecordRedisProperties properties;

    RedisPlatformSessionRecordRepository(
        RedisTemplate<String, PlatformSessionRecord> redisTemplate,
        StringRedisTemplate stringRedisTemplate,
        PlatformSessionRecordRedisProperties properties
    ) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.properties = properties;
    }

    @Override
    public <S extends PlatformSessionRecord> S save(S record) {
        if (record == null || !StringUtils.hasText(record.getSessionId())) {
            throw new IllegalArgumentException("sessionId is required");
        }
        Instant now = Instant.now();
        if (record.getCreatedAt() == null) {
            record.setCreatedAt(now);
        }
        if (record.getLastActivityAt() == null) {
            record.setLastActivityAt(record.getCreatedAt());
        }
        redisTemplate.opsForValue().set(recordKey(record.getSessionId()), record);
        stringRedisTemplate.opsForSet().add(properties.indexKey(), record.getSessionId());
        return record;
    }

    @Override
    public Optional<PlatformSessionRecord> findById(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(redisTemplate.opsForValue().get(recordKey(sessionId)));
    }

    @Override
    public List<PlatformSessionRecord> findAll() {
        Set<String> sessionIds = stringRedisTemplate.opsForSet().members(properties.indexKey());
        if (sessionIds == null || sessionIds.isEmpty()) {
            return List.of();
        }
        return sessionIds.stream()
            .map(this::findById)
            .flatMap(Optional::stream)
            .toList();
    }

    @Override
    public void deleteAll() {
        Set<String> sessionIds = stringRedisTemplate.opsForSet().members(properties.indexKey());
        if (sessionIds != null && !sessionIds.isEmpty()) {
            redisTemplate.delete(sessionIds.stream()
                .map(this::recordKey)
                .toList());
        }
        stringRedisTemplate.delete(properties.indexKey());
    }

    @Override
    public Optional<PlatformSessionRecord> findBySessionIdAndOwnerUserId(String sessionId, String ownerUserId) {
        return findById(sessionId)
            .filter(record -> Objects.equals(record.getOwnerUserId(), ownerUserId));
    }

    @Override
    public List<PlatformSessionRecord> findAllByOwnerUserIdOrderByCreatedAtDesc(String ownerUserId) {
        return filterAndSort(
            record -> Objects.equals(record.getOwnerUserId(), ownerUserId),
            Comparator.comparing(this::createdAtOrEpoch).reversed()
        );
    }

    @Override
    public List<PlatformSessionRecord> findAllByOwnerUserIdAndStateInOrderByCreatedAtDesc(
        String ownerUserId,
        Collection<PlatformSessionState> states
    ) {
        return filterAndSort(
            record -> Objects.equals(record.getOwnerUserId(), ownerUserId) && contains(states, record.getState()),
            Comparator.comparing(this::createdAtOrEpoch).reversed()
        );
    }

    @Override
    public long countByWorkshopIdAndModeAndState(
        String workshopId,
        PlatformSessionMode mode,
        PlatformSessionState state
    ) {
        return findAll().stream()
            .filter(record -> Objects.equals(record.getWorkshopId(), workshopId))
            .filter(record -> record.getMode() == mode)
            .filter(record -> record.getState() == state)
            .count();
    }

    @Override
    public List<PlatformSessionRecord> findAllByExpiresAtBeforeAndStateInOrderByExpiresAtAsc(
        Instant cutoff,
        Collection<PlatformSessionState> states
    ) {
        Instant effectiveCutoff = cutoff == null ? Instant.now() : cutoff;
        return filterAndSort(
            record -> record.getExpiresAt() != null
                && record.getExpiresAt().isBefore(effectiveCutoff)
                && contains(states, record.getState()),
            Comparator.comparing(this::expiresAtOrMax)
        );
    }

    @Override
    public List<PlatformSessionRecord> findAllByWorkspaceCleanupStateInOrderByLastActivityAtAsc(
        Collection<WorkspaceCleanupState> states
    ) {
        return filterAndSort(
            record -> contains(states, record.getWorkspaceCleanupState()),
            Comparator.comparing(this::lastActivityAtOrEpoch)
        );
    }

    @Override
    public List<PlatformSessionRecord> findAllByStateInAndInternalRuntimeRefIsNotNullOrderByLastActivityAtAsc(
        Collection<PlatformSessionState> states
    ) {
        return filterAndSort(
            record -> contains(states, record.getState()) && StringUtils.hasText(record.getInternalRuntimeRef()),
            Comparator.comparing(this::lastActivityAtOrEpoch)
        );
    }

    private List<PlatformSessionRecord> filterAndSort(
        Predicate<PlatformSessionRecord> predicate,
        Comparator<PlatformSessionRecord> comparator
    ) {
        return findAll().stream()
            .filter(predicate)
            .sorted(comparator)
            .toList();
    }

    private boolean contains(Collection<?> values, Object value) {
        return values != null && values.contains(value);
    }

    private Instant createdAtOrEpoch(PlatformSessionRecord record) {
        return record.getCreatedAt() == null ? Instant.EPOCH : record.getCreatedAt();
    }

    private Instant expiresAtOrMax(PlatformSessionRecord record) {
        return record.getExpiresAt() == null ? Instant.MAX : record.getExpiresAt();
    }

    private Instant lastActivityAtOrEpoch(PlatformSessionRecord record) {
        return record.getLastActivityAt() == null ? Instant.EPOCH : record.getLastActivityAt();
    }

    private String recordKey(String sessionId) {
        return properties.keyPrefix() + sessionId.trim();
    }
}
