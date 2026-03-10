<template>
  <div class="memory-lab">
    <WorkshopHeader
      :hub-url="workshopHubUrl"
      :steps="['Setup', 'Working Memory', 'Long-Term Memory', 'Test']"
      :current-step="currentStage"
      clickable
      @step-click="goToStage"
    />

    <div class="main-container">
      <!-- Server Status -->
      <div class="status-bar" :class="{ connected: serverStatus === 'connected' }">
        <span class="status-dot"></span>
        <span>Agent Memory Server: {{ serverStatus }}</span>
        <button @click="checkHealth" class="btn btn-sm">Refresh</button>
      </div>

      <!-- STAGE 1: Setup -->
      <div v-if="currentStage === 1" class="stage-content">
        <h2>Lab Setup</h2>
        <p class="intro">Let's verify everything is running before we start coding.</p>

        <div class="checklist">
          <div class="check-item" :class="{ done: checks.ams }">
            <span class="check-icon">{{ checks.ams ? '[x]' : '[ ]' }}</span>
            <div class="check-content">
              <strong>Agent Memory Server</strong>
              <p>Running on port 8000</p>
            </div>
            <button @click="checkAMS" class="btn btn-sm">Check</button>
          </div>
          <div class="check-item" :class="{ done: checks.redis }">
            <span class="check-icon">{{ checks.redis ? '[x]' : '[ ]' }}</span>
            <div class="check-content">
              <strong>Redis</strong>
              <p>Required by AMS for storage</p>
            </div>
          </div>
        </div>

        <div class="concept-box">
          <h4>Integration Pattern: Code-Driven</h4>
          <p>In this lab, you'll explicitly control when to store and retrieve memories using Java code.
             This gives you predictable behavior and full control over memory operations.</p>
        </div>

        <div class="alert alert-info" v-if="!checks.ams">
          <strong>Not Connected?</strong> Start the services with:
          <pre><code>cd java-springboot/4_agent_memory
docker-compose up -d</code></pre>
        </div>

        <WorkshopStageNav :show-prev="false" @next="nextStage" :disabled="!checks.ams" next-text="Exercise 1: Working Memory" />
      </div>

      <!-- STAGE 2: Working Memory Exercise -->
      <div v-if="currentStage === 2" class="stage-content">
        <h2>Exercise 1: Working Memory</h2>
        <p class="intro">Store and retrieve a conversation session.</p>

        <div class="exercise-panel">
          <div class="task-description">
            <h4>Your Task</h4>
            <p>Create a working memory session that stores a conversation between a user and assistant.</p>
          </div>

          <div class="interactive-section">
            <div class="form-group">
              <label>Session ID</label>
              <input v-model="exercise1.sessionId" placeholder="my-session-123" />
            </div>
            <div class="form-group">
              <label>User ID</label>
              <input v-model="exercise1.userId" placeholder="alice" />
            </div>
            <div class="form-group">
              <label>User Message</label>
              <input v-model="exercise1.userMessage" placeholder="Hi, I'm Alice and I work at TechCorp" />
            </div>
            <div class="form-group">
              <label>Assistant Response</label>
              <input v-model="exercise1.assistantMessage" placeholder="Nice to meet you, Alice!" />
            </div>
            <div class="button-row">
              <button @click="storeWorkingMemory" class="btn btn-primary" :disabled="loading">
                {{ loading ? 'Storing...' : 'Store Working Memory' }}
              </button>
              <button @click="retrieveWorkingMemory" class="btn btn-secondary" :disabled="loading">
                Retrieve
              </button>
            </div>
          </div>

          <div class="result-box" v-if="exercise1.result">
            <h4>{{ exercise1.result.success ? 'Success!' : 'Error' }}</h4>
            <pre><code>{{ JSON.stringify(exercise1.result.data, null, 2) }}</code></pre>
          </div>
        </div>

        <div class="code-reference">
          <h4>Java Code Reference</h4>
          <pre><code>// Store working memory
WorkingMemory memory = new WorkingMemory();
memory.setSessionId(sessionId);
memory.setUserId(userId);
memory.setMessages(List.of(
    new MemoryMessage("user", userMessage),
    new MemoryMessage("assistant", assistantMessage)
));

agentMemoryService.putWorkingMemory(sessionId, memory).block();</code></pre>
        </div>

        <WorkshopStageNav @prev="prevStage" @next="nextStage" next-text="Exercise 2: Long-Term Memory" />
      </div>

      <!-- STAGE 3: Long-Term Memory Exercise -->
      <div v-if="currentStage === 3" class="stage-content">
        <h2>Exercise 2: Long-Term Memory</h2>
        <p class="intro">Store facts and search by meaning using semantic search.</p>

        <div class="exercise-panel">
          <div class="task-description">
            <h4>Your Task</h4>
            <p>Store a user preference and then search for it using natural language.</p>
          </div>

          <div class="interactive-section">
            <div class="form-group">
              <label>User ID</label>
              <input v-model="exercise2.userId" placeholder="alice" />
            </div>
            <div class="form-group">
              <label>Memory to Store</label>
              <input v-model="exercise2.memoryText" placeholder="Alice prefers video calls over phone calls" />
            </div>
            <button @click="storeLongTermMemory" class="btn btn-primary" :disabled="loading">
              {{ loading ? 'Storing...' : 'Store Memory' }}
            </button>

            <div class="divider"></div>

            <div class="form-group">
              <label>Search Query (try different phrasing!)</label>
              <input v-model="exercise2.searchQuery" placeholder="how does the user like to communicate?" @keyup.enter="searchMemories" />
            </div>
            <button @click="searchMemories" class="btn btn-secondary" :disabled="loading">
              {{ loading ? 'Searching...' : 'Search Memories' }}
            </button>
          </div>

          <div class="result-box" v-if="exercise2.searchResults.length">
            <h4>Search Results ({{ exercise2.searchResults.length }})</h4>
            <div v-for="(mem, idx) in exercise2.searchResults" :key="idx" class="memory-result">
              <div class="memory-text">{{ mem.text }}</div>
              <div class="memory-score">Similarity: {{ (1 - mem.dist).toFixed(2) }}</div>
            </div>
          </div>
        </div>

        <div class="concept-box highlight">
          <h4>Semantic Search Magic</h4>
          <p>Notice how the search finds "prefers video calls" even when you ask "how does user communicate?" -
             this is semantic search using vector embeddings, not keyword matching!</p>
        </div>

        <WorkshopStageNav @prev="prevStage" @next="nextStage" next-text="Final Test" />
      </div>

      <!-- STAGE 4: Final Test -->
      <div v-if="currentStage === 4" class="stage-content">
        <h2>Putting It All Together</h2>
        <p class="intro">Test your understanding by building a memory-enriched context.</p>

        <div class="exercise-panel">
          <div class="task-description">
            <h4>Scenario</h4>
            <p>Imagine your agent receives a new message. Build the context by combining working memory (recent conversation) with relevant long-term memories.</p>
          </div>

          <div class="interactive-section">
            <div class="form-group">
              <label>New User Message</label>
              <input v-model="exercise3.newMessage" placeholder="Can you schedule a meeting for tomorrow?" />
            </div>
            <button @click="buildContext" class="btn btn-primary" :disabled="loading">
              Build Enriched Context
            </button>
          </div>

          <div class="context-result" v-if="exercise3.context">
            <h4>Enriched Context for LLM</h4>
            <div class="context-section">
              <h5>Working Memory (Conversation History)</h5>
              <pre>{{ exercise3.context.workingMemory }}</pre>
            </div>
            <div class="context-section">
              <h5>Relevant Long-Term Memories</h5>
              <pre>{{ exercise3.context.relevantMemories }}</pre>
            </div>
          </div>
        </div>

        <div class="completion-box">
          <h4>Congratulations!</h4>
          <p>You've learned the code-driven pattern for agent memory. Your agent can now:</p>
          <ul>
            <li>Store conversation context in working memory</li>
            <li>Persist important facts to long-term memory</li>
            <li>Search memories by meaning with semantic search</li>
            <li>Build enriched context for better LLM responses</li>
          </ul>
          <router-link to="/learn" class="btn btn-primary">Explore More Resources</router-link>
        </div>

        <WorkshopStageNav @prev="prevStage" :show-next="false" />
      </div>
    </div>
  </div>
</template>

<script>
import { WorkshopHeader, WorkshopStageNav } from '../utils/components';
import { getApiUrl, getWorkshopHubUrl } from '../utils/basePath';
import { loadProgress, saveProgress } from '../utils/progress';

export default {
  name: 'MemoryLab',
  components: { WorkshopHeader, WorkshopStageNav },
  data() {
    return {
      currentStage: 1,
      serverStatus: 'checking...',
      loading: false,
      checks: { ams: false, redis: false },
      exercise1: {
        sessionId: 'lab-session-' + Date.now(),
        userId: 'alice',
        userMessage: "Hi, I'm Alice and I work at TechCorp",
        assistantMessage: "Nice to meet you, Alice! How can I help you today?",
        result: null
      },
      exercise2: {
        userId: 'alice',
        memoryText: 'Alice prefers video calls over phone calls',
        searchQuery: 'how does the user like to communicate?',
        searchResults: []
      },
      exercise3: {
        newMessage: 'Can you schedule a meeting for tomorrow?',
        context: null
      }
    };
  },
  computed: {
    workshopHubUrl() { return getWorkshopHubUrl(); }
  },
  mounted() {
    const progress = loadProgress();
    this.currentStage = progress.labStage || 1;
    this.checkHealth();
  },
  methods: {
    async checkHealth() {
      try {
        const res = await fetch(getApiUrl('/api/memory/health'));
        const data = await res.json();
        this.serverStatus = data.status === 'ok' ? 'connected' : 'error';
        this.checks.ams = data.status === 'ok';
        this.checks.redis = data.status === 'ok';
      } catch { this.serverStatus = 'disconnected'; this.checks.ams = false; }
    },
    async checkAMS() { await this.checkHealth(); },
    nextStage() { if (this.currentStage < 4) { this.currentStage++; this.saveState(); } },
    prevStage() { if (this.currentStage > 1) { this.currentStage--; this.saveState(); } },
    goToStage(step) { if (step >= 1 && step <= 4) { this.currentStage = step; this.saveState(); } },
    saveState() { const progress = loadProgress(); progress.labStage = this.currentStage; saveProgress(progress); },

    async storeWorkingMemory() {
      this.loading = true;
      try {
        const res = await fetch(getApiUrl(`/api/demo/conversation?userId=${encodeURIComponent(this.exercise1.userId)}&sessionId=${encodeURIComponent(this.exercise1.sessionId)}`), { method: 'POST' });
        await res.json();
        // Add messages
        await fetch(getApiUrl(`/api/demo/conversation/${encodeURIComponent(this.exercise1.sessionId)}/message?role=user&content=${encodeURIComponent(this.exercise1.userMessage)}`), { method: 'POST' });
        await fetch(getApiUrl(`/api/demo/conversation/${encodeURIComponent(this.exercise1.sessionId)}/message?role=assistant&content=${encodeURIComponent(this.exercise1.assistantMessage)}`), { method: 'POST' });
        this.exercise1.result = { success: true, data: { sessionId: this.exercise1.sessionId, messages: 2 } };
      } catch (e) { this.exercise1.result = { success: false, data: { error: e.message } }; }
      this.loading = false;
    },
    async retrieveWorkingMemory() {
      this.loading = true;
      try {
        const res = await fetch(getApiUrl(`/api/demo/conversation/${encodeURIComponent(this.exercise1.sessionId)}`));
        const data = await res.json();
        this.exercise1.result = { success: data.success, data };
      } catch (e) { this.exercise1.result = { success: false, data: { error: e.message } }; }
      this.loading = false;
    },
    async storeLongTermMemory() {
      this.loading = true;
      try {
        const res = await fetch(getApiUrl(`/api/demo/preference?userId=${encodeURIComponent(this.exercise2.userId)}&preference=${encodeURIComponent(this.exercise2.memoryText)}`), { method: 'POST' });
        await res.json();
      } catch (e) { console.error(e); }
      this.loading = false;
    },
    async searchMemories() {
      this.loading = true;
      try {
        const res = await fetch(getApiUrl(`/api/demo/preferences?userId=${encodeURIComponent(this.exercise2.userId)}&query=${encodeURIComponent(this.exercise2.searchQuery)}`));
        const data = await res.json();
        this.exercise2.searchResults = data.memories || [];
      } catch (e) { console.error(e); }
      this.loading = false;
    },
    async buildContext() {
      this.loading = true;
      try {
        // Get working memory
        const wmRes = await fetch(getApiUrl(`/api/demo/conversation/${encodeURIComponent(this.exercise1.sessionId)}`));
        const wmData = await wmRes.json();
        // Search for relevant memories
        const ltmRes = await fetch(getApiUrl(`/api/demo/preferences?userId=${encodeURIComponent(this.exercise2.userId)}&query=${encodeURIComponent(this.exercise3.newMessage)}`));
        const ltmData = await ltmRes.json();
        this.exercise3.context = {
          workingMemory: JSON.stringify(wmData.messages || [], null, 2),
          relevantMemories: JSON.stringify((ltmData.memories || []).map(m => m.text), null, 2)
        };
      } catch (e) { console.error(e); }
      this.loading = false;
    }
  }
};
</script>

<style scoped>
.memory-lab {
  min-height: 100vh;
  background: linear-gradient(135deg, var(--color-dark-900) 0%, var(--color-dark-800) 100%);
  padding: var(--spacing-6);
}
.main-container { max-width: 900px; margin: 0 auto; padding-top: var(--spacing-4); }
.stage-content { background: var(--color-surface); border-radius: var(--radius-xl); padding: var(--spacing-6); box-shadow: var(--shadow-xl); border: 1px solid var(--color-border); }
h2 { color: var(--color-text); margin-bottom: var(--spacing-4); }
.intro { color: var(--color-text-secondary); margin-bottom: var(--spacing-5); line-height: 1.6; }

.status-bar { display: flex; align-items: center; gap: var(--spacing-3); background: var(--color-surface); padding: var(--spacing-3); border-radius: var(--radius-lg); margin-bottom: var(--spacing-4); color: var(--color-text-secondary); }
.status-dot { width: 10px; height: 10px; border-radius: 50%; background: #ef4444; }
.status-bar.connected .status-dot { background: #10b981; }

.checklist { margin-bottom: var(--spacing-4); }
.check-item { display: flex; align-items: center; gap: var(--spacing-3); padding: var(--spacing-3); background: var(--color-dark-800); border-radius: var(--radius-md); margin-bottom: var(--spacing-2); }
.check-item.done { background: rgba(16, 185, 129, 0.1); border-left: 3px solid #10b981; }
.check-icon { font-family: monospace; color: var(--color-text-secondary); }
.check-item.done .check-icon { color: #10b981; }
.check-content { flex: 1; }
.check-content strong { color: var(--color-text); display: block; }
.check-content p { color: var(--color-text-secondary); font-size: var(--font-size-sm); margin: 0; }

.concept-box { background: var(--color-dark-800); border-radius: var(--radius-lg); padding: var(--spacing-4); margin-bottom: var(--spacing-4); }
.concept-box.highlight { border-left: 3px solid var(--color-primary); }
.concept-box h4 { color: var(--color-text); margin-bottom: var(--spacing-2); }
.concept-box p { color: var(--color-text-secondary); margin: 0; line-height: 1.6; }

.exercise-panel { background: var(--color-dark-800); border-radius: var(--radius-lg); padding: var(--spacing-4); margin-bottom: var(--spacing-4); }
.task-description { margin-bottom: var(--spacing-4); padding-bottom: var(--spacing-3); border-bottom: 1px solid var(--color-border); }
.task-description h4 { color: var(--color-text); margin-bottom: var(--spacing-1); }
.task-description p { color: var(--color-text-secondary); margin: 0; }

.interactive-section { margin-bottom: var(--spacing-4); }
.form-group { margin-bottom: var(--spacing-3); }
.form-group label { display: block; color: var(--color-text); font-size: var(--font-size-sm); margin-bottom: var(--spacing-1); }
.form-group input { width: 100%; padding: var(--spacing-2) var(--spacing-3); border-radius: var(--radius-md); background: var(--color-dark-900); border: 1px solid var(--color-border); color: var(--color-text); }
.button-row { display: flex; gap: var(--spacing-2); margin-top: var(--spacing-3); }
.divider { height: 1px; background: var(--color-border); margin: var(--spacing-4) 0; }

.result-box, .context-result { background: var(--color-dark-900); border-radius: var(--radius-md); padding: var(--spacing-3); margin-top: var(--spacing-3); }
.result-box h4, .context-result h4 { color: var(--color-text); margin-bottom: var(--spacing-2); font-size: var(--font-size-sm); }
.result-box pre, .context-result pre { margin: 0; color: var(--color-text-secondary); font-size: var(--font-size-xs); overflow-x: auto; }

.memory-result { background: var(--color-dark-800); padding: var(--spacing-2); border-radius: var(--radius-sm); margin-bottom: var(--spacing-2); }
.memory-text { color: var(--color-text); margin-bottom: var(--spacing-1); }
.memory-score { color: var(--color-text-secondary); font-size: var(--font-size-xs); }

.context-section { margin-bottom: var(--spacing-3); }
.context-section h5 { color: var(--color-text); font-size: var(--font-size-sm); margin-bottom: var(--spacing-1); }

.code-reference { background: var(--color-dark-900); border-radius: var(--radius-lg); padding: var(--spacing-4); margin-bottom: var(--spacing-4); }
.code-reference h4 { color: var(--color-text); margin-bottom: var(--spacing-2); font-size: var(--font-size-sm); }
.code-reference pre { margin: 0; overflow-x: auto; }
.code-reference code { font-family: 'Courier New', monospace; font-size: var(--font-size-xs); color: #e0e0e0; }

.completion-box { background: rgba(16, 185, 129, 0.1); border: 1px solid #10b981; border-radius: var(--radius-lg); padding: var(--spacing-4); text-align: center; }
.completion-box h4 { color: #10b981; margin-bottom: var(--spacing-2); }
.completion-box p { color: var(--color-text-secondary); margin-bottom: var(--spacing-3); }
.completion-box ul { text-align: left; margin: 0 0 var(--spacing-4) var(--spacing-6); color: var(--color-text-secondary); }

.alert { padding: var(--spacing-3); border-radius: var(--radius-md); margin-bottom: var(--spacing-4); }
.alert-info { background: rgba(59, 130, 246, 0.1); border-left: 3px solid #3b82f6; }
.alert pre { background: var(--color-dark-900); padding: var(--spacing-2); border-radius: var(--radius-sm); margin-top: var(--spacing-2); }
.alert code { font-size: var(--font-size-xs); color: #e0e0e0; }
</style>
