package com.redis.workshop.platform.controlplane.persistence.repository;

import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionRecord;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

@Profile("!cloudrun")
public interface JpaPlatformSessionRecordRepository
    extends PlatformSessionRecordRepository, JpaRepository<PlatformSessionRecord, String> {
}
