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
            <h3>Test Your Implementation</h3>
            <p>Follow these tests to verify your memory implementation works correctly.</p>

            <!-- Test 1: Working Memory -->
            <div v-if="testStep === 1" class="test-step">
              <h4>Test 1: Working Memory</h4>
              <p>Send a message: <code>Hi, my name is Alice</code></p>
              <p>Then send: <code>What is my name?</code></p>
              <p class="observation"><strong>Observe:</strong> The AI remembers your name within the same session. This is working memory - conversation history stored in AMS.</p>
              <button @click="completeStep(1)" class="btn btn-primary">Completed - Next Test</button>
            </div>

            <!-- Test 2: Store Long-Term Memory -->
            <div v-else-if="testStep === 2" class="test-step">
              <h4>Test 2: Store Long-Term Memory</h4>
              <p>Click "Show Memories" in the header. Then add a memory:</p>
              <p><code>Alice loves hiking in the mountains</code></p>
              <p class="observation"><strong>Observe:</strong> This stores a fact in long-term memory with vector embeddings for semantic search.</p>
              <button @click="completeStep(2)" class="btn btn-primary">Completed - Next Test</button>
            </div>

            <!-- Test 3: Clear Session -->
            <div v-else-if="testStep === 3" class="test-step">
              <h4>Test 3: Clear Working Memory</h4>
              <p>Click <strong>Clear Chat</strong> to delete the session.</p>
              <p class="observation"><strong>Observe:</strong> This calls DELETE on working memory. The conversation is gone, but long-term memories persist!</p>
              <button @click="completeStep(3)" class="btn btn-primary">Completed - Next Test</button>
            </div>

            <!-- Test 4: Long-Term Memory Retrieval -->
            <div v-else-if="testStep === 4" class="test-step">
              <h4>Test 4: Long-Term Memory Retrieval</h4>
              <p>Make sure "Use Long-Term Memory" is checked. Then ask:</p>
              <p><code>What activities do I enjoy?</code></p>
              <p class="observation"><strong>Observe:</strong> Even in a new session, the AI knows about hiking! It retrieved the memory using semantic search.</p>
              <button @click="completeStep(4)" class="btn btn-primary">Completed - Next Test</button>
            </div>

            <!-- Test 5: Redis Insight -->
            <div v-else-if="testStep === 5" class="test-step">
              <h4>Test 5: Explore in Redis Insight</h4>
              <p>Open <a href="http://localhost:5540" target="_blank" class="link">Redis Insight</a> and explore:</p>
              <ul>
                <li><code>working_memory:*</code> - Session data</li>
                <li><code>long_term_memory:*</code> - Stored facts with embeddings</li>
              </ul>
              <p class="observation"><strong>Observe:</strong> Long-term memories have vector embeddings enabling semantic search!</p>
              <button @click="completeStep(5)" class="btn btn-primary">Completed - Finish</button>
            </div>

            <!-- All Tests Complete -->
            <div v-else class="test-complete">
              <h4>All Tests Complete!</h4>
              <div class="alert alert-success">You've successfully implemented AI memory with Redis!</div>
              <h4>Key Takeaways</h4>
              <ul>
                <li><strong>Working Memory</strong> - Session-scoped conversation context</li>
                <li><strong>Long-Term Memory</strong> - Persistent facts with semantic search</li>
                <li><strong>AMS + Redis</strong> - Fast, scalable memory for AI agents</li>
              </ul>
              <div class="button-row">
                <button @click="restartTests" class="btn btn-primary">Restart Tests</button>
              </div>
            </div>

            <div class="nav-links">
              <router-link to="/" class="nav-link">Back to Workshop Home</router-link>
              <router-link to="/editor" class="nav-link">Open Code Editor</router-link>
            </div>
          </template>
        </div>
      </div>

      <!-- Right Section: Chat -->
      <div class="chat-section">
        <div class="chat-header">
          <div class="header-left">
            <h3>AI Chat with Memory</h3>
            <span class="session-info">Session: {{ sessionId.slice(0, 8) }}... | User: {{ userId }}</span>
          </div>
          <div class="header-right">
            <label class="toggle-label">
              <input type="checkbox" v-model="useMemory" />
              <span>Use Long-Term Memory</span>
            </label>
            <button @click="clearChat" class="btn btn-sm">Clear Chat</button>
            <button @click="showMemoryPanel = !showMemoryPanel" class="btn btn-sm">
              {{ showMemoryPanel ? 'Hide' : 'Show' }} Memories
            </button>
          </div>
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
            <div class="memories-list">
              <div v-for="(mem, idx) in relevantMemories" :key="idx" class="memory-item">{{ mem }}</div>
              <p v-if="relevantMemories.length === 0" class="no-memories">No memories yet</p>
            </div>
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
      relevantMemories: [],
      newMemory: '',
      testStep: 1
    };
  },
  mounted() {
    this.loadTestStep();
  },
  methods: {
    loadTestStep() {
      const saved = localStorage.getItem(STORAGE_KEY);
      if (saved) {
        try { this.testStep = JSON.parse(saved).testStep || 1; } catch (e) { this.testStep = 1; }
      }
    },
    saveTestStep() {
      const saved = localStorage.getItem(STORAGE_KEY);
      let data = {};
      if (saved) { try { data = JSON.parse(saved); } catch (e) { data = {}; } }
      data.testStep = this.testStep;
      localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
    },
    completeStep(step) { this.testStep = step + 1; this.saveTestStep(); },
    restartTests() { this.testStep = 1; this.saveTestStep(); },
    async validateKey() {
      this.validating = true;
      this.keyError = '';
      try {
        const res = await fetch('/api/chat/validate-key', {
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
        const res = await fetch('/api/chat', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ sessionId: this.sessionId, userId: this.userId, message: userMsg, apiKey: this.apiKey, useMemory: this.useMemory })
        });
        const data = await res.json();
        this.messages.push({ role: 'assistant', content: data.response });
        if (data.relevantMemories) this.relevantMemories = data.relevantMemories;
      } catch (e) { this.messages.push({ role: 'assistant', content: 'Error: ' + e.message }); }
      this.loading = false;
      this.$nextTick(() => this.scrollToBottom());
    },
    async addMemory() {
      if (!this.newMemory.trim()) return;
      await fetch('/api/chat/memory', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ userId: this.userId, text: this.newMemory }) });
      this.relevantMemories.push(this.newMemory);
      this.newMemory = '';
    },
    async clearChat() {
      await fetch(`/api/chat/session/${this.sessionId}?userId=${encodeURIComponent(this.userId)}`, { method: 'DELETE' });
      this.messages = [];
    },
    scrollToBottom() {
      const panel = this.$refs.messagesPanel;
      if (panel) panel.scrollTop = panel.scrollHeight;
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
.header-left h3 { color: var(--color-text); margin: 0; font-size: var(--font-size-base); }
.session-info { color: var(--color-text-secondary); font-size: var(--font-size-xs); }
.header-right { display: flex; gap: var(--spacing-2); align-items: center; }
.toggle-label { display: flex; align-items: center; gap: var(--spacing-1); color: var(--color-text-secondary); font-size: var(--font-size-xs); cursor: pointer; }

.chat-body { flex: 1; display: flex; overflow: hidden; }
.messages-panel { flex: 1; overflow-y: auto; padding: var(--spacing-4); display: flex; flex-direction: column; gap: var(--spacing-3); }
.empty-state { text-align: center; color: var(--color-text-secondary); margin: auto; }

.message { max-width: 80%; padding: var(--spacing-3); border-radius: var(--radius-lg); }
.message.user { background: #DC382C; color: white; align-self: flex-end; border-bottom-right-radius: 4px; }
.message.assistant { background: var(--color-dark-800); color: var(--color-text); align-self: flex-start; border-bottom-left-radius: 4px; }
.typing { opacity: 0.7; }

.memory-panel { width: 240px; border-left: 1px solid var(--color-border); padding: var(--spacing-3); overflow-y: auto; background: #252526; }
.memory-panel h4 { color: var(--color-text); margin: 0 0 var(--spacing-3); font-size: var(--font-size-sm); }
.memory-input { display: flex; gap: var(--spacing-2); margin-bottom: var(--spacing-3); }
.memory-input input { flex: 1; padding: var(--spacing-2); border-radius: var(--radius-sm); background: var(--color-dark-800); border: 1px solid var(--color-border); color: var(--color-text); font-size: var(--font-size-xs); }
.memory-item { background: var(--color-dark-800); padding: var(--spacing-2); border-radius: var(--radius-sm); margin-bottom: var(--spacing-2); font-size: var(--font-size-xs); color: var(--color-text-secondary); }
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
</style>

