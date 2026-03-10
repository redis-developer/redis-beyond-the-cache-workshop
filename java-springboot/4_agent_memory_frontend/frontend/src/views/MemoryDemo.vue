<template>
  <div class="memory-demo">
    <WorkshopHeader
      :hub-url="workshopHubUrl"
      :steps="['Home', 'Demo']"
      :current-step="2"
    />

    <div class="main-container">
      <h1>Interactive Demo</h1>
      
      <div class="status-bar" :class="{ connected: serverStatus === 'connected' }">
        <span class="status-dot"></span>
        <span>Agent Memory Server: {{ serverStatus }}</span>
        <button @click="checkHealth" class="btn btn-sm">Refresh</button>
      </div>

      <div class="demo-grid">
        <!-- Working Memory Section -->
        <div class="demo-section">
          <h2>Working Memory</h2>
          <p class="section-desc">Session-based conversation context</p>

          <div class="form-group">
            <label>Session ID</label>
            <input v-model="sessionId" placeholder="my-session" />
          </div>

          <div class="form-group">
            <label>User ID</label>
            <input v-model="userId" placeholder="alice" />
          </div>

          <div class="button-row">
            <button @click="startConversation" class="btn btn-primary" :disabled="loading">
              Start Session
            </button>
            <button @click="getConversation" class="btn btn-secondary" :disabled="loading">
              Load Session
            </button>
          </div>

          <div class="conversation-box" v-if="conversation.messages.length">
            <div v-for="(msg, idx) in conversation.messages" :key="idx" class="message" :class="msg.role">
              <span class="role">{{ msg.role }}</span>
              <span class="content">{{ msg.content }}</span>
            </div>
          </div>

          <div class="form-group" v-if="conversation.messages.length">
            <label>Add Message</label>
            <div class="input-row">
              <select v-model="newMessageRole">
                <option value="user">user</option>
                <option value="assistant">assistant</option>
              </select>
              <input v-model="newMessageContent" placeholder="Type a message..." @keyup.enter="addMessage" />
              <button @click="addMessage" class="btn btn-primary btn-sm">Send</button>
            </div>
          </div>
        </div>

        <!-- Long-Term Memory Section -->
        <div class="demo-section">
          <h2>Long-Term Memory</h2>
          <p class="section-desc">Persistent facts and preferences</p>

          <div class="form-group">
            <label>Store a Preference</label>
            <input v-model="newPreference" placeholder="User prefers dark mode" />
            <button @click="storePreference" class="btn btn-primary" :disabled="loading">
              Store
            </button>
          </div>

          <div class="form-group">
            <label>Search Memories</label>
            <div class="input-row">
              <input v-model="searchQuery" placeholder="user preferences" @keyup.enter="searchMemories" />
              <button @click="searchMemories" class="btn btn-secondary" :disabled="loading">
                Search
              </button>
            </div>
          </div>

          <div class="results-box" v-if="searchResults.length">
            <h4>Search Results ({{ searchResults.length }})</h4>
            <div v-for="(memory, idx) in searchResults" :key="idx" class="memory-item">
              <div class="memory-text">{{ memory.text }}</div>
              <div class="memory-meta">
                <span v-if="memory.dist">Distance: {{ memory.dist.toFixed(3) }}</span>
                <span v-if="memory.topics">Topics: {{ memory.topics.join(', ') }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Response Log -->
      <div class="log-section" v-if="logs.length">
        <h3>API Response Log</h3>
        <div class="log-entries">
          <div v-for="(log, idx) in logs" :key="idx" class="log-entry" :class="log.success ? 'success' : 'error'">
            <span class="log-time">{{ log.time }}</span>
            <span class="log-action">{{ log.action }}</span>
            <pre class="log-data">{{ JSON.stringify(log.data, null, 2) }}</pre>
          </div>
        </div>
      </div>

      <div class="back-link">
        <router-link to="/" class="btn btn-secondary">← Back to Home</router-link>
      </div>
    </div>
  </div>
</template>

<script>
import { WorkshopHeader } from '../utils/components';
import { getApiUrl } from '../utils/basePath';

export default {
  name: 'MemoryDemo',
  components: { WorkshopHeader },
  data() {
    return {
      serverStatus: 'checking...',
      loading: false,
      sessionId: 'demo-session',
      userId: 'demo-user',
      conversation: { messages: [] },
      newMessageRole: 'user',
      newMessageContent: '',
      newPreference: '',
      searchQuery: '',
      searchResults: [],
      logs: []
    };
  },
  computed: {
    workshopHubUrl() {
      return window.location.protocol + '//' + window.location.hostname + ':9000';
    }
  },
  mounted() {
    this.checkHealth();
  },
  methods: {
    async checkHealth() {
      try {
        const res = await fetch(getApiUrl('/api/memory/health'));
        const data = await res.json();
        this.serverStatus = data.status;
      } catch {
        this.serverStatus = 'disconnected';
      }
    },
    log(action, data, success = true) {
      this.logs.unshift({
        time: new Date().toLocaleTimeString(),
        action,
        data,
        success
      });
      if (this.logs.length > 10) this.logs.pop();
    },
    async startConversation() {
      this.loading = true;
      try {
        const res = await fetch(getApiUrl(`/api/demo/conversation?userId=${encodeURIComponent(this.userId)}`), {
          method: 'POST'
        });
        const data = await res.json();
        if (data.success) {
          this.sessionId = data.sessionId;
          this.conversation = { messages: [{ role: 'system', content: 'Conversation started' }] };
        }
        this.log('Start Conversation', data, data.success);
      } catch (e) {
        this.log('Start Conversation', { error: e.message }, false);
      }
      this.loading = false;
    },
    async getConversation() {
      this.loading = true;
      try {
        const res = await fetch(getApiUrl(`/api/demo/conversation/${encodeURIComponent(this.sessionId)}`));
        const data = await res.json();
        if (data.success) {
          this.conversation = { messages: data.messages || [] };
        }
        this.log('Get Conversation', data, data.success);
      } catch (e) {
        this.log('Get Conversation', { error: e.message }, false);
      }
      this.loading = false;
    },
    async addMessage() {
      if (!this.newMessageContent.trim()) return;
      this.loading = true;
      try {
        const res = await fetch(
          getApiUrl(`/api/demo/conversation/${encodeURIComponent(this.sessionId)}/message?role=${this.newMessageRole}&content=${encodeURIComponent(this.newMessageContent)}`),
          { method: 'POST' }
        );
        const data = await res.json();
        if (data.success) {
          this.conversation.messages.push({ role: this.newMessageRole, content: this.newMessageContent });
          this.newMessageContent = '';
        }
        this.log('Add Message', data, data.success);
      } catch (e) {
        this.log('Add Message', { error: e.message }, false);
      }
      this.loading = false;
    },
    async storePreference() {
      if (!this.newPreference.trim()) return;
      this.loading = true;
      try {
        const res = await fetch(
          getApiUrl(`/api/demo/preference?userId=${encodeURIComponent(this.userId)}&preference=${encodeURIComponent(this.newPreference)}`),
          { method: 'POST' }
        );
        const data = await res.json();
        this.log('Store Preference', data, data.success);
        if (data.success) this.newPreference = '';
      } catch (e) {
        this.log('Store Preference', { error: e.message }, false);
      }
      this.loading = false;
    },
    async searchMemories() {
      this.loading = true;
      try {
        const res = await fetch(
          getApiUrl(`/api/demo/preferences?userId=${encodeURIComponent(this.userId)}&query=${encodeURIComponent(this.searchQuery || 'preferences')}`)
        );
        const data = await res.json();
        this.searchResults = data.memories || [];
        this.log('Search Memories', data, data.success);
      } catch (e) {
        this.log('Search Memories', { error: e.message }, false);
      }
      this.loading = false;
    }
  }
};
</script>

<style scoped>
.memory-demo {
  min-height: 100vh;
  background: linear-gradient(135deg, var(--color-dark-900) 0%, var(--color-dark-800) 100%);
  padding: var(--spacing-6);
}
.main-container { max-width: 1100px; margin: 0 auto; }
h1 { color: var(--color-text); margin-bottom: var(--spacing-4); }
.status-bar {
  display: flex; align-items: center; gap: var(--spacing-3);
  background: var(--color-surface); padding: var(--spacing-3); border-radius: var(--radius-lg);
  margin-bottom: var(--spacing-4); color: var(--color-text-secondary);
}
.status-dot { width: 10px; height: 10px; border-radius: 50%; background: #ef4444; }
.status-bar.connected .status-dot { background: #10b981; }
.demo-grid { display: grid; grid-template-columns: 1fr 1fr; gap: var(--spacing-4); margin-bottom: var(--spacing-4); }
.demo-section { background: var(--color-surface); border-radius: var(--radius-xl); padding: var(--spacing-5); }
.demo-section h2 { color: var(--color-text); font-size: var(--font-size-lg); margin-bottom: var(--spacing-2); }
.section-desc { color: var(--color-text-secondary); font-size: var(--font-size-sm); margin-bottom: var(--spacing-4); }
.form-group { margin-bottom: var(--spacing-4); }
.form-group label { display: block; color: var(--color-text); font-size: var(--font-size-sm); margin-bottom: var(--spacing-2); }
.form-group input, .form-group select {
  width: 100%; padding: var(--spacing-2) var(--spacing-3); border-radius: var(--radius-md);
  background: var(--color-dark-800); border: 1px solid var(--color-border); color: var(--color-text);
}
.input-row { display: flex; gap: var(--spacing-2); }
.input-row input { flex: 1; }
.input-row select { width: auto; }
.button-row { display: flex; gap: var(--spacing-2); margin-bottom: var(--spacing-4); }
.conversation-box { background: var(--color-dark-900); border-radius: var(--radius-md); padding: var(--spacing-3); max-height: 200px; overflow-y: auto; margin-bottom: var(--spacing-4); }
.message { padding: var(--spacing-2); margin-bottom: var(--spacing-2); border-radius: var(--radius-sm); }
.message.user { background: rgba(59, 130, 246, 0.2); }
.message.assistant { background: rgba(16, 185, 129, 0.2); }
.message.system { background: rgba(156, 163, 175, 0.2); font-style: italic; }
.message .role { font-weight: bold; color: var(--color-text); margin-right: var(--spacing-2); text-transform: uppercase; font-size: var(--font-size-xs); }
.message .content { color: var(--color-text-secondary); }
.results-box { background: var(--color-dark-900); border-radius: var(--radius-md); padding: var(--spacing-3); }
.results-box h4 { color: var(--color-text); margin-bottom: var(--spacing-3); font-size: var(--font-size-sm); }
.memory-item { background: var(--color-dark-800); padding: var(--spacing-3); border-radius: var(--radius-sm); margin-bottom: var(--spacing-2); }
.memory-text { color: var(--color-text); margin-bottom: var(--spacing-1); }
.memory-meta { color: var(--color-text-secondary); font-size: var(--font-size-xs); display: flex; gap: var(--spacing-3); }
.log-section { background: var(--color-surface); border-radius: var(--radius-xl); padding: var(--spacing-5); margin-bottom: var(--spacing-4); }
.log-section h3 { color: var(--color-text); margin-bottom: var(--spacing-3); }
.log-entries { max-height: 300px; overflow-y: auto; }
.log-entry { background: var(--color-dark-900); padding: var(--spacing-3); border-radius: var(--radius-sm); margin-bottom: var(--spacing-2); border-left: 3px solid #10b981; }
.log-entry.error { border-left-color: #ef4444; }
.log-time { color: var(--color-text-secondary); font-size: var(--font-size-xs); margin-right: var(--spacing-2); }
.log-action { color: var(--color-text); font-weight: bold; }
.log-data { margin: var(--spacing-2) 0 0 0; color: var(--color-text-secondary); font-size: var(--font-size-xs); overflow-x: auto; }
.back-link { text-align: center; }
@media (max-width: 768px) { .demo-grid { grid-template-columns: 1fr; } }
</style>
