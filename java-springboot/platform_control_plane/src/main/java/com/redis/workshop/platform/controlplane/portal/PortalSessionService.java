package com.redis.workshop.platform.controlplane.portal;

import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionRecord;
import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionState;
import com.redis.workshop.platform.controlplane.persistence.repository.PlatformSessionRecordRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

@Service
public class PortalSessionService {

    private static final int TOKEN_BYTES = 32;
    private static final EnumSet<PlatformSessionState> PORTAL_SESSION_STATES = EnumSet.of(
        PlatformSessionState.REQUESTED,
        PlatformSessionState.ADMITTED,
        PlatformSessionState.PROVISIONING,
        PlatformSessionState.INITIALIZING,
        PlatformSessionState.READY,
        PlatformSessionState.DEGRADED,
        PlatformSessionState.TERMINATING,
        PlatformSessionState.CLEANUP_PENDING
    );
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+$"
    );

    private final RedisTemplate<String, PortalSession> redisTemplate;
    private final RedisTemplate<String, PortalContactPreference> contactPreferenceRedisTemplate;
    private final PlatformSessionRecordRepository sessionRepository;
    private final PortalSessionProperties properties;
    private final SecureRandom secureRandom;

    public PortalSessionService(
        @Qualifier("portalSessionRedisTemplate") RedisTemplate<String, PortalSession> redisTemplate,
        @Qualifier("portalContactPreferenceRedisTemplate")
        RedisTemplate<String, PortalContactPreference> contactPreferenceRedisTemplate,
        PlatformSessionRecordRepository sessionRepository,
        PortalSessionProperties properties
    ) {
        this.redisTemplate = redisTemplate;
        this.contactPreferenceRedisTemplate = contactPreferenceRedisTemplate;
        this.sessionRepository = sessionRepository;
        this.properties = properties;
        this.secureRandom = new SecureRandom();
    }

    public PortalLoginResult createSession(String email) {
        return createSession(email, true);
    }

    public PortalLoginResult createSession(String email, boolean allowMarketingContact) {
        String normalizedEmail = normalizeEmail(email);
        String token = generateToken();
        Instant now = Instant.now();
        storeContactPreference(normalizedEmail, allowMarketingContact, now);
        PortalSession session = new PortalSession(
            normalizedEmail,
            now,
            now.plus(properties.ttl()),
            activeSessionIdsFor(normalizedEmail)
        );
        redisTemplate.opsForValue().set(redisKey(token), session, properties.ttl());
        return new PortalLoginResult(token, session);
    }

    public Optional<PortalSession> findByToken(String token) {
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }

        String key = redisKey(token);
        PortalSession session = redisTemplate.opsForValue().get(key);
        if (session == null) {
            return Optional.empty();
        }

        if (isExpired(session)) {
            redisTemplate.delete(key);
            return Optional.empty();
        }
        return Optional.of(session);
    }

    public Optional<PortalSession> attachSession(String token, String sessionId) {
        return updateSession(token, session -> session.withSessionId(sessionId));
    }

    public Optional<PortalSession> detachSession(String token, String sessionId) {
        return updateSession(token, session -> session.withoutSessionId(sessionId));
    }

    public void logout(String token) {
        if (StringUtils.hasText(token)) {
            redisTemplate.delete(redisKey(token));
        }
    }

    private Optional<PortalSession> updateSession(String token, UnaryOperator<PortalSession> updater) {
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }

        String key = redisKey(token);
        PortalSession session = redisTemplate.opsForValue().get(key);
        if (session == null || isExpired(session)) {
            redisTemplate.delete(key);
            return Optional.empty();
        }

        PortalSession updatedSession = updater.apply(session);
        Duration remainingTtl = Duration.between(Instant.now(), updatedSession.expiresAt());
        if (remainingTtl.isZero() || remainingTtl.isNegative()) {
            redisTemplate.delete(key);
            return Optional.empty();
        }

        redisTemplate.opsForValue().set(key, updatedSession, remainingTtl);
        return Optional.of(updatedSession);
    }

    private List<String> activeSessionIdsFor(String normalizedEmail) {
        return sessionRepository.findAllByOwnerUserIdOrderByCreatedAtDesc(normalizedEmail).stream()
            .filter(record -> PORTAL_SESSION_STATES.contains(record.getState()))
            .map(PlatformSessionRecord::getSessionId)
            .toList();
    }

    private boolean isExpired(PortalSession session) {
        return session.expiresAt() != null && !session.expiresAt().isAfter(Instant.now());
    }

    private void storeContactPreference(String normalizedEmail, boolean allowMarketingContact, Instant now) {
        PortalContactPreference preference = new PortalContactPreference(
            normalizedEmail,
            allowMarketingContact,
            now
        );
        contactPreferenceRedisTemplate.opsForValue()
            .set(contactPreferenceRedisKey(normalizedEmail), preference);
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw invalidEmail();
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw invalidEmail();
        }
        return normalizedEmail;
    }

    private ResponseStatusException invalidEmail() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid email is required");
    }

    private String generateToken() {
        byte[] tokenBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String redisKey(String token) {
        return properties.redisKeyPrefix() + sha256Hex(token);
    }

    private String contactPreferenceRedisKey(String normalizedEmail) {
        return properties.contactPreferenceRedisKeyPrefix() + sha256Hex(normalizedEmail);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is not available", ex);
        }
    }
}
