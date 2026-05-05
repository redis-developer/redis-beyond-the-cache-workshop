package com.redis.workshop.springai.multiagents.agent.newsagent;

import java.time.OffsetDateTime;
import java.util.List;

public record NewsSnapshot(
        String ticker,
        String companyName,
        List<String> headlines,
        String summary,
        OffsetDateTime asOf,
        String source
) {
}
