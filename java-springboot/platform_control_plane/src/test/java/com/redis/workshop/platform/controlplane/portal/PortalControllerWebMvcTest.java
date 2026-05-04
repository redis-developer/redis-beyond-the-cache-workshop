package com.redis.workshop.platform.controlplane.portal;

import com.redis.workshop.platform.controlplane.audit.AuditTrailService;
import com.redis.workshop.platform.controlplane.audit.ReleaseCatalogAuditHooks;
import com.redis.workshop.platform.controlplane.observability.SessionLifecycleMetricsRecorder;
import com.redis.workshop.platform.controlplane.release.ReleaseCatalogService;
import com.redis.workshop.platform.controlplane.security.SecurityConfiguration;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PortalController.class)
@Import(SecurityConfiguration.class)
class PortalControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PortalSessionService portalSessionService;

    @MockitoBean
    private AuditTrailService auditTrailService;

    @MockitoBean
    private SessionLifecycleMetricsRecorder sessionLifecycleMetricsRecorder;

    @MockitoBean
    private ReleaseCatalogService releaseCatalogService;

    @MockitoBean
    private ReleaseCatalogAuditHooks releaseCatalogAuditHooks;

    @Test
    void loginSetsHttpOnlySameSiteCookieAndReturnsUser() throws Exception {
        given(portalSessionService.createSession(" Learner@Example.COM ", true)).willReturn(
            new PortalLoginResult("opaque-token", session("learner@example.com"))
        );

        mockMvc.perform(post("/api/portal/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\" Learner@Example.COM \"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authenticated").value(true))
            .andExpect(jsonPath("$.email").value("learner@example.com"))
            .andExpect(jsonPath("$.sessionIds[0]").value("sess-001"))
            .andExpect(cookie().httpOnly("portal_session", true))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Lax")));
    }

    @Test
    void loginPassesDeclinedMarketingContactPreference() throws Exception {
        given(portalSessionService.createSession("learner@example.com", false)).willReturn(
            new PortalLoginResult("opaque-token", session("learner@example.com"))
        );

        mockMvc.perform(post("/api/portal/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"learner@example.com\",\"allowMarketingContact\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authenticated").value(true));

        verify(portalSessionService).createSession("learner@example.com", false);
    }

    @Test
    void loginRejectsBlankEmailBeforeCreatingSession() throws Exception {
        mockMvc.perform(post("/api/portal/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void currentUserReturnsAnonymousWithoutPortalCookie() throws Exception {
        mockMvc.perform(get("/api/portal/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authenticated").value(false));
    }

    @Test
    void currentUserReturnsAuthenticatedUserForValidPortalCookie() throws Exception {
        given(portalSessionService.findByToken("opaque-token")).willReturn(Optional.of(session("learner@example.com")));

        mockMvc.perform(get("/api/portal/me")
                .cookie(new Cookie("portal_session", "opaque-token")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authenticated").value(true))
            .andExpect(jsonPath("$.email").value("learner@example.com"))
            .andExpect(jsonPath("$.sessionIds[0]").value("sess-001"));
    }

    @Test
    void logoutDeletesRedisSessionAndClearsCookie() throws Exception {
        given(portalSessionService.findByToken("opaque-token")).willReturn(Optional.of(session("learner@example.com")));

        mockMvc.perform(post("/api/portal/logout")
                .cookie(new Cookie("portal_session", "opaque-token")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authenticated").value(false))
            .andExpect(cookie().maxAge("portal_session", 0))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Lax")));

        verify(portalSessionService).logout("opaque-token");
    }

    private PortalSession session(String email) {
        return new PortalSession(
            email,
            Instant.parse("2026-04-21T10:00:00Z"),
            Instant.parse("2026-04-21T18:00:00Z"),
            List.of("sess-001")
        );
    }
}
