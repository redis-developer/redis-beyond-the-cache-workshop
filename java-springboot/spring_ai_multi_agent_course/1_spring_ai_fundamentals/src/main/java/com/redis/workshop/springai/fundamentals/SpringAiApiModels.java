package com.redis.workshop.springai.fundamentals;

record StatusResponse(boolean apiKeyConfigured, String model) {
}

record PromptRequest(String message) {
}

record ToolsRequest(String ticker, String question) {
}

record MemoryRequest(String conversationId, String message) {
}

record HelperRequest(String conversationId, String ticker, String question) {
}

record PromptResponse(boolean success, String answer, String error) {
}

record StructuredResponse(boolean success, StockDecision decision, String error) {
}

record ToolsResponse(boolean success, String answer, MarketDataTools.StockSnapshot snapshot, boolean toolInvoked,
                     String error) {
}

record MemoryResponse(boolean success, String conversationId, String answer, int messageCount, String error) {
}

record HelperResponse(boolean success, StockHelperResult result, MarketDataTools.StockSnapshot snapshot,
                      boolean toolInvoked, String error) {
}

record StockDecision(String finishReason, String resolvedTicker, String resolvedQuestion, String reasoning) {
}

record StockHelperResult(String ticker, String question, String answer, String action, String reasoning,
                         String riskLevel) {
}
