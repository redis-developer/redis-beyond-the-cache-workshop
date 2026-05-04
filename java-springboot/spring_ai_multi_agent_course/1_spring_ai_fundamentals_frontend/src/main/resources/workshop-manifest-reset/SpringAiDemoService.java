package com.redis.workshop.springai.fundamentals;

import java.util.UUID;

import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SpringAiDemoService {

    private static final String MISSING_API_KEY_ERROR =
            "OpenAI API key is not configured. Set OPENAI_API_KEY or spring.ai.openai.api-key.";

    private final String apiKey;
    private final String model;
    private final ChatMemory chatMemory;
    private final ChatMemoryRepository chatMemoryRepository;
    private final MarketDataTools marketDataTools;
    private volatile ChatClient chatClient;
    private volatile ChatClient memoryChatClient;

    public SpringAiDemoService(
            @Value("${spring.ai.openai.api-key:}") String apiKey,
            @Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}") String model,
            ChatMemory chatMemory,
            ChatMemoryRepository chatMemoryRepository,
            MarketDataTools marketDataTools) {
        this.apiKey = apiKey;
        this.model = model;
        this.chatMemory = chatMemory;
        this.chatMemoryRepository = chatMemoryRepository;
        this.marketDataTools = marketDataTools;
    }

    public StatusResponse status() {
        return new StatusResponse(apiKeyConfigured(), model);
    }

    public PromptResponse prompt(PromptRequest request) {
        if (!apiKeyConfigured()) {
            return new PromptResponse(false, null, MISSING_API_KEY_ERROR);
        }
        try {
            String answer = client().prompt()
                    .system("You are a concise Spring AI fundamentals assistant.")
                    .user(nonBlank(request.message(), "Explain what Spring AI ChatClient does."))
                    .call()
                    .content();
            return new PromptResponse(true, answer, null);
        } catch (Exception ex) {
            return new PromptResponse(false, null, ex.getMessage());
        }
    }

    public StructuredResponse structured(PromptRequest request) {
        if (!apiKeyConfigured()) {
            return new StructuredResponse(false, null, MISSING_API_KEY_ERROR);
        }
        try {
            ResponseEntity<ChatResponse, StockDecision> response = client().prompt()
                    .system("""
                            Extract a stock ticker and the user's main question.
                            Return concise reasoning for why those values were selected.
                            If no ticker is present, use RDIS.
                            """)
                    .user(nonBlank(request.message(), "Should I look at MSFT for an AI portfolio?"))
                    .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                    .call()
                    .responseEntity(StockDecision.class);
            StockDecision parsed = response.entity();
            String finishReason = response.response().getResult().getMetadata().getFinishReason();
            return new StructuredResponse(true,
                    new StockDecision(finishReason, parsed.resolvedTicker(), parsed.resolvedQuestion(),
                            parsed.reasoning()),
                    null);
        } catch (Exception ex) {
            return new StructuredResponse(false, null, ex.getMessage());
        }
    }

    public ToolsResponse tools(ToolsRequest request) {
        if (!apiKeyConfigured()) {
            return new ToolsResponse(false, null, snapshot(request.ticker()), false, MISSING_API_KEY_ERROR);
        }
        try {
            marketDataTools.resetInvocation();
            String answer = client().prompt()
                    .system("Answer stock questions by using the stock_snapshot tool before responding.")
                    .user("Ticker: " + nonBlank(request.ticker(), "RDIS") + "\nQuestion: "
                            + nonBlank(request.question(), "What does the synthetic snapshot say?"))
                    .tools(marketDataTools)
                    .call()
                    .content();
            return new ToolsResponse(true, answer, snapshot(request.ticker()), marketDataTools.wasInvoked(), null);
        } catch (Exception ex) {
            return new ToolsResponse(false, null, snapshot(request.ticker()), marketDataTools.wasInvoked(),
                    ex.getMessage());
        }
    }

    public MemoryResponse memory(MemoryRequest request) {
        String conversationId = nonBlank(request.conversationId(), UUID.randomUUID().toString());
        if (!apiKeyConfigured()) {
            return new MemoryResponse(false, conversationId, null, messageCount(conversationId), MISSING_API_KEY_ERROR);
        }
        try {
            String answer = memoryClient().prompt()
                    .system("You are a helpful assistant. Use the conversation history when it is relevant.")
                    .user(nonBlank(request.message(), "Remember that I am learning Spring AI."))
                    .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .call()
                    .content();
            return new MemoryResponse(true, conversationId, answer, messageCount(conversationId), null);
        } catch (Exception ex) {
            return new MemoryResponse(false, conversationId, null, messageCount(conversationId), ex.getMessage());
        }
    }

    public HelperResponse helper(HelperRequest request) {
        if (!apiKeyConfigured()) {
            return new HelperResponse(false, null, snapshot(request.ticker()), false, MISSING_API_KEY_ERROR);
        }
        try {
            marketDataTools.resetInvocation();
            StockHelperResult result = client().prompt()
                    .system("""
                            You are a single stock helper.
                            Use the stock_snapshot tool first, then return the structured helper result.
                            action must be one of WATCH, REVIEW, or AVOID.
                            riskLevel must be LOW, MEDIUM, or HIGH.
                            """)
                    .user("Ticker: " + nonBlank(request.ticker(), "RDIS") + "\nQuestion: "
                            + nonBlank(request.question(), "What should I notice in this snapshot?"))
                    .tools(marketDataTools)
                    .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                    .call()
                    .entity(StockHelperResult.class);
            return new HelperResponse(true, result, snapshot(request.ticker()), marketDataTools.wasInvoked(), null);
        } catch (Exception ex) {
            return new HelperResponse(false, null, snapshot(request.ticker()), marketDataTools.wasInvoked(),
                    ex.getMessage());
        }
    }

    private ChatClient client() {
        ChatClient current = chatClient;
        if (current == null) {
            synchronized (this) {
                if (chatClient == null) {
                    chatClient = ChatClient.builder(chatModel()).build();
                }
                current = chatClient;
            }
        }
        return current;
    }

    private ChatClient memoryClient() {
        ChatClient current = memoryChatClient;
        if (current == null) {
            synchronized (this) {
                if (memoryChatClient == null) {
                    memoryChatClient = ChatClient.builder(chatModel())
                            .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                            .build();
                }
                current = memoryChatClient;
            }
        }
        return current;
    }

    private ChatModel chatModel() {
        OpenAiApi openAiApi = OpenAiApi.builder().apiKey(apiKey).build();
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder().model(model).temperature(0.2).build())
                .build();
    }

    private boolean apiKeyConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    private int messageCount(String conversationId) {
        return chatMemoryRepository.findByConversationId(conversationId).size();
    }

    private MarketDataTools.StockSnapshot snapshot(String ticker) {
        return marketDataTools.snapshotFor(ticker);
    }

    private String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
