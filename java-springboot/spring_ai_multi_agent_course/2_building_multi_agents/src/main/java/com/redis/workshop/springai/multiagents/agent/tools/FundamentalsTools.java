package com.redis.workshop.springai.multiagents.agent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.redis.workshop.springai.multiagents.agent.fundamentalsagent.FundamentalsSnapshot;
import com.redis.workshop.springai.multiagents.providers.FundamentalsProvider;

@Component
public class FundamentalsTools {

    private final FundamentalsProvider fundamentalsProvider;

    public FundamentalsTools(FundamentalsProvider fundamentalsProvider) {
        this.fundamentalsProvider = fundamentalsProvider;
    }

    public FundamentalsSnapshot previewSnapshot(String ticker) {
        return fundamentalsProvider.fetchSnapshot(ticker);
    }

    @Tool(description = "Fetch a simple fundamentals snapshot for a stock ticker.")
    public FundamentalsSnapshot getFundamentalsSnapshot(
            @ToolParam(description = "The stock ticker symbol in uppercase, for example MSFT.")
            String ticker
    ) {
        return fundamentalsProvider.fetchSnapshot(ticker);
    }
}
