package com.redis.workshop.springai.multiagents.providers;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.redis.workshop.springai.multiagents.agent.newsagent.NewsSnapshot;

import tools.jackson.databind.JsonNode;

@Component
public class NewsProvider {

    private final RestClient restClient;
    private final String apiKey;
    private final int maxResults;

    public NewsProvider(
            @Value("${stock-analysis.news.tavily.base-url}") String baseUrl,
            @Value("${stock-analysis.news.tavily.api-key}") String apiKey,
            @Value("${stock-analysis.news.tavily.max-results}") int maxResults
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.apiKey = apiKey;
        this.maxResults = maxResults;
    }

    public NewsSnapshot fetchSnapshot(String ticker) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Tavily API key is not configured. Set TAVILY_API_KEY in Env.");
        }

        String symbol = normalize(ticker);
        JsonNode response = restClient.post()
                .uri("/search")
                .header("Authorization", "Bearer " + apiKey)
                .body(requestBody(symbol))
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            throw new IllegalStateException("Tavily returned an empty response.");
        }

        if ("error".equalsIgnoreCase(optionalText(response, "status", ""))) {
            throw new IllegalStateException("Tavily error: " + optionalText(response, "error", "Unknown error"));
        }

        List<String> headlines = response.path("results").isArray()
                ? response.path("results").valueStream()
                .limit(maxResults)
                .map(result -> optionalText(result, "title", "Untitled result"))
                .toList()
                : List.of();

        return new NewsSnapshot(
                symbol,
                symbol,
                headlines,
                summary(response, headlines),
                OffsetDateTime.now(ZoneOffset.UTC),
                "tavily"
        );
    }

    private Map<String, Object> requestBody(String ticker) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", ticker + " investor news");
        body.put("topic", "finance");
        body.put("search_depth", "basic");
        body.put("max_results", maxResults);
        body.put("include_answer", "basic");
        body.put("include_raw_content", false);
        return body;
    }

    private String summary(JsonNode response, List<String> headlines) {
        String answer = optionalText(response, "answer", "");
        if (!answer.isBlank()) {
            return answer;
        }

        return headlines.isEmpty()
                ? "Tavily returned no recent finance headlines."
                : "Tavily returned " + headlines.size() + " recent finance headline(s).";
    }

    private String normalize(String ticker) {
        return ticker == null || ticker.isBlank()
                ? "RDIS"
                : ticker.trim().toUpperCase(Locale.ROOT);
    }

    private String optionalText(JsonNode node, String fieldName, String defaultValue) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull() || field.isMissingNode()) {
            return defaultValue;
        }
        return field.asText(defaultValue);
    }
}
