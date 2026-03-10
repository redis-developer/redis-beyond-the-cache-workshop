<template>
  <div class="memory-home">
    <WorkshopHeader
      :hub-url="workshopHubUrl"
      :steps="['The Problem', 'The Solution', 'Working Memory', 'Long-Term Memory']"
      :current-step="currentStage"
      clickable
      @step-click="goToStage"
    />

    <div class="main-container">
      <!-- STAGE 1: The Problem - LLMs are Stateless -->
      <div v-if="currentStage === 1" class="stage-content">
        <div class="section-header">
          <img src="@/assets/logo/small.png" alt="Redis Logo" width="32" height="32" />
          <h2>The Problem: LLMs Are Stateless</h2>
        </div>

        <p class="intro">
          Before we solve anything, let's understand <strong>why AI agents need memory</strong> in the first place.
        </p>

        <div class="concept-box highlight">
          <h4>How LLMs Actually Work</h4>
          <p>
            Large Language Models like GPT-4, Claude, or Llama are <strong>stateless functions</strong>.
            Each API call is completely independent - the model has no memory of previous interactions.
          </p>
          <div class="code-example inline">
            <pre><code>// Each call is independent - no shared state
response1 = llm.complete("My name is Alice")  // "Nice to meet you, Alice!"
response2 = llm.complete("What's my name?")   // "I don't know your name."</code></pre>
          </div>
        </div>

        <div class="concept-box">
          <h4>The Technical Reality</h4>
          <ul>
            <li><strong>No persistent state:</strong> LLMs don't store information between API calls</li>
            <li><strong>Context window only:</strong> They only "remember" what's in the current prompt</li>
            <li><strong>Token limits:</strong> Context windows have hard limits (4K-128K tokens)</li>
            <li><strong>Cost scales with context:</strong> Larger prompts = more expensive API calls</li>
          </ul>
        </div>

        <div class="problem-demo">
          <h4>The Naive Solution: Pass Everything</h4>
          <p>The simplest approach is to include the entire conversation history in every request:</p>
          <div class="code-example">
            <pre><code>// Conversation grows with every turn
messages = [
  {"role": "user", "content": "My name is Alice"},
  {"role": "assistant", "content": "Nice to meet you!"},
  {"role": "user", "content": "I work at TechCorp"},
  {"role": "assistant", "content": "Interesting!"},
  // ... 100 more messages ...
  {"role": "user", "content": "What's my name?"}
]
response = llm.complete(messages)  // Works, but expensive!</code></pre>
          </div>
          <div class="alert alert-warning">
            <strong>Problems:</strong> Token limits exceeded, costs explode, irrelevant context dilutes responses,
            and you still can't remember anything across different conversation sessions.
          </div>
        </div>

        <div class="step-item">
          <h4>What We Need</h4>
          <ul class="step-list">
            <li><strong>Session context:</strong> Remember the current conversation efficiently</li>
            <li><strong>Cross-session memory:</strong> Remember facts and preferences forever</li>
            <li><strong>Intelligent retrieval:</strong> Fetch only relevant memories, not everything</li>
            <li><strong>Scalability:</strong> Handle millions of users and conversations</li>
          </ul>
        </div>

        <WorkshopStageNav :show-prev="false" @next="nextStage" next-text="The Solution: Agent Memory Server" />
      </div>

      <!-- STAGE 2: Introducing Agent Memory Server -->
      <div v-if="currentStage === 2" class="stage-content">
        <h2>Introducing Agent Memory Server</h2>
        <p class="intro">
          <strong>Agent Memory Server (AMS)</strong> is an open-source memory layer built on Redis
          that gives your AI agents the ability to remember.
        </p>

        <div class="architecture-diagram">
          <div class="arch-layer">
            <div class="arch-box agent">
              <div class="arch-icon">
                <svg viewBox="0 0 24 24" fill="currentColor" width="24" height="24">
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
                </svg>
              </div>
              <div class="arch-label">Your AI Agent</div>
              <div class="arch-sublabel">Spring AI / LangChain / Custom</div>
            </div>
          </div>

          <div class="arch-connector">
            <div class="connector-line"></div>
            <div class="connector-label">
              <span class="connector-badge">REST API</span>
              <span class="connector-detail">store / retrieve / search</span>
            </div>
            <div class="connector-line"></div>
          </div>

          <div class="arch-layer">
            <div class="arch-box ams">
              <div class="arch-icon">
                <svg viewBox="0 0 24 24" fill="currentColor" width="24" height="24">
                  <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-7 14c-3.31 0-6-2.69-6-6s2.69-6 6-6 6 2.69 6 6-2.69 6-6 6z"/>
                </svg>
              </div>
              <div class="arch-label">Agent Memory Server</div>
              <div class="arch-sublabel">Extraction / Deduplication / Search</div>
            </div>
          </div>

          <div class="arch-connector">
            <div class="connector-line"></div>
            <div class="connector-label">
              <span class="connector-badge redis-badge">Vector Search + JSON</span>
            </div>
            <div class="connector-line"></div>
          </div>

          <div class="arch-layer">
            <div class="arch-box redis">
              <div class="arch-icon">
                <img src="@/assets/logo/small.png" alt="Redis" width="24" height="24" />
              </div>
              <div class="arch-label">Redis</div>
              <div class="arch-sublabel">Sub-millisecond latency</div>
            </div>
          </div>
        </div>

        <div class="concept-box highlight">
          <h4>Two Types of Memory</h4>
          <p>AMS provides a <strong>two-tier memory architecture</strong> that mirrors how humans remember:</p>
          <div class="two-tier-intro">
            <div class="tier-box working">
              <h5>Working Memory</h5>
              <p>Short-term, session-scoped context. Like your "mental scratchpad" during a conversation.</p>
            </div>
            <div class="tier-box longterm">
              <h5>Long-Term Memory</h5>
              <p>Persistent facts and preferences. Like memories that last across conversations.</p>
            </div>
          </div>
        </div>

        <div class="concept-box">
          <h4>Why AMS?</h4>
          <ul>
            <li><strong>Built on Redis:</strong> Sub-millisecond latency, native vector search</li>
            <li><strong>Handles the hard parts:</strong> Memory extraction, deduplication, semantic search</li>
            <li><strong>Production ready:</strong> Multi-tenancy, authentication, namespaces</li>
            <li><strong>Simple REST API:</strong> Works with any language or framework</li>
          </ul>
        </div>

        <WorkshopStageNav @prev="prevStage" @next="nextStage" next-text="Deep Dive: Working Memory" />
      </div>

      <!-- STAGE 3: Working Memory -->
      <div v-if="currentStage === 3" class="stage-content">
        <h2>Working Memory: Session Context</h2>
        <p class="intro">
          Working memory solves the <strong>session context problem</strong>. It stores the current conversation
          and automatically manages context size through summarization.
        </p>

        <div class="concept-box highlight">
          <h4>What Working Memory Stores</h4>
          <div class="memory-diagram">
            <div class="memory-block">
              <span class="label">Session ID</span>
              <span class="value">"chat_alice_123"</span>
            </div>
            <div class="memory-block">
              <span class="label">Messages</span>
              <span class="value">Conversation history</span>
            </div>
            <div class="memory-block">
              <span class="label">Context</span>
              <span class="value">System prompt, metadata</span>
            </div>
            <div class="memory-block">
              <span class="label">Summary</span>
              <span class="value">Auto-generated from long conversations</span>
            </div>
          </div>
        </div>

        <div class="concept-box">
          <h4>Key Features</h4>
          <ul>
            <li><strong>Efficient storage:</strong> Conversations are stored outside the LLM, retrieved when needed</li>
            <li><strong>Auto-summarization:</strong> When context usage exceeds ~70% of the model's token limit, AMS automatically summarizes older messages to make room for new ones</li>
            <li><strong>TTL support:</strong> Sessions can auto-expire after inactivity (e.g., 30 minutes)</li>
            <li><strong>User isolation:</strong> Each user/session has separate memory</li>
          </ul>
        </div>

        <div class="concept-box">
          <h4>Automatic Summarization</h4>
          <p>
            As conversations grow, they consume more of the LLM's context window. AMS monitors token usage
            and automatically summarizes when needed:
          </p>
          <div class="summarization-flow">
            <div class="flow-step">
              <span class="flow-icon">1</span>
              <span class="flow-text">Context fills up (messages accumulate)</span>
            </div>
            <div class="flow-arrow">-></div>
            <div class="flow-step">
              <span class="flow-icon">2</span>
              <span class="flow-text">Threshold reached (~70%)</span>
            </div>
            <div class="flow-arrow">-></div>
            <div class="flow-step">
              <span class="flow-icon">3</span>
              <span class="flow-text">Older messages summarized</span>
            </div>
            <div class="flow-arrow">-></div>
            <div class="flow-step">
              <span class="flow-icon">4</span>
              <span class="flow-text">Context freed for new messages</span>
            </div>
          </div>
        </div>

        <div class="code-example">
          <h4>How It Works</h4>
          <pre><code>// 1. User starts a conversation
workingMemory = {
  session_id: "chat_alice_123",
  messages: [
    {role: "user", content: "Hi, I'm Alice from TechCorp"},
    {role: "assistant", content: "Hello Alice! How can I help?"}
  ],
  user_id: "alice"
}

// 2. Store in working memory
PUT /v1/working-memory/chat_alice_123
Body: workingMemory

// 3. Later, retrieve context for next message
GET /v1/working-memory/chat_alice_123
// Returns: messages + any auto-generated summary</code></pre>
        </div>

        <div class="alert alert-info">
          <strong>Key Insight:</strong> Working memory is <em>session-scoped</em>. When Alice starts a new
          conversation tomorrow, she gets a fresh working memory. But what about her preferences and facts
          we learned yesterday?
        </div>

        <WorkshopStageNav @prev="prevStage" @next="nextStage" next-text="Solution: Long-Term Memory" />
      </div>

      <!-- STAGE 4: Long-Term Memory -->
      <div v-if="currentStage === 4" class="stage-content">
        <h2>Long-Term Memory: Persistent Knowledge</h2>
        <p class="intro">
          Long-term memory solves the <strong>cross-session knowledge problem</strong>. It stores facts,
          preferences, and important information that should persist forever.
        </p>

        <div class="concept-box highlight">
          <h4>Memory Types</h4>
          <p>AMS supports three types of long-term memory, each optimized for different use cases:</p>
          <div class="memory-types-detailed">
            <div class="memory-type-card">
              <div class="type-header semantic">
                <h5>Semantic</h5>
                <span class="type-badge">Most Common</span>
              </div>
              <p class="type-desc">Facts, preferences, and general knowledge about the user.</p>
              <div class="type-example">
                <code>"Alice prefers morning meetings"</code>
              </div>
              <div class="type-when">
                <strong>Use when:</strong> Storing user attributes, preferences, learned facts that don't have a specific time context.
              </div>
            </div>
            <div class="memory-type-card">
              <div class="type-header episodic">
                <h5>Episodic</h5>
              </div>
              <p class="type-desc">Events with temporal context - things that happened at a specific time.</p>
              <div class="type-example">
                <code>"Alice joined TechCorp on Jan 15, 2024"</code>
              </div>
              <div class="type-when">
                <strong>Use when:</strong> Recording appointments, meetings, deadlines, or any time-sensitive information.
              </div>
            </div>
            <div class="memory-type-card">
              <div class="type-header message">
                <h5>Message</h5>
              </div>
              <p class="type-desc">Verbatim messages to preserve exactly as stated.</p>
              <div class="type-example">
                <code>"Remember: My anniversary is July 20"</code>
              </div>
              <div class="type-when">
                <strong>Use when:</strong> User explicitly asks to remember something, or you need exact wording preserved.
              </div>
            </div>
          </div>
          <div class="workshop-note">
            <strong>In this workshop:</strong> We use <code>SEMANTIC</code> type for simplicity. The concepts apply to all types.
          </div>
        </div>

        <div class="concept-box">
          <h4>Why Long-Term Memory Is Necessary</h4>
          <ul>
            <li><strong>Persistence:</strong> Information survives across sessions, devices, and time</li>
            <li><strong>Semantic search:</strong> Find memories by meaning, not just keywords</li>
            <li><strong>Personalization:</strong> Agent knows user preferences without being told again</li>
            <li><strong>Continuous learning:</strong> Agent gets smarter with every interaction</li>
          </ul>
        </div>

        <div class="code-example">
          <h4>How Semantic Search Works</h4>
          <pre><code>// 1. Store a memory (text is converted to vector embeddings)
POST /v1/long-term-memory/
{
  "memories": [{
    "text": "Alice prefers video calls over phone calls",
    "memory_type": "semantic",
    "user_id": "alice"
  }]
}

// 2. Later, search by meaning (not exact keywords)
POST /v1/long-term-memory/search
{
  "text": "how does the user like to communicate?",
  "user_id": {"eq": "alice"},
  "limit": 5
}
// Returns: "Alice prefers video calls over phone calls"
// (matched by MEANING, not keywords!)</code></pre>
        </div>

        <div class="two-tier-summary">
          <h4>Two-Tier Memory Architecture</h4>
          <div class="tier-comparison">
            <div class="tier working">
              <h5>Working Memory</h5>
              <ul>
                <li>Session-scoped</li>
                <li>Conversation context</li>
                <li>Auto-summarization</li>
                <li>Short-lived (TTL)</li>
              </ul>
            </div>
            <div class="tier-arrow">+</div>
            <div class="tier longterm">
              <h5>Long-Term Memory</h5>
              <ul>
                <li>User-scoped</li>
                <li>Facts and preferences</li>
                <li>Semantic search</li>
                <li>Persistent forever</li>
              </ul>
            </div>
          </div>
        </div>

        <div class="button-group" style="margin-top: var(--spacing-5);">
          <router-link to="/challenges" class="btn btn-primary btn-large">
            Next: Understanding the Challenges
          </router-link>
        </div>

        <WorkshopStageNav @prev="prevStage" :show-next="false" />
      </div>
    </div>
  </div>
</template>

<script>
import { WorkshopHeader, WorkshopStageNav } from '../utils/components';
import { getWorkshopHubUrl } from '../utils/basePath';
import { loadProgress, saveProgress } from '../utils/progress';

export default {
  name: 'MemoryHome',
  components: { WorkshopHeader, WorkshopStageNav },
  data() {
    return {
      currentStage: 1
    };
  },
  computed: {
    workshopHubUrl() {
      return getWorkshopHubUrl();
    }
  },
  mounted() {
    const progress = loadProgress();
    this.currentStage = progress.homeStage || 1;
  },
  methods: {
    nextStage() {
      if (this.currentStage < 4) {
        this.currentStage++;
        this.saveState();
      }
    },
    prevStage() {
      if (this.currentStage > 1) {
        this.currentStage--;
        this.saveState();
      }
    },
    goToStage(step) {
      if (step >= 1 && step <= 4) {
        this.currentStage = step;
        this.saveState();
      }
    },
    saveState() {
      const progress = loadProgress();
      progress.homeStage = this.currentStage;
      saveProgress(progress);
    }
  }
};
</script>

<style scoped>
.memory-home {
  min-height: 100vh;
  background: linear-gradient(135deg, var(--color-dark-900) 0%, var(--color-dark-800) 100%);
  padding: var(--spacing-6);
}
.main-container { max-width: 900px; margin: 0 auto; padding-top: var(--spacing-4); }
.stage-content {
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  padding: var(--spacing-6);
  box-shadow: var(--shadow-xl);
  border: 1px solid var(--color-border);
}
.section-header { display: flex; align-items: center; gap: var(--spacing-3); margin-bottom: var(--spacing-4); }
.section-header h2 { margin: 0; color: var(--color-text); font-size: var(--font-size-xl); }
h2 { color: var(--color-text); margin-bottom: var(--spacing-4); }
.intro { color: var(--color-text-secondary); margin-bottom: var(--spacing-5); line-height: 1.6; font-size: var(--font-size-base); }

.concept-box {
  background: var(--color-dark-800);
  border-radius: var(--radius-lg);
  padding: var(--spacing-4);
  margin-bottom: var(--spacing-4);
}
.concept-box.highlight { border-left: 3px solid var(--color-primary); }
.concept-box h4 { color: var(--color-text); margin-bottom: var(--spacing-3); }
.concept-box p { color: var(--color-text-secondary); line-height: 1.6; margin: 0 0 var(--spacing-3); }
.concept-box ul { margin: 0 0 0 var(--spacing-4); color: var(--color-text-secondary); line-height: 1.8; }

.code-example {
  background: var(--color-dark-900);
  border-radius: var(--radius-lg);
  padding: var(--spacing-4);
  margin-bottom: var(--spacing-4);
}
.code-example.inline { margin: var(--spacing-3) 0 0; }
.code-example h4 { color: var(--color-text); margin-bottom: var(--spacing-2); font-size: var(--font-size-sm); }
.code-example pre { margin: 0; overflow-x: auto; }
.code-example code { font-family: 'Courier New', monospace; font-size: var(--font-size-xs); color: #e0e0e0; }

.problem-demo { margin-bottom: var(--spacing-4); }
.problem-demo h4 { color: var(--color-text); margin-bottom: var(--spacing-2); }
.problem-demo p { color: var(--color-text-secondary); margin-bottom: var(--spacing-3); }

.step-item { margin-bottom: var(--spacing-4); }
.step-item h4 { color: var(--color-text); margin-bottom: var(--spacing-2); }
.step-list { margin: var(--spacing-3) 0 0 var(--spacing-5); color: var(--color-text-secondary); line-height: 1.8; }

.memory-diagram { display: flex; flex-wrap: wrap; gap: var(--spacing-2); margin-top: var(--spacing-3); }
.memory-block {
  background: var(--color-dark-900);
  padding: var(--spacing-2) var(--spacing-3);
  border-radius: var(--radius-sm);
  flex: 1 1 45%;
  min-width: 180px;
}
.memory-block .label { display: block; font-size: var(--font-size-xs); color: var(--color-text-secondary); margin-bottom: 2px; }
.memory-block .value { color: var(--color-text); font-size: var(--font-size-sm); }

.memory-types-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: var(--spacing-3); margin-top: var(--spacing-3); }
.memory-type {
  background: var(--color-dark-900);
  padding: var(--spacing-3);
  border-radius: var(--radius-md);
  text-align: center;
}
.memory-type h5 { color: var(--color-text); margin: 0 0 var(--spacing-1); font-size: var(--font-size-sm); }
.memory-type p { color: var(--color-text-secondary); font-size: var(--font-size-xs); margin: 0 0 var(--spacing-2); }
.memory-type code { display: block; font-size: 0.7rem; color: #a5d6ff; word-break: break-word; }

.memory-types-detailed { display: flex; flex-direction: column; gap: var(--spacing-3); margin-top: var(--spacing-3); }
.memory-type-card {
  background: var(--color-dark-900);
  border-radius: var(--radius-md);
  padding: var(--spacing-3);
}
.type-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  margin-bottom: var(--spacing-2);
  padding-bottom: var(--spacing-2);
  border-bottom: 2px solid;
}
.type-header.semantic { border-color: #10b981; }
.type-header.episodic { border-color: #f59e0b; }
.type-header.message { border-color: #3b82f6; }
.type-header h5 { color: var(--color-text); margin: 0; font-size: var(--font-size-sm); }
.type-badge {
  background: #10b981;
  color: white;
  font-size: 0.65rem;
  padding: 2px 6px;
  border-radius: var(--radius-sm);
  font-weight: bold;
}
.type-desc { color: var(--color-text-secondary); font-size: var(--font-size-sm); margin: 0 0 var(--spacing-2); }
.type-example {
  background: var(--color-dark-800);
  padding: var(--spacing-2);
  border-radius: var(--radius-sm);
  margin-bottom: var(--spacing-2);
}
.type-example code { font-size: var(--font-size-xs); color: #a5d6ff; }
.type-when {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  line-height: 1.5;
}
.type-when strong { color: var(--color-text); }
.workshop-note {
  margin-top: var(--spacing-3);
  padding: var(--spacing-2) var(--spacing-3);
  background: rgba(220, 56, 44, 0.1);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}
.workshop-note code { background: var(--color-dark-800); padding: 1px 4px; border-radius: 2px; }

.two-tier-summary { margin-top: var(--spacing-5); }
.two-tier-summary h4 { color: var(--color-text); margin-bottom: var(--spacing-3); text-align: center; }
.tier-comparison { display: flex; align-items: center; justify-content: center; gap: var(--spacing-3); flex-wrap: wrap; }
.tier {
  background: var(--color-dark-800);
  padding: var(--spacing-4);
  border-radius: var(--radius-lg);
  flex: 1;
  min-width: 200px;
  max-width: 300px;
}
.tier h5 { color: var(--color-text); margin: 0 0 var(--spacing-2); text-align: center; }
.tier ul { margin: 0; padding-left: var(--spacing-4); color: var(--color-text-secondary); font-size: var(--font-size-sm); }
.tier.working { border-top: 3px solid #f59e0b; }
.tier.longterm { border-top: 3px solid #10b981; }
.tier-arrow { font-size: 1.5rem; color: var(--color-text-secondary); font-weight: bold; }

.btn-large { padding: var(--spacing-3) var(--spacing-6); font-size: var(--font-size-base); }

.architecture-diagram {
  background: var(--color-dark-800);
  border-radius: var(--radius-lg);
  padding: var(--spacing-5);
  margin-bottom: var(--spacing-4);
}
.arch-layer { display: flex; justify-content: center; }
.arch-box {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
  padding: var(--spacing-3) var(--spacing-4);
  border-radius: var(--radius-md);
  min-width: 280px;
  border: 2px solid;
}
.arch-box.agent {
  background: rgba(59, 130, 246, 0.15);
  border-color: #3b82f6;
}
.arch-box.ams {
  background: rgba(220, 56, 44, 0.15);
  border-color: var(--color-primary);
}
.arch-box.redis {
  background: rgba(16, 185, 129, 0.15);
  border-color: #10b981;
}
.arch-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.arch-box.agent .arch-icon { background: #3b82f6; color: white; }
.arch-box.ams .arch-icon { background: var(--color-primary); color: white; }
.arch-box.redis .arch-icon { background: transparent; }
.arch-label { color: var(--color-text); font-weight: bold; font-size: var(--font-size-base); }
.arch-sublabel { color: var(--color-text-secondary); font-size: var(--font-size-xs); }

.arch-connector {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--spacing-2) 0;
}
.connector-line {
  width: 2px;
  height: 16px;
  background: linear-gradient(180deg, var(--color-border) 0%, var(--color-text-secondary) 50%, var(--color-border) 100%);
}
.connector-label {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: var(--spacing-1) 0;
}
.connector-badge {
  background: var(--color-dark-900);
  color: var(--color-text);
  padding: 4px 12px;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  font-weight: 600;
  border: 1px solid var(--color-border);
}
.connector-badge.redis-badge {
  border-color: #10b981;
  color: #10b981;
}
.connector-detail {
  color: var(--color-text-secondary);
  font-size: 0.65rem;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.two-tier-intro { display: flex; gap: var(--spacing-3); margin-top: var(--spacing-3); }
.tier-box { flex: 1; background: var(--color-dark-900); padding: var(--spacing-3); border-radius: var(--radius-md); }
.tier-box h5 { color: var(--color-text); margin: 0 0 var(--spacing-2); }
.tier-box p { color: var(--color-text-secondary); font-size: var(--font-size-sm); margin: 0; }
.tier-box.working { border-top: 3px solid #f59e0b; }
.tier-box.longterm { border-top: 3px solid #10b981; }
.alert { padding: var(--spacing-3); border-radius: var(--radius-md); margin-bottom: var(--spacing-4); }
.alert-warning { background: rgba(245, 158, 11, 0.1); border-left: 3px solid #f59e0b; color: var(--color-text-secondary); }
.alert-info { background: rgba(59, 130, 246, 0.1); border-left: 3px solid #3b82f6; color: var(--color-text-secondary); }

.summarization-flow {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--spacing-2);
  margin-top: var(--spacing-3);
}
.flow-step {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  background: var(--color-dark-900);
  padding: var(--spacing-2) var(--spacing-3);
  border-radius: var(--radius-sm);
}
.flow-icon {
  width: 24px;
  height: 24px;
  background: var(--color-primary);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-xs);
  font-weight: bold;
  flex-shrink: 0;
}
.flow-text { color: var(--color-text-secondary); font-size: var(--font-size-xs); }
.flow-arrow { color: var(--color-text-secondary); font-size: var(--font-size-sm); }

@media (max-width: 600px) {
  .memory-types-grid { grid-template-columns: 1fr; }
  .tier-comparison { flex-direction: column; }
  .tier-arrow { transform: rotate(90deg); }
  .summarization-flow { flex-direction: column; align-items: flex-start; }
  .flow-arrow { transform: rotate(90deg); align-self: center; }
}
</style>
