package com.redis.workshop.session.frontend.infrastructure;

import com.redis.workshop.infrastructure.FrontendRuntimeProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.ByteArrayOutputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SessionAuthProxyControllerTest {

    private HttpClient httpClient;
    private FrontendRuntimeProperties runtimeProperties;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        httpClient = mock(HttpClient.class);
        runtimeProperties = new FrontendRuntimeProperties();
        runtimeProperties.setBackendUrl("http://backend.internal:18080");
        mockMvc = MockMvcBuilders.standaloneSetup(new SessionAuthProxyController(runtimeProperties, httpClient)).build();
    }

    @Test
    void proxiesLoginFormRequestAndPreservesCredentials() throws Exception {
        HttpResponse<byte[]> backendResponse = mockBackendResponse(
            302,
            new byte[0],
            Map.of(
                "location", List.of("http://backend.internal:18080/welcome"),
                "set-cookie", List.of("JSESSIONID=abc; Path=/internal; Domain=backend.internal; HttpOnly")
            )
        );

        when(httpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any()))
            .thenReturn(backendResponse);

        mockMvc.perform(
                post("/login")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("username", "user")
                    .param("password", "password")
                    .header(HttpHeaders.HOST, "frontend.local:8080")
            )
            .andExpect(status().isFound())
            .andExpect(header().string(HttpHeaders.LOCATION, "http://frontend.local:8080/welcome"))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, not(containsString("Domain="))))
            .andExpect(content().bytes(new byte[0]));

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(requestCaptor.capture(), ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any());
        HttpRequest outboundRequest = requestCaptor.getValue();

        assertThat(outboundRequest.method()).isEqualTo("POST");
        assertThat(outboundRequest.uri().toString()).isEqualTo("http://backend.internal:18080/login");
        assertThat(readBody(outboundRequest)).contains("username=user");
        assertThat(readBody(outboundRequest)).contains("password=password");
        assertThat(outboundRequest.headers().firstValue("Content-Type")).contains("application/x-www-form-urlencoded");
        assertThat(outboundRequest.headers().firstValue("X-Forwarded-Host")).contains("frontend.local:8080");
    }

    @Test
    void handlesBackendUrlWithTrailingSlash() throws Exception {
        runtimeProperties.setBackendUrl("http://backend.internal:18080/");

        HttpResponse<byte[]> backendResponse = mockBackendResponse(302, new byte[0], Map.of());
        when(httpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any()))
            .thenReturn(backendResponse);

        mockMvc.perform(
                post("/login")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("username", "user")
                    .param("password", "password")
            )
            .andExpect(status().isFound());

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(requestCaptor.capture(), ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any());
        assertThat(requestCaptor.getValue().uri().toString()).isEqualTo("http://backend.internal:18080/login");
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<byte[]> mockBackendResponse(int statusCode, byte[] body, Map<String, List<String>> headers) {
        HttpResponse<byte[]> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(statusCode);
        when(response.body()).thenReturn(body);
        when(response.headers()).thenReturn(java.net.http.HttpHeaders.of(headers, (name, value) -> true));
        return response;
    }

    private String readBody(HttpRequest request) throws InterruptedException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CountDownLatch complete = new CountDownLatch(1);
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer item) {
                byte[] bytes = new byte[item.remaining()];
                item.get(bytes);
                output.writeBytes(bytes);
            }

            @Override
            public void onError(Throwable throwable) {
                complete.countDown();
            }

            @Override
            public void onComplete() {
                complete.countDown();
            }
        });

        assertThat(complete.await(1, TimeUnit.SECONDS)).isTrue();
        return output.toString(StandardCharsets.UTF_8);
    }
}
