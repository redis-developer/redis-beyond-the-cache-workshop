<template>
  <div class="memory-demo">
    <div class="main-container">
      <!-- Left Sidebar: Tutorial Tests -->
      <div class="workshop-panel">
        <div class="workshop-header">
          <h2><img src="@/assets/logo/small.png" alt="Redis Logo" width="28" height="28" class="logo-small" /> Test Your Memory</h2>
        </div>
        <div class="workshop-content">
          <!-- API Key Setup -->
          <div v-if="!apiKey" class="api-key-setup">
            <h3>Setup Required</h3>
            <p>Enter your OpenAI API key to test the AI chat with memory.</p>
            <input v-model="tempApiKey" type="password" placeholder="sk-..." @keyup.enter="validateKey" />
            <button @click="validateKey" class="btn btn-primary" :disabled="validating">
              {{ validating ? 'Validating...' : 'Continue' }}
            </button>
            <p v-if="keyError" class="error">{{ keyError }}</p>
          </div>

          <!-- Guided Tests -->
          <template v-else>
            <div class="test-progress">
              <span class="progress-label">Progress:</span>
              <span class="progress-value">{{ testStep > 8 ? 8 : testStep - 1 }}/8 tests</span>
            </div>

            <!-- Test 1: Working Memory Basics -->
            <div v-if="testStep === 1" class="test-step">
              <h4>Test 1: Working Memory Basics</h4>
              <p class="test-instructions">Send these messages in order:</p>
              <ol class="test-actions">
                <li>Send: <code>Hi, my name is Alice</code></li>
                <li>Send: <code>What is my name?</code></li>
              </ol>
              <div class="observations">
                <h5>What to observe:</h5>
                <label class="obs-item"><input type="checkbox" v-model="observations.t1_recall" /> AI correctly recalls "Alice"</label>
                <label class="obs-item"><input type="checkbox" v-model="observations.t1_session" /> Session Info bar shows 2+ messages</label>
                <label class="obs-item"><input type="checkbox" v-model="observations.t1_context" /> Context bar shows small % (1-5%)</label>
              </div>
              <p class="test-explanation">Working memory stores conversation history within a session. AMS automatically persists this to Redis.</p>
              <button @click="completeStep(1)" class="btn btn-primary" :disabled="!canProceed(1)">Next Test</button>
            </div>

            <!-- Test 2: Context Window Growth -->
            <div v-else-if="testStep === 2" class="test-step">
              <h4>Test 2: Context Window Growth</h4>
              <p class="test-instructions">Continue the conversation with 4-5 more exchanges:</p>
              <ol class="test-actions">
                <li>Send: <code>I work as a software engineer</code></li>
                <li>Send: <code>My favorite color is blue</code></li>
                <li>Send: <code>Tell me everything you know about me</code></li>
              </ol>
              <div class="observations">
                <h5>What to observe:</h5>
                <label class="obs-item"><input type="checkbox" v-model="observations.t2_growth" /> Context bar grows with each message</label>
                <label class="obs-item"><input type="checkbox" v-model="observations.t2_recall" /> AI remembers all details you shared</label>
                <label class="obs-item"><input type="checkbox" v-model="observations.t2_count" /> Message count increases in session info</label>
              </div>
              <p class="test-explanation">As conversation grows, context usage increases. At ~70%, AMS automatically summarizes older messages to stay within limits.</p>
              <button @click="completeStep(2)" class="btn btn-primary" :disabled="!canProceed(2)">Next Test</button>
            </div>

            <!-- Test 3: Store Long-Term Memories -->
            <div v-else-if="testStep === 3" class="test-step">
              <h4>Test 3: Store Long-Term Memories</h4>
              <p class="test-instructions">Click "Show Memories" and add these facts:</p>
              <ol class="test-actions">
                <li>Add: <code>Alice loves hiking in the mountains</code></li>
                <li>Add: <code>Alice is vegetarian and enjoys cooking</code></li>
              </ol>
              <div class="observations">
                <h5>What to observe:</h5>
                <label class="obs-item"><input type="checkbox" v-model="observations.t3_stored" /> Memories appear in "Stored" section</label>
                <label class="obs-item"><input type="checkbox" v-model="observations.t3_type" /> Memory type shows as SEMANTIC</label>
              </div>
              <p class="test-explanation">Long-term memories are stored with vector embeddings, enabling semantic search - finding memories by meaning, not just keywords.</p>
              <button @click="completeStep(3)" class="btn btn-primary" :disabled="!canProceed(3)">Next Test</button>
            </div>

            <!-- Test 4: Semantic Search Demo -->
            <div v-else-if="testStep === 4" class="test-step">
              <h4>Test 4: Semantic Search</h4>
              <p class="test-instructions">Test semantic matching (meaning, not keywords):</p>
              <ol class="test-actions">
                <li>Ensure "Use Long-Term Memory" is checked</li>
                <li>Ask: <code>What outdoor activities do I enjoy?</code></li>
              </ol>
              <div class="observations">
                <h5>What to observe:</h5>
                <label class="obs-item"><input type="checkbox" v-model="observations.t4_hiking" /> AI mentions hiking (semantic match!)</label>
                <label class="obs-item"><input type="checkbox" v-model="observations.t4_retrieved" /> "Retrieved This Turn" shows the hiking memory</label>
              </div>
              <p class="test-explanation">Notice: you asked about "outdoor activities" but the memory says "hiking". Semantic search found the match by understanding meaning!</p>
              <button @click="completeStep(4)" class="btn btn-primary" :disabled="!canProceed(4)">Next Test</button>
            </div>

            <!-- Test 5: Clear Working Memory -->
            <div v-else-if="testStep === 5" class="test-step">
              <h4>Test 5: Clear Working Memory</h4>
              <p class="test-instructions">Reset the session:</p>
              <ol class="test-actions">
                <li>Click <strong>Clear Chat</strong> button</li>
                <li>Note: Keep "Show Memories" panel open</li>
              </ol>
              <div class="observations">
                <h5>What to observe:</h5>
                <label class="obs-item"><input type="checkbox" v-model="observations.t5_cleared" /> Conversation messages are gone</label>
                <label class="obs-item"><input type="checkbox" v-model="observations.t5_context" /> Context bar resets (no percentage shown)</label>
                <label class="obs-item"><input type="checkbox" v-model="observations.t5_ltm" /> Long-term memories still visible in sidebar</label>
              </div>
              <p class="test-explanation">Working memory (session) is deleted, but long-term memories persist! This is the key difference between the two memory types.</p>
              <button @click="completeStep(5)" class="btn btn-primary" :disabled="!canProceed(5)">Next Test</button>
            </div>

            <!-- Test 6: Cross-Session Persistence -->
            <div v-else-if="testStep === 6" class="test-step">
              <h4>Test 6: Cross-Session Persistence</h4>
              <p class="test-instructions">In this fresh session, test long-term memory:</p>
              <ol class="test-actions">
                <li>Ensure "Use Long-Term Memory" is checked</li>
                <li>Ask: <code>What kind of food do I prefer?</code></li>
              </ol>
              <div class="observations">
                <h5>What to observe:</h5>
                <label class="obs-item"><input type="checkbox" v-model="observations.t6_vegetarian" /> AI knows you're vegetarian</label>
                <label class="obs-item"><input type="checkbox" v-model="observations.t6_cooking" /> AI may mention cooking</label>
                <label class="obs-item"><input type="checkbox" v-model="observations.t6_retrieved" /> Check "Retrieved This Turn" for matched memory</label>
              </div>
              <p class="test-explanation">Long-term memory persisted across sessions! The AI retrieved relevant facts from previous interactions.</p>
              <button @click="completeStep(6)" class="btn btn-primary" :disabled="!canProceed(6)">Next Test</button>
            </div>

            <!-- Test 7: TTL and Session Lifecycle -->
            <div v-else-if="testStep === 7" class="test-step">
              <h4>Test 7: TTL and Session Lifecycle</h4>
              <p class="test-instructions">Observe the session lifecycle:</p>
              <ol class="test-actions">
                <li>Look at the TTL countdown in the session info bar</li>
                <li>Send a message and watch TTL behavior</li>
              </ol>
              <div class="observations">
                <h5>What to observe:</h5>
                <label class="obs-item"><input type="checkbox" v-model="observations.t7_ttl" /> TTL shows countdown (starts at 30:00)</label>
                <label class="obs-item"><input type="checkbox" v-model="observations.t7_countdown" /> TTL counts down each second</label>
              </div>
              <p class="test-explanation">Working memory has a TTL (Time-To-Live). After 30 minutes of inactivity, the session automatically expires. This prevents stale data buildup.</p>
              <button @click="completeStep(7)" class="btn btn-primary" :disabled="!canProceed(7)">Next Test</button>
            </div>

            <!-- Test 8: Redis Insight Exploration -->
            <div v-else-if="testStep === 8" class="test-step insight-test">
              <h4>Test 8: Redis Insight Exploration</h4>
              <p class="test-instructions">Open <a href="http://localhost:5540" target="_blank" class="link">Redis Insight</a> and explore the data structures:</p>

              <div class="insight-guide">
                <div class="insight-section">
                  <h5>1. Working Memory</h5>
                  <div class="key-pattern">
                    <span class="pattern-label">Key:</span>
                    <code>working_memory:workshop:session-default</code>
                  </div>
                  <ul class="field-list">
                    <li><strong>messages</strong> - JSON array of conversation</li>
                    <li><strong>summary</strong> - Auto-generated summary (if context exceeded)</li>
                    <li><strong>tokens</strong> - Current token count</li>
                  </ul>
                  <div class="insight-action">
                    <span class="action-icon">Try:</span> Right-click key and check TTL
                  </div>
                </div>

                <div class="insight-section">
                  <h5>2. Long-Term Memory</h5>
                  <div class="key-pattern">
                    <span class="pattern-label">Keys:</span>
                    <code>long_term_memory:workshop:*</code>
                  </div>
                  <ul class="field-list">
                    <li><strong>text</strong> - The stored memory</li>
                    <li><strong>user_id</strong> - Owner (alice or bob)</li>
                    <li><strong>memory_type</strong> - semantic/episodic/message</li>
                    <li><strong>embedding</strong> - Vector (1536 dims)</li>
                  </ul>
                </div>

                <div class="insight-section">
                  <h5>3. CLI Commands</h5>
                  <div class="cli-box">
                    <code>KEYS working_memory:workshop:*</code>
                    <code>HGETALL working_memory:workshop:session-default</code>
                    <code>TTL working_memory:workshop:session-default</code>
                    <code>FT.INFO idx:long_term_memory:workshop</code>
                  </div>
                </div>
              </div>

              <div class="observations">
                <h5>Verify you found:</h5>
                <label class="obs-item"><input type="checkbox" v-model="observations.t8_working" /> Working memory key with messages array</label>
                <label class="obs-item"><input type="checkbox" v-model="observations.t8_ltm" /> Long-term memory with embedding field</label>
                <label class="obs-item"><input type="checkbox" v-model="observations.t8_ttl" /> TTL value on working memory (default: 1800s)</label>
              </div>
              <button @click="completeStep(8)" class="btn btn-primary" :disabled="!canProceed(8)">Complete</button>
            </div>

            <!-- All Tests Complete -->
            <div v-else class="test-complete">
              <h4>All Tests Complete!</h4>
              <div class="alert alert-success">You've mastered AI memory with Redis AMS!</div>
              <h4>Key Takeaways</h4>
              <ul class="takeaways">
                <li><strong>Working Memory</strong> - Session-scoped, auto-summarizes, has TTL</li>
                <li><strong>Long-Term Memory</strong> - Persistent, semantic search via vectors</li>
                <li><strong>Context Management</strong> - AMS handles window limits automatically</li>
                <li><strong>Cross-Session</strong> - Facts persist and retrieve across sessions</li>
              </ul>
              <h4>Want to Learn More?</h4>
              <p class="learn-more-text">Dive deeper into memory types, lifecycle, summarization, and advanced features.</p>
              <div class="button-row">
                <router-link to="/learn" class="btn btn-primary">Reference Guide</router-link>
                <button @click="restartTests" class="btn btn-secondary">Restart Tests</button>
              </div>
            </div>

            <div class="nav-links">
              <router-link to="/" class="nav-link">Back to Home</router-link>
              <router-link to="/editor" class="nav-link">Code Editor</router-link>
              <router-link to="/learn" class="nav-link">Reference Guide</router-link>
            </div>
          </template>
        </div>
      </div>

      <!-- Right Section: Chat -->
      <div class="chat-section">
        <div class="chat-header">
          <div class="header-left">
            <h3>AI Chat with Memory</h3>
            <div class="session-controls">
              <div class="session-field">
                <label>Session:</label>
                <input v-model="sessionId" type="text" class="session-input" placeholder="session-id" />
              </div>
              <div class="session-field">
                <label>User:</label>
                <input v-model="userId" type="text" class="session-input" placeholder="user-id" />
              </div>
            </div>
          </div>
          <div class="header-right">
            <div v-if="contextPercentage != null" class="context-usage">
              <span class="context-label">Context:</span>
              <div class="context-bar">
                <div class="context-fill" :style="{ width: (contextPercentage * 100) + '%' }" :class="{ warning: contextPercentage > 0.7, danger: contextPercentage > 0.9 }"></div>
              </div>
              <span class="context-value">{{ (contextPercentage * 100).toFixed(1) }}%</span>
            </div>
            <label class="toggle-label">
              <input type="checkbox" v-model="useMemory" />
              <span>Use Long-Term Memory</span>
            </label>
            <button @click="clearChat" class="btn btn-sm">Clear Chat</button>
            <button @click="showMemoryPanel = !showMemoryPanel" class="btn btn-sm">
              {{ showMemoryPanel ? 'Hide' : 'Show' }} Memories
            </button>
            <button @click="showConsole = !showConsole" class="btn btn-sm btn-console">
              {{ showConsole ? 'Hide' : 'Show' }} Console
            </button>
          </div>
        </div>

        <!-- Session Info Bar -->
        <div v-if="sessionInfo" class="session-info-bar">
          <span class="session-label">Session:</span>
          <span class="session-value">{{ sessionInfo.conversationId }}</span>
          <span class="session-divider">|</span>
          <span class="session-label">Messages:</span>
          <span class="session-value">{{ sessionInfo.messageCount }}</span>
          <span class="session-divider">|</span>
          <span class="session-label">TTL:</span>
          <span class="session-value" :class="{ 'ttl-warning': ttlRemaining != null && ttlRemaining < 300 }">
            {{ ttlRemaining != null ? formatTtl(ttlRemaining) : formatTtl(sessionInfo.ttlSeconds) }}
          </span>
        </div>

        <div class="chat-body">
          <div class="messages-panel" ref="messagesPanel">
            <div v-if="messages.length === 0" class="empty-state">
              <p>Start chatting to test your memory implementation!</p>
            </div>
            <div v-for="(msg, idx) in messages" :key="idx" :class="['message', msg.role]">
              <div class="message-content">{{ msg.content }}</div>
            </div>
            <div v-if="loading" class="message assistant">
              <div class="message-content typing">Thinking...</div>
            </div>
          </div>

          <div v-if="showMemoryPanel" class="memory-panel">
            <h4>Long-Term Memories</h4>
            <div class="memory-input">
              <input v-model="newMemory" placeholder="Add a memory..." @keyup.enter="addMemory" />
              <button @click="addMemory" class="btn btn-sm">Add</button>
            </div>

            <!-- Last Retrieved (used in current response) -->
            <div v-if="lastRetrievedMemories.length > 0" class="memory-section">
              <div class="memory-section-header">Retrieved This Turn</div>
              <div v-for="(mem, idx) in lastRetrievedMemories" :key="'r-'+idx" class="memory-item retrieved">{{ mem }}</div>
            </div>

            <!-- All Stored Memories -->
            <div class="memory-section">
              <div class="memory-section-header">Stored ({{ storedMemories.length }})</div>
              <div v-for="mem in storedMemories" :key="mem.id" class="memory-item stored">
                <span class="memory-text">{{ mem.text }}</span>
                <span class="memory-type">{{ mem.type }}</span>
              </div>
              <p v-if="storedMemories.length === 0" class="no-memories">No memories stored yet</p>
            </div>
          </div>
        </div>

        <!-- Memory Console Panel -->
        <div v-if="showConsole" class="console-panel">
          <div class="console-header">
            <span class="console-title">=== Memory Console ===</span>
            <button @click="consoleEvents = []" class="btn btn-sm">Clear</button>
          </div>
          <div class="console-output" ref="consoleOutput">
            <div v-if="consoleEvents.length === 0" class="console-empty">
              Send a message to see memory events...
            </div>
            <div v-for="(event, idx) in consoleEvents" :key="idx" class="console-event">{{ event }}</div>
          </div>
        </div>

        <div class="chat-input">
          <input v-model="inputMessage" placeholder="Type a message..." @keyup.enter="sendMessage" :disabled="loading || !apiKey" />
          <button @click="sendMessage" class="btn btn-primary" :disabled="loading || !inputMessage.trim() || !apiKey">Send</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getApiUrl } from '../utils/basePath';

const STORAGE_KEY = 'agentMemoryWorkshop';

export default {
  name: 'MemoryChat',
  data() {
    return {
      apiKey: localStorage.getItem('openai_api_key') || '',
      tempApiKey: '',
      keyError: '',
      validating: false,
      sessionId: 'session-default',
      userId: 'user-default',
      messages: [],
      inputMessage: '',
      loading: false,
      useMemory: true,
      showMemoryPanel: false,
      showConsole: false,
      storedMemories: [],
      lastRetrievedMemories: [],
      newMemory: '',
      testStep: 1,
      contextPercentage: null,
      consoleEvents: [],
      sessionInfo: null,
      sessionStartTime: null,
      ttlTimer: null,
      ttlRemaining: null,
      memoryPollTimer: null,
      observations: {
        t1_recall: false, t1_session: false, t1_context: false,
        t2_growth: false, t2_recall: false, t2_count: false,
        t3_stored: false, t3_type: false,
        t4_hiking: false, t4_retrieved: false,
        t5_cleared: false, t5_context: false, t5_ltm: false,
        t6_vegetarian: false, t6_cooking: false, t6_retrieved: false,
        t7_ttl: false, t7_countdown: false,
        t8_working: false, t8_ltm: false, t8_ttl: false
      }
    };
  },
  mounted() {
    this.loadTestStep();
    this.loadObservations();
    this.startMemoryPolling();
  },
  beforeUnmount() {
    if (this.ttlTimer) clearInterval(this.ttlTimer);
    if (this.memoryPollTimer) clearInterval(this.memoryPollTimer);
  },
  methods: {
    loadTestStep() {
      const saved = localStorage.getItem(STORAGE_KEY);
      if (saved) {
        try { this.testStep = JSON.parse(saved).testStep || 1; } catch (e) { this.testStep = 1; }
      }
    },
    loadObservations() {
      const saved = localStorage.getItem(STORAGE_KEY);
      if (saved) {
        try {
          const data = JSON.parse(saved);
          if (data.observations) this.observations = { ...this.observations, ...data.observations };
        } catch (e) { /* ignore */ }
      }
    },
    saveTestStep() {
      const saved = localStorage.getItem(STORAGE_KEY);
      let data = {};
      if (saved) { try { data = JSON.parse(saved); } catch (e) { data = {}; } }
      data.testStep = this.testStep;
      data.observations = this.observations;
      localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
    },
    canProceed(step) {
      // Require at least 2 observations checked per test (flexible validation)
      const prefix = `t${step}_`;
      const checked = Object.keys(this.observations).filter(k => k.startsWith(prefix) && this.observations[k]).length;
      return checked >= 2;
    },
    completeStep(step) { this.testStep = step + 1; this.saveTestStep(); },
    restartTests() {
      this.testStep = 1;
      this.observations = {
        t1_recall: false, t1_session: false, t1_context: false,
        t2_growth: false, t2_recall: false, t2_count: false,
        t3_stored: false, t3_type: false,
        t4_hiking: false, t4_retrieved: false,
        t5_cleared: false, t5_context: false, t5_ltm: false,
        t6_vegetarian: false, t6_cooking: false, t6_retrieved: false,
        t7_ttl: false, t7_countdown: false,
        t8_working: false, t8_ltm: false, t8_ttl: false
      };
      this.saveTestStep();
    },
    async validateKey() {
      this.validating = true;
      this.keyError = '';
      try {
        const res = await fetch(getApiUrl('/api/chat/validate-key'), {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ apiKey: this.tempApiKey })
        });
        const data = await res.json();
        if (data.valid) {
          this.apiKey = this.tempApiKey;
          localStorage.setItem('openai_api_key', this.apiKey);
        } else { this.keyError = data.error || 'Invalid API key'; }
      } catch (e) { this.keyError = 'Failed to validate key'; }
      this.validating = false;
    },
    async sendMessage() {
      if (!this.inputMessage.trim() || this.loading || !this.apiKey) return;
      const userMsg = this.inputMessage.trim();
      this.messages.push({ role: 'user', content: userMsg });
      this.inputMessage = '';
      this.loading = true;
      this.$nextTick(() => this.scrollToBottom());
      try {
        const res = await fetch(getApiUrl('/api/chat'), {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ sessionId: this.sessionId, userId: this.userId, message: userMsg, apiKey: this.apiKey, useMemory: this.useMemory })
        });
        const data = await res.json();
        this.messages.push({ role: 'assistant', content: data.response });
        if (data.relevantMemories) {
          this.lastRetrievedMemories = data.relevantMemories;
        }
        if (data.contextPercentage != null) this.contextPercentage = data.contextPercentage;
        if (data.events) {
          this.consoleEvents.push(...data.events);
          this.$nextTick(() => this.scrollConsole());
        }
        if (data.sessionInfo) {
          this.sessionInfo = data.sessionInfo;
          // Start TTL countdown on first message
          if (!this.sessionStartTime) {
            this.sessionStartTime = Date.now();
            this.ttlRemaining = data.sessionInfo.ttlSeconds;
            this.startTtlTimer();
          }
        }
      } catch (e) { this.messages.push({ role: 'assistant', content: 'Error: ' + e.message }); }
      this.loading = false;
      this.$nextTick(() => this.scrollToBottom());
    },
    startTtlTimer() {
      if (this.ttlTimer) clearInterval(this.ttlTimer);
      this.ttlTimer = setInterval(() => {
        if (this.sessionStartTime && this.sessionInfo) {
          const elapsed = Math.floor((Date.now() - this.sessionStartTime) / 1000);
          this.ttlRemaining = Math.max(0, this.sessionInfo.ttlSeconds - elapsed);
          if (this.ttlRemaining <= 0) {
            clearInterval(this.ttlTimer);
            this.consoleEvents.push(this.timestamp() + ' Session TTL expired');
          }
        }
      }, 1000);
    },
    async addMemory() {
      if (!this.newMemory.trim()) return;
      await fetch(getApiUrl('/api/chat/memory'), { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ userId: this.userId, text: this.newMemory }) });
      this.newMemory = '';
      // Immediately fetch updated memories
      await this.fetchStoredMemories();
    },
    startMemoryPolling() {
      this.fetchStoredMemories();
      this.memoryPollTimer = setInterval(() => this.fetchStoredMemories(), 5000);
    },
    async fetchStoredMemories() {
      try {
        const res = await fetch(getApiUrl(`/api/chat/memories/${encodeURIComponent(this.userId)}`));
        const memories = await res.json();
        this.storedMemories = memories.map(m => ({
          id: m.id,
          text: m.text,
          type: m.memoryType,
          createdAt: m.createdAt
        }));
      } catch (e) {
        // Ignore polling errors
      }
    },
    async clearChat() {
      await fetch(getApiUrl(`/api/chat/session/${this.sessionId}?userId=${encodeURIComponent(this.userId)}`), { method: 'DELETE' });
      this.messages = [];
      this.contextPercentage = null;
      this.sessionInfo = null;
      this.sessionStartTime = null;
      this.ttlRemaining = null;
      if (this.ttlTimer) { clearInterval(this.ttlTimer); this.ttlTimer = null; }
      this.consoleEvents.push(this.timestamp() + ' Session cleared: ' + this.userId + ':' + this.sessionId);
      this.$nextTick(() => this.scrollConsole());
    },
    scrollToBottom() {
      const panel = this.$refs.messagesPanel;
      if (panel) panel.scrollTop = panel.scrollHeight;
    },
    scrollConsole() {
      const console = this.$refs.consoleOutput;
      if (console) console.scrollTop = console.scrollHeight;
    },
    formatTtl(seconds) {
      if (seconds < 0) return 'Unknown';
      const mins = Math.floor(seconds / 60);
      const secs = seconds % 60;
      return mins + ':' + String(secs).padStart(2, '0');
    },
    timestamp() {
      const now = new Date();
      return '[' + now.toTimeString().substring(0, 8) + ']';
    }
  }
};
</script>

<style scoped>
.memory-demo { margin: 0; padding: 0; background: #1a1a1a; overflow: hidden; }
.main-container { display: flex; height: 100vh; width: 100vw; }

.workshop-panel { width: 340px; min-width: 340px; background: #1e1e1e; border-right: 1px solid var(--color-border); display: flex; flex-direction: column; overflow: hidden; }
.workshop-header { background: #252526; padding: var(--spacing-4); border-bottom: 1px solid var(--color-border); }
.workshop-header h2 { margin: 0; color: #DC382C; font-size: var(--font-size-lg); display: flex; align-items: center; gap: var(--spacing-2); }
.logo-small { display: inline-block; }
.workshop-content { flex: 1; overflow-y: auto; padding: var(--spacing-5); color: #cccccc; font-size: var(--font-size-sm); line-height: 1.5; }
.workshop-content h3 { color: #fff; font-size: var(--font-size-base); margin-top: 0; margin-bottom: var(--spacing-2); }
.workshop-content h4 { color: #4fc3f7; font-size: var(--font-size-sm); margin-top: var(--spacing-4); margin-bottom: var(--spacing-2); }
.workshop-content code { background: #2d2d2d; padding: 0.15rem 0.3rem; border-radius: var(--radius-sm); color: #ce9178; font-size: 0.8rem; }
.workshop-content ul { padding-left: var(--spacing-5); margin-bottom: var(--spacing-3); }
.workshop-content li { margin-bottom: var(--spacing-1); }
.link { color: #569cd6; text-decoration: none; }
.link:hover { text-decoration: underline; }

.api-key-setup { background: #252526; border-radius: var(--radius-md); padding: var(--spacing-4); }
.api-key-setup input { width: 100%; padding: var(--spacing-2); margin: var(--spacing-3) 0; background: var(--color-dark-800); border: 1px solid var(--color-border); border-radius: var(--radius-md); color: var(--color-text); }
.error { color: #ef4444; font-size: var(--font-size-sm); margin-top: var(--spacing-2); }

.test-step { background: #252526; border-radius: var(--radius-md); padding: var(--spacing-4); margin-top: var(--spacing-4); }
.test-step h4 { margin-top: 0; color: #4fc3f7; }
.test-step .observation { background: rgba(79, 195, 247, 0.1); border-left: 3px solid #4fc3f7; padding: var(--spacing-3); margin: var(--spacing-3) 0; font-size: var(--font-size-xs); }
.test-step .btn { margin-top: var(--spacing-3); }

.test-complete h4 { margin-top: var(--spacing-4); }
.test-complete h4:first-child { margin-top: 0; }
.alert-success { background: rgba(34, 197, 94, 0.1); border: 1px solid #22c55e; border-radius: var(--radius-md); padding: var(--spacing-3); color: #22c55e; margin-bottom: var(--spacing-3); }
.button-row { display: flex; gap: var(--spacing-3); margin-top: var(--spacing-4); }

.nav-links { margin-top: var(--spacing-4); padding-top: var(--spacing-3); border-top: 1px solid var(--color-border); display: flex; flex-direction: column; gap: var(--spacing-2); }
.nav-link { color: var(--color-text-secondary); text-decoration: none; font-size: var(--font-size-sm); }
.nav-link:hover { color: #DC382C; }

.chat-section { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.chat-header { display: flex; justify-content: space-between; align-items: center; padding: var(--spacing-3); background: #252526; border-bottom: 1px solid var(--color-border); flex-wrap: wrap; gap: var(--spacing-2); }
.header-left h3 { color: var(--color-text); margin: 0 0 var(--spacing-2) 0; font-size: var(--font-size-base); }
.session-controls { display: flex; gap: var(--spacing-3); align-items: center; }
.session-field { display: flex; align-items: center; gap: var(--spacing-1); }
.session-field label { color: var(--color-text-secondary); font-size: var(--font-size-xs); white-space: nowrap; }
.session-input { padding: 4px 8px; border-radius: var(--radius-sm); background: var(--color-dark-800); border: 1px solid var(--color-border); color: var(--color-text); font-size: var(--font-size-xs); width: 120px; }
.session-input:focus { outline: none; border-color: #DC382C; }
.header-right { display: flex; gap: var(--spacing-2); align-items: center; }
.context-usage { display: flex; align-items: center; gap: 6px; padding: 4px 8px; background: var(--color-dark-800); border-radius: var(--radius-sm); }
.context-label { color: var(--color-text-secondary); font-size: var(--font-size-xs); }
.context-bar { width: 60px; height: 8px; background: #3c3c3c; border-radius: 4px; overflow: hidden; }
.context-fill { height: 100%; background: #4caf50; transition: width 0.3s, background 0.3s; }
.context-fill.warning { background: #ff9800; }
.context-fill.danger { background: #f44336; }
.context-value { color: var(--color-text); font-size: var(--font-size-xs); min-width: 40px; }
.toggle-label { display: flex; align-items: center; gap: var(--spacing-1); color: var(--color-text-secondary); font-size: var(--font-size-xs); cursor: pointer; }

.chat-body { flex: 1; display: flex; overflow: hidden; }
.messages-panel { flex: 1; overflow-y: auto; padding: var(--spacing-4); display: flex; flex-direction: column; gap: var(--spacing-3); }
.empty-state { text-align: center; color: var(--color-text-secondary); margin: auto; }

.message { max-width: 80%; padding: var(--spacing-3); border-radius: var(--radius-lg); }
.message.user { background: #DC382C; color: white; align-self: flex-end; border-bottom-right-radius: 4px; }
.message.assistant { background: var(--color-dark-800); color: var(--color-text); align-self: flex-start; border-bottom-left-radius: 4px; }
.typing { opacity: 0.7; }

.memory-panel { width: 280px; border-left: 1px solid var(--color-border); padding: var(--spacing-3); overflow-y: auto; background: #252526; }
.memory-panel h4 { color: var(--color-text); margin: 0 0 var(--spacing-3); font-size: var(--font-size-sm); }
.memory-input { display: flex; gap: var(--spacing-2); margin-bottom: var(--spacing-3); }
.memory-input input { flex: 1; padding: var(--spacing-2); border-radius: var(--radius-sm); background: var(--color-dark-800); border: 1px solid var(--color-border); color: var(--color-text); font-size: var(--font-size-xs); }
.memory-section { margin-bottom: var(--spacing-3); }
.memory-section-header { color: #4fc3f7; font-size: 10px; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: var(--spacing-2); padding-bottom: var(--spacing-1); border-bottom: 1px solid var(--color-border); }
.memory-item { background: var(--color-dark-800); padding: var(--spacing-2); border-radius: var(--radius-sm); margin-bottom: var(--spacing-2); font-size: var(--font-size-xs); color: var(--color-text-secondary); }
.memory-item.retrieved { border-left: 2px solid #4caf50; background: rgba(76, 175, 80, 0.1); }
.memory-item.stored { display: flex; flex-direction: column; gap: 2px; }
.memory-text { color: var(--color-text-secondary); }
.memory-type { font-size: 9px; color: #888; text-transform: uppercase; }
.no-memories { color: var(--color-text-secondary); font-size: var(--font-size-xs); font-style: italic; }

.chat-input { display: flex; gap: var(--spacing-2); padding: var(--spacing-3); border-top: 1px solid var(--color-border); background: #252526; }
.chat-input input { flex: 1; padding: var(--spacing-3); border-radius: var(--radius-md); background: var(--color-dark-800); border: 1px solid var(--color-border); color: var(--color-text); }
.chat-input input:focus { outline: none; border-color: #DC382C; }

.btn { padding: var(--spacing-2) var(--spacing-4); border: none; border-radius: var(--radius-md); font-size: var(--font-size-sm); font-weight: var(--font-weight-semibold); cursor: pointer; }
.btn-primary { background: #DC382C; color: white; }
.btn-primary:hover:not(:disabled) { background: #c42f24; }
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-sm { padding: var(--spacing-1) var(--spacing-2); font-size: var(--font-size-xs); background: var(--color-dark-800); color: var(--color-text); border: 1px solid var(--color-border); }
.btn-sm:hover { background: var(--color-dark-700); }
.btn-console { background: rgba(79, 195, 247, 0.15); border-color: #4fc3f7; color: #4fc3f7; }
.btn-console:hover { background: rgba(79, 195, 247, 0.25); }

/* Session Info Bar */
.session-info-bar { display: flex; align-items: center; gap: var(--spacing-2); padding: var(--spacing-2) var(--spacing-3); background: #1e1e1e; border-bottom: 1px solid var(--color-border); font-size: var(--font-size-xs); }
.session-label { color: var(--color-text-secondary); }
.session-value { color: var(--color-text); font-family: monospace; }
.session-divider { color: var(--color-border); }
.ttl-warning { color: #ff9800; }

/* Memory Console Panel */
.console-panel { background: #0d1117; border-top: 1px solid var(--color-border); max-height: 180px; display: flex; flex-direction: column; }
.console-header { display: flex; justify-content: space-between; align-items: center; padding: var(--spacing-2) var(--spacing-3); background: #161b22; border-bottom: 1px solid var(--color-border); }
.console-title { color: #4fc3f7; font-family: monospace; font-size: var(--font-size-xs); }
.console-output { flex: 1; overflow-y: auto; padding: var(--spacing-2) var(--spacing-3); font-family: monospace; font-size: 11px; line-height: 1.5; }
.console-empty { color: var(--color-text-secondary); font-style: italic; }
.console-event { color: #8b949e; white-space: pre-wrap; word-break: break-word; }

/* Test Progress & Observations */
.test-progress { display: flex; align-items: center; gap: var(--spacing-2); padding: var(--spacing-2) var(--spacing-3); background: rgba(220, 56, 44, 0.1); border-radius: var(--radius-sm); margin-bottom: var(--spacing-3); }
.progress-label { color: var(--color-text-secondary); font-size: var(--font-size-xs); }
.progress-value { color: #DC382C; font-weight: var(--font-weight-semibold); font-size: var(--font-size-sm); }

.test-step h4 { color: var(--color-text); margin: 0 0 var(--spacing-2) 0; font-size: var(--font-size-base); }
.test-instructions { color: var(--color-text-secondary); font-size: var(--font-size-sm); margin-bottom: var(--spacing-2); }
.test-actions { margin: var(--spacing-2) 0; padding-left: var(--spacing-4); }
.test-actions li { color: var(--color-text-secondary); font-size: var(--font-size-sm); margin-bottom: var(--spacing-1); }
.test-actions code { background: var(--color-dark-800); padding: 2px 6px; border-radius: var(--radius-sm); color: #e0e0e0; font-size: var(--font-size-xs); }

.observations { background: var(--color-dark-800); padding: var(--spacing-3); border-radius: var(--radius-sm); margin: var(--spacing-3) 0; border-left: 3px solid #4fc3f7; }
.observations h5 { color: #4fc3f7; margin: 0 0 var(--spacing-2) 0; font-size: var(--font-size-xs); text-transform: uppercase; letter-spacing: 0.5px; }
.obs-item { display: flex; align-items: flex-start; gap: var(--spacing-2); color: var(--color-text-secondary); font-size: var(--font-size-sm); cursor: pointer; margin-bottom: var(--spacing-1); }
.obs-item input[type="checkbox"] { margin-top: 3px; accent-color: #4caf50; }
.obs-item:has(input:checked) { color: #4caf50; }

.test-explanation { color: #888; font-size: var(--font-size-xs); font-style: italic; margin: var(--spacing-2) 0; padding: var(--spacing-2); background: rgba(255, 255, 255, 0.03); border-radius: var(--radius-sm); }

/* Test 8: Redis Insight Guide */
.insight-test .insight-guide { margin: var(--spacing-3) 0; }
.insight-section { background: var(--color-dark-800); border-radius: var(--radius-md); padding: var(--spacing-3); margin-bottom: var(--spacing-2); border-left: 3px solid var(--color-brand); }
.insight-section h5 { color: var(--color-text); margin: 0 0 var(--spacing-2) 0; font-size: var(--font-size-sm); }
.key-pattern { display: flex; align-items: center; gap: var(--spacing-2); margin-bottom: var(--spacing-2); flex-wrap: wrap; }
.key-pattern .pattern-label { color: var(--color-text-secondary); font-size: var(--font-size-xs); font-weight: 600; }
.insight-section code { background: var(--color-dark-900); padding: 2px 6px; border-radius: var(--radius-sm); color: #f59e0b; font-family: monospace; font-size: var(--font-size-xs); }
.field-list { margin: 0 0 var(--spacing-2) 0; padding-left: var(--spacing-4); }
.field-list li { color: var(--color-text-secondary); font-size: var(--font-size-xs); margin-bottom: 2px; }
.field-list strong { color: #60a5fa; }
.insight-action { background: rgba(245, 158, 11, 0.1); padding: var(--spacing-1) var(--spacing-2); border-radius: var(--radius-sm); font-size: var(--font-size-xs); color: var(--color-text-secondary); }
.action-icon { color: #f59e0b; font-weight: 600; margin-right: 4px; }
.cli-box { display: flex; flex-direction: column; gap: 4px; }
.cli-box code { display: block; padding: 4px 8px; }

.test-complete .alert-success { background: rgba(76, 175, 80, 0.15); border: 1px solid #4caf50; color: #4caf50; padding: var(--spacing-3); border-radius: var(--radius-sm); margin-bottom: var(--spacing-3); }
.takeaways { padding-left: var(--spacing-4); }
.takeaways li { color: var(--color-text-secondary); font-size: var(--font-size-sm); margin-bottom: var(--spacing-1); }
.takeaways strong { color: var(--color-text); }
.learn-more-text { color: var(--color-text-secondary); font-size: var(--font-size-sm); margin-bottom: var(--spacing-3); }

.btn-secondary { background: var(--color-dark-800); color: var(--color-text); border: 1px solid var(--color-border); }
.btn-secondary:hover { background: var(--color-dark-700); }
</style>
