package com.redis.workshop.springai.fundamentals;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class MarketDataTools {

    private final AtomicBoolean invoked = new AtomicBoolean(false);

    private static final Map<String, StockSnapshot> SNAPSHOTS = Map.of(
            "AAPL", new StockSnapshot("AAPL", "Apple Inc.", new BigDecimal("187.42"), new BigDecimal("1.18"),
                    "Consumer hardware demand is steady with services growth offsetting margin pressure."),
            "MSFT", new StockSnapshot("MSFT", "Microsoft Corporation", new BigDecimal("421.56"), new BigDecimal("0.84"),
                    "Cloud demand remains resilient and AI infrastructure spend is elevated."),
            "NVDA", new StockSnapshot("NVDA", "NVIDIA Corporation", new BigDecimal("882.31"), new BigDecimal("2.64"),
                    "Data center revenue growth remains the dominant catalyst with valuation risk elevated."),
            "RDIS", new StockSnapshot("RDIS", "Redis Workshop Index", new BigDecimal("64.80"), new BigDecimal("1.92"),
                    "Synthetic workshop ticker showing healthy application performance demand."));

    public void resetInvocation() {
        invoked.set(false);
    }

    public boolean wasInvoked() {
        return invoked.get();
    }

    public StockSnapshot snapshotFor(String ticker) {
        String normalized = normalizeTicker(ticker);
        return SNAPSHOTS.getOrDefault(normalized, new StockSnapshot(normalized, normalized + " Synthetic Equity",
                new BigDecimal("101.25"), new BigDecimal("0.35"),
                "Synthetic fallback data for workshop tool calling demos."));
    }

    @Tool(name = "stock_snapshot", description = "Return a synthetic stock snapshot for a ticker symbol.")
    public StockSnapshot stockSnapshot(
            @ToolParam(description = "Ticker symbol, for example AAPL or MSFT.") String ticker) {
        invoked.set(true);
        return snapshotFor(ticker);
    }

    private String normalizeTicker(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            return "RDIS";
        }
        return ticker.trim().toUpperCase(Locale.ROOT);
    }

    public record StockSnapshot(String ticker, String companyName, BigDecimal price, BigDecimal changePercent,
                                String summary, Instant asOf) {

        public StockSnapshot(String ticker, String companyName, BigDecimal price, BigDecimal changePercent,
                             String summary) {
            this(ticker, companyName, price, changePercent, summary, Instant.parse("2026-05-04T09:00:00Z"));
        }
    }
}
