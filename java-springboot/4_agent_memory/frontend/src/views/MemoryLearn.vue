<template>
  <div class="memory-learn">
    <WorkshopHeader
      :hub-url="workshopHubUrl"
      :steps="['Home', 'Learn']"
      :current-step="2"
    />

    <div class="main-container">
      <h1>Learn More</h1>

      <div class="doc-section">
        <h2>Architecture Overview</h2>
        <div class="architecture-diagram">
          <div class="arch-box client">Your App</div>
          <div class="arch-arrow">→</div>
          <div class="arch-box server">Agent Memory Server</div>
          <div class="arch-arrow">→</div>
          <div class="arch-box redis">Redis</div>
        </div>
        <p>
          The Agent Memory Server acts as a middleware between your application and Redis.
          It handles embedding generation, semantic search, and memory management automatically.
        </p>
      </div>

      <div class="doc-section">
        <h2>Key Features</h2>
        <div class="features">
          <div class="feature">
            <h4>Automatic Summarization</h4>
            <p>Working memory automatically summarizes long conversations to stay within token limits.</p>
          </div>
          <div class="feature">
            <h4>Deduplication</h4>
            <p>Long-term memories are deduplicated to avoid storing redundant information.</p>
          </div>
          <div class="feature">
            <h4>Multi-tenant</h4>
            <p>Support for namespaces and user isolation for multi-tenant applications.</p>
          </div>
          <div class="feature">
            <h4>Semantic Search</h4>
            <p>Vector-based search finds memories by meaning, not just keywords.</p>
          </div>
        </div>
      </div>

      <div class="doc-section">
        <h2>Java Client Usage</h2>
        <pre class="code-block"><code>// Add to build.gradle.kts
implementation("com.github.redis.agent-memory-server:agent-memory-client:0.1.0")

// Create client
var client = AgentMemoryClient.builder()
    .baseUrl("http://localhost:8000")
    .build();

// Store working memory
var memory = WorkingMemory.builder()
    .sessionId("session-123")
    .messages(List.of(
        new Message("user", "Hello!"),
        new Message("assistant", "Hi there!")
    ))
    .build();
client.putWorkingMemory("session-123", memory);

// Search long-term memory
var results = client.searchLongTermMemory(
    SearchRequest.builder()
        .text("user preferences")
        .limit(5)
        .build()
);</code></pre>
      </div>

      <div class="doc-section">
        <h2>Resources</h2>
        <ul class="resource-list">
          <li>
            <a href="https://redis.github.io/agent-memory-server/" target="_blank">
              Official Documentation
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
        </ul>
      </div>

      <div class="back-link">
        <router-link to="/" class="btn btn-secondary">← Back to Home</router-link>
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
.memory-learn {
  min-height: 100vh;
  background: linear-gradient(135deg, var(--color-dark-900) 0%, var(--color-dark-800) 100%);
  padding: var(--spacing-6);
}
.main-container { max-width: 900px; margin: 0 auto; }
h1 { color: var(--color-text); margin-bottom: var(--spacing-5); }
.doc-section {
  background: var(--color-surface); border-radius: var(--radius-xl); padding: var(--spacing-5);
  margin-bottom: var(--spacing-4);
}
.doc-section h2 { color: var(--color-text); margin-bottom: var(--spacing-4); font-size: var(--font-size-lg); }
.doc-section p { color: var(--color-text-secondary); line-height: 1.6; }
.architecture-diagram {
  display: flex; align-items: center; justify-content: center; gap: var(--spacing-3);
  margin-bottom: var(--spacing-4);
}
.arch-box {
  padding: var(--spacing-3) var(--spacing-4); border-radius: var(--radius-md);
  font-weight: bold; color: var(--color-text);
}
.arch-box.client { background: #3b82f6; }
.arch-box.server { background: #10b981; }
.arch-box.redis { background: #dc382d; }
.arch-arrow { color: var(--color-text-secondary); font-size: 1.5rem; }
.features { display: grid; grid-template-columns: 1fr 1fr; gap: var(--spacing-4); }
.feature { background: var(--color-dark-800); padding: var(--spacing-4); border-radius: var(--radius-lg); }
.feature h4 { color: var(--color-text); margin-bottom: var(--spacing-2); }
.feature p { color: var(--color-text-secondary); font-size: var(--font-size-sm); margin: 0; }
.code-block {
  background: var(--color-dark-900); padding: var(--spacing-4); border-radius: var(--radius-lg);
  overflow-x: auto;
}
.code-block code { color: #e0e0e0; font-family: 'Courier New', monospace; font-size: var(--font-size-sm); }
.resource-list { margin: 0; padding: 0; list-style: none; }
.resource-list li { margin-bottom: var(--spacing-3); }
.resource-list a { color: var(--color-brand); text-decoration: none; font-size: var(--font-size-base); }
.resource-list a:hover { text-decoration: underline; }
.back-link { text-align: center; margin-top: var(--spacing-4); }
@media (max-width: 768px) { .features { grid-template-columns: 1fr; } }
</style>

