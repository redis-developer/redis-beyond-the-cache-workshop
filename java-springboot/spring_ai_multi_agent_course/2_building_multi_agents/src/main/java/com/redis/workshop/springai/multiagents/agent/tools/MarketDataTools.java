package com.redis.workshop.springai.multiagents.agent.tools;

// Stage 3 imports to enable:
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.redis.workshop.springai.multiagents.agent.marketdataagent.MarketSnapshot;
import com.redis.workshop.springai.multiagents.providers.MarketDataProvider;

@Component
public class MarketDataTools {

    private final MarketDataProvider marketDataProvider;

    public MarketDataTools(MarketDataProvider marketDataProvider) {
        this.marketDataProvider = marketDataProvider;
    }

    public MarketSnapshot previewSnapshot(String ticker) {
        return marketDataProvider.fetchSnapshot(ticker);
    }

    // Stage 3 tool method enabled.

    @Tool(description = "Fetch the latest market snapshot for a stock ticker.")
    public MarketSnapshot getMarketSnapshot(
            @ToolParam(description = "The stock ticker symbol in uppercase, for example AAPL.")
            String ticker
    ) {
        return marketDataProvider.fetchSnapshot(ticker);
    }
}
