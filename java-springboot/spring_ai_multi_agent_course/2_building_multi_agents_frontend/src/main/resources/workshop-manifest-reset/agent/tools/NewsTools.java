package com.redis.workshop.springai.multiagents.agent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.redis.workshop.springai.multiagents.agent.newsagent.NewsSnapshot;
import com.redis.workshop.springai.multiagents.providers.NewsProvider;

@Component
public class NewsTools {

    private final NewsProvider newsProvider;

    public NewsTools(NewsProvider newsProvider) {
        this.newsProvider = newsProvider;
    }

    public NewsSnapshot previewSnapshot(String ticker) {
        return newsProvider.fetchSnapshot(ticker);
    }

    @Tool(description = "Fetch a short news snapshot for a stock ticker.")
    public NewsSnapshot getNewsSnapshot(
            @ToolParam(description = "The stock ticker symbol in uppercase, for example MSFT.")
            String ticker
    ) {
        return newsProvider.fetchSnapshot(ticker);
    }
}
