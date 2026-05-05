package com.redis.workshop.springai.multiagents.agent.marketdataagent;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record MarketSnapshot(
        String symbol,
        String companyName,
        BigDecimal currentPrice,
        BigDecimal previousClose,
        BigDecimal absoluteChange,
        BigDecimal percentChange,
        OffsetDateTime asOf,
        String source
) {
}
