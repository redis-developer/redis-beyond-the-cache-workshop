package com.redis.workshop.springai.fundamentals;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// Stage 1 imports to enable:
// import org.springframework.ai.chat.client.ChatClient;
// import org.springframework.ai.chat.model.ChatModel;

// Stage 2 imports to enable:
// import org.springframework.ai.chat.client.AdvisorParams;
// import org.springframework.ai.chat.client.ResponseEntity;
// import org.springframework.ai.chat.model.ChatResponse;

// Stage 5 import to enable:
// import java.util.UUID;

// Stage 4 imports to enable:
// import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
// import org.springframework.ai.chat.memory.ChatMemory;
// import org.springframework.ai.chat.memory.ChatMemoryRepository;

@Service
public class SpringAiDemoService {

    private static final String PLACEHOLDER_ANSWER = """
            Spring AI is not wired yet. This chat is only proving that the UI and controller path work.
            In Stage 1, replace this hardcoded response with a ChatClient call.
            """;

    private static final String NOT_IMPLEMENTED = "This part is not implemented yet.";

    private final String apiKey;
    private final String model;

    // Stage 1 fields to enable:
    // private final ChatModel chatModel;
    // private volatile ChatClient chatClient;

    // Stage 3 field to enable:
    // private final MarketDataTools marketDataTools;

    // Stage 4 fields to enable:
    // private final ChatMemory chatMemory;
    // private final ChatMemoryRepository chatMemoryRepository;
    // private volatile ChatClient memoryChatClient;

    public SpringAiDemoService(
            @Value("${spring.ai.openai.api-key:}") String apiKey,
            @Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}") String model
            // Stage 1 constructor parameter to enable:
            // , ChatModel chatModel
            // Stage 3 constructor parameter to enable:
            // , MarketDataTools marketDataTools
            // Stage 4 constructor parameters to enable:
            // , ChatMemory chatMemory
            // , ChatMemoryRepository chatMemoryRepository
    ) {
        this.apiKey = apiKey;
        this.model = model;
        // Stage 1 assignment to enable:
        // this.chatModel = chatModel;
        // Stage 3 assignment to enable:
        // this.marketDataTools = marketDataTools;
        // Stage 4 assignments to enable:
        // this.chatMemory = chatMemory;
        // this.chatMemoryRepository = chatMemoryRepository;
    }

    public StatusResponse status() {
        return new StatusResponse(apiKeyConfigured(), model);
    }

    public PromptResponse prompt(PromptRequest request) {
        return new PromptResponse(true, PLACEHOLDER_ANSWER.trim(), null);

        /*
        Stage 1 prompt block to enable:
        Comment out the hardcoded return above, then uncomment this block.

        try {
            String answer = client().prompt()
                    .system("You are a concise stock analysis assistant.")
                    .user(nonBlank(request.message(), "Explain what Spring AI ChatClient does."))
                    .call()
                    .content();

            return new PromptResponse(true, answer, null);
        } catch (Exception ex) {
            return new PromptResponse(false, null, ex.getMessage());
        }
        */
    }

    public StructuredResponse structured(PromptRequest request) {
        return new StructuredResponse(false, null, NOT_IMPLEMENTED);

        /*
        Stage 2 structured output block to enable:
        Comment out the scaffold return above, then uncomment this block.

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
        */
    }

    public ToolsResponse tools(ToolsRequest request) {
        return new ToolsResponse(false, null, null, false, NOT_IMPLEMENTED);

        /*
        Stage 3 tools block to enable:
        Comment out the scaffold return above, then uncomment this block.

        try {
            marketDataTools.resetInvocation();
            String answer = client().prompt()
                    .system("Answer stock questions by using the stock_snapshot tool before responding.")
                    .user("Ticker: " + nonBlank(request.ticker(), "RDIS") + "\\nQuestion: "
                            + nonBlank(request.question(), "What does the synthetic snapshot say?"))
                    .tools(marketDataTools)
                    .call()
                    .content();

            return new ToolsResponse(true, answer, marketDataTools.snapshotFor(request.ticker()),
                    marketDataTools.wasInvoked(), null);
        } catch (Exception ex) {
            return new ToolsResponse(false, null, marketDataTools.snapshotFor(request.ticker()),
                    marketDataTools.wasInvoked(), ex.getMessage());
        }
        */
    }

    public MemoryResponse memory(MemoryRequest request) {
        String conversationId = nonBlank(request.conversationId(), "fundamentals-demo");
        return new MemoryResponse(false, conversationId, null, 0, NOT_IMPLEMENTED);

        /*
        Stage 4 memory block to enable:
        Comment out the scaffold return above, then uncomment this block.

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
        */
    }

    public HelperResponse helper(HelperRequest request) {
        return new HelperResponse(false, null, null, false, NOT_IMPLEMENTED);

        /*
        Stage 5 final helper block to enable:
        Comment out the scaffold return above, then uncomment this block.

        try {
            String conversationId = nonBlank(request.conversationId(), UUID.randomUUID().toString());
            marketDataTools.resetInvocation();
            StockHelperResult result = memoryClient().prompt()
                    .system("""
                            You are a single stock helper.
                            Use the stock_snapshot tool first, then return the structured helper result.
                            action must be one of WATCH, REVIEW, or AVOID.
                            riskLevel must be LOW, MEDIUM, or HIGH.
                            """)
                    .user("Ticker: " + nonBlank(request.ticker(), "RDIS") + "\\nQuestion: "
                            + nonBlank(request.question(), "What should I notice in this snapshot?"))
                    .tools(marketDataTools)
                    .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                    .call()
                    .entity(StockHelperResult.class);

            return new HelperResponse(true, result, marketDataTools.snapshotFor(request.ticker()),
                    marketDataTools.wasInvoked(), null);
        } catch (Exception ex) {
            return new HelperResponse(false, null, marketDataTools.snapshotFor(request.ticker()),
                    marketDataTools.wasInvoked(), ex.getMessage());
        }
        */
    }

    /*
    Stage 1 client helper to enable:
    Uncomment this method after enabling the Stage 1 imports, fields, constructor parameter, and assignment.

    private ChatClient client() {
        ChatClient current = chatClient;
        if (current == null) {
            synchronized (this) {
                if (chatClient == null) {
                    chatClient = ChatClient.builder(chatModel).build();
                }
                current = chatClient;
            }
        }
        return current;
    }
    */

    /*
    Stage 4 memory client helper to enable:

    private ChatClient memoryClient() {
        ChatClient current = memoryChatClient;
        if (current == null) {
            synchronized (this) {
                if (memoryChatClient == null) {
                    memoryChatClient = ChatClient.builder(chatModel)
                            .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                            .build();
                }
                current = memoryChatClient;
            }
        }
        return current;
    }

    private int messageCount(String conversationId) {
        return chatMemoryRepository.findByConversationId(conversationId).size();
    }
    */

    private boolean apiKeyConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    private String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
