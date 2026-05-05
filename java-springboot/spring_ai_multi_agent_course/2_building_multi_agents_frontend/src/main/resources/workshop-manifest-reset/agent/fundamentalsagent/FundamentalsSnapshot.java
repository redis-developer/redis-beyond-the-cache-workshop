package com.redis.workshop.springai.multiagents.agent.fundamentalsagent;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record FundamentalsSnapshot(
        String ticker,
        String companyName,
        BigDecimal revenueGrowthPercent,
        BigDecimal grossMarginPercent,
        BigDecimal operatingMarginPercent,
        BigDecimal freeCashFlowMarginPercent,
        String summary,
        OffsetDateTime asOf,
        String source
) {
}
