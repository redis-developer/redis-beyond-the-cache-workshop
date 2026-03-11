package com.redis.workshop.locks.frontend;

import org.hamcrest.Matchers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    classes = DistributedLocksFrontendApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "workshop.backend.url=http://127.0.0.1:1"
)
@AutoConfigureMockMvc
class DistributedLocksFrontendIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void editorApiIsExposedInFrontendModule() throws Exception {
        mockMvc.perform(get("/api/editor/files"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.files", hasSize(3)))
            .andExpect(jsonPath("$", hasKey("files")))
            .andExpect(jsonPath("$", hasKey("workshopTitle")))
            .andExpect(jsonPath("$", hasKey("workshopDescription")))
            .andExpect(jsonPath("$.files[*].name", not(Matchers.hasItem("PurchaseService.java"))))
            .andExpect(jsonPath("$.workshopTitle").value("Distributed Locks"))
            .andExpect(jsonPath("$.workshopDescription").value("Protect shared workflows with Redis-based distributed locks."));
    }

    @Test
    void inventoryApiIsHandledByProxyInFrontendModule() throws Exception {
        mockMvc.perform(get("/api/inventory"))
            .andExpect(status().isBadGateway());
    }

    @Test
    void purchaseServiceIsReadOnlyReviewContent() throws Exception {
        mockMvc.perform(get("/api/editor/file/PurchaseService.java"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.error").value("Invalid file name: PurchaseService.java"));

        mockMvc.perform(get("/api/review/purchase-service"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fileName").value("PurchaseService.java"))
            .andExpect(jsonPath("$.language").value("java"))
            .andExpect(jsonPath("$.content").value(Matchers.containsString("lockManager.withLock(")));
    }

    @Test
    void contentDrivenViewsAreExposedForWorkshopThree() throws Exception {
        mockMvc.perform(get("/api/content/manifest"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.workshopId").value("3_distributed_locks"))
            .andExpect(jsonPath("$.views", hasSize(3)));

        mockMvc.perform(get("/api/content/views/locks-home"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.viewId").value("locks-home"))
            .andExpect(jsonPath("$.pageType").value("stage-flow"))
            .andExpect(jsonPath("$.stages", hasSize(2)));

        mockMvc.perform(get("/api/content/views/locks-implement"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.viewId").value("locks-implement"))
            .andExpect(jsonPath("$.pageType").value("narrative"))
            .andExpect(jsonPath("$.sections", hasSize(3)));

        mockMvc.perform(get("/api/content/views/locks-editor"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.viewId").value("locks-editor"))
            .andExpect(jsonPath("$.pageType").value("editor"))
            .andExpect(jsonPath("$.sections", hasSize(6)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/",
        "/reentrant",
        "/reentrant/implement",
        "/reentrant/editor",
        "/reentrant/demo",
        "/demo",
        "/editor",
        "/implement",
        "/implementation",
        "/complete",
        "/scenario/1",
        "/scenario/1/2"
    })
    void spaRoutesForwardToIndexHtml(String route) throws Exception {
        mockMvc.perform(get(route))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/index.html"));
    }
}
