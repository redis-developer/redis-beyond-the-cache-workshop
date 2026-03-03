<template>
  <div class="memory-challenges">
    <WorkshopHeader
      :hub-url="workshopHubUrl"
      :steps="['Challenges', 'Why Redis', 'Agent Memory Server']"
      :current-step="currentStage"
      clickable
      @step-click="goToStage"
    />

    <div class="main-container">
      <!-- STAGE 1: The Real Challenges -->
      <div v-if="currentStage === 1" class="stage-content">
        <div class="section-header">
          <img src="@/assets/logo/small.png" alt="Redis Logo" width="32" height="32" />
          <h2>Understanding the Real Challenges</h2>
        </div>

        <p class="intro">
          Now that we understand <em>what</em> memory we need, let's understand
          <strong>why building it is hard</strong>.
        </p>

        <div class="concept-box highlight">
          <h4>The Hard Part Is NOT Storage</h4>
          <p>
            Storing text in a database is trivial. The real challenges are
            <strong>extraction</strong> and <strong>retrieval</strong>.
          </p>
        </div>

        <div class="challenge-grid">
          <div class="challenge-card">
            <h4>Memory Extraction</h4>
            <p class="challenge-question">What should we remember?</p>
            <ul>
              <li>Which parts of a conversation contain memorable facts?</li>
              <li>How do we extract structured data from natural language?</li>
              <li>How do we avoid storing irrelevant chatter?</li>
              <li>How do we resolve pronouns? ("He said he likes it" - who? what?)</li>
            </ul>
            <div class="example-box">
              <strong>Input:</strong> "Oh yeah, I forgot to mention - my wife Sarah and I moved to Austin last month"<br/>
              <strong>Extract:</strong> User is married to Sarah. User lives in Austin. User relocated recently.
            </div>
          </div>

          <div class="challenge-card">
            <h4>Memory Retrieval</h4>
            <p class="challenge-question">What memories are relevant right now?</p>
            <ul>
              <li>User asks about restaurants - do we need their food preferences?</li>
              <li>How do we search by meaning, not just keywords?</li>
              <li>How do we rank memories by relevance AND recency?</li>
              <li>How do we handle millions of memories efficiently?</li>
            </ul>
            <div class="example-box">
              <strong>Query:</strong> "Find a good place for dinner"<br/>
              <strong>Relevant:</strong> "User is vegetarian", "User lives in Austin", "User dislikes crowded places"
            </div>
          </div>
        </div>

        <div class="concept-box">
          <h4>Storage Challenges (Still Important)</h4>
          <ul>
            <li><strong>Speed:</strong> Memory lookup must be fast (milliseconds, not seconds)</li>
            <li><strong>Vector search:</strong> Semantic search requires vector embeddings + similarity search</li>
            <li><strong>Scalability:</strong> Handle millions of users, billions of memories</li>
            <li><strong>Flexibility:</strong> Filter by user, topic, time, memory type</li>
          </ul>
        </div>

        <WorkshopStageNav :show-prev="false" @next="nextStage" next-text="Why Redis Is Ideal" />
      </div>

      <!-- STAGE 2: Why Redis -->
      <div v-if="currentStage === 2" class="stage-content">
        <h2>Why Redis Is Ideal for Agent Memory</h2>
        <p class="intro">
          Redis is not just a cache. It's a <strong>real-time data platform</strong> with native support
          for the exact features agent memory requires.
        </p>

        <div class="redis-features">
          <div class="feature-row">
            <div class="feature-icon-box">
              <span class="feature-number">1</span>
            </div>
            <div class="feature-content">
              <h4>Vector Search (Redis Query Engine)</h4>
              <p>Native vector similarity search for semantic retrieval. Store embeddings alongside metadata and search by meaning.</p>
              <code>FT.SEARCH memories "(@user_id:{alice})=>[KNN 5 @embedding $query_vec]"</code>
            </div>
          </div>

          <div class="feature-row">
            <div class="feature-icon-box">
              <span class="feature-number">2</span>
            </div>
            <div class="feature-content">
              <h4>Sub-Millisecond Latency</h4>
              <p>In-memory architecture means memory retrieval doesn't slow down your agent. Critical for real-time conversations.</p>
            </div>
          </div>

          <div class="feature-row">
            <div class="feature-icon-box">
              <span class="feature-number">3</span>
            </div>
            <div class="feature-content">
              <h4>Rich Data Structures</h4>
              <p>JSON documents, sorted sets for recency ranking, hashes for metadata - all the primitives you need.</p>
            </div>
          </div>

          <div class="feature-row">
            <div class="feature-icon-box">
              <span class="feature-number">4</span>
            </div>
            <div class="feature-content">
              <h4>TTL and Expiration</h4>
              <p>Working memory sessions can auto-expire. No manual cleanup needed.</p>
            </div>
          </div>

          <div class="feature-row">
            <div class="feature-icon-box">
              <span class="feature-number">5</span>
            </div>
            <div class="feature-content">
              <h4>Horizontal Scalability</h4>
              <p>Redis Cluster scales to handle billions of memories across multiple nodes.</p>
            </div>
          </div>
        </div>

        <WorkshopStageNav @prev="prevStage" @next="nextStage" next-text="Enter: Agent Memory Server" />
      </div>

      <!-- STAGE 3: Agent Memory Server -->
      <div v-if="currentStage === 3" class="stage-content">
        <h2>Agent Memory Server</h2>
        <p class="intro">
          Agent Memory Server (AMS) is an <strong>open-source memory layer</strong> built on Redis
          that handles all the hard parts for you.
        </p>

        <div class="ams-hero">
          <div class="ams-diagram">
            <div class="diagram-row">
              <div class="diagram-box agent">Your AI Agent</div>
            </div>
            <div class="diagram-arrow">REST API / MCP</div>
            <div class="diagram-row">
              <div class="diagram-box ams">Agent Memory Server</div>
            </div>
            <div class="diagram-arrow">Vector Search + JSON</div>
            <div class="diagram-row">
              <div class="diagram-box redis">Redis</div>
            </div>
          </div>
        </div>

        <div class="concept-box highlight">
          <h4>What AMS Handles For You</h4>
          <ul>
            <li><strong>Memory extraction:</strong> LLM-powered extraction of facts from conversations</li>
            <li><strong>Semantic search:</strong> Vector embeddings + similarity search out of the box</li>
            <li><strong>Deduplication:</strong> Avoids storing redundant memories</li>
            <li><strong>Context grounding:</strong> Resolves pronouns and references automatically</li>
            <li><strong>Working memory management:</strong> Sessions, summarization, TTL</li>
            <li><strong>Multi-tenancy:</strong> User isolation, namespaces, authentication</li>
          </ul>
        </div>

        <div class="integration-patterns">
          <h4>Three Integration Patterns</h4>
          <p>AMS supports three ways to integrate memory into your agent:</p>
          <div class="pattern-grid">
            <div class="pattern-card">
              <h5>LLM-Driven</h5>
              <p>LLM decides when to store/retrieve using tool calls</p>
              <span class="pattern-tag">Best for: Chatbots</span>
            </div>
            <div class="pattern-card selected">
              <h5>Code-Driven</h5>
              <p>Your code explicitly controls memory operations</p>
              <span class="pattern-tag">Best for: Predictable behavior</span>
              <span class="workshop-badge">This Workshop</span>
            </div>
            <div class="pattern-card">
              <h5>Background</h5>
              <p>Automatic extraction from conversations</p>
              <span class="pattern-tag">Best for: Continuous learning</span>
            </div>
          </div>
        </div>

        <div class="ready-box">
          <h4>Ready to Build?</h4>
          <p>In the hands-on lab, you'll implement code-driven memory integration using Java and the AMS REST API.</p>
          <router-link to="/lab" class="btn btn-primary btn-large">
            Start Hands-On Lab
          </router-link>
        </div>

        <WorkshopStageNav @prev="prevStage" :show-next="false" />
      </div>
    </div>
  </div>
</template>

<script>
import { WorkshopHeader, WorkshopStageNav } from '../utils/components';
import { loadProgress, saveProgress } from '../utils/progress';

export default {
  name: 'MemoryChallenges',
  components: { WorkshopHeader, WorkshopStageNav },
  data() {
    return { currentStage: 1 };
  },
  computed: {
    workshopHubUrl() {
      return window.location.protocol + '//' + window.location.hostname + ':9000';
    }
  },
  mounted() {
    const progress = loadProgress();
    this.currentStage = progress.challengesStage || 1;
  },
  methods: {
    nextStage() { if (this.currentStage < 3) { this.currentStage++; this.saveState(); } },
    prevStage() { if (this.currentStage > 1) { this.currentStage--; this.saveState(); } },
    goToStage(step) { if (step >= 1 && step <= 3) { this.currentStage = step; this.saveState(); } },
    saveState() { const progress = loadProgress(); progress.challengesStage = this.currentStage; saveProgress(progress); }
  }
};
</script>

<style scoped>
.memory-challenges {
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
.intro { color: var(--color-text-secondary); margin-bottom: var(--spacing-5); line-height: 1.6; }

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

.challenge-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(350px, 1fr)); gap: var(--spacing-4); margin-bottom: var(--spacing-4); }
.challenge-card {
  background: var(--color-dark-800);
  border-radius: var(--radius-lg);
  padding: var(--spacing-4);
}
.challenge-card h4 { color: var(--color-text); margin-bottom: var(--spacing-1); }
.challenge-question { color: var(--color-primary); font-style: italic; margin-bottom: var(--spacing-3); }
.challenge-card ul { margin: 0 0 var(--spacing-3) var(--spacing-4); color: var(--color-text-secondary); font-size: var(--font-size-sm); line-height: 1.7; }
.example-box {
  background: var(--color-dark-900);
  padding: var(--spacing-3);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.redis-features { margin-bottom: var(--spacing-4); }
.feature-row { display: flex; gap: var(--spacing-4); margin-bottom: var(--spacing-4); align-items: flex-start; }
.feature-icon-box {
  width: 40px; height: 40px;
  background: var(--color-primary);
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.feature-number { color: white; font-weight: bold; font-size: var(--font-size-base); }
.feature-content { flex: 1; }
.feature-content h4 { color: var(--color-text); margin: 0 0 var(--spacing-1); }
.feature-content p { color: var(--color-text-secondary); margin: 0 0 var(--spacing-2); font-size: var(--font-size-sm); }
.feature-content code { display: block; background: var(--color-dark-900); padding: var(--spacing-2); border-radius: var(--radius-sm); font-size: var(--font-size-xs); color: #a5d6ff; overflow-x: auto; }

.ams-hero { margin-bottom: var(--spacing-5); }
.ams-diagram { display: flex; flex-direction: column; align-items: center; gap: var(--spacing-2); }
.diagram-row { display: flex; justify-content: center; }
.diagram-box {
  padding: var(--spacing-3) var(--spacing-5);
  border-radius: var(--radius-md);
  font-weight: bold;
  text-align: center;
}
.diagram-box.agent { background: #3b82f6; color: white; }
.diagram-box.ams { background: var(--color-primary); color: white; }
.diagram-box.redis { background: #10b981; color: white; }
.diagram-arrow { color: var(--color-text-secondary); font-size: var(--font-size-sm); padding: var(--spacing-1) 0; }

.integration-patterns { margin-bottom: var(--spacing-5); }
.integration-patterns h4 { color: var(--color-text); margin-bottom: var(--spacing-2); }
.integration-patterns > p { color: var(--color-text-secondary); margin-bottom: var(--spacing-3); }
.pattern-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: var(--spacing-3); }
.pattern-card {
  background: var(--color-dark-800);
  padding: var(--spacing-3);
  border-radius: var(--radius-md);
  text-align: center;
  border: 2px solid transparent;
  position: relative;
}
.pattern-card.selected { border-color: var(--color-primary); background: rgba(220, 56, 44, 0.1); }
.pattern-card h5 { color: var(--color-text); margin: 0 0 var(--spacing-2); }
.pattern-card p { color: var(--color-text-secondary); font-size: var(--font-size-xs); margin: 0 0 var(--spacing-2); }
.pattern-tag { display: inline-block; font-size: 0.65rem; color: var(--color-text-secondary); background: var(--color-dark-900); padding: 2px 6px; border-radius: var(--radius-sm); }
.workshop-badge {
  position: absolute;
  top: -8px; right: -8px;
  background: var(--color-primary);
  color: white;
  font-size: 0.6rem;
  padding: 2px 6px;
  border-radius: var(--radius-sm);
  font-weight: bold;
}

.ready-box { text-align: center; padding: var(--spacing-4); background: var(--color-dark-800); border-radius: var(--radius-lg); margin-bottom: var(--spacing-4); }
.ready-box h4 { color: var(--color-text); margin-bottom: var(--spacing-2); }
.ready-box p { color: var(--color-text-secondary); margin-bottom: var(--spacing-4); }
.btn-large { padding: var(--spacing-3) var(--spacing-6); font-size: var(--font-size-base); }

.alert { padding: var(--spacing-3); border-radius: var(--radius-md); margin-bottom: var(--spacing-4); color: var(--color-text-secondary); }
.alert-info { background: rgba(59, 130, 246, 0.1); border-left: 3px solid #3b82f6; }
.alert-warning { background: rgba(245, 158, 11, 0.1); border-left: 3px solid #f59e0b; }

@media (max-width: 768px) {
  .challenge-grid { grid-template-columns: 1fr; }
  .pattern-grid { grid-template-columns: 1fr; }
  .feature-row { flex-direction: column; align-items: center; text-align: center; }
}
</style>

