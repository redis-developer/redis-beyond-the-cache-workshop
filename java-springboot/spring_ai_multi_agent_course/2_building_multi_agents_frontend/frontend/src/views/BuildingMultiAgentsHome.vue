<template>
  <div class="multi-agents-home">
    <main v-if="isEmbeddedLearnerApp" class="multi-agents-chat-app">
      <header class="multi-agents-chat-app__header">
        <div>
          <p class="multi-agents-chat-app__eyebrow">Learner app</p>
          <h1 class="multi-agents-chat-app__title">Stock Analysis Chat</h1>
        </div>
        <span
          class="multi-agents-chat-app__status"
          :class="{ 'multi-agents-chat-app__status--warning': chatStatusWarning }"
        >
          {{ chatStatusLabel }}
        </span>
      </header>

      <section
        ref="chatTranscript"
        class="multi-agents-chat-app__transcript"
        aria-label="Stock analysis chat transcript"
      >
        <article
          v-for="message in chatMessages"
          :key="message.id"
          class="multi-agents-chat-message"
          :class="`multi-agents-chat-message--${message.role}`"
        >
          <p class="multi-agents-chat-message__role">
            {{ message.role === 'user' ? 'You' : 'Assistant' }}
          </p>
          <p class="multi-agents-chat-message__text">{{ message.text }}</p>

          <div v-if="message.metrics?.length" class="multi-agents-chat-message__metrics">
            <span
              v-for="metric in message.metrics"
              :key="`${message.id}-${metric.label}`"
              class="multi-agents-chat-message__metric"
            >
              <span>{{ metric.label }}</span>
              <strong>{{ metric.value }}</strong>
            </span>
          </div>

          <div v-if="message.steps?.length" class="multi-agents-chat-steps">
            <p class="multi-agents-chat-steps__title">Agent steps</p>
            <ol class="multi-agents-chat-steps__list">
              <li
                v-for="step in message.steps"
                :key="`${message.id}-${step.agent}-${step.message}`"
                class="multi-agents-chat-steps__item"
              >
                <strong>{{ step.agent }}</strong>
                <span>{{ step.message }}</span>
              </li>
            </ol>
          </div>

          <details v-if="message.detailsJson" class="multi-agents-chat-details">
            <summary>Structured details</summary>
            <pre>{{ message.detailsJson }}</pre>
          </details>
        </article>
      </section>

      <form class="multi-agents-chat-composer" @submit.prevent="sendChatMessage">
        <label class="multi-agents-chat-composer__message">
          <span>Message</span>
          <textarea
            v-model="chatInput"
            rows="3"
            placeholder="Ask about a stock, for example: What is the current price for Duolingo?"
            aria-label="Message"
            @keydown.enter.exact.prevent="sendChatMessage"
          ></textarea>
        </label>
        <button
          class="multi-agents-chat-composer__send"
          type="submit"
          :disabled="chatBusy || !chatInput.trim()"
        >
          {{ chatBusy ? 'Sending' : 'Send' }}
        </button>
      </form>
    </main>

    <WorkshopShell
      v-else
      title="Module 2: Building Multi Agents"
      eyebrow=""
      summary=""
      :runtime="shellRuntime"
      :links="shellLinks"
      :actions="shellActions"
      :learner-app="learnerApp"
      :runtime-logs="runtimeLogs"
      :runtime-actions-disabled="runtimeInteractionBusy"
      :default-side-panel="defaultSidePanel"
      @runtime-action="handleRuntimeAction"
      @logs-toggle="handleRuntimeLogsToggle"
    >
      <template #instructions>
        <div class="multi-agents-instructions">
          <header class="multi-agents-instructions__header">
            <p class="multi-agents-instructions__title">{{ instructionsHeaderTitle }}</p>
          </header>

          <div class="multi-agents-instructions__body">
            <p
              v-if="editorStepStatusMessage"
              class="editor-step-status"
              :class="editorStepStatusClass"
              aria-live="polite"
            >
              {{ editorStepStatusMessage }}
            </p>
            <div v-if="contentError" class="content-state content-state--error">
              {{ contentError }}
            </div>
            <div v-else-if="!content" class="content-state">
              Loading workshop instructions...
            </div>
            <template v-else>
              <WorkshopContentRenderer
                :content="content"
                :context="contentContext"
                :show-title="false"
                :show-summary="false"
                :show-stage-title="false"
                @action="handleContentAction"
              />

              <nav
                v-if="hasStageNavigation"
                class="stage-navigation"
                aria-label="Stage navigation"
              >
                <button
                  v-if="previousStage"
                  class="stage-navigation__button stage-navigation__button--previous"
                  type="button"
                  :aria-label="`Go to ${previousStageAriaLabel}`"
                  @click="goToPage(previousStage)"
                >
                  <span aria-hidden="true">&larr;</span>
                  <span class="stage-navigation__label">{{ previousStageLabel }}</span>
                </button>

                <button
                  v-if="nextStage"
                  class="stage-navigation__button stage-navigation__button--next"
                  type="button"
                  :aria-label="`Go to ${nextStageAriaLabel}`"
                  @click="goToPage(nextStage)"
                >
                  <span class="stage-navigation__label">{{ nextStageLabel }}</span>
                  <span aria-hidden="true">&rarr;</span>
                </button>
              </nav>
            </template>
          </div>
        </div>
      </template>
    </WorkshopShell>
  </div>
</template>

<script>
import {
  getApiUrl,
  getBasePath,
  getCodeEditorFileUrl,
  getCodeEditorUrl,
  getWorkshopHubUrl,
  loadEditorWorkspaceMetadata,
  WorkshopContentRenderer,
  WorkshopShell
} from '../../../../../../workshop-frontend-shared/src/index.js'
import { fetchWorkshopContent } from '../utils/workshopContent'

const READY_STATES = new Set(['CHILD_READY', 'READY', 'RUNNING'])
const FAILED_STATES = new Set(['CHILD_FAILED', 'DISABLED', 'FAILED'])
const BUSY_STATES = new Set(['CHILD_STARTING', 'REBUILDING', 'RESTARTING', 'STARTING'])
const POLL_INTERVAL_MS = 2000
const POLL_TIMEOUT_MS = 480000
const MIN_RUNTIME_BUSY_MS = 2000
const CONVERSATION_STORAGE_KEY = 'spring-ai-building-multi-agents-conversation-id'

const PAGES = [
  { id: '0', title: 'Designing systems' },
  { id: '1', title: 'Agent structure' },
  { id: '2', title: 'Agent contracts' },
  { id: '3', title: 'Market data agent' },
  { id: '4', title: 'Coordinator flow' },
  { id: '5', title: 'Orchestration' }
]

function normalizeRunnerState(state) {
  return String(state || '').trim().toUpperCase()
}

function isBusyRunnerState(state) {
  return BUSY_STATES.has(normalizeRunnerState(state))
}

function loadConversationId() {
  const existing = window.sessionStorage.getItem(CONVERSATION_STORAGE_KEY)
  if (existing) {
    return existing
  }

  const conversationId = `multi-agents-${Date.now()}-${Math.random().toString(16).slice(2)}`
  window.sessionStorage.setItem(CONVERSATION_STORAGE_KEY, conversationId)
  return conversationId
}

function isRebuildRunnerState(state) {
  return normalizeRunnerState(state) === 'REBUILDING'
}

const EDITOR_STEP_CONFIG = {
  '3.enableToolImports': {
    fileName: 'MarketDataTools.java',
    line: 3,
    transform: content => content
      .replace(
        '// import org.springframework.ai.tool.annotation.Tool;',
        'import org.springframework.ai.tool.annotation.Tool;'
      )
      .replace(
        '// import org.springframework.ai.tool.annotation.ToolParam;',
        'import org.springframework.ai.tool.annotation.ToolParam;'
      )
  },
  '3.enableToolMethod': {
    fileName: 'MarketDataTools.java',
    line: 22,
    transform: content => content
      .replace(
        `    /*
    Stage 3 tool method to enable:

    @Tool(description = "Fetch the latest market snapshot for a stock ticker.")`,
        `    // Stage 3 tool method enabled.

    @Tool(description = "Fetch the latest market snapshot for a stock ticker.")`
      )
      .replace(
        `    }
    */
}`,
        `    }
}`
      )
  },
  '3.enableConfigImports': {
    fileName: 'MarketDataAgentConfig.java',
    line: 3,
    transform: content => content
      .replace(
        '// import org.springframework.ai.chat.client.AdvisorParams;',
        'import org.springframework.ai.chat.client.AdvisorParams;'
      )
      .replace(
        '// import org.springframework.ai.chat.client.ChatClient;',
        'import org.springframework.ai.chat.client.ChatClient;'
      )
      .replace(
        '// import org.springframework.ai.chat.model.ChatModel;',
        'import org.springframework.ai.chat.model.ChatModel;'
      )
      .replace(
        '// import org.springframework.context.annotation.Bean;',
        'import org.springframework.context.annotation.Bean;'
      )
      .replace(
        '// import org.springframework.context.annotation.Configuration;',
        'import org.springframework.context.annotation.Configuration;'
      )
  },
  '3.enableConfigAnnotation': {
    fileName: 'MarketDataAgentConfig.java',
    line: 10,
    transform: content => content
      .replace(
        `/*
Stage 3 annotation to enable:
@Configuration
*/
public class MarketDataAgentConfig {`,
        `@Configuration
public class MarketDataAgentConfig {`
      )
  },
  '3.enableDefaultPrompt': {
    fileName: 'MarketDataAgentConfig.java',
    line: 13,
    transform: content => content
      .replace(
        /    private static final String DEFAULT_PROMPT = """[\s\S]*?            """;/,
      `    private static final String DEFAULT_PROMPT = """
            ROLE
            You are the Market Data Agent for a stock-analysis system.

            RESPONSIBILITY
            Use the available tools to fetch current market data for the requested ticker and return a grounded result.

            RULES
            - Always use the market-data tools before returning a completed result.
            - Never invent prices, percentages, timestamps, or sources.
            - Use the exact tool result to populate finalResponse.
            - Keep message concise and directly useful to the user.
            - Return valid JSON matching the requested schema.

            COMPLETION
            - Return finishReason = COMPLETED when finalResponse is available.
            - Return finishReason = ERROR only when the task cannot be completed.
            """;`
      )
  },
  '3.enableChatClientBean': {
    fileName: 'MarketDataAgentConfig.java',
    line: 36,
    transform: content => content
      .replace(
        `    /*
    Stage 3 ChatClient bean to enable:

    @Bean("marketDataChatClient")`,
        `    // Stage 3 ChatClient bean enabled.

    @Bean("marketDataChatClient")`
      )
      .replace(
        `    }
    */
}`,
        `    }
}`
      )
  },
  '3.enableAgentImports': {
    fileName: 'MarketDataAgent.java',
    line: 3,
    transform: content => content
      .replace(
        '// import org.springframework.ai.chat.client.ChatClient;',
        'import org.springframework.ai.chat.client.ChatClient;'
      )
      .replace(
        '// import org.springframework.ai.chat.client.ResponseEntity;',
        'import org.springframework.ai.chat.client.ResponseEntity;'
      )
      .replace(
        '// import org.springframework.ai.chat.model.ChatResponse;',
        'import org.springframework.ai.chat.model.ChatResponse;'
      )
      .replace(
        '// import org.springframework.beans.factory.annotation.Qualifier;',
        'import org.springframework.beans.factory.annotation.Qualifier;'
      )
      .replace(
        '// import org.springframework.stereotype.Service;',
        'import org.springframework.stereotype.Service;'
      )
      .replace(
        '// import com.redis.workshop.springai.multiagents.agent.orchestration.TokenUsageSummary;',
        'import com.redis.workshop.springai.multiagents.agent.orchestration.TokenUsageSummary;'
      )
  },
  '3.enableAgentAnnotation': {
    fileName: 'MarketDataAgent.java',
    line: 11,
    transform: content => content
      .replace(
        `/*
Stage 3 annotation to enable:
@Service
*/
public class MarketDataAgent {`,
        `@Service
public class MarketDataAgent {`
      )
  },
  '3.enableAgentConstructor': {
    fileName: 'MarketDataAgent.java',
    line: 14,
    transform: content => content
      .replace(
        `    /*
    Stage 3 field and constructor to enable:

    private final ChatClient marketDataChatClient;`,
        `    // Stage 3 field and constructor enabled.

    private final ChatClient marketDataChatClient;`
      )
      .replace(
        `    }
    */

    public MarketDataResult execute`,
        `    }

    public MarketDataResult execute`
      )
  },
  '3.enableAgentExecute': {
    fileName: 'MarketDataAgent.java',
    line: 22,
    transform: content => content
      .replace(
        '        return MarketDataResult.error("Market Data Agent is not wired yet.");',
        '        // return MarketDataResult.error("Market Data Agent is not wired yet.");'
      )
      .replace(
        /        \/\*\n        Stage 3 execute block to enable:[\s\S]*?        \*\/\n    }\n\n    private String buildPrompt/,
        `        // Stage 3 execute block enabled.

        ResponseEntity<ChatResponse, MarketDataResult> response = marketDataChatClient
                .prompt()
                .user(buildPrompt(ticker, question))
                .call()
                .responseEntity(MarketDataResult.class);

        TokenUsageSummary tokenUsage = TokenUsageSummary.from(response.response());
        MarketDataResult entity = response.entity();

        if (entity == null || entity.getFinalResponse() == null
                || entity.getFinishReason() != MarketDataResult.FinishReason.COMPLETED) {
            throw new IllegalStateException("Market Data Agent returned an invalid response.");
        }

        entity.setTokenUsage(tokenUsage);
        return entity;
    }

    private String buildPrompt`
      )
  },
  '3.enableRuntimePrompt': {
    fileName: 'MarketDataAgent.java',
    line: 48,
    transform: content => content
      .replace('        return "";', '        // return "";')
      .replace(
        /        \/\*\n        Stage 3 runtime prompt to enable:[\s\S]*?        \*\/\n    }\n}/,
        `        // Stage 3 runtime prompt enabled.

        return """
                TICKER
                %s

                USER_QUESTION
                %s

                INSTRUCTIONS
                Use the available tool to fetch the current market snapshot.
                Populate finalResponse with the exact tool values.
                Keep message to one concise sentence.
                """.formatted(ticker.toUpperCase(), question);
    }
}`
      )
  },
  '3.enableServiceDependency': {
    fileName: 'MultiAgentsService.java',
    line: 6,
    transform: content => content
      .replace(
        '// import com.redis.workshop.springai.multiagents.agent.marketdataagent.MarketDataAgent;',
        'import com.redis.workshop.springai.multiagents.agent.marketdataagent.MarketDataAgent;'
      )
      .replace(
        '// private final MarketDataAgent marketDataAgent;',
        'private final MarketDataAgent marketDataAgent;'
      )
      .replace('// , MarketDataAgent marketDataAgent', ', MarketDataAgent marketDataAgent')
      .replace('// this.marketDataAgent = marketDataAgent;', 'this.marketDataAgent = marketDataAgent;')
  },
  '3.enableServiceRun': {
    fileName: 'MultiAgentsService.java',
    line: 44,
    transform: content => content
      .replace(
        `        return new MultiAgentsRunResponse(true, PLACEHOLDER_ANSWER.trim(), List.of(
                new MultiAgentStep("Coordinator Agent", "Received: " + userMessage),
                new MultiAgentStep("Market Data Agent", "Stage 3 will run this specialist."),
                new MultiAgentStep("Fundamentals Agent", "Stage 5 will run this specialist."),
                new MultiAgentStep("News Agent", "Stage 5 will run this specialist."),
                new MultiAgentStep("Synthesis Agent", "Later stages will combine specialist outputs.")
        ), null, null, null, null, null, null);`,
        `        // return new MultiAgentsRunResponse(true, PLACEHOLDER_ANSWER.trim(), List.of(
        //         new MultiAgentStep("Coordinator Agent", "Received: " + userMessage),
        //         new MultiAgentStep("Market Data Agent", "Stage 3 will run this specialist."),
        //         new MultiAgentStep("Fundamentals Agent", "Stage 5 will run this specialist."),
        //         new MultiAgentStep("News Agent", "Stage 5 will run this specialist."),
        //         new MultiAgentStep("Synthesis Agent", "Later stages will combine specialist outputs.")
        // ), null, null, null, null, null, null);`
      )
      .replace(
        `        /*
        Stage 3 run block to enable:
        Comment out the placeholder return above, then uncomment this block.

        String ticker = nonBlank(request.ticker(), "NVDA").toUpperCase();
        var result = marketDataAgent.execute(ticker, task);`,
        `        // Stage 3 run block enabled.

        String ticker = nonBlank(request.ticker(), "NVDA").toUpperCase();
        var result = marketDataAgent.execute(ticker, task);`
      )
      .replace(
        `        return new MultiAgentsRunResponse(true, result.getMessage(), List.of(
                new MultiAgentStep("Coordinator Agent", "Using the requested ticker " + ticker + "."),
                new MultiAgentStep("Market Data Agent", "Fetched a market snapshot with a Spring AI tool."),
                new MultiAgentStep("Fundamentals Agent", "Not implemented yet."),
                new MultiAgentStep("News Agent", "Not implemented yet."),
                new MultiAgentStep("Synthesis Agent", "Not implemented yet.")
        ), result, null, null, null, null, null);
        */
    }`,
        `        return new MultiAgentsRunResponse(true, result.getMessage(), List.of(
                new MultiAgentStep("Coordinator Agent", "Using the requested ticker " + ticker + "."),
                new MultiAgentStep("Market Data Agent", "Fetched a market snapshot with a Spring AI tool."),
                new MultiAgentStep("Fundamentals Agent", "Not implemented yet."),
                new MultiAgentStep("News Agent", "Not implemented yet."),
                new MultiAgentStep("Synthesis Agent", "Not implemented yet.")
        ), result, null, null, null, null, null);
    }`
      )
  },
  '4.enableRoutingConfigImports': {
    fileName: 'CoordinatorRoutingAgentConfig.java',
    line: 3,
    transform: content => content
      .replace('// import org.springframework.ai.chat.client.AdvisorParams;', 'import org.springframework.ai.chat.client.AdvisorParams;')
      .replace('// import org.springframework.ai.chat.client.ChatClient;', 'import org.springframework.ai.chat.client.ChatClient;')
      .replace('// import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;', 'import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;')
      .replace('// import org.springframework.ai.chat.memory.ChatMemory;', 'import org.springframework.ai.chat.memory.ChatMemory;')
      .replace('// import org.springframework.ai.chat.model.ChatModel;', 'import org.springframework.ai.chat.model.ChatModel;')
      .replace('// import org.springframework.context.annotation.Bean;', 'import org.springframework.context.annotation.Bean;')
      .replace('// import org.springframework.context.annotation.Configuration;', 'import org.springframework.context.annotation.Configuration;')
  },
  '4.enableRoutingConfigAnnotation': {
    fileName: 'CoordinatorRoutingAgentConfig.java',
    line: 10,
    transform: content => content.replace(
      `/*
Stage 4 annotation to enable:
@Configuration
*/
public class CoordinatorRoutingAgentConfig {`,
      `@Configuration
public class CoordinatorRoutingAgentConfig {`
    )
  },
  '4.enableRoutingPrompt': {
    fileName: 'CoordinatorRoutingAgentConfig.java',
    line: 13,
    transform: content => content.replace(
      /    private static final String DEFAULT_PROMPT = """[\s\S]*?            """;/,
      `    private static final String DEFAULT_PROMPT = """
            ROLE
            You are the Coordinator Routing Agent for a stock-analysis system.

            RESPONSIBILITY
            Decide whether the request is in scope, whether you need more information, and which specialized agents should run.

            AVAILABLE AGENTS
            - MARKET_DATA: quote, recent price movement, basic price context
            - FUNDAMENTALS: financial health, valuation, earnings, margins, revenue trends
            - NEWS: recent events, headlines, macro or company-specific developments

            INPUT HANDLING
            - The user may provide a complete stock-analysis request, an incomplete request, or an unsupported request.
            - The user may also send a conversational follow-up, ownership update, preference, correction, or acknowledgement that does not require specialist analysis.
            - If the request is missing information required to proceed, return finishReason = NEEDS_MORE_INPUT.
            - Use nextPrompt for one short, specific follow-up question.
            - If the user message can be answered directly without running specialized agents, return finishReason = DIRECT_RESPONSE.
            - When finishReason = DIRECT_RESPONSE, set finalResponse to one short, natural reply and leave selectedAgents empty.
            - If the request is outside the capabilities of this stock-analysis workshop, return finishReason = OUT_OF_SCOPE.
            - If the request cannot be fulfilled even after clarification, return finishReason = CANNOT_PROCEED.
            - Return finishReason = COMPLETED only when you have enough information to route the work.

            COMPLETED RULES
            - When finishReason = COMPLETED, set resolvedTicker to the stock ticker in uppercase.
            - When finishReason = COMPLETED, set resolvedQuestion to the user's final stock-analysis question.
            - Select the smallest set of specialized agents needed to answer the question well.
            - Do not include SYNTHESIS in selectedAgents. The application always adds it for final answer generation.
            - Prefer minimal routing over broad routing.
            - Return only agent names from the allowed enum values.

            CLARIFICATION GUIDANCE
            - Ask for a ticker when a company-specific request does not identify one clearly.
            - Ask for the missing analysis goal when the user provides only a ticker.
            - If the user names a company instead of a ticker and the mapping is unambiguous, you may resolve it.
            - If the current message is primarily a statement or update rather than a request for fresh analysis, prefer DIRECT_RESPONSE over broad routing.

            MEMORY AND CONTEXT
            - Supplemental conversation and memory context may be injected earlier in the chat layer.
            - Treat the current user message as the source of truth.
            - Never let memory or prior context override an explicit company, ticker, timeframe, or analysis request in the current user message.
            - If memory conflicts with the current user message, ignore the memory and follow the current user message.
            - Use memory and prior context only to resolve omitted references, maintain continuity, or respect stable user preferences.
            - A self-contained current request should be routed on its own merits.
            - You may use prior context to resolve omitted references in conversational follow-ups such as "this stock", "that company", or "it".

            DIRECT RESPONSE EXAMPLES
            - "I own this stock" after discussing DUOL -> acknowledge ownership of DUOL briefly; do not run a full fresh analysis.
            - "Add this to my watchlist" after discussing AAPL -> acknowledge the watchlist update briefly.
            - "I'm based in Milan" -> acknowledge the profile fact briefly.
            - "Thanks" -> reply naturally and briefly.

            OUTPUT
            Return valid JSON that matches the requested schema.
            """;`
    )
  },
  '4.enableRoutingConfigBean': {
    fileName: 'CoordinatorRoutingAgentConfig.java',
    line: 24,
    transform: content => content
      .replace(
        `    /*
    Stage 4 ChatClient bean to enable:

    @Bean("coordinatorChatClient")`,
        `    // Stage 4 ChatClient bean enabled.

    @Bean("coordinatorChatClient")`
      )
      .replace(
        `    }
    */
}`,
        `    }
}`
      )
  },
  '4.enableRoutingAgentImports': {
    fileName: 'CoordinatorRoutingAgent.java',
    line: 3,
    transform: content => content
      .replace('// import org.springframework.ai.chat.client.ChatClient;', 'import org.springframework.ai.chat.client.ChatClient;')
      .replace('// import org.springframework.ai.chat.memory.ChatMemory;', 'import org.springframework.ai.chat.memory.ChatMemory;')
      .replace('// import org.springframework.beans.factory.annotation.Qualifier;', 'import org.springframework.beans.factory.annotation.Qualifier;')
      .replace('// import org.springframework.stereotype.Service;', 'import org.springframework.stereotype.Service;')
  },
  '4.enableRoutingAgentAnnotation': {
    fileName: 'CoordinatorRoutingAgent.java',
    line: 8,
    transform: content => content.replace(
      `/*
Stage 4 annotation to enable:
@Service
*/
public class CoordinatorRoutingAgent {`,
      `@Service
public class CoordinatorRoutingAgent {`
    )
  },
  '4.enableRoutingAgentConstructor': {
    fileName: 'CoordinatorRoutingAgent.java',
    line: 12,
    transform: content => content
      .replace(
        `    /*
    Stage 4 field and constructor to enable:

    private final ChatClient coordinatorChatClient;`,
        `    // Stage 4 field and constructor enabled.

    private final ChatClient coordinatorChatClient;`
      )
      .replace(
        `    }
    */

    public RoutingDecision route`,
        `    }

    public RoutingDecision route`
      )
  },
  '4.enableRoutingAgentRoute': {
    fileName: 'CoordinatorRoutingAgent.java',
    line: 23,
    transform: content => content
      .replace(
        '        throw new UnsupportedOperationException("Stage 4: implement route(...)");',
        '        // throw new UnsupportedOperationException("Stage 4: implement route(...)");'
      )
      .replace(
        /        \/\*\n        Stage 4 route block to enable:[\s\S]*?        \*\/\n    }\n}/,
        `        // Stage 4 route block enabled.

        RoutingDecision decision = coordinatorChatClient.prompt()
                .user(userMessage)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, ChatMemory.DEFAULT_CONVERSATION_ID))
                .call()
                .entity(RoutingDecision.class);

        if (decision == null || decision.getFinishReason() == null) {
            throw new IllegalStateException("Coordinator returned an invalid routing decision.");
        }

        return decision;
    }
}`
      )
  },
  '4.enableCoordinatorAgentService': {
    fileName: 'CoordinatorAgent.java',
    line: 10,
    transform: content => content
      .replace('// import org.springframework.stereotype.Service;', 'import org.springframework.stereotype.Service;')
      .replace(
        `/*
Stage 4 annotation to enable:
@Service
*/
public class CoordinatorAgent {`,
        `@Service
public class CoordinatorAgent {`
      )
  },
  '5.enableFundamentalsConfig': {
    fileName: 'FundamentalsAgentConfig.java',
    line: 3,
    transform: content => content
      .replace('// import org.springframework.ai.chat.client.AdvisorParams;', 'import org.springframework.ai.chat.client.AdvisorParams;')
      .replace('// import org.springframework.ai.chat.client.ChatClient;', 'import org.springframework.ai.chat.client.ChatClient;')
      .replace('// import org.springframework.ai.chat.model.ChatModel;', 'import org.springframework.ai.chat.model.ChatModel;')
      .replace('// import org.springframework.context.annotation.Bean;', 'import org.springframework.context.annotation.Bean;')
      .replace('// import org.springframework.context.annotation.Configuration;', 'import org.springframework.context.annotation.Configuration;')
      .replace('// import com.redis.workshop.springai.multiagents.agent.tools.FundamentalsTools;', 'import com.redis.workshop.springai.multiagents.agent.tools.FundamentalsTools;')
      .replace(
        `/*
Stage 5 annotation to enable:
@Configuration
*/
public class FundamentalsAgentConfig {`,
        `@Configuration
public class FundamentalsAgentConfig {`
      )
      .replace(
        /    private static final String DEFAULT_PROMPT = """[\s\S]*?            """;/,
        `    private static final String DEFAULT_PROMPT = """
            ROLE
            You are the Fundamentals Agent for a stock-analysis system.

            RESPONSIBILITY
            Use the available tool to fetch a grounded fundamentals snapshot for the requested ticker and return a concise investor-focused result.

            RULES
            - Always use the fundamentals tool before returning a completed result.
            - Never invent revenue, income, margins, valuation ratios, filing dates, or source fields.
            - Use the exact tool result to populate finalResponse.
            - message should answer the user's question in plain language and stay concise.
            - Return valid JSON matching the requested schema.

            COMPLETION
            - Return finishReason = COMPLETED when finalResponse is available.
            - Return finishReason = ERROR only when the task cannot be completed.
            """;`
      )
      .replace(
        `    /*
    Stage 5 ChatClient bean to enable:

    @Bean("fundamentalsChatClient")`,
        `    // Stage 5 ChatClient bean enabled.

    @Bean("fundamentalsChatClient")`
      )
      .replace(
        `    }
    */
}`,
        `    }
}`
      )
  },
  '5.enableFundamentalsAgent': {
    fileName: 'FundamentalsAgent.java',
    line: 3,
    transform: content => content
      .replace('// import org.springframework.ai.chat.client.ChatClient;', 'import org.springframework.ai.chat.client.ChatClient;')
      .replace('// import org.springframework.ai.chat.client.ResponseEntity;', 'import org.springframework.ai.chat.client.ResponseEntity;')
      .replace('// import org.springframework.ai.chat.model.ChatResponse;', 'import org.springframework.ai.chat.model.ChatResponse;')
      .replace('// import org.springframework.beans.factory.annotation.Qualifier;', 'import org.springframework.beans.factory.annotation.Qualifier;')
      .replace('// import org.springframework.stereotype.Service;', 'import org.springframework.stereotype.Service;')
      .replace('// import com.redis.workshop.springai.multiagents.agent.orchestration.TokenUsageSummary;', 'import com.redis.workshop.springai.multiagents.agent.orchestration.TokenUsageSummary;')
      .replace(
        `/*
Stage 5 annotation to enable:
@Service
*/
public class FundamentalsAgent {`,
        `@Service
public class FundamentalsAgent {`
      )
      .replace(
        `    /*
    Stage 5 field and constructor to enable:

    private final ChatClient fundamentalsChatClient;`,
        `    // Stage 5 field and constructor enabled.

    private final ChatClient fundamentalsChatClient;`
      )
      .replace(
        `    }
    */

    public FundamentalsResult execute`,
        `    }

    public FundamentalsResult execute`
      )
      .replace(
        '        return FundamentalsResult.error("Fundamentals Agent is not wired yet.");',
        '        // return FundamentalsResult.error("Fundamentals Agent is not wired yet.");'
      )
      .replace(
        /        \/\*\n        Stage 5 execute block to enable:[\s\S]*?        \*\/\n    }\n\n    private String buildPrompt/,
        `        // Stage 5 execute block enabled.

        ResponseEntity<ChatResponse, FundamentalsResult> response = fundamentalsChatClient
                .prompt()
                .user(buildPrompt(ticker, question))
                .call()
                .responseEntity(FundamentalsResult.class);

        TokenUsageSummary tokenUsage = TokenUsageSummary.from(response.response());
        FundamentalsResult entity = response.entity();

        if (entity == null || entity.getFinalResponse() == null
                || entity.getFinishReason() != FundamentalsResult.FinishReason.COMPLETED) {
            throw new IllegalStateException("Fundamentals Agent returned an invalid response.");
        }

        entity.setTokenUsage(tokenUsage);
        return entity;
    }

    private String buildPrompt`
      )
      .replace('        return "";', '        // return "";')
      .replace(
        /        \/\*\n        Stage 5 runtime prompt to enable:[\s\S]*?        \*\/\n    }\n}/,
        `        // Stage 5 runtime prompt enabled.

        return """
                TICKER
                %s

                USER_QUESTION
                %s

                INSTRUCTIONS
                Use the available tool to fetch the fundamentals snapshot.
                Populate finalResponse with the exact tool values.
                Keep message to one concise sentence.
                """.formatted(ticker.toUpperCase(), question);
    }
}`
      )
  },
  '5.enableNewsConfig': {
    fileName: 'NewsAgentConfig.java',
    line: 3,
    transform: content => content
      .replace('// import org.springframework.ai.chat.client.AdvisorParams;', 'import org.springframework.ai.chat.client.AdvisorParams;')
      .replace('// import org.springframework.ai.chat.client.ChatClient;', 'import org.springframework.ai.chat.client.ChatClient;')
      .replace('// import org.springframework.ai.chat.model.ChatModel;', 'import org.springframework.ai.chat.model.ChatModel;')
      .replace('// import org.springframework.context.annotation.Bean;', 'import org.springframework.context.annotation.Bean;')
      .replace('// import org.springframework.context.annotation.Configuration;', 'import org.springframework.context.annotation.Configuration;')
      .replace('// import com.redis.workshop.springai.multiagents.agent.tools.NewsTools;', 'import com.redis.workshop.springai.multiagents.agent.tools.NewsTools;')
      .replace(
        `/*
Stage 5 annotation to enable:
@Configuration
*/
public class NewsAgentConfig {`,
        `@Configuration
public class NewsAgentConfig {`
      )
      .replace(
        /    private static final String DEFAULT_PROMPT = """[\s\S]*?            """;/,
        `    private static final String DEFAULT_PROMPT = """
            ROLE
            You are the News Agent for a stock-analysis system.

            RESPONSIBILITY
            Use the available tool to fetch a grounded hybrid news snapshot for the requested ticker and return a concise investor-focused result.

            RULES
            - Always use the news tool before returning a completed result.
            - Never invent filings, headlines, publishers, dates, summaries, or sources.
            - Use the exact tool result to populate finalResponse.
            - message should answer the user's question in plain language and stay concise.
            - Return valid JSON matching the requested schema.

            COMPLETION
            - Return finishReason = COMPLETED when finalResponse is available.
            - Return finishReason = ERROR only when the task cannot be completed.
            """;`
      )
      .replace(
        `    /*
    Stage 5 ChatClient bean to enable:

    @Bean("newsChatClient")`,
        `    // Stage 5 ChatClient bean enabled.

    @Bean("newsChatClient")`
      )
      .replace(
        `    }
    */
}`,
        `    }
}`
      )
  },
  '5.enableNewsAgent': {
    fileName: 'NewsAgent.java',
    line: 3,
    transform: content => content
      .replace('// import org.springframework.ai.chat.client.ChatClient;', 'import org.springframework.ai.chat.client.ChatClient;')
      .replace('// import org.springframework.ai.chat.client.ResponseEntity;', 'import org.springframework.ai.chat.client.ResponseEntity;')
      .replace('// import org.springframework.ai.chat.model.ChatResponse;', 'import org.springframework.ai.chat.model.ChatResponse;')
      .replace('// import org.springframework.beans.factory.annotation.Qualifier;', 'import org.springframework.beans.factory.annotation.Qualifier;')
      .replace('// import org.springframework.stereotype.Service;', 'import org.springframework.stereotype.Service;')
      .replace('// import com.redis.workshop.springai.multiagents.agent.orchestration.TokenUsageSummary;', 'import com.redis.workshop.springai.multiagents.agent.orchestration.TokenUsageSummary;')
      .replace(
        `/*
Stage 5 annotation to enable:
@Service
*/
public class NewsAgent {`,
        `@Service
public class NewsAgent {`
      )
      .replace(
        `    /*
    Stage 5 field and constructor to enable:

    private final ChatClient newsChatClient;`,
        `    // Stage 5 field and constructor enabled.

    private final ChatClient newsChatClient;`
      )
      .replace(
        `    }
    */

    public NewsResult execute`,
        `    }

    public NewsResult execute`
      )
      .replace(
        '        return NewsResult.error("News Agent is not wired yet.");',
        '        // return NewsResult.error("News Agent is not wired yet.");'
      )
      .replace(
        /        \/\*\n        Stage 5 execute block to enable:[\s\S]*?        \*\/\n    }\n\n    private String buildPrompt/,
        `        // Stage 5 execute block enabled.

        ResponseEntity<ChatResponse, NewsResult> response = newsChatClient
                .prompt()
                .user(buildPrompt(ticker, question))
                .call()
                .responseEntity(NewsResult.class);

        TokenUsageSummary tokenUsage = TokenUsageSummary.from(response.response());
        NewsResult entity = response.entity();

        if (entity == null || entity.getFinalResponse() == null
                || entity.getFinishReason() != NewsResult.FinishReason.COMPLETED) {
            throw new IllegalStateException("News Agent returned an invalid response.");
        }

        entity.setTokenUsage(tokenUsage);
        return entity;
    }

    private String buildPrompt`
      )
      .replace('        return "";', '        // return "";')
      .replace(
        /        \/\*\n        Stage 5 runtime prompt to enable:[\s\S]*?        \*\/\n    }\n}/,
        `        // Stage 5 runtime prompt enabled.

        return """
                TICKER
                %s

                USER_QUESTION
                %s

                INSTRUCTIONS
                Use the available tool to fetch the news snapshot.
                Populate finalResponse with the exact tool values.
                Keep message to one concise sentence.
                """.formatted(ticker.toUpperCase(), question);
    }
}`
      )
  },
  '5.enableSynthesisConfig': {
    fileName: 'SynthesisAgentConfig.java',
    line: 3,
    transform: content => content
      .replace('// import org.springframework.ai.chat.client.AdvisorParams;', 'import org.springframework.ai.chat.client.AdvisorParams;')
      .replace('// import org.springframework.ai.chat.client.ChatClient;', 'import org.springframework.ai.chat.client.ChatClient;')
      .replace('// import org.springframework.ai.chat.model.ChatModel;', 'import org.springframework.ai.chat.model.ChatModel;')
      .replace('// import org.springframework.context.annotation.Bean;', 'import org.springframework.context.annotation.Bean;')
      .replace('// import org.springframework.context.annotation.Configuration;', 'import org.springframework.context.annotation.Configuration;')
      .replace(
        `/*
Stage 5 annotation to enable:
@Configuration
*/
public class SynthesisAgentConfig {`,
        `@Configuration
public class SynthesisAgentConfig {`
      )
      .replace(
        /    private static final String DEFAULT_PROMPT = """[\s\S]*?            """;/,
        `    private static final String DEFAULT_PROMPT = """
            ROLE
            You are the Synthesis Agent for a stock-analysis system.

            RESPONSIBILITY
            Combine the structured outputs from specialized agents into one grounded answer.

            RULES
            - Use only the information provided in the prompt.
            - Do not invent prices, metrics, headlines, or technical signals.
            - Mention when signals are mixed or incomplete.
            - Be concise and practical for an investor who asked the question.
            - Do not mention internal agent names unless it helps clarify uncertainty.

            OUTPUT
            Return valid JSON matching the requested schema.
            The finalAnswer should be a concise paragraph or two.
            """;`
      )
      .replace(
        `    /*
    Stage 5 ChatClient bean to enable:

    @Bean("synthesisChatClient")`,
        `    // Stage 5 ChatClient bean enabled.

    @Bean("synthesisChatClient")`
      )
      .replace(
        `    }
    */
}`,
        `    }
}`
      )
  },
  '5.enableSynthesisAgent': {
    fileName: 'SynthesisAgent.java',
    line: 3,
    transform: content => content
      .replace('// import org.springframework.ai.chat.client.ChatClient;', 'import org.springframework.ai.chat.client.ChatClient;')
      .replace('// import org.springframework.ai.chat.client.ResponseEntity;', 'import org.springframework.ai.chat.client.ResponseEntity;')
      .replace('// import org.springframework.ai.chat.model.ChatResponse;', 'import org.springframework.ai.chat.model.ChatResponse;')
      .replace('// import org.springframework.beans.factory.annotation.Qualifier;', 'import org.springframework.beans.factory.annotation.Qualifier;')
      .replace('// import org.springframework.stereotype.Service;', 'import org.springframework.stereotype.Service;')
      .replace('// import com.redis.workshop.springai.multiagents.agent.orchestration.TokenUsageSummary;', 'import com.redis.workshop.springai.multiagents.agent.orchestration.TokenUsageSummary;')
      .replace(
        `/*
Stage 5 annotation to enable:
@Service
*/
public class SynthesisAgent {`,
        `@Service
public class SynthesisAgent {`
      )
      .replace(
        `    /*
    Stage 5 field and constructor to enable:

    private final ChatClient synthesisChatClient;`,
        `    // Stage 5 field and constructor enabled.

    private final ChatClient synthesisChatClient;`
      )
      .replace(
        `    }
    */

    public SynthesisResult execute`,
        `    }

    public SynthesisResult execute`
      )
      .replace(
        '        return new SynthesisResult("Synthesis Agent is not wired yet.", null);',
        '        // return new SynthesisResult("Synthesis Agent is not wired yet.", null);'
      )
      .replace(
        /        \/\*\n        Stage 5 execute block to enable:[\s\S]*?        \*\/\n    }\n\n    private String buildPrompt/,
        `        // Stage 5 execute block enabled.

        ResponseEntity<ChatResponse, SynthesisResponse> response = synthesisChatClient
                .prompt()
                .user(buildPrompt(request, marketData, fundamentals, news))
                .call()
                .responseEntity(SynthesisResponse.class);

        SynthesisResponse entity = response.entity();

        if (entity == null || entity.finalAnswer() == null || entity.finalAnswer().isBlank()) {
            throw new IllegalStateException("Synthesis Agent returned an invalid response.");
        }

        return new SynthesisResult(
                entity.finalAnswer().trim(),
                TokenUsageSummary.from(response.response())
        );
    }

    private String buildPrompt`
      )
      .replace('        return "";', '        // return "";')
      .replace(
        /        \/\*\n        Stage 5 runtime prompt to enable:[\s\S]*?        \*\/\n    }\n\n    private String section/,
        `        // Stage 5 runtime prompt enabled.

        return """
                QUESTION
                %s

                REQUESTED_TICKER
                %s

                MARKET_DATA
                %s

                FUNDAMENTALS
                %s

                NEWS
                %s

                INSTRUCTIONS
                Answer the user directly.
                Ground the answer in the specialist outputs.
                Keep the answer to one short paragraph.
                """.formatted(
                request.question(),
                request.ticker(),
                section(marketData, "No market data result."),
                section(fundamentals, "No fundamentals result."),
                section(news, "No news result.")
        );
    }

    private String section`
      )
  },
  '5.enableOrchestrationImports': {
    fileName: 'AgentOrchestrationService.java',
    line: 3,
    transform: content => content
      .replace('// import org.springframework.stereotype.Service;', 'import org.springframework.stereotype.Service;')
      .replace('// import com.redis.workshop.springai.multiagents.agent.coordinatoragent.CoordinatorAgent;', 'import com.redis.workshop.springai.multiagents.agent.coordinatoragent.CoordinatorAgent;')
      .replace('// import com.redis.workshop.springai.multiagents.agent.coordinatoragent.ExecutionPlan;', 'import com.redis.workshop.springai.multiagents.agent.coordinatoragent.ExecutionPlan;')
      .replace('// import com.redis.workshop.springai.multiagents.agent.coordinatoragent.RoutingDecision;', 'import com.redis.workshop.springai.multiagents.agent.coordinatoragent.RoutingDecision;')
      .replace('// import com.redis.workshop.springai.multiagents.agent.fundamentalsagent.FundamentalsAgent;', 'import com.redis.workshop.springai.multiagents.agent.fundamentalsagent.FundamentalsAgent;')
      .replace('// import com.redis.workshop.springai.multiagents.agent.fundamentalsagent.FundamentalsResult;', 'import com.redis.workshop.springai.multiagents.agent.fundamentalsagent.FundamentalsResult;')
      .replace('// import com.redis.workshop.springai.multiagents.agent.marketdataagent.MarketDataAgent;', 'import com.redis.workshop.springai.multiagents.agent.marketdataagent.MarketDataAgent;')
      .replace('// import com.redis.workshop.springai.multiagents.agent.marketdataagent.MarketDataResult;', 'import com.redis.workshop.springai.multiagents.agent.marketdataagent.MarketDataResult;')
      .replace('// import com.redis.workshop.springai.multiagents.agent.newsagent.NewsAgent;', 'import com.redis.workshop.springai.multiagents.agent.newsagent.NewsAgent;')
      .replace('// import com.redis.workshop.springai.multiagents.agent.newsagent.NewsResult;', 'import com.redis.workshop.springai.multiagents.agent.newsagent.NewsResult;')
      .replace('// import com.redis.workshop.springai.multiagents.agent.synthesisagent.SynthesisAgent;', 'import com.redis.workshop.springai.multiagents.agent.synthesisagent.SynthesisAgent;')
      .replace('// import com.redis.workshop.springai.multiagents.agent.synthesisagent.SynthesisResult;', 'import com.redis.workshop.springai.multiagents.agent.synthesisagent.SynthesisResult;')
  },
  '5.enableOrchestrationAnnotation': {
    fileName: 'AgentOrchestrationService.java',
    line: 11,
    transform: content => content.replace(
      `/*
Stage 5 annotation to enable:
@Service
*/
public class AgentOrchestrationService {`,
      `@Service
public class AgentOrchestrationService {`
    )
  },
  '5.enableOrchestrationConstructor': {
    fileName: 'AgentOrchestrationService.java',
    line: 15,
    transform: content => content
      .replace(
        `    /*
    Stage 5 fields and constructor to enable:

    private final CoordinatorAgent coordinatorAgent;`,
        `    // Stage 5 fields and constructor enabled.

    private final CoordinatorAgent coordinatorAgent;`
      )
      .replace(
        `    }
    */

    public AnalysisResponse analyze`,
        `    }

    public AnalysisResponse analyze`
      )
  },
  '5.enableOrchestrationAnalyze': {
    fileName: 'AgentOrchestrationService.java',
    line: 28,
    transform: content => content
      .replace(
        '        throw new UnsupportedOperationException("Stage 5: implement analyze(...)");',
        '        // throw new UnsupportedOperationException("Stage 5: implement analyze(...)");'
      )
      .replace(
        /        \/\*\n        Stage 5 orchestration block to enable:[\s\S]*?    \/\*\n    Stage 5 helper to enable:[\s\S]*?    \*\/\n}/,
        `        // Stage 5 orchestration block enabled.

        RoutingDecision decision = coordinatorAgent.execute(userMessage);

        if (decision.getFinishReason() != RoutingDecision.FinishReason.COMPLETED) {
            String answer = nonBlank(decision.getFinalResponse(), decision.getNextPrompt());
            return new AnalysisResponse(answer, decision, null, null, null, null, null);
        }

        ExecutionPlan plan = coordinatorAgent.createPlan(decision);
        AnalysisRequest request = coordinatorAgent.toAnalysisRequest(decision);
        MarketDataResult marketData = plan.selectedAgents().contains(AgentType.MARKET_DATA)
                ? marketDataAgent.execute(request.ticker(), request.question())
                : null;
        FundamentalsResult fundamentals = plan.selectedAgents().contains(AgentType.FUNDAMENTALS)
                ? fundamentalsAgent.execute(request.ticker(), request.question())
                : null;
        NewsResult news = plan.selectedAgents().contains(AgentType.NEWS)
                ? newsAgent.execute(request.ticker(), request.question())
                : null;
        SynthesisResult synthesis = synthesisAgent.execute(request, marketData, fundamentals, news);

        return new AnalysisResponse(synthesis.finalAnswer(), decision, plan, marketData, fundamentals, news, synthesis);
    }

    private String nonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }
}`
      )
  },
  '5.enableServiceOrchestrationDependency': {
    fileName: 'MultiAgentsService.java',
    line: 8,
    transform: content => content
      .replace('// import com.redis.workshop.springai.multiagents.agent.orchestration.AgentOrchestrationService;', 'import com.redis.workshop.springai.multiagents.agent.orchestration.AgentOrchestrationService;')
      .replace('// private final AgentOrchestrationService orchestrationService;', 'private final AgentOrchestrationService orchestrationService;')
      .replace('// , AgentOrchestrationService orchestrationService', ', AgentOrchestrationService orchestrationService')
      .replace('// this.orchestrationService = orchestrationService;', 'this.orchestrationService = orchestrationService;')
  },
  '5.enableServiceOrchestrationRun': {
    fileName: 'MultiAgentsService.java',
    line: 66,
    transform: content => content
      .replace('import java.util.List;', 'import java.util.ArrayList;\nimport java.util.List;')
      .replace(
        `        return new MultiAgentsRunResponse(true, result.getMessage(), List.of(
                new MultiAgentStep("Coordinator Agent", "Using the requested ticker " + ticker + "."),
                new MultiAgentStep("Market Data Agent", "Fetched a market snapshot with a Spring AI tool."),
                new MultiAgentStep("Fundamentals Agent", "Not implemented yet."),
                new MultiAgentStep("News Agent", "Not implemented yet."),
                new MultiAgentStep("Synthesis Agent", "Not implemented yet.")
        ), result, null, null, null, null, null);`,
        `        // return new MultiAgentsRunResponse(true, result.getMessage(), List.of(
        //         new MultiAgentStep("Coordinator Agent", "Using the requested ticker " + ticker + "."),
        //         new MultiAgentStep("Market Data Agent", "Fetched a market snapshot with a Spring AI tool."),
        //         new MultiAgentStep("Fundamentals Agent", "Not implemented yet."),
        //         new MultiAgentStep("News Agent", "Not implemented yet."),
        //         new MultiAgentStep("Synthesis Agent", "Not implemented yet.")
        // ), result, null, null, null, null, null);`
      )
      .replace(
        `        /*
        Stage 5 orchestration run block to enable:
        Comment out the Stage 3 return above, then uncomment this block.

        var analysis = orchestrationService.analyze(userMessage);`,
        `        // Stage 5 orchestration run block enabled.

        var analysis = orchestrationService.analyze(userMessage);`
      )
      .replace(
        `        return new MultiAgentsRunResponse(
                true,
                analysis.answer(),
                stepsFor(analysis),
                analysis.marketData(),
                analysis.fundamentals(),
                analysis.news(),
                analysis.synthesis(),
                analysis,
                null
        );
        */
    }`,
        `        return new MultiAgentsRunResponse(
                true,
                analysis.answer(),
                stepsFor(analysis),
                analysis.marketData(),
                analysis.fundamentals(),
                analysis.news(),
                analysis.synthesis(),
                analysis,
                null
        );
    }`
      )
      .replace(
        `    /*
    Stage 5 helper to enable:
    Add \`import java.util.ArrayList;\` with the other imports.

    private List<MultiAgentStep> stepsFor`,
        `    // Stage 5 helper enabled.

    private List<MultiAgentStep> stepsFor`
      )
      .replace(
        `    }
    */
}`,
        `    }
}`
      )
  }
}

export default {
  name: 'BuildingMultiAgentsHome',
  components: {
    WorkshopContentRenderer,
    WorkshopShell
  },
  props: {
    pageId: { type: String, default: '0' }
  },
  data() {
    return {
      activeCodeEditorColumn: 1,
      activeCodeEditorFilePath: '',
      activeCodeEditorLine: 0,
      appFrameVersion: 0,
      chatBusy: false,
      chatConversationId: loadConversationId(),
      chatInput: '',
      chatMessageCounter: 1,
      chatMessages: [
        {
          id: 'assistant-intro',
          role: 'assistant',
          text: 'Ask a stock analysis question. I will show the final answer first, then the agents that ran.',
          metrics: [],
          steps: [],
          detailsJson: ''
        }
      ],
      chatStatusLabel: 'Checking status',
      chatStatusWarning: false,
      codeEditorFilePathByName: {},
      codeEditorOpenRequest: 0,
      codeEditorWorkspaceRoot: '',
      content: null,
      contentError: '',
      editorStepStatusMessage: '',
      editorStepStatusTimer: null,
      editorStepStatusType: '',
      frontendState: 'READY',
      backendState: 'READY',
      navigationPageTitles: {},
      runtimeBusy: false,
      runtimeLogs: [],
      runtimeMonitorActive: false,
      runtimeState: 'READY',
      runtimeStatusLoaded: false,
      runtimeStatusMessage: ''
    }
  },
  computed: {
    activePageIndex() {
      return this.pageOrder.findIndex(pageId => pageId === this.pageId)
    },
    appFrameMessage() {
      if (!this.runtimeStatusLoaded) {
        return 'Checking learner app status before loading the frame.'
      }

      if (!this.runtimeInteractionBusy) {
        return ''
      }

      return this.runtimeDisplayState === 'REBUILDING'
        ? 'The learner app is rebuilding. The frame will reload automatically when it is ready.'
        : 'The learner app is restarting. The frame will reload automatically when it is ready.'
    },
    basePath() {
      return getBasePath()
    },
    codeEditorUrl() {
      if (this.activeCodeEditorFilePath) {
        return getCodeEditorFileUrl(this.activeCodeEditorFilePath, {
          workspaceRoot: this.codeEditorWorkspaceRoot,
          line: this.activeCodeEditorLine,
          column: this.activeCodeEditorColumn,
          requestId: this.codeEditorOpenRequest
        })
      }

      return getCodeEditorUrl({ workspaceRoot: this.codeEditorWorkspaceRoot })
    },
    contentActionHandlers() {
      return {
        applyEditorStep: ({ args }) => this.applyEditorStep(args?.stepId),
        openEditor: () => this.openEditor(),
        openFile: ({ args }) => this.openFile(args?.file, args),
        openHub: () => window.open(this.workshopHubUrl, '_blank', 'noopener'),
        openRedisInsight: () => this.openRedisInsightPanel(),
        openRoute: ({ args }) => this.openRoute(args?.route),
        recompileApp: () => this.restartRuntime(true),
        restartRuntime: () => this.restartRuntime(false)
      }
    },
    contentContext() {
      return {
        links: {
          editor: this.codeEditorUrl,
          hub: this.workshopHubUrl,
          learnerApp: this.learnerAppUrl,
          redisInsight: this.redisInsightFrameUrl,
          workshopHub: this.workshopHubUrl
        },
        runtime: this.shellRuntime
      }
    },
    editorStepStatusClass() {
      return {
        'editor-step-status--error': this.editorStepStatusType === 'error',
        'editor-step-status--success': this.editorStepStatusType === 'success'
      }
    },
    defaultSidePanel() {
      return this.stageUsesInlineEditor ? 'codeEditor' : 'learnerApp'
    },
    hasStageNavigation() {
      return this.pageOrder.length > 1 && this.activePageIndex >= 0
    },
    instructionsHeaderTitle() {
      const title = this.content?.title
        || this.navigationPageTitles[this.pageId]
        || `Stage ${this.pageId}`

      return this.formatInstructionsTitle(title)
    },
    isEmbeddedLearnerApp() {
      return this.$route.query.embeddedLearnerApp === '1'
    },
    learnerApp() {
      return {
        title: 'Learner app',
        url: this.learnerAppUrl,
        message: this.appFrameMessage
      }
    },
    learnerAppUrl() {
      const route = this.buildRouteUrl('/app/')
      const separator = route.includes('?') ? '&' : '?'
      const params = new URLSearchParams({
        stage: this.pageId,
        frame: String(this.appFrameVersion)
      })
      return `${route}${separator}${params.toString()}`
    },
    nextStage() {
      if (!this.hasStageNavigation) {
        return null
      }

      return this.pageOrder[this.activePageIndex + 1] || null
    },
    nextStageLabel() {
      return this.stageNavigationLabel('nextLabel', this.nextStage)
    },
    nextStageAriaLabel() {
      return this.stageNavigationAriaLabel(this.nextStageLabel, this.nextStage)
    },
    pageOrder() {
      return PAGES.map(page => page.id)
    },
    previousStage() {
      if (!this.hasStageNavigation) {
        return null
      }

      return this.pageOrder[this.activePageIndex - 1] || null
    },
    previousStageLabel() {
      return this.stageNavigationLabel('previousLabel', this.previousStage)
    },
    previousStageAriaLabel() {
      return this.stageNavigationAriaLabel(this.previousStageLabel, this.previousStage)
    },
    redisInsightFrameUrl() {
      const normalizedBasePath = this.basePath && this.basePath !== '/' ? this.basePath : ''
      return `${window.location.origin}${normalizedBasePath}/redis-insight/`
    },
    runtimeDisplayState() {
      if (!this.runtimeStatusLoaded) {
        return 'STARTING'
      }

      return isRebuildRunnerState(this.runtimeState) ? 'REBUILDING' : 'RESTARTING'
    },
    runtimeInteractionBusy() {
      return !this.runtimeStatusLoaded
        || this.runtimeBusy
        || isBusyRunnerState(this.runtimeState)
        || isBusyRunnerState(this.backendState)
    },
    shellActions() {
      return ['restartRuntime', 'rebuildRuntime', 'openRedisInsight', 'openEditor', 'openHub', 'refreshStatus']
    },
    shellLinks() {
      return {
        editor: this.codeEditorUrl,
        hub: this.workshopHubUrl,
        learnerApp: this.learnerAppUrl,
        redisInsight: this.redisInsightFrameUrl,
        workshopHub: this.workshopHubUrl
      }
    },
    shellRuntime() {
      return {
        state: this.runtimeState,
        frontendState: this.frontendState,
        backendState: this.backendState,
        status: this.runtimeStatusMessage,
        rebuildAvailable: true
      }
    },
    stageUsesInlineEditor() {
      return ['3', '4', '5'].includes(this.pageId)
    },
    workshopHubUrl() {
      return getWorkshopHubUrl()
    }
  },
  watch: {
    async pageId() {
      this.contentError = ''
      this.content = null
      await Promise.all([
        this.loadContent(),
        this.loadNavigationPageTitles()
      ])
    }
  },
  async mounted() {
    if (this.isEmbeddedLearnerApp) {
      await this.loadLearnerAppStatus()
      return
    }

    await Promise.all([
      this.loadContent(),
      this.loadCodeEditorFiles(),
      this.loadNavigationPageTitles(),
      this.refreshRuntimeState()
    ])
  },
  beforeUnmount() {
    this.clearEditorStepStatusTimer()
  },
  methods: {
    async applyEditorStep(stepId) {
      const config = EDITOR_STEP_CONFIG[stepId]
      if (!config) {
        this.showEditorStepStatus('No editor step is configured for this button.', 'error')
        return
      }

      try {
        this.showEditorStepStatus(`Applying change to ${config.fileName}...`, 'success', 15000)
        const currentContent = await this.loadEditableFile(config.fileName)
        const nextContent = config.transform(currentContent)
        const changed = nextContent !== currentContent
        if (changed) {
          await this.saveEditableFile(config.fileName, nextContent)
        }
        await this.openFile(config.fileName, { line: config.line, forceReload: true })
        this.showEditorStepStatus(
          changed
            ? `Applied change to ${config.fileName}.`
            : `${config.fileName} already has this change.`,
          'success',
          15000
        )
      } catch (error) {
        console.error('Failed to apply editor step:', error)
        this.showEditorStepStatus(error.message || 'Failed to apply editor step.', 'error')
      }
    },
    applyRuntimeLogs(status) {
      if (!Array.isArray(status?.recentLogs)) {
        return
      }

      this.runtimeLogs = status.recentLogs.slice(-80)
    },
    applyRuntimeStatus(status) {
      const state = status?.state
      this.applyRuntimeLogs(status)

      if (!state) {
        return
      }

      this.runtimeState = normalizeRunnerState(state)
      this.backendState = this.runtimeState
      this.frontendState = 'READY'

      if (isBusyRunnerState(this.runtimeState)) {
        this.runtimeStatusMessage = isRebuildRunnerState(this.runtimeState)
          ? 'Rebuilding learner runtime...'
          : 'Restarting learner runtime...'
      } else if (READY_STATES.has(this.runtimeState)) {
        this.runtimeStatusMessage = 'Runtime is ready'
      }
    },
    buildRouteUrl(route) {
      const normalizedRoute = route.startsWith('/') ? route : `/${route}`
      if (!this.basePath || this.basePath === '/') {
        return normalizedRoute
      }
      return `${this.basePath}${normalizedRoute}`
    },
    async fetchSessionStatus() {
      try {
        const response = await fetch(getApiUrl('/internal/session-runner/status'), {
          credentials: 'include'
        })

        if (!response.ok) {
          return null
        }

        const status = await response.json()
        this.applyRuntimeLogs(status)
        return status
      } catch {
        return null
      }
    },
    formatInstructionsTitle(title) {
      const match = String(title || '').match(/^STAGE\s+(\d+):\s*(.+)$/i)
      if (!match) {
        return title
      }

      return `#${match[1]}: ${match[2]}`
    },
    async goToPage(pageId) {
      if (PAGES.some(page => page.id === pageId)) {
        await this.$router.push(`/${pageId}`).catch(() => {})
      }
    },
    handleContentAction(payload) {
      const handler = this.contentActionHandlers[payload.actionId]
      if (typeof handler === 'function') {
        handler(payload)
      }
    },
    handleEditorRecompileStart() {
      this.runtimeBusy = true
      this.runtimeState = 'REBUILDING'
      this.backendState = 'REBUILDING'
      this.runtimeStatusMessage = 'Rebuilding learner runtime...'
    },
    async handleEditorRecompileSuccess() {
      this.runtimeBusy = false
      this.runtimeState = 'READY'
      this.backendState = 'READY'
      this.frontendState = 'READY'
      this.runtimeStatusMessage = 'Runtime is ready'
      this.appFrameVersion += 1
      await this.refreshRuntimeState()
    },
    handleEditorRecompileError(error) {
      this.runtimeBusy = false
      this.runtimeState = 'FAILED'
      this.backendState = 'FAILED'
      this.runtimeStatusMessage = error?.message || 'Recompile failed'
    },
    handleEditorResetSuccess() {
      this.activeCodeEditorFilePath = ''
      this.activeCodeEditorLine = 0
      this.activeCodeEditorColumn = 1
      this.codeEditorOpenRequest += 1
      this.showEditorStepStatus('Code reset. Recompile App when you are ready.', 'success')
    },
    handleRuntimeAction(action) {
      if (action.type === 'restart') {
        this.restartRuntime(false)
      }

      if (action.type === 'rebuild') {
        this.restartRuntime(true)
      }

      if (action.type === 'refresh') {
        this.refreshLearnerAppFrame()
      }

      if (action.type === 'recompile-start') {
        this.handleEditorRecompileStart()
      }

      if (action.type === 'recompile-success') {
        this.handleEditorRecompileSuccess()
      }

      if (action.type === 'recompile-error') {
        this.handleEditorRecompileError(action.payload)
      }

      if (action.type === 'reset-success') {
        this.handleEditorResetSuccess()
      }
    },
    async handleRuntimeLogsToggle(open) {
      if (open) {
        await this.refreshRuntimeState()
      }
    },
    buildChatDetails(payload) {
      const details = {}
      if (payload?.marketData) {
        details.marketData = payload.marketData
      }
      if (payload?.fundamentals) {
        details.fundamentals = payload.fundamentals
      }
      if (payload?.news) {
        details.news = payload.news
      }
      if (payload?.synthesis) {
        details.synthesis = payload.synthesis
      }
      if (payload?.analysis) {
        details.analysis = payload.analysis
      }
      if (payload?.error) {
        details.error = payload.error
      }

      return Object.keys(details).length ? JSON.stringify(details, null, 2) : ''
    },
    buildChatMetrics(payload, elapsedMs) {
      return [
        {
          label: 'Time',
          value: this.formatElapsedTime(elapsedMs)
        },
        {
          label: 'Tokens',
          value: this.formatTokenUsage(payload)
        }
      ]
    },
    chatTokenUsage(payload) {
      const outputs = [
        payload?.marketData,
        payload?.fundamentals,
        payload?.news,
        payload?.synthesis
      ]

      return outputs.reduce((total, output) => {
        const value = Number(output?.tokenUsage?.totalTokens)
        return total + (Number.isFinite(value) ? value : 0)
      }, 0)
    },
    formatElapsedTime(elapsedMs) {
      if (!Number.isFinite(elapsedMs)) {
        return 'n/a'
      }

      if (elapsedMs < 1000) {
        return `${Math.max(1, Math.round(elapsedMs))} ms`
      }

      return `${(elapsedMs / 1000).toFixed(1)} s`
    },
    formatTokenUsage(payload) {
      const totalTokens = this.chatTokenUsage(payload)
      return totalTokens > 0 ? totalTokens.toLocaleString() : 'n/a'
    },
    async loadLearnerAppStatus() {
      try {
        const response = await fetch(getApiUrl('/api/multi-agents/status'), {
          credentials: 'include'
        })

        if (!response.ok) {
          throw new Error(await this.readErrorMessage(response, 'Status unavailable'))
        }

        const payload = await response.json()
        this.chatStatusWarning = !payload.apiKeyConfigured
        this.chatStatusLabel = payload.apiKeyConfigured
          ? `Ready ${payload.model || ''}`.trim()
          : 'OpenAI key missing'
      } catch {
        this.chatStatusWarning = true
        this.chatStatusLabel = 'Status unavailable'
      }
    },
    async loadCodeEditorFiles() {
      try {
        const metadata = await loadEditorWorkspaceMetadata()
        this.codeEditorFilePathByName = metadata.filePathMap
        this.codeEditorWorkspaceRoot = metadata.codeEditorWorkspaceRoot || metadata.workspaceRoot
      } catch (error) {
        console.warn('Failed to load code editor file metadata:', error)
      }
    },
    async loadEditableFile(fileName) {
      const response = await fetch(getApiUrl(`/api/editor/file/${encodeURIComponent(fileName)}`), {
        cache: 'no-store',
        credentials: 'include'
      })

      if (!response.ok) {
        throw new Error(`Failed to load ${fileName}: ${response.status}`)
      }

      const payload = await response.json()
      if (payload.error) {
        throw new Error(payload.error)
      }
      if (typeof payload.content !== 'string') {
        throw new Error(`No content returned for ${fileName}`)
      }

      return payload.content
    },
    async loadContent() {
      try {
        this.content = await fetchWorkshopContent(this.pageId)
        this.storeNavigationPageTitle(this.pageId, this.content)
      } catch (error) {
        this.contentError = error.message || 'Failed to load workshop content.'
      }
    },
    async loadNavigationPageTitles() {
      const missingPageIds = this.pageOrder.filter(pageId => !this.navigationPageTitles[pageId])
      if (missingPageIds.length === 0) {
        return
      }

      const titles = {}

      await Promise.all(missingPageIds.map(async pageId => {
        try {
          const content = pageId === this.pageId && this.content
            ? this.content
            : await fetchWorkshopContent(pageId)

          if (content?.title) {
            titles[pageId] = content.title
          }
        } catch (error) {
          console.warn(`Unable to load navigation title for page ${pageId}`, error)
        }
      }))

      if (Object.keys(titles).length > 0) {
        this.navigationPageTitles = {
          ...this.navigationPageTitles,
          ...titles
        }
      }
    },
    async monitorRuntimeUntilReady() {
      if (this.runtimeMonitorActive) {
        return
      }

      this.runtimeMonitorActive = true
      this.runtimeBusy = true
      const visibleStateStartedAt = Date.now()

      try {
        await this.waitForReadyState()
        await this.waitForMinimumBusyState(visibleStateStartedAt)
        this.runtimeState = 'READY'
        this.backendState = 'READY'
        this.frontendState = 'READY'
        this.runtimeStatusMessage = 'Runtime is ready'
        this.appFrameVersion += 1
      } catch (error) {
        await this.waitForMinimumBusyState(visibleStateStartedAt)
        this.runtimeState = 'FAILED'
        this.backendState = 'FAILED'
        this.runtimeStatusMessage = error?.message || 'Restart failed'
      } finally {
        this.runtimeBusy = false
        this.runtimeMonitorActive = false
      }
    },
    normalizeEditorLine(value) {
      const number = Number.parseInt(value, 10)
      return Number.isFinite(number) && number > 0 ? number : 0
    },
    normalizeAgentSteps(steps) {
      if (!Array.isArray(steps)) {
        return []
      }

      return steps.map(step => ({
        agent: step.agent || step.name || step.agentName || 'Agent',
        message: step.message || step.action || step.description || ''
      }))
    },
    async openCodeEditorPanel(fileName = '', options = {}) {
      this.replaceToolQuery('code-editor')

      if (fileName) {
        await this.openEmbeddedEditorFile(fileName, options)
      }
    },
    async openEditor() {
      await this.openCodeEditorPanel()
    },
    async openEmbeddedEditorFile(fileName, options = {}) {
      if (!fileName) {
        return
      }

      if (!Object.keys(this.codeEditorFilePathByName).length) {
        await this.loadCodeEditorFiles()
      }

      const workspacePath = this.codeEditorFilePathByName[fileName]
      if (!workspacePath) {
        return
      }

      if (options.forceReload) {
        this.activeCodeEditorFilePath = ''
        this.activeCodeEditorLine = 0
        this.activeCodeEditorColumn = 1
        this.codeEditorOpenRequest += 1
        await this.$nextTick()
      }

      this.codeEditorOpenRequest += 1
      this.activeCodeEditorFilePath = workspacePath
      this.activeCodeEditorLine = this.normalizeEditorLine(options.line)
      this.activeCodeEditorColumn = this.normalizeEditorLine(options.column) || 1
    },
    async openFile(fileName = '', options = {}) {
      await this.openCodeEditorPanel(fileName, options)
    },
    openRedisInsightPanel() {
      this.replaceToolQuery('redis-insight')
    },
    async openRoute(route) {
      if (!route) {
        return
      }

      if (route === '/editor' || route === 'editor' || /^\/6(\?.*)?$/.test(route)) {
        await this.$router.push(route === '/editor' || route === 'editor' ? '/6' : route).catch(() => {})
        return
      }

      if (/^\/[0-5](\?.*)?$/.test(route)) {
        await this.$router.push(route).catch(() => {})
        return
      }

      await this.goToPage(String(route).replace('/', ''))
    },
    async saveEditableFile(fileName, content) {
      const response = await fetch(getApiUrl(`/api/editor/file/${encodeURIComponent(fileName)}`), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        credentials: 'include',
        body: JSON.stringify({ content })
      })
      const payload = await response.json()

      if (!response.ok || payload.error) {
        throw new Error(payload.error || `Failed to save ${fileName}: ${response.status}`)
      }

      return payload
    },
    replacePendingChatMessage(pendingId, nextMessage) {
      this.chatMessages = this.chatMessages.map(message => (
        message.id === pendingId ? nextMessage : message
      ))
    },
    async refreshRuntimeState() {
      try {
        const status = await this.fetchSessionStatus()
        const state = status?.state
        if (state) {
          this.applyRuntimeStatus(status)
          if (isBusyRunnerState(state)) {
            this.monitorRuntimeUntilReady()
          }
        } else {
          this.runtimeState = 'READY'
          this.backendState = 'READY'
          this.frontendState = 'READY'
          this.runtimeStatusMessage = 'Runtime is ready'
        }
      } finally {
        this.runtimeStatusLoaded = true
      }
    },
    refreshLearnerAppFrame() {
      this.refreshRuntimeState()
      this.appFrameVersion += 1
    },
    async scrollChatToBottom() {
      await this.$nextTick()
      const transcript = this.$refs.chatTranscript
      if (transcript) {
        transcript.scrollTop = transcript.scrollHeight
      }
    },
    replaceToolQuery(tool) {
      const nextQuery = { ...this.$route.query }
      if (tool) {
        nextQuery.tool = tool
      } else {
        delete nextQuery.tool
      }

      if (nextQuery.tool === this.$route.query.tool) {
        return
      }

      this.$router.replace({
        path: this.$route.path,
        query: nextQuery
      }).catch(() => {})
    },
    async restartRuntime(rebuild) {
      if (this.runtimeInteractionBusy) {
        return
      }

      this.runtimeBusy = true
      this.runtimeState = rebuild ? 'REBUILDING' : 'RESTARTING'
      this.backendState = this.runtimeState
      this.runtimeStatusMessage = rebuild ? 'Rebuilding learner runtime...' : 'Restarting learner runtime...'
      const visibleStateStartedAt = Date.now()

      try {
        await this.$nextTick()
        await this.sendRestartRequest(rebuild)
        await this.waitForReadyState()
        await this.waitForMinimumBusyState(visibleStateStartedAt)
        this.runtimeState = 'READY'
        this.backendState = 'READY'
        this.frontendState = 'READY'
        this.runtimeStatusMessage = 'Runtime is ready'
        this.appFrameVersion += 1
      } catch (error) {
        await this.waitForMinimumBusyState(visibleStateStartedAt)
        this.runtimeState = 'FAILED'
        this.backendState = 'FAILED'
        this.runtimeStatusMessage = error?.message || 'Restart failed'
      } finally {
        this.runtimeBusy = false
      }
    },
    async sendRestartRequest(rebuild) {
      const response = await fetch(getApiUrl('/internal/session-runner/restart'), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        credentials: 'include',
        body: JSON.stringify({ rebuild, async: true })
      })

      if (!response.ok) {
        throw new Error(await this.readErrorMessage(response, 'Failed to restart session'))
      }
    },
    async sendChatMessage() {
      const task = this.chatInput.trim()
      if (!task || this.chatBusy) {
        return
      }

      const userMessage = {
        id: `user-${this.chatMessageCounter++}`,
        role: 'user',
        text: task,
        metrics: [],
        steps: [],
        detailsJson: ''
      }
      const pendingId = `assistant-${this.chatMessageCounter++}`
      const pendingMessage = {
        id: pendingId,
        role: 'assistant',
        text: 'Working through the agent flow.',
        metrics: [],
        steps: [],
        detailsJson: ''
      }

      this.chatMessages = [...this.chatMessages, userMessage, pendingMessage]
      this.chatInput = ''
      this.chatBusy = true
      await this.scrollChatToBottom()

      try {
        const startedAt = performance.now()
        const response = await fetch(getApiUrl('/api/multi-agents/run'), {
          method: 'POST',
          credentials: 'include',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            conversationId: this.chatConversationId,
            task
          })
        })

        if (!response.ok) {
          throw new Error(await this.readErrorMessage(response, 'Agent request failed'))
        }

        const payload = await response.json()
        const elapsedMs = performance.now() - startedAt
        this.replacePendingChatMessage(pendingId, {
          id: pendingId,
          role: 'assistant',
          text: payload.answer || payload.error || 'The agents did not return an answer.',
          metrics: this.buildChatMetrics(payload, elapsedMs),
          steps: this.normalizeAgentSteps(payload.steps),
          detailsJson: this.buildChatDetails(payload)
        })
      } catch (error) {
        this.replacePendingChatMessage(pendingId, {
          id: pendingId,
          role: 'assistant',
          text: error.message || 'The agent request failed.',
          metrics: [],
          steps: [],
          detailsJson: ''
        })
      } finally {
        this.chatBusy = false
        await this.scrollChatToBottom()
      }
    },
    sleep(ms) {
      return new Promise(resolve => setTimeout(resolve, ms))
    },
    stageNavigationAriaLabel(label, pageId) {
      return label || this.stageNavigationFallbackLabel(pageId)
    },
    stageNavigationFallbackLabel(pageId) {
      if (!pageId) {
        return ''
      }

      return this.navigationPageTitles[pageId] || PAGES.find(page => page.id === pageId)?.title || `Stage ${pageId}`
    },
    stageNavigationLabel(labelKey, pageId) {
      if (!pageId) {
        return ''
      }

      if (
        this.content?.navigation
        && Object.prototype.hasOwnProperty.call(this.content.navigation, labelKey)
      ) {
        return this.content.navigation[labelKey] || ''
      }

      return this.stageNavigationFallbackLabel(pageId)
    },
    storeNavigationPageTitle(pageId, content) {
      if (!pageId || !content?.title) {
        return
      }

      this.navigationPageTitles = {
        ...this.navigationPageTitles,
        [pageId]: content.title
      }
    },
    async waitForMinimumBusyState(startedAt) {
      const remainingMs = MIN_RUNTIME_BUSY_MS - (Date.now() - startedAt)
      if (remainingMs > 0) {
        await this.sleep(remainingMs)
      }
    },
    async waitForReadyState() {
      const startedAt = Date.now()

      while (Date.now() - startedAt < POLL_TIMEOUT_MS) {
        const status = await this.fetchSessionStatus()
        const state = normalizeRunnerState(status?.state)

        if (READY_STATES.has(state)) {
          return
        }
        if (FAILED_STATES.has(state)) {
          throw new Error(status?.lastError || 'Session restart failed')
        }

        await this.sleep(POLL_INTERVAL_MS)
      }

      throw new Error('Timed out waiting for the session to restart')
    },
    async readErrorMessage(response, fallbackMessage) {
      const text = await response.text()

      if (!text) {
        return fallbackMessage
      }

      try {
        const data = JSON.parse(text)
        if (data?.message) {
          return `${fallbackMessage}: ${data.message}`
        }
        if (data?.detail) {
          return `${fallbackMessage}: ${data.detail}`
        }
        if (data?.error) {
          return `${fallbackMessage}: ${data.error}`
        }
      } catch {
        return `${fallbackMessage}: ${text}`
      }

      return fallbackMessage
    },
    showEditorStepStatus(message, type = 'success', durationMs = 15000) {
      this.editorStepStatusMessage = message
      this.editorStepStatusType = type
      this.clearEditorStepStatusTimer()
      if (durationMs <= 0) {
        return
      }
      this.editorStepStatusTimer = window.setTimeout(() => {
        this.editorStepStatusMessage = ''
        this.editorStepStatusType = ''
        this.editorStepStatusTimer = null
      }, durationMs)
    },
    clearEditorStepStatusTimer() {
      if (!this.editorStepStatusTimer) {
        return
      }

      window.clearTimeout(this.editorStepStatusTimer)
      this.editorStepStatusTimer = null
    }
  }
}
</script>

<style scoped>
.multi-agents-home {
  min-height: 100vh;
}

:deep(.workshop-shell__instructions) {
  display: flex;
  padding: 0;
  overflow: hidden;
}

.multi-agents-instructions {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  min-height: 100%;
}

.multi-agents-instructions__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-3);
  min-height: 2.75rem;
  padding: var(--spacing-2) var(--spacing-3);
  border-bottom: 1px solid var(--color-border-light, rgba(71, 85, 105, 0.3));
  background: rgba(13, 26, 34, 0.95);
}

.multi-agents-instructions__title {
  min-width: 0;
  margin: 0;
  color: var(--color-text);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  line-height: 1.2;
}

.multi-agents-instructions__body {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
  overflow: auto;
  padding: var(--spacing-4);
}

.content-state {
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.content-state--error {
  color: #fca5a5;
}

.editor-step-status {
  margin: 0 0 var(--spacing-3);
  border: 1px solid rgba(74, 222, 128, 0.35);
  border-radius: var(--radius-lg);
  background: rgba(74, 222, 128, 0.1);
  color: #bbf7d0;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  line-height: 1.45;
  padding: 0.65rem 0.85rem;
}

.editor-step-status--error {
  border-color: rgba(248, 113, 113, 0.45);
  background: rgba(248, 113, 113, 0.1);
  color: #fecaca;
}

:deep(.content-step-item),
:deep(.content-widget) {
  background: var(--color-dark-800);
}

:deep(.content-step-item__heading h4),
:deep(.workshop-markdown h2),
:deep(.workshop-markdown h3),
:deep(.workshop-markdown h4) {
  color: var(--color-text);
  font-weight: var(--font-weight-semibold);
}

:deep(.content-section__header .workshop-markdown),
:deep(.workshop-markdown),
:deep(.content-step-item__heading .workshop-markdown) {
  color: var(--color-text-secondary);
}

.stage-navigation {
  position: relative;
  display: flex;
  gap: var(--spacing-3);
  justify-content: space-between;
  margin-top: var(--spacing-6);
  padding-top: var(--spacing-3);
  background: rgba(13, 26, 34, 0.96);
  border-top: 1px solid var(--color-border);
}

.stage-navigation__button {
  align-items: center;
  display: inline-flex;
  gap: var(--spacing-2);
  min-height: 2.5rem;
  padding: 0.65rem 0.95rem;
  border: 1px solid var(--color-restart-border, rgba(59, 130, 246, 0.4));
  border-radius: var(--radius-lg);
  background: var(--color-restart-bg, rgba(59, 130, 246, 0.2));
  color: var(--color-restart-text, #93c5fd);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  line-height: 1;
  cursor: pointer;
  transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease;
}

.stage-navigation__button:hover {
  background: rgba(59, 130, 246, 0.28);
  border-color: rgba(59, 130, 246, 0.58);
  color: #bfdbfe;
}

.stage-navigation__button--next {
  margin-left: auto;
}

.stage-navigation__button--previous {
  margin-right: auto;
}

.stage-navigation__label {
  line-height: 1.25;
  text-align: left;
}

.stage-navigation__button--next .stage-navigation__label {
  text-align: right;
}

.multi-agents-chat-app {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  gap: var(--spacing-3);
  min-height: 100vh;
  padding: var(--spacing-3);
  background: #0b151c;
  color: var(--color-text);
}

.multi-agents-chat-app__header,
.multi-agents-chat-composer {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: rgba(13, 26, 34, 0.96);
}

.multi-agents-chat-app__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--spacing-3);
  padding: var(--spacing-3);
}

.multi-agents-chat-app__eyebrow,
.multi-agents-chat-message__role,
.multi-agents-chat-steps__title,
.multi-agents-chat-composer span {
  margin: 0;
  color: var(--color-text-muted, #94a3b8);
  font-size: 0.72rem;
  font-weight: var(--font-weight-bold);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.multi-agents-chat-app__title {
  margin: 0.25rem 0 0;
  color: var(--color-text);
  font-size: 1.15rem;
  line-height: 1.2;
}

.multi-agents-chat-app__status {
  flex: 0 0 auto;
  border: 1px solid rgba(74, 222, 128, 0.35);
  border-radius: 999px;
  background: rgba(74, 222, 128, 0.08);
  color: #bbf7d0;
  font-size: 0.78rem;
  font-weight: var(--font-weight-semibold);
  padding: 0.35rem 0.65rem;
}

.multi-agents-chat-app__status--warning {
  border-color: rgba(251, 191, 36, 0.38);
  background: rgba(251, 191, 36, 0.08);
  color: #fde68a;
}

.multi-agents-chat-app__transcript {
  display: flex;
  min-height: 0;
  flex-direction: column;
  gap: var(--spacing-3);
  overflow: auto;
  padding-right: 0.25rem;
}

.multi-agents-chat-message {
  width: min(88%, 48rem);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: rgba(13, 26, 34, 0.92);
  padding: var(--spacing-3);
}

.multi-agents-chat-message--user {
  align-self: flex-end;
  border-color: rgba(220, 56, 44, 0.45);
  background: rgba(220, 56, 44, 0.11);
}

.multi-agents-chat-message--assistant {
  align-self: flex-start;
}

.multi-agents-chat-message__text {
  margin: 0.45rem 0 0;
  color: var(--color-text);
  font-size: 0.98rem;
  line-height: 1.55;
  white-space: pre-wrap;
}

.multi-agents-chat-message__metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
  margin-top: 0.75rem;
}

.multi-agents-chat-message__metric {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  border: 1px solid rgba(71, 85, 105, 0.36);
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.34);
  color: var(--color-text-secondary);
  font-size: 0.76rem;
  line-height: 1;
  padding: 0.35rem 0.55rem;
}

.multi-agents-chat-message__metric span {
  color: var(--color-text-muted, #94a3b8);
}

.multi-agents-chat-message__metric strong {
  color: var(--color-text);
  font-weight: var(--font-weight-semibold);
}

.multi-agents-chat-steps {
  margin-top: var(--spacing-3);
  border-top: 1px solid var(--color-border-light, rgba(71, 85, 105, 0.32));
  padding-top: var(--spacing-3);
}

.multi-agents-chat-steps__list {
  display: grid;
  gap: 0.45rem;
  margin: 0.6rem 0 0;
  padding: 0;
  list-style: none;
}

.multi-agents-chat-steps__item {
  display: grid;
  gap: 0.2rem;
  border: 1px solid rgba(71, 85, 105, 0.45);
  border-radius: var(--radius-md);
  background: rgba(4, 12, 18, 0.45);
  color: var(--color-text-secondary);
  line-height: 1.4;
  padding: 0.65rem 0.75rem;
}

.multi-agents-chat-steps__item strong {
  color: var(--color-text);
  font-size: 0.9rem;
}

.multi-agents-chat-details {
  margin-top: var(--spacing-3);
  color: var(--color-text-secondary);
}

.multi-agents-chat-details summary {
  cursor: pointer;
  font-size: 0.85rem;
  font-weight: var(--font-weight-semibold);
}

.multi-agents-chat-details pre {
  overflow: auto;
  margin: 0.65rem 0 0;
  border: 1px solid rgba(71, 85, 105, 0.45);
  border-radius: var(--radius-md);
  background: rgba(4, 12, 18, 0.62);
  color: #dbeafe;
  font-size: 0.78rem;
  line-height: 1.45;
  padding: 0.75rem;
  white-space: pre-wrap;
}

.multi-agents-chat-composer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: var(--spacing-3);
  padding: var(--spacing-3);
}

.multi-agents-chat-composer label {
  display: grid;
  gap: 0.35rem;
}

.multi-agents-chat-composer input,
.multi-agents-chat-composer textarea {
  width: 100%;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: #071017;
  color: var(--color-text);
  font: inherit;
  line-height: 1.45;
  padding: 0.7rem 0.8rem;
}

.multi-agents-chat-composer input:focus,
.multi-agents-chat-composer textarea:focus {
  border-color: rgba(96, 165, 250, 0.6);
  outline: none;
}

.multi-agents-chat-composer__message textarea {
  min-height: 3rem;
  resize: vertical;
}

.multi-agents-chat-composer__send {
  align-self: end;
  min-height: 3rem;
  border: 1px solid rgba(220, 56, 44, 0.55);
  border-radius: var(--radius-md);
  background: rgba(220, 56, 44, 0.16);
  color: #fecaca;
  cursor: pointer;
  font: inherit;
  font-weight: var(--font-weight-bold);
  padding: 0.7rem 1rem;
}

.multi-agents-chat-composer__send:hover:not(:disabled) {
  background: rgba(220, 56, 44, 0.24);
}

.multi-agents-chat-composer__send:disabled {
  cursor: not-allowed;
  opacity: 0.62;
}

@media (max-width: 900px) {
  .multi-agents-instructions__header {
    align-items: flex-start;
    flex-direction: column;
  }

  .stage-navigation {
    flex-wrap: wrap;
  }

  .stage-navigation__button {
    flex: 1 1 auto;
    justify-content: center;
  }

  .multi-agents-chat-app__header,
  .multi-agents-chat-composer {
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .multi-agents-chat-message {
    width: 100%;
  }

  .multi-agents-chat-composer {
    grid-template-columns: 1fr;
  }
}
</style>
