package com.redis.workshop.platform.controlplane.portal;

import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionState;
import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionRecord;
import com.redis.workshop.platform.controlplane.persistence.repository.PlatformSessionRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortalSessionServiceTest {

    private static final PortalSessionProperties PROPERTIES = new PortalSessionProperties(
        "portal_session",
        Duration.ofHours(8),
        false,
        "test:portal:",
        "test:portal:contacts:"
    );

    @Mock
    private RedisTemplate<String, PortalSession> redisTemplate;

    @Mock
    private RedisTemplate<String, PortalContactPreference> contactPreferenceRedisTemplate;

    @Mock
    private ValueOperations<String, PortalSession> valueOperations;

    @Mock
    private ValueOperations<String, PortalContactPreference> contactPreferenceValueOperations;

    @Mock
    private PlatformSessionRecordRepository sessionRepository;

    private PortalSessionService portalSessionService;

    @BeforeEach
    void setUp() {
        portalSessionService = new PortalSessionService(
            redisTemplate,
            contactPreferenceRedisTemplate,
            sessionRepository,
            PROPERTIES
        );
    }

    @Test
    void createsRedisBackedJsonSessionWithNormalizedEmailHashedTokenKeyAndActiveSessionIds() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(contactPreferenceRedisTemplate.opsForValue()).thenReturn(contactPreferenceValueOperations);
        when(sessionRepository.findAllByOwnerUserIdOrderByCreatedAtDesc("learner@example.com"))
            .thenReturn(List.of(
                sessionRecord("sess-ready", PlatformSessionState.READY),
                sessionRecord("sess-terminated", PlatformSessionState.TERMINATED)
            ));
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PortalSession> valueCaptor = ArgumentCaptor.forClass(PortalSession.class);
        ArgumentCaptor<String> contactKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PortalContactPreference> contactValueCaptor =
            ArgumentCaptor.forClass(PortalContactPreference.class);

        PortalLoginResult result = portalSessionService.createSession(" Learner@Example.COM ");

        verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture(), eq(PROPERTIES.ttl()));
        verify(contactPreferenceValueOperations).set(contactKeyCaptor.capture(), contactValueCaptor.capture());
        assertThat(result.token()).isNotBlank();
        assertThat(result.session().email()).isEqualTo("learner@example.com");
        assertThat(keyCaptor.getValue())
            .startsWith(PROPERTIES.redisKeyPrefix())
            .doesNotContain(result.token())
            .hasSize(PROPERTIES.redisKeyPrefix().length() + 64);

        PortalSession storedSession = valueCaptor.getValue();
        assertThat(storedSession.email()).isEqualTo("learner@example.com");
        assertThat(storedSession.expiresAt()).isAfter(storedSession.createdAt());
        assertThat(storedSession.sessionIds()).containsExactly("sess-ready");

        assertThat(contactKeyCaptor.getValue())
            .startsWith(PROPERTIES.contactPreferenceRedisKeyPrefix())
            .doesNotContain("learner@example.com")
            .hasSize(PROPERTIES.contactPreferenceRedisKeyPrefix().length() + 64);
        PortalContactPreference storedPreference = contactValueCaptor.getValue();
        assertThat(storedPreference.email()).isEqualTo("learner@example.com");
        assertThat(storedPreference.allowMarketingContact()).isTrue();
        assertThat(storedPreference.updatedAt()).isNotNull();
    }

    @Test
    void storesDeclinedMarketingContactPreferenceOutsidePortalSession() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(contactPreferenceRedisTemplate.opsForValue()).thenReturn(contactPreferenceValueOperations);
        when(sessionRepository.findAllByOwnerUserIdOrderByCreatedAtDesc("learner@example.com"))
            .thenReturn(List.of());
        ArgumentCaptor<PortalContactPreference> contactValueCaptor =
            ArgumentCaptor.forClass(PortalContactPreference.class);

        portalSessionService.createSession("learner@example.com", false);

        verify(contactPreferenceValueOperations).set(any(), contactValueCaptor.capture());
        assertThat(contactValueCaptor.getValue().email()).isEqualTo("learner@example.com");
        assertThat(contactValueCaptor.getValue().allowMarketingContact()).isFalse();
    }

    @Test
    void rejectsInvalidEmailInput() {
        assertThatExceptionOfType(ResponseStatusException.class)
            .isThrownBy(() -> portalSessionService.createSession("not-an-email"))
            .satisfies(exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void resolvesStoredSessionFromOpaqueToken() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(contactPreferenceRedisTemplate.opsForValue()).thenReturn(contactPreferenceValueOperations);
        when(sessionRepository.findAllByOwnerUserIdOrderByCreatedAtDesc("learner@example.com"))
            .thenReturn(List.of());
        PortalLoginResult result = portalSessionService.createSession("learner@example.com");
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PortalSession> valueCaptor = ArgumentCaptor.forClass(PortalSession.class);
        verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture(), eq(PROPERTIES.ttl()));
        when(valueOperations.get(keyCaptor.getValue())).thenReturn(valueCaptor.getValue());

        assertThat(portalSessionService.findByToken(result.token()))
            .hasValueSatisfying(session -> assertThat(session.email()).isEqualTo("learner@example.com"));
    }

    @Test
    void attachesAndDetachesSessionIdsOnStoredJsonSession() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        PortalSession session = new PortalSession(
            "learner@example.com",
            Instant.now().minusSeconds(10),
            Instant.now().plus(PROPERTIES.ttl()),
            List.of("sess-existing")
        );
        when(valueOperations.get(any())).thenReturn(session, session.withSessionId("sess-new"));
        ArgumentCaptor<PortalSession> valueCaptor = ArgumentCaptor.forClass(PortalSession.class);

        portalSessionService.attachSession("opaque-token", "sess-new");
        portalSessionService.detachSession("opaque-token", "sess-existing");

        verify(valueOperations, times(2)).set(any(), valueCaptor.capture(), any(Duration.class));
        assertThat(valueCaptor.getAllValues().get(0).sessionIds())
            .containsExactly("sess-existing", "sess-new");
        assertThat(valueCaptor.getAllValues().get(1).sessionIds())
            .containsExactly("sess-new");
    }

    @Test
    void logoutDeletesHashedRedisKeyOnly() {
        String token = "opaque-token";
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);

        portalSessionService.logout(token);

        verify(redisTemplate).delete(keyCaptor.capture());
        assertThat(keyCaptor.getValue())
            .startsWith(PROPERTIES.redisKeyPrefix())
            .doesNotContain(token)
            .hasSize(PROPERTIES.redisKeyPrefix().length() + 64);
    }

    private PlatformSessionRecord sessionRecord(String sessionId, PlatformSessionState state) {
        PlatformSessionRecord record = new PlatformSessionRecord();
        record.setSessionId(sessionId);
        record.setState(state);
        return record;
    }
}
