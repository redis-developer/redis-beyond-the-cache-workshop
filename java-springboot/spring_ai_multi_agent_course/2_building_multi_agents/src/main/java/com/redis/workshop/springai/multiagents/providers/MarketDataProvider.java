package com.redis.workshop.springai.multiagents.providers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.redis.workshop.springai.multiagents.agent.marketdataagent.MarketSnapshot;

import tools.jackson.databind.JsonNode;

@Component
public class MarketDataProvider {

    private final RestClient restClient;
    private final String apiKey;

    public MarketDataProvider(
            @Value("${stock-analysis.market-data.twelve-data.base-url}") String baseUrl,
            @Value("${stock-analysis.market-data.twelve-data.api-key}") String apiKey
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.apiKey = apiKey;
    }

    public MarketSnapshot fetchSnapshot(String ticker) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Twelve Data API key is not configured. Set TWELVE_DATA_API_KEY in Env.");
        }

        String symbol = normalize(ticker);
        JsonNode response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/quote")
                        .queryParam("symbol", symbol)
                        .queryParam("apikey", apiKey)
                        .build())
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            throw new IllegalStateException("Twelve Data returned an empty response.");
        }

        if ("error".equalsIgnoreCase(optionalText(response, "status", ""))) {
            throw new IllegalStateException("Twelve Data error: " + optionalText(response, "message", "Unknown error"));
        }

        return new MarketSnapshot(
                optionalText(response, "symbol", symbol),
                optionalText(response, "name", symbol),
                moneyField(response, "close"),
                moneyField(response, "previous_close"),
                moneyField(response, "change"),
                percentField(response, "percent_change"),
                timestamp(response),
                "twelve-data"
        );
    }

    private String normalize(String ticker) {
        return ticker == null || ticker.isBlank()
                ? "RDIS"
                : ticker.trim().toUpperCase(Locale.ROOT);
    }

    private BigDecimal moneyField(JsonNode response, String fieldName) {
        return new BigDecimal(textField(response, fieldName)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal percentField(JsonNode response, String fieldName) {
        return new BigDecimal(textField(response, fieldName).replace("%", "").trim())
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String textField(JsonNode response, String fieldName) {
        String value = optionalText(response, fieldName, "");
        if (value.isBlank()) {
            throw new IllegalStateException("Twelve Data response is missing field " + fieldName + ".");
        }
        return value.trim();
    }

    private OffsetDateTime timestamp(JsonNode response) {
        JsonNode timestamp = response.get("timestamp");
        if (timestamp != null && !timestamp.isNull() && !timestamp.asText().isBlank()) {
            return OffsetDateTime.ofInstant(Instant.ofEpochSecond(timestamp.asLong()), ZoneOffset.UTC);
        }

        String datetime = optionalText(response, "datetime", "");
        if (datetime.isBlank()) {
            return OffsetDateTime.now(ZoneOffset.UTC);
        }

        return LocalDateTime.parse(datetime.trim().replace(" ", "T")).atOffset(ZoneOffset.UTC);
    }

    private String optionalText(JsonNode node, String fieldName, String defaultValue) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull() || field.isMissingNode()) {
            return defaultValue;
        }
        return field.asText(defaultValue);
    }
}
