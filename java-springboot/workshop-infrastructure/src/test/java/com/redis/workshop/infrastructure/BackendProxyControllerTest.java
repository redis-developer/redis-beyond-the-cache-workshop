package com.redis.workshop.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BackendProxyControllerTest {

    private HttpClient httpClient;
    private FrontendRuntimeProperties runtimeProperties;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        httpClient = mock(HttpClient.class);
        runtimeProperties = new FrontendRuntimeProperties();
        runtimeProperties.setBackendUrl("http://backend.internal:18080");
        mockMvc = MockMvcBuilders.standaloneSetup(new BackendProxyController(runtimeProperties, httpClient)).build();
    }

    @Test
    void proxiesApiRequestAndRewritesLocationAndCookieHeaders() throws Exception {
        HttpResponse<byte[]> backendResponse = mockBackendResponse(
            302,
            "redirect".getBytes(StandardCharsets.UTF_8),
            Map.of(
                "location", List.of("http://backend.internal:18080/login"),
                "set-cookie", List.of("SESSION=abc; Path=/internal; Domain=backend.internal; HttpOnly"),
                "content-type", List.of("text/plain")
            )
        );

        when(httpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any()))
            .thenReturn(backendResponse);

        mockMvc.perform(
                get("/api/search")
                    .queryParam("q", "redis")
                    .header("Host", "frontend.local:8081")
                    .header("Cookie", "SESSION=abc")
            )
            .andExpect(status().isFound())
            .andExpect(header().string("Location", "http://frontend.local:8081/login"))
            .andExpect(header().string("Set-Cookie", containsString("Path=/")))
            .andExpect(header().string("Set-Cookie", not(containsString("Domain="))))
            .andExpect(content().string("redirect"));

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(requestCaptor.capture(), ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any());
        HttpRequest outboundRequest = requestCaptor.getValue();

        assertThat(outboundRequest.method()).isEqualTo("GET");
        assertThat(outboundRequest.uri().toString()).isEqualTo("http://backend.internal:18080/api/search?q=redis");
        assertThat(outboundRequest.headers().firstValue("Cookie")).contains("SESSION=abc");
        assertThat(outboundRequest.headers().firstValue("X-Forwarded-Host")).contains("frontend.local:8081");
        assertThat(outboundRequest.headers().firstValue("X-Forwarded-Proto")).contains("http");
    }

    @Test
    void returnsServiceUnavailableWhenBackendUrlIsMissing() throws Exception {
        FrontendRuntimeProperties missingBackendProperties = new FrontendRuntimeProperties() {
            @Override
            public Optional<URI> resolveBackendUri() {
                return Optional.empty();
            }
        };

        MockMvc localMockMvc = MockMvcBuilders
            .standaloneSetup(new BackendProxyController(missingBackendProperties, httpClient))
            .build();

        localMockMvc.perform(get("/api/search"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(content().json("{\"error\":\"Backend URL is not configured\"}"));

        verifyNoInteractions(httpClient);
    }

    @Test
    void doesNotProxySharedContentEndpoints() throws Exception {
        mockMvc.perform(get("/api/content/manifest"))
            .andExpect(status().isNotFound());

        verifyNoInteractions(httpClient);
    }

    @Test
    void proxiesLearnerAppRequests() throws Exception {
        HttpResponse<byte[]> backendResponse = mockBackendResponse(
            200,
            "<html>app</html>".getBytes(StandardCharsets.UTF_8),
            Map.of("content-type", List.of("text/html"))
        );

        when(httpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any()))
            .thenReturn(backendResponse);

        mockMvc.perform(get("/app/").queryParam("frame", "2"))
            .andExpect(status().isOk())
            .andExpect(content().string("<html>app</html>"));

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(requestCaptor.capture(), ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any());
        assertThat(requestCaptor.getValue().uri().toString()).isEqualTo("http://backend.internal:18080/app/?frame=2");
    }

    @Test
    void prefersSessionBackendUrlWhenConfigured() throws Exception {
        runtimeProperties.setSessionBackendUrl("http://session.internal:19090");
        mockMvc = MockMvcBuilders.standaloneSetup(new BackendProxyController(runtimeProperties, httpClient)).build();

        HttpResponse<byte[]> backendResponse = mockBackendResponse(
            200,
            "ok".getBytes(StandardCharsets.UTF_8),
            Map.of("content-type", List.of("text/plain"))
        );

        when(httpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any()))
            .thenReturn(backendResponse);

        mockMvc.perform(get("/api/search").queryParam("q", "redis"))
            .andExpect(status().isOk())
            .andExpect(content().string("ok"));

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(requestCaptor.capture(), ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any());
        assertThat(requestCaptor.getValue().uri().toString()).isEqualTo("http://session.internal:19090/api/search?q=redis");
    }

    @Test
    void returnsServiceUnavailableWhenSessionRunnerChildIsNotReady() throws Exception {
        FrontendRuntimeProperties localRuntimeProperties = new FrontendRuntimeProperties();
        SessionRunnerProperties runnerProperties = new SessionRunnerProperties();
        runnerProperties.setEnabled(true);
        runnerProperties.setChildPort(19191);
        LocalSessionRunnerManager runnerManager = mock(LocalSessionRunnerManager.class);
        when(runnerManager.isChildReady()).thenReturn(false);

        MockMvc localMockMvc = MockMvcBuilders
            .standaloneSetup(new BackendProxyController(
                new SessionRuntimeResolver(localRuntimeProperties, runnerProperties),
                httpClient,
                runnerProperties,
                Optional.of(runnerManager)
            ))
            .build();

        localMockMvc.perform(get("/api/search"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(content().json("{\"error\":\"Session runner child process is not ready\"}"));

        verifyNoInteractions(httpClient);
    }

    @Test
    void proxiesToSessionRunnerChildWhenReady() throws Exception {
        FrontendRuntimeProperties localRuntimeProperties = new FrontendRuntimeProperties();
        SessionRunnerProperties runnerProperties = new SessionRunnerProperties();
        runnerProperties.setEnabled(true);
        runnerProperties.setChildPort(19191);
        LocalSessionRunnerManager runnerManager = mock(LocalSessionRunnerManager.class);
        when(runnerManager.isChildReady()).thenReturn(true);
        MockMvc localMockMvc = MockMvcBuilders
            .standaloneSetup(new BackendProxyController(
                new SessionRuntimeResolver(localRuntimeProperties, runnerProperties),
                httpClient,
                runnerProperties,
                Optional.of(runnerManager)
            ))
            .build();

        HttpResponse<byte[]> backendResponse = mockBackendResponse(
            200,
            "ok".getBytes(StandardCharsets.UTF_8),
            Map.of("content-type", List.of("text/plain"))
        );

        when(httpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any()))
            .thenReturn(backendResponse);

        localMockMvc.perform(get("/api/search").queryParam("q", "redis"))
            .andExpect(status().isOk())
            .andExpect(content().string("ok"));

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(requestCaptor.capture(), ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any());
        assertThat(requestCaptor.getValue().uri().toString()).isEqualTo("http://127.0.0.1:19191/api/search?q=redis");
    }

    @Test
    void proxiesSessionRunnerRedisInsightAndRewritesProxyPath() throws Exception {
        FrontendRuntimeProperties localRuntimeProperties = new FrontendRuntimeProperties();
        SessionRunnerProperties runnerProperties = new SessionRunnerProperties();
        runnerProperties.setEnabled(true);
        runnerProperties.setRedisInsightCommand("redisinsight");

        MockMvc localMockMvc = MockMvcBuilders
            .standaloneSetup(new BackendProxyController(
                new SessionRuntimeResolver(localRuntimeProperties, runnerProperties),
                httpClient,
                runnerProperties,
                Optional.empty()
            ))
            .build();

        HttpResponse<byte[]> redisInsightResponse = mockBackendResponse(
            200,
            "<script src=\"/redis-insight/assets/app.js\"></script>".getBytes(StandardCharsets.UTF_8),
            Map.of(
                "content-type", List.of("text/html"),
                "set-cookie", List.of("RI_SESSION=abc; Path=/redis-insight; HttpOnly")
            )
        );

        when(httpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any()))
            .thenReturn(redisInsightResponse);

        localMockMvc.perform(get("/redis-insight/").header("X-Forwarded-Prefix", "/session/sess-001"))
            .andExpect(status().isOk())
            .andExpect(header().string("Set-Cookie", containsString("Path=/session/sess-001/redis-insight/")))
            .andExpect(content().string(containsString("/session/sess-001/redis-insight/assets/app.js")));

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(requestCaptor.capture(), ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any());
        assertThat(requestCaptor.getValue().uri().toString()).isEqualTo("http://127.0.0.1:5540/redis-insight/");
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<byte[]> mockBackendResponse(int statusCode, byte[] body, Map<String, List<String>> headers) {
        HttpResponse<byte[]> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(statusCode);
        when(response.body()).thenReturn(body);
        when(response.headers()).thenReturn(HttpHeaders.of(headers, (name, value) -> true));
        return response;
    }
}
