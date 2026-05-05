package com.redis.workshop.springai.multiagents.memory;

import java.util.HashMap;

import com.redis.agentmemory.models.workingmemory.MemoryStrategyConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "agent-memory")
public class AgentMemoryProperties {

    private final Server server = new Server();
    private final WorkingMemory workingMemory = new WorkingMemory();

    public Server getServer() {
        return server;
    }

    public WorkingMemory getWorkingMemory() {
        return workingMemory;
    }

    public MemoryStrategyConfig toLongTermMemoryStrategy() {
        String strategy = workingMemory.strategy == null || workingMemory.strategy.isBlank()
                ? "discrete"
                : workingMemory.strategy.trim();
        HashMap<String, Object> config = new HashMap<>();

        if ("custom".equalsIgnoreCase(strategy)) {
            if (workingMemory.customPrompt == null || workingMemory.customPrompt.isBlank()) {
                throw new IllegalStateException(
                        "agent-memory.working-memory.custom-prompt is required when strategy is custom"
                );
            }
            config.put("custom_prompt", workingMemory.customPrompt);
        }

        return MemoryStrategyConfig.builder()
                .strategy(strategy)
                .config(config)
                .build();
    }

    public static class Server {

        private String url = "http://localhost:8000";
        private String namespace = "stock-analysis";

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }
    }

    public static class WorkingMemory {

        private String strategy = "discrete";
        private String customPrompt;

        public String getStrategy() {
            return strategy;
        }

        public void setStrategy(String strategy) {
            this.strategy = strategy;
        }

        public String getCustomPrompt() {
            return customPrompt;
        }

        public void setCustomPrompt(String customPrompt) {
            this.customPrompt = customPrompt;
        }
    }
}
