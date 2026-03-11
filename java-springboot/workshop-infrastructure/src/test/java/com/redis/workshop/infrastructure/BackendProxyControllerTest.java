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

    @SuppressWarnings("unchecked")
    private HttpResponse<byte[]> mockBackendResponse(int statusCode, byte[] body, Map<String, List<String>> headers) {
        HttpResponse<byte[]> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(statusCode);
        when(response.body()).thenReturn(body);
        when(response.headers()).thenReturn(HttpHeaders.of(headers, (name, value) -> true));
        return response;
    }
}
