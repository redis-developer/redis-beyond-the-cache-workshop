<template>
  <div class="memory-learn">
    <WorkshopHeader
      :hub-url="workshopHubUrl"
      :steps="['Home', 'Learn']"
      :current-step="2"
    />

    <div class="main-container">
      <h1>AMS Reference Guide</h1>

      <!-- Table of Contents -->
      <nav class="toc">
        <h3>Contents</h3>
        <ul>
          <li><a href="#architecture">Architecture Overview</a></li>
          <li><a href="#memory-types">Memory Types</a></li>
          <li><a href="#working-memory">Working Memory Lifecycle</a></li>
          <li><a href="#summarization">Summarization</a></li>
          <li><a href="#semantic-search">Semantic Search</a></li>
          <li><a href="#ttl">TTL and Expiration</a></li>
          <li><a href="#deduplication">Deduplication</a></li>
          <li><a href="#integration">Integration Patterns</a></li>
          <li><a href="#advanced">Advanced Features</a></li>
          <li><a href="#sdk">Java SDK Reference</a></li>
          <li><a href="#resources">Resources</a></li>
        </ul>
      </nav>

      <!-- 1. Architecture Overview -->
      <div id="architecture" class="doc-section">
        <h2>Architecture Overview</h2>
        <div class="architecture-diagram">
          <div class="arch-layer">
            <div class="arch-box client">
              <span>Your App</span>
            </div>
          </div>
          <div class="arch-arrow">→</div>
          <div class="arch-layer">
            <div class="arch-box server">
              <span>Agent Memory Server</span>
            </div>
          </div>
          <div class="arch-arrow">→</div>
          <div class="arch-layer">
            <div class="arch-box redis">
              <img src="@/assets/logo/small.png" alt="Redis" width="18" height="18" />
              <span>Redis</span>
            </div>
          </div>
        </div>
        <p>AMS acts as middleware between your application and Redis, handling:</p>
        <ul class="feature-list">
          <li><strong>Embedding generation</strong> - Converts text to vectors automatically</li>
          <li><strong>Semantic search</strong> - Finds memories by meaning using Redis Vector Search</li>
          <li><strong>Context management</strong> - Tracks token usage and triggers summarization</li>
          <li><strong>Memory lifecycle</strong> - Handles TTL, deduplication, and cleanup</li>
        </ul>
      </div>

      <!-- 2. Memory Types -->
      <div id="memory-types" class="doc-section">
        <h2>Memory Types Deep Dive</h2>
        <p class="section-intro">AMS supports three memory types, each optimized for different use cases:</p>

        <div class="memory-type-card">
          <div class="type-header semantic">
            <span class="type-badge">SEMANTIC</span>
            <span class="type-title">Facts and Preferences</span>
          </div>
          <div class="type-content">
            <p><strong>What:</strong> Timeless facts about the user - preferences, attributes, learned information.</p>
            <p><strong>When to use:</strong> User traits that don't change frequently.</p>
            <div class="example-box">
              <span class="example-label">Examples:</span>
              <ul>
                <li>"User prefers dark mode"</li>
                <li>"Alice is a software engineer"</li>
                <li>"User is vegetarian"</li>
              </ul>
            </div>
            <p><strong>Storage:</strong> Embedded as vectors for semantic similarity search. No temporal weighting.</p>
          </div>
        </div>

        <div class="memory-type-card">
          <div class="type-header episodic">
            <span class="type-badge">EPISODIC</span>
            <span class="type-title">Events with Temporal Context</span>
          </div>
          <div class="type-content">
            <p><strong>What:</strong> Time-bound events, appointments, things that happened or will happen.</p>
            <p><strong>When to use:</strong> Scheduling, reminders, event tracking.</p>
            <div class="example-box">
              <span class="example-label">Examples:</span>
              <ul>
                <li>"User has a meeting on March 15 at 2pm"</li>
                <li>"User went hiking last weekend"</li>
                <li>"Anniversary is July 20"</li>
              </ul>
            </div>
            <p><strong>Storage:</strong> Includes <code>event_date</code> field for temporal queries and recency ranking.</p>
          </div>
        </div>

        <div class="memory-type-card">
          <div class="type-header message">
            <span class="type-badge">MESSAGE</span>
            <span class="type-title">Verbatim Preservation</span>
          </div>
          <div class="type-content">
            <p><strong>What:</strong> Important messages to preserve exactly as stated.</p>
            <p><strong>When to use:</strong> User explicitly asks to remember something specific.</p>
            <div class="example-box">
              <span class="example-label">Examples:</span>
              <ul>
                <li>"Remember: My WiFi password is Guest2024"</li>
                <li>"Note: Mom's phone is 555-1234"</li>
                <li>"Save this: Project deadline is Friday"</li>
              </ul>
            </div>
            <p><strong>Storage:</strong> Exact text preserved. Often extracted from working memory messages.</p>
          </div>
        </div>
      </div>

      <!-- 3. Working Memory Lifecycle -->
      <div id="working-memory" class="doc-section">
        <h2>Working Memory Lifecycle</h2>
        <p class="section-intro">Working memory manages conversation history within a session. Here's how it flows:</p>

        <div class="lifecycle-diagram">
          <div class="lifecycle-step">
            <div class="step-number">1</div>
            <div class="step-content">
              <strong>Session Created</strong>
              <p>New session starts when first message is sent. TTL timer begins.</p>
            </div>
          </div>
          <div class="lifecycle-arrow">↓</div>
          <div class="lifecycle-step">
            <div class="step-number">2</div>
            <div class="step-content">
              <strong>Messages Accumulate</strong>
              <p>Each exchange adds user + assistant messages. Context % grows.</p>
            </div>
          </div>
          <div class="lifecycle-arrow">↓</div>
          <div class="lifecycle-step highlight">
            <div class="step-number">3</div>
            <div class="step-content">
              <strong>Context Threshold (~70%)</strong>
              <p>When context reaches ~70% of window, summarization triggers.</p>
            </div>
          </div>
          <div class="lifecycle-arrow">↓</div>
          <div class="lifecycle-step">
            <div class="step-number">4</div>
            <div class="step-content">
              <strong>Summarization</strong>
              <p>Older messages compressed into summary. Recent messages preserved.</p>
            </div>
          </div>
          <div class="lifecycle-arrow">↓</div>
          <div class="lifecycle-step">
            <div class="step-number">5</div>
            <div class="step-content">
              <strong>Continue or Expire</strong>
              <p>Session continues with summary context, or expires after TTL (default: 30 min).</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 4. Summarization -->
      <div id="summarization" class="doc-section">
        <h2>Summarization Explained</h2>

        <div class="info-grid">
          <div class="info-card">
            <h4>When It Triggers</h4>
            <p>Summarization triggers when context usage exceeds the threshold (default ~70% of context window).</p>
            <p>The threshold is configured via <code>SUMMARIZATION_THRESHOLD=0.7</code> in AMS.</p>
          </div>
          <div class="info-card">
            <h4>What Gets Summarized</h4>
            <p>Older messages are summarized while recent messages are preserved verbatim.</p>
            <p>The summary captures key facts, decisions, and context from the conversation.</p>
          </div>
          <div class="info-card">
            <h4>How It's Stored</h4>
            <p>The summary replaces older messages in working memory as a <code>context</code> field.</p>
            <p>Recent messages remain as individual message objects.</p>
          </div>
          <div class="info-card">
            <h4>Impact on Responses</h4>
            <p>The LLM sees: summary + recent messages. This maintains context while staying within token limits.</p>
            <p>Users experience seamless continuity even in long conversations.</p>
          </div>
        </div>
      </div>

      <!-- 5. Semantic Search -->
      <div id="semantic-search" class="doc-section">
        <h2>Semantic Search Flow</h2>
        <p class="section-intro">How AMS finds relevant memories by meaning, not keywords:</p>

        <div class="flow-diagram">
          <div class="flow-step">
            <span class="flow-icon">1</span>
            <div class="flow-content">
              <strong>User Message</strong>
              <code>"What outdoor activities do I enjoy?"</code>
            </div>
          </div>
          <div class="flow-arrow">↓</div>
          <div class="flow-step">
            <span class="flow-icon">2</span>
            <div class="flow-content">
              <strong>Convert to Vector</strong>
              <p>AMS generates embedding using configured model (e.g., text-embedding-3-small)</p>
            </div>
          </div>
          <div class="flow-arrow">↓</div>
          <div class="flow-step">
            <span class="flow-icon">3</span>
            <div class="flow-content">
              <strong>Redis Vector Search (KNN)</strong>
              <p>Find K nearest neighbors in vector space, filtered by userId</p>
            </div>
          </div>
          <div class="flow-arrow">↓</div>
          <div class="flow-step">
            <span class="flow-icon">4</span>
            <div class="flow-content">
              <strong>Return Matches</strong>
              <code>"Alice loves hiking in the mountains"</code>
              <p class="match-note">Matched by meaning: "outdoor activities" ~ "hiking"</p>
            </div>
          </div>
          <div class="flow-arrow">↓</div>
          <div class="flow-step">
            <span class="flow-icon">5</span>
            <div class="flow-content">
              <strong>Inject into Prompt</strong>
              <p>Memories added to system prompt for LLM context</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 6. TTL and Expiration -->
      <div id="ttl" class="doc-section">
        <h2>TTL and Expiration</h2>

        <div class="concept-box">
          <h4>Why TTL Matters</h4>
          <p>TTL (Time-To-Live) automatically cleans up inactive sessions, preventing stale data buildup and reducing storage costs.</p>
        </div>

        <table class="config-table">
          <thead>
            <tr>
              <th>Setting</th>
              <th>Default</th>
              <th>Description</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><code>ttl_seconds</code></td>
              <td>1800 (30 min)</td>
              <td>Time until working memory expires after last access</td>
            </tr>
            <tr>
              <td><code>last_accessed</code></td>
              <td>Auto-updated</td>
              <td>Timestamp updated on each read/write operation</td>
            </tr>
          </tbody>
        </table>

        <p>In Redis, this translates to key expiration. You can see TTL in Redis Insight on working memory keys.</p>
      </div>

      <!-- 7. Deduplication -->
      <div id="deduplication" class="doc-section">
        <h2>Deduplication</h2>

        <div class="concept-box">
          <h4>Why Deduplication Matters</h4>
          <p>Without deduplication, streaming responses, retries, and repeated saves would create duplicate messages in memory.</p>
        </div>

        <div class="info-grid two-col">
          <div class="info-card">
            <h4>Working Memory</h4>
            <p>Messages are deduplicated by <code>role + content</code> match.</p>
            <p>Prevents duplicate user/assistant messages from accumulating.</p>
          </div>
          <div class="info-card">
            <h4>Long-Term Memory</h4>
            <p>Memories are hashed and compared before storing.</p>
            <p>Semantically similar memories can be merged or rejected.</p>
          </div>
        </div>
      </div>

      <!-- 8. Integration Patterns -->
      <div id="integration" class="doc-section">
        <h2>Integration Patterns</h2>
        <p class="section-intro">Three ways to integrate AMS with your application:</p>

        <div class="pattern-cards">
          <div class="pattern-card active">
            <div class="pattern-header">
              <span class="pattern-badge">This Workshop</span>
              <h4>Code-Driven</h4>
            </div>
            <p>Your application explicitly calls AMS APIs to store and retrieve memories.</p>
            <ul>
              <li>Full control over what's stored</li>
              <li>Predictable behavior</li>
              <li>Best for: Most production apps</li>
            </ul>
          </div>

          <div class="pattern-card">
            <div class="pattern-header">
              <h4>LLM-Driven (Tool Calls)</h4>
            </div>
            <p>LLM decides when to store/retrieve memories via function calling.</p>
            <ul>
              <li>More autonomous behavior</li>
              <li>LLM chooses relevance</li>
              <li>Best for: Autonomous agents</li>
            </ul>
          </div>

          <div class="pattern-card">
            <div class="pattern-header">
              <h4>Background Extraction</h4>
            </div>
            <p>AMS automatically extracts facts from conversations asynchronously.</p>
            <ul>
              <li>Zero application code changes</li>
              <li>Automatic topic/entity extraction</li>
              <li>Best for: Quick integration</li>
            </ul>
          </div>
        </div>
      </div>

      <!-- 9. Advanced Features -->
      <div id="advanced" class="doc-section">
        <h2>Advanced Features</h2>
        <p class="section-intro">These features are available in AMS but not covered in this workshop:</p>

        <div class="advanced-grid">
          <div class="advanced-item">
            <h4>Memory Decay</h4>
            <p>Memories can lose relevance over time. Configure decay factors to prioritize recent information in search results.</p>
            <code>recency_boost, recency_weight</code>
          </div>
          <div class="advanced-item">
            <h4>Forgetting Policies</h4>
            <p>Automatic memory removal based on rules: time-based, count-based, or relevance-based cleanup.</p>
            <code>POST /v1/long-term-memory/forget</code>
          </div>
          <div class="advanced-item">
            <h4>Topic/Entity Extraction</h4>
            <p>AMS can automatically extract topics and named entities from memories for filtering.</p>
            <code>ENABLE_TOPIC_EXTRACTION, ENABLE_NER</code>
          </div>
          <div class="advanced-item">
            <h4>Summary Views</h4>
            <p>Aggregate memories into summaries grouped by user, session, or custom fields.</p>
            <code>/v1/summary-views</code>
          </div>
        </div>
      </div>

      <!-- 10. Java SDK Reference -->
      <div id="sdk" class="doc-section">
        <h2>Java SDK Reference</h2>

        <h4>Installation</h4>
        <pre class="code-block"><code>// build.gradle.kts
implementation("com.redis:agent-memory-client-java:0.1.0")</code></pre>

        <h4>Client Initialization</h4>
        <pre class="code-block"><code>MemoryAPIClient client = MemoryAPIClient.builder("http://localhost:8000")
    .defaultNamespace("my-app")
    .timeout(30.0)
    .build();</code></pre>

        <h4>Working Memory Operations</h4>
        <pre class="code-block"><code>// Get working memory
WorkingMemoryResponse memory = client.workingMemory()
    .getWorkingMemory(sessionId, userId, namespace, "gpt-4o-mini", null);

// Append messages
client.workingMemory().appendMessagesToWorkingMemory(
    sessionId, messages, namespace, "gpt-4o-mini", null, userId);

// Delete session
client.workingMemory().deleteWorkingMemory(sessionId, userId, namespace);</code></pre>

        <h4>Long-Term Memory Operations</h4>
        <pre class="code-block"><code>// Store memory
MemoryRecord record = MemoryRecord.builder()
    .text("User prefers dark mode")
    .userId(userId)
    .memoryType(MemoryType.SEMANTIC)
    .build();
client.longTermMemory().createLongTermMemories(List.of(record));

// Search memories
SearchRequest request = SearchRequest.builder()
    .text("user preferences")
    .userId(userId)
    .limit(5)
    .build();
MemoryRecordResults results = client.longTermMemory()
    .searchLongTermMemories(request);</code></pre>
      </div>

      <!-- 11. Resources -->
      <div id="resources" class="doc-section">
        <h2>Resources</h2>
        <ul class="resource-list">
          <li>
            <a href="https://redis.github.io/agent-memory-server/" target="_blank">
              Official AMS Documentation
            </a>
          </li>
          <li>
            <a href="https://github.com/redis/agent-memory-server" target="_blank">
              GitHub Repository
            </a>
          </li>
          <li>
            <a href="https://redis.github.io/agent-memory-server/api/sdks/java-sdk/" target="_blank">
              Java SDK Guide
            </a>
          </li>
          <li>
            <a href="https://redis.io/docs/latest/develop/interact/search-and-query/query/vector-search/" target="_blank">
              Redis Vector Search Documentation
            </a>
          </li>
        </ul>
      </div>

      <div class="back-link">
        <router-link to="/" class="btn btn-secondary">Back to Home</router-link>
        <router-link to="/demo" class="btn btn-primary">Try the Demo</router-link>
      </div>
    </div>
  </div>
</template>

<script>
import { WorkshopHeader } from '../utils/components';

export default {
  name: 'MemoryLearn',
  components: { WorkshopHeader },
  computed: {
    workshopHubUrl() {
      return window.location.protocol + '//' + window.location.hostname + ':9000';
    }
  }
};
</script>

<style scoped>
.memory-learn { min-height: 100vh; background: linear-gradient(135deg, var(--color-dark-900) 0%, var(--color-dark-800) 100%); padding: var(--spacing-6); }
.main-container { max-width: 900px; margin: 0 auto; }
h1 { color: var(--color-text); margin-bottom: var(--spacing-4); }

/* Table of Contents */
.toc { background: var(--color-surface); border-radius: var(--radius-lg); padding: var(--spacing-4); margin-bottom: var(--spacing-4); }
.toc h3 { color: var(--color-text); margin: 0 0 var(--spacing-2) 0; font-size: var(--font-size-sm); text-transform: uppercase; letter-spacing: 0.5px; }
.toc ul { margin: 0; padding: 0; list-style: none; display: flex; flex-wrap: wrap; gap: var(--spacing-2); }
.toc li a { color: var(--color-text-secondary); text-decoration: none; font-size: var(--font-size-sm); padding: 4px 8px; border-radius: var(--radius-sm); background: var(--color-dark-800); transition: all 0.2s; }
.toc li a:hover { color: var(--color-brand); background: rgba(220, 56, 44, 0.15); }

/* Sections */
.doc-section { background: var(--color-surface); border-radius: var(--radius-xl); padding: var(--spacing-5); margin-bottom: var(--spacing-4); scroll-margin-top: var(--spacing-4); }
.doc-section h2 { color: var(--color-text); margin-bottom: var(--spacing-3); font-size: var(--font-size-lg); border-bottom: 1px solid var(--color-border); padding-bottom: var(--spacing-2); }
.doc-section h4 { color: var(--color-text); margin: var(--spacing-3) 0 var(--spacing-2) 0; font-size: var(--font-size-base); }
.doc-section p { color: var(--color-text-secondary); line-height: 1.6; margin-bottom: var(--spacing-2); }
.section-intro { color: var(--color-text-secondary); margin-bottom: var(--spacing-4); }

/* Architecture */
.architecture-diagram { display: flex; align-items: center; justify-content: center; gap: var(--spacing-3); margin-bottom: var(--spacing-4); flex-wrap: wrap; }
.arch-box { display: flex; align-items: center; gap: var(--spacing-2); padding: var(--spacing-2) var(--spacing-3); border-radius: var(--radius-md); font-weight: 600; border: 2px solid; font-size: var(--font-size-sm); }
.arch-box.client { background: rgba(59, 130, 246, 0.2); border-color: #3b82f6; color: #3b82f6; }
.arch-box.server { background: rgba(16, 185, 129, 0.2); border-color: #10b981; color: #10b981; }
.arch-box.redis { background: rgba(220, 56, 44, 0.2); border-color: #dc382d; color: #dc382d; }
.arch-arrow { color: var(--color-text-secondary); font-size: var(--font-size-lg); }
.feature-list { margin: var(--spacing-2) 0; padding-left: var(--spacing-4); }
.feature-list li { color: var(--color-text-secondary); margin-bottom: var(--spacing-1); font-size: var(--font-size-sm); }
.feature-list strong { color: var(--color-text); }

/* Memory Type Cards */
.memory-type-card { background: var(--color-dark-800); border-radius: var(--radius-lg); margin-bottom: var(--spacing-3); overflow: hidden; }
.type-header { display: flex; align-items: center; gap: var(--spacing-2); padding: var(--spacing-3); }
.type-header.semantic { background: linear-gradient(90deg, rgba(139, 92, 246, 0.3), transparent); border-left: 3px solid #8b5cf6; }
.type-header.episodic { background: linear-gradient(90deg, rgba(245, 158, 11, 0.3), transparent); border-left: 3px solid #f59e0b; }
.type-header.message { background: linear-gradient(90deg, rgba(59, 130, 246, 0.3), transparent); border-left: 3px solid #3b82f6; }
.type-badge { font-size: var(--font-size-xs); font-weight: 700; padding: 2px 8px; border-radius: var(--radius-sm); }
.type-header.semantic .type-badge { background: #8b5cf6; color: white; }
.type-header.episodic .type-badge { background: #f59e0b; color: black; }
.type-header.message .type-badge { background: #3b82f6; color: white; }
.type-title { color: var(--color-text); font-weight: 600; }
.type-content { padding: var(--spacing-3); }
.type-content p { margin-bottom: var(--spacing-2); font-size: var(--font-size-sm); }
.example-box { background: var(--color-dark-900); padding: var(--spacing-2) var(--spacing-3); border-radius: var(--radius-sm); margin: var(--spacing-2) 0; }
.example-label { color: var(--color-text-secondary); font-size: var(--font-size-xs); text-transform: uppercase; }
.example-box ul { margin: var(--spacing-1) 0 0 var(--spacing-3); padding: 0; }
.example-box li { color: #a0a0a0; font-size: var(--font-size-sm); font-style: italic; }

/* Lifecycle Diagram */
.lifecycle-diagram { display: flex; flex-direction: column; gap: var(--spacing-1); }
.lifecycle-step { display: flex; gap: var(--spacing-3); align-items: flex-start; padding: var(--spacing-3); background: var(--color-dark-800); border-radius: var(--radius-md); }
.lifecycle-step.highlight { border: 1px solid #f59e0b; background: rgba(245, 158, 11, 0.1); }
.step-number { width: 28px; height: 28px; border-radius: 50%; background: var(--color-brand); color: white; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: var(--font-size-sm); flex-shrink: 0; }
.step-content strong { color: var(--color-text); font-size: var(--font-size-sm); }
.step-content p { margin: var(--spacing-1) 0 0 0; color: var(--color-text-secondary); font-size: var(--font-size-xs); }
.lifecycle-arrow { color: var(--color-text-secondary); text-align: center; font-size: var(--font-size-sm); }

/* Info Grid */
.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: var(--spacing-3); }
.info-grid.two-col { grid-template-columns: 1fr 1fr; }
.info-card { background: var(--color-dark-800); padding: var(--spacing-3); border-radius: var(--radius-md); }
.info-card h4 { color: var(--color-text); margin: 0 0 var(--spacing-2) 0; font-size: var(--font-size-sm); }
.info-card p { color: var(--color-text-secondary); font-size: var(--font-size-sm); margin: 0 0 var(--spacing-1) 0; }

/* Flow Diagram */
.flow-diagram { display: flex; flex-direction: column; gap: var(--spacing-1); }
.flow-step { display: flex; gap: var(--spacing-3); align-items: flex-start; padding: var(--spacing-3); background: var(--color-dark-800); border-radius: var(--radius-md); }
.flow-icon { width: 24px; height: 24px; border-radius: 50%; background: #10b981; color: white; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: var(--font-size-xs); flex-shrink: 0; }
.flow-content { flex: 1; }
.flow-content strong { color: var(--color-text); font-size: var(--font-size-sm); display: block; }
.flow-content code { background: var(--color-dark-900); padding: 2px 6px; border-radius: var(--radius-sm); color: #4ade80; font-size: var(--font-size-xs); display: inline-block; margin-top: 4px; }
.flow-content p { margin: 4px 0 0 0; color: var(--color-text-secondary); font-size: var(--font-size-xs); }
.match-note { color: #f59e0b !important; font-style: italic; }
.flow-arrow { color: var(--color-text-secondary); text-align: center; font-size: var(--font-size-sm); }

/* Concept Box */
.concept-box { background: rgba(59, 130, 246, 0.1); border: 1px solid rgba(59, 130, 246, 0.3); border-radius: var(--radius-md); padding: var(--spacing-3); margin-bottom: var(--spacing-3); }
.concept-box h4 { color: #60a5fa; margin: 0 0 var(--spacing-1) 0; font-size: var(--font-size-sm); }
.concept-box p { color: var(--color-text-secondary); font-size: var(--font-size-sm); margin: 0; }

/* Config Table */
.config-table { width: 100%; border-collapse: collapse; margin: var(--spacing-3) 0; font-size: var(--font-size-sm); }
.config-table th { background: var(--color-dark-800); color: var(--color-text); padding: var(--spacing-2); text-align: left; font-weight: 600; }
.config-table td { padding: var(--spacing-2); color: var(--color-text-secondary); border-bottom: 1px solid var(--color-border); }
.config-table code { background: var(--color-dark-900); padding: 2px 6px; border-radius: var(--radius-sm); color: #f59e0b; }

/* Pattern Cards */
.pattern-cards { display: grid; grid-template-columns: repeat(3, 1fr); gap: var(--spacing-3); }
.pattern-card { background: var(--color-dark-800); border-radius: var(--radius-md); padding: var(--spacing-3); border: 1px solid var(--color-border); }
.pattern-card.active { border-color: var(--color-brand); background: rgba(220, 56, 44, 0.05); }
.pattern-header { margin-bottom: var(--spacing-2); }
.pattern-badge { font-size: var(--font-size-xs); background: var(--color-brand); color: white; padding: 2px 6px; border-radius: var(--radius-sm); margin-bottom: var(--spacing-1); display: inline-block; }
.pattern-header h4 { color: var(--color-text); margin: var(--spacing-1) 0 0 0; font-size: var(--font-size-sm); }
.pattern-card p { color: var(--color-text-secondary); font-size: var(--font-size-xs); margin-bottom: var(--spacing-2); }
.pattern-card ul { margin: 0; padding-left: var(--spacing-3); }
.pattern-card li { color: var(--color-text-secondary); font-size: var(--font-size-xs); margin-bottom: 2px; }

/* Advanced Grid */
.advanced-grid { display: grid; grid-template-columns: 1fr 1fr; gap: var(--spacing-3); }
.advanced-item { background: var(--color-dark-800); padding: var(--spacing-3); border-radius: var(--radius-md); }
.advanced-item h4 { color: var(--color-text); margin: 0 0 var(--spacing-1) 0; font-size: var(--font-size-sm); }
.advanced-item p { color: var(--color-text-secondary); font-size: var(--font-size-xs); margin: 0 0 var(--spacing-2) 0; }
.advanced-item code { background: var(--color-dark-900); padding: 2px 6px; border-radius: var(--radius-sm); color: #888; font-size: var(--font-size-xs); }

/* Code Blocks */
.code-block { background: var(--color-dark-900); padding: var(--spacing-3); border-radius: var(--radius-md); overflow-x: auto; margin-bottom: var(--spacing-3); }
.code-block code { color: #e0e0e0; font-family: 'JetBrains Mono', 'Fira Code', monospace; font-size: var(--font-size-xs); white-space: pre; }

/* Resources */
.resource-list { margin: 0; padding: 0; list-style: none; }
.resource-list li { margin-bottom: var(--spacing-2); }
.resource-list a { color: var(--color-brand); text-decoration: none; font-size: var(--font-size-sm); }
.resource-list a:hover { text-decoration: underline; }

/* Back Link */
.back-link { display: flex; justify-content: center; gap: var(--spacing-3); margin-top: var(--spacing-4); }

/* Responsive */
@media (max-width: 768px) {
  .info-grid, .pattern-cards, .advanced-grid { grid-template-columns: 1fr; }
  .toc ul { flex-direction: column; }
}
</style>

