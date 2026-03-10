<template>
  <div class="session-home">
    <!-- Workshop Header with Hub Link -->
    <WorkshopHeader :hub-url="workshopHubUrl" />

    <div class="welcome-container">
      <!-- Left Column: Instructions -->
      <div class="instructions-column">
        <!-- STAGE 1: In-Memory Sessions (Problem) -->
        <div v-if="!redisEnabled && !stage1Completed" class="instructions">
          <h2>STAGE 1: Problem - In-Memory Sessions</h2>
          <div class="alert alert-warning">
            <strong>Current State:</strong> Sessions are stored in-memory (not in Redis)
          </div>
          <p class="intro">Let's see the problem with in-memory sessions. Complete each test to proceed:</p>

          <div class="test-item" :class="{ 'completed': stage1Tests.test1 }">
            <div class="test-header">
              <h4>Test 1: Note Your Session ID</h4>
              <span v-if="stage1Tests.test1" class="test-check">Done</span>
            </div>
            <p class="test-description">Your current session ID is shown on the right: <code>{{ sessionInfo.sessionId }}</code>. This ID identifies your session in the server's memory.</p>
            <button v-if="!stage1Tests.test1" @click="completeStage1Test('test1')" class="btn btn-outline btn-sm">
              I've noted my Session ID
            </button>
          </div>

          <div v-if="stage1Tests.test1" class="test-item" :class="{ 'completed': stage1Tests.test2 }">
            <div class="test-header">
              <h4>Test 2: Restart the Application</h4>
              <span v-if="stage1Tests.test2" class="test-check">Done</span>
            </div>
            <p class="test-description">Go to the <a :href="workshopHubUrl" target="_blank">Workshop Hub</a> and restart the app (without rebuilding). This simulates a server restart or deployment.</p>
            <button v-if="!stage1Tests.test2" @click="completeStage1Test('test2')" class="btn btn-outline btn-sm">
              I've restarted the app
            </button>
          </div>

          <div v-if="stage1Tests.test2" class="test-item" :class="{ 'completed': stage1Tests.test3 }">
            <div class="test-header">
              <h4>Test 3: Refresh This Page</h4>
              <span v-if="stage1Tests.test3" class="test-check">Done</span>
            </div>
            <p class="test-description">Try to refresh this page. You'll be logged out because in-memory sessions are lost when the server restarts. Log back in and notice your Session ID is now different - your previous session was lost!</p>
            <div v-if="previousSessionId && !stage1Tests.test3" class="session-comparison" :class="{ 'success': !sessionIdChanged }">
              <div class="session-old">
                <span class="label">Previous Session ID:</span>
                <code>{{ previousSessionId }}</code>
              </div>
              <div class="session-new">
                <span class="label">New Session ID:</span>
                <code>{{ sessionInfo.sessionId }}</code>
              </div>
              <p v-if="sessionIdChanged" class="comparison-note">Your session ID changed! This proves the old session was lost.</p>
              <p v-else class="comparison-note warning">Session IDs match - try restarting the app and refreshing.</p>
            </div>
            <button v-if="!stage1Tests.test3" @click="completeStage1Test('test3')" class="btn btn-outline btn-sm">
              I logged back in and have a new Session ID
            </button>
          </div>

          <div v-if="stage1Tests.test3" class="test-item">
            <div class="test-header">
              <h4>Test 4: Proceed to Stage 2</h4>
            </div>
            <p class="test-description">You've experienced the problem with in-memory sessions. Now let's fix it using Redis!</p>
            <button @click="markStage1Complete" class="btn btn-primary">
              Show Me How to Fix This
            </button>
          </div>
        </div>

        <!-- STAGE 2: Instructions (Only shown after Stage 1 is completed) -->
        <div v-if="!redisEnabled && stage1Completed" class="instructions">
          <h2>STAGE 2: Enable Redis Session Management</h2>

          <div class="alert alert-info">
            <strong>Option 1: Use the In-Browser Code Editor</strong>
            <p style="margin: 0.5rem 0 0 0;">
              <router-link to="/editor" class="editor-link">Open Code Editor →</router-link>
            </p>
          </div>

          <p class="intro"><strong>Option 2: Edit files in your IDE</strong></p>

          <div class="step-item">
            <h4>Step 1: Add Redis Dependencies</h4>
            <p class="step-description">Open <code>build.gradle.kts</code> and uncomment the Redis dependencies: <code>spring-boot-starter-data-redis</code> and <code>spring-session-data-redis</code></p>
          </div>

          <div class="step-item">
            <h4>Step 2: Configure Application Properties</h4>
            <p class="step-description">Open <code>application.properties</code>, change <code>spring.session.store-type=none</code> to <code>spring.session.store-type=redis</code>, and uncomment the 3 Redis session configuration lines.</p>
          </div>

          <div class="step-item">
            <h4>Step 3: Configure Spring Security</h4>
            <p class="step-description">Open <code>SecurityConfig.java</code> and uncomment the <code>HttpSessionSecurityContextRepository</code> configuration (imports, bean, and filter chain config).</p>
          </div>

          <div class="step-item">
            <h4>Step 4: Restart and Verify</h4>
            <p class="step-description">Go to the <a :href="workshopHubUrl" target="_blank">Workshop Hub</a> and rebuild & restart the app. Then come back to see the difference!</p>
          </div>
        </div>

        <!-- STAGE 3: Redis Sessions (Solution) -->
        <div v-if="redisEnabled" class="instructions">
          <h2>STAGE 3: Redis Session Management Enabled!</h2>
          <div class="alert alert-success">
            <strong>Sessions are now stored in Redis (distributed storage)</strong>
          </div>
          <p class="intro">Let's verify it works. Complete each test to proceed:</p>

          <div class="test-item" :class="{ 'completed': stage3Tests.test1 }">
            <div class="test-header">
              <h4>Test 1: Note Your Session ID</h4>
              <span v-if="stage3Tests.test1" class="test-check">Done</span>
            </div>
            <p class="test-description">Note your Session ID on the right: <code>{{ sessionInfo.sessionId }}</code>. This ID is now stored in Redis, not in application memory.</p>
            <button v-if="!stage3Tests.test1" @click="completeStage3Test('test1')" class="btn btn-outline btn-sm">
              I've noted my Session ID
            </button>
          </div>

          <div v-if="stage3Tests.test1" class="test-item" :class="{ 'completed': stage3Tests.test2 }">
            <div class="test-header">
              <h4>Test 2: Restart the Application</h4>
              <span v-if="stage3Tests.test2" class="test-check">Done</span>
            </div>
            <p class="test-description">Go to the <a :href="workshopHubUrl" target="_blank">Workshop Hub</a> and restart the app (without rebuilding).</p>
            <button v-if="!stage3Tests.test2" @click="completeStage3Test('test2')" class="btn btn-outline btn-sm">
              I've restarted the app
            </button>
          </div>

          <div v-if="stage3Tests.test2" class="test-item" :class="{ 'completed': stage3Tests.test3 }">
            <div class="test-header">
              <h4>Test 3: Still Logged In!</h4>
              <span v-if="stage3Tests.test3" class="test-check">Done</span>
            </div>
            <p class="test-description">Refresh this page - you're still logged in with the same Session ID! Redis preserved your session across the restart.</p>
            <div v-if="previousSessionId && !stage3Tests.test3" class="session-comparison success">
              <div class="session-old">
                <span class="label">Previous Session ID:</span>
                <code>{{ previousSessionId }}</code>
              </div>
              <div class="session-new">
                <span class="label">Current Session ID:</span>
                <code>{{ sessionInfo.sessionId }}</code>
              </div>
              <p v-if="!sessionIdChanged" class="comparison-note success">Same Session ID! Redis preserved your session.</p>
              <p v-else class="comparison-note warning">Session ID changed - this shouldn't happen with Redis enabled.</p>
            </div>
            <button v-if="!stage3Tests.test3" @click="completeStage3Test('test3')" class="btn btn-outline btn-sm">
              I'm still logged in!
            </button>
          </div>

          <div v-if="stage3Tests.test3" class="test-item" :class="{ 'completed': stage3Tests.test4 }">
            <div class="test-header">
              <h4>Test 4: View in Redis</h4>
              <span v-if="stage3Tests.test4" class="test-check">Done</span>
            </div>
            <p class="test-description">Use Redis Insight and search for <code>spring:session</code> to see your session data stored in Redis.</p>
            <a :href="redisInsightUrl" target="_blank" class="btn btn-outline btn-sm" style="margin-right: 0.5rem;">
              Open Redis Insight
            </a>
            <button v-if="!stage3Tests.test4" @click="completeStage3Test('test4')" class="btn btn-outline btn-sm">
              I've seen my session in Redis
            </button>
          </div>

          <div v-if="stage3Tests.test4" class="test-item" :class="{ 'completed': stage3Tests.test5 }">
            <div class="test-header">
              <h4>Test 5: Multiple Sessions (Optional)</h4>
              <span v-if="stage3Tests.test5" class="test-check">Done</span>
            </div>
            <p class="test-description">Open in incognito or a different browser - you'll get a different Session ID. Max 2 concurrent sessions are allowed.</p>
            <button v-if="!stage3Tests.test5" @click="completeStage3Test('test5')" class="btn btn-outline btn-sm">
              I've tested multiple sessions
            </button>
          </div>

          <div v-if="stage3Tests.test5" class="test-item" :class="{ 'completed': stage3Tests.test6 }">
            <div class="test-header">
              <h4>Test 6: Max Sessions Limit (Optional)</h4>
              <span v-if="stage3Tests.test6" class="test-check">Done</span>
            </div>
            <p class="test-description">Try a 3rd browser - login will be blocked. This security feature prevents unlimited concurrent sessions.</p>
            <button v-if="!stage3Tests.test6" @click="completeStage3Test('test6')" class="btn btn-outline btn-sm">
              I've tested the session limit
            </button>
          </div>

          <div v-if="stage3Tests.test4" class="completion-box">
            <h3>Workshop Complete!</h3>
            <p>You've successfully learned how to implement distributed session management with Redis.</p>
            <h4>What You Learned:</h4>
            <ul>
              <li>Sessions persist across application restarts</li>
              <li>Multiple application instances can share sessions (horizontal scaling)</li>
              <li>Session data is stored in Redis, not in application memory</li>
              <li>Enables zero-downtime deployments and load balancing</li>
            </ul>

            <div class="alert alert-info" style="margin-top: 1rem;">
              <strong>Want to try again?</strong> Restore all files to their original state.
            </div>
            <button @click="restartLab" class="btn btn-warning" :disabled="restartingLab">
              {{ restartingLab ? 'Restoring files...' : 'Restart Lab' }}
            </button>
          </div>
        </div>
      </div>

      <!-- Right Column: System Details -->
      <div class="details-column">
        <div class="details-card">
          <div class="header">
            <img src="@/assets/logo/small.png" alt="Redis Logo" width="32" height="32" />
            <h2>Session Details</h2>
          </div>

          <div class="session-info">
            <div class="info-item">
              <span class="info-label">Session ID</span>
              <span class="info-value">{{ sessionInfo.sessionId }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">Storage Type</span>
              <span class="info-value" :class="{ 'redis-enabled': redisEnabled }">{{ sessionInfo.storageType }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">Username</span>
              <span class="info-value">{{ sessionInfo.username }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">Created At</span>
              <span class="info-value">{{ sessionInfo.createdAt }}</span>
            </div>
          </div>

          <div class="actions">
            <button @click="refreshSession" class="btn btn-secondary">
              Refresh Session
            </button>
            <button @click="handleLogout" class="btn btn-danger">
              Logout
            </button>
          </div>

          <button @click="resetProgress" class="btn btn-link">
            Reset Test Progress
          </button>

          <div class="footer-info">
            <p v-if="redisEnabled"><strong>Tip:</strong> Session data persists in Redis even if the application restarts!</p>
            <p v-else><strong>Tip:</strong> In-memory sessions are lost when the application restarts.</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal using shared component -->
    <WorkshopModal
      v-model="modal.show"
      :title="modal.title"
      :message="modal.message"
      :type="modal.type"
      @confirm="handleModalConfirm"
      @cancel="handleModalCancel"
    />
  </div>
</template>




<script>
import { getBasePath } from "../utils/basePath";
import { WorkshopModal, WorkshopHeader } from "../utils/components";

export default {
  name: 'SessionHome',
  components: {
    WorkshopModal,
    WorkshopHeader
  },
  data() {
    return {
      sessionInfo: {
        sessionId: 'ABC123DEF456',
        storageType: 'In-Memory',
        username: 'user',
        createdAt: new Date().toLocaleString()
      },
      redisEnabled: false,
      stage1Completed: false,
      restartingLab: false,
      previousSessionId: null,
      stage1Tests: {
        test1: false,
        test2: false,
        test3: false
      },
      stage3Tests: {
        test1: false,
        test2: false,
        test3: false,
        test4: false,
        test5: false,
        test6: false
      },
      modal: {
        show: false,
        type: 'alert',
        title: '',
        message: '',
        onConfirm: null
      }
    };
  },
  computed: {
    basePath() {
      return getBasePath();
    },
    workshopHubUrl() {
      const protocol = window.location.protocol;
      const hostname = window.location.hostname;
      return `${protocol}//${hostname}:9000`;
    },
    redisInsightUrl() {
      // Redis Insight runs on port 5540 (exposed from internal container)
      const protocol = window.location.protocol;
      const hostname = window.location.hostname;
      return `${protocol}//${hostname}:5540`;
    },
    sessionIdChanged() {
      return this.previousSessionId && this.previousSessionId !== this.sessionInfo.sessionId;
    }
  },
  async mounted() {
    await this.fetchSessionInfo();
    this.loadTestProgress();
  },
  methods: {
    loadTestProgress() {
      const saved1 = localStorage.getItem('stage1Tests');
      const saved3 = localStorage.getItem('stage3Tests');
      const savedSessionId = localStorage.getItem('previousSessionId');
      if (saved1) this.stage1Tests = JSON.parse(saved1);
      if (saved3) this.stage3Tests = JSON.parse(saved3);
      if (savedSessionId) this.previousSessionId = savedSessionId;
    },
    saveTestProgress() {
      localStorage.setItem('stage1Tests', JSON.stringify(this.stage1Tests));
      localStorage.setItem('stage3Tests', JSON.stringify(this.stage3Tests));
    },
    saveSessionId() {
      localStorage.setItem('previousSessionId', this.sessionInfo.sessionId);
      this.previousSessionId = this.sessionInfo.sessionId;
    },
    completeStage1Test(test) {
      if (test === 'test1') {
        this.saveSessionId();
      }
      this.stage1Tests[test] = true;
      this.saveTestProgress();
    },
    completeStage3Test(test) {
      if (test === 'test1') {
        this.saveSessionId();
      }
      this.stage3Tests[test] = true;
      this.saveTestProgress();
    },
    async fetchSessionInfo() {
      try {
        const response = await fetch(`${this.basePath}/api/session-info`, {
          credentials: 'include'
        });
        if (response.status === 401 || response.status === 403) {
          // Session is invalid, redirect to login
          window.location.href = `${this.basePath}/login`;
          return;
        }
        if (response.ok) {
          const data = await response.json();
          this.sessionInfo = {
            sessionId: data.sessionId,
            storageType: data.sessionStorage,
            username: data.username,
            createdAt: new Date(data.creationTime).toLocaleString()
          };
          this.redisEnabled = data.redisEnabled;
          this.stage1Completed = data.stage1Completed;
        }
      } catch (error) {
        console.error('Error fetching session info:', error);
      }
    },
    async markStage1Complete() {
      try {
        const response = await fetch(`${this.basePath}/api/mark-stage1-complete`, {
          method: 'POST',
          credentials: 'include'
        });
        if (response.ok) {
          this.stage1Completed = true;
        }
      } catch (error) {
        console.error('Error marking stage 1 complete:', error);
      }
    },
    showModal(type, title, message, onConfirm = null) {
      this.modal = {
        show: true,
        type,
        title,
        message,
        onConfirm
      };
    },
    handleModalConfirm() {
      if (this.modal.onConfirm) {
        this.modal.onConfirm();
      }
      this.modal.show = false;
      this.modal.onConfirm = null;
    },
    handleModalCancel() {
      this.modal.show = false;
      this.modal.onConfirm = null;
    },
    resetProgress() {
      this.showModal('confirm', 'Reset Progress', 'Reset all test progress? You will start from the first test again.', () => {
        this.stage1Tests = { test1: false, test2: false, test3: false };
        this.stage3Tests = { test1: false, test2: false, test3: false, test4: false, test5: false, test6: false };
        this.previousSessionId = null;
        localStorage.removeItem('stage1Tests');
        localStorage.removeItem('stage3Tests');
        localStorage.removeItem('previousSessionId');
      });
    },
    async restartLab() {
      this.showModal('confirm', 'Restart Lab', 'Are you sure you want to restart the lab? This will restore all files to their original state and reset your progress. You will need to restart the application after this.', async () => {
        this.restartingLab = true;
        try {
          const response = await fetch(`${this.basePath}/api/editor/restore`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include'
          });
          const data = await response.json();

          if (data.success) {
            // Reset test progress
            this.stage1Tests = { test1: false, test2: false, test3: false };
            this.stage3Tests = { test1: false, test2: false, test3: false, test4: false, test5: false, test6: false };
            this.previousSessionId = null;
            localStorage.removeItem('stage1Tests');
            localStorage.removeItem('stage3Tests');
            localStorage.removeItem('previousSessionId');
            this.showModal('alert', 'Lab Reset', 'Lab reset! Please restart the application from the Workshop Hub.\n\nThen refresh this page to start from Stage 1.');
          } else {
            this.showModal('alert', 'Error', 'Error: ' + (data.error || 'Failed to restore files'));
          }
        } catch (error) {
          console.error('Error restarting lab:', error);
          this.showModal('alert', 'Error', 'Failed to restore files. Please try again.');
        } finally {
          this.restartingLab = false;
        }
      });
    },
    async handleLogout() {
      try {
        // Call Spring Security's logout endpoint to invalidate the session
        await fetch(`${this.basePath}/logout`, {
          method: 'POST',
          credentials: 'include'
        });
      } catch (error) {
        console.error('Error during logout:', error);
      } finally {
        // Redirect to login page after logout
        window.location.href = `${this.basePath}/login?logout`;
      }
    },
    refreshSession() {
      this.fetchSessionInfo();
    }
  }
};
</script>

<style scoped>
.session-home {
  min-height: 100vh;
  background: linear-gradient(135deg, var(--color-dark-900) 0%, var(--color-dark-800) 100%);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: var(--spacing-6);
}

.welcome-container {
  width: 100%;
  max-width: 1200px;
  display: grid;
  grid-template-columns: 1fr 350px;
  gap: var(--spacing-6);
  align-items: start;
  padding-top: var(--spacing-6);
}

/* Left Column - Instructions */
.instructions-column {
  min-width: 0;
}

.instructions {
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  padding: var(--spacing-6);
  box-shadow: var(--shadow-xl);
  border: 1px solid var(--color-border);
}

.instructions h2 {
  margin-bottom: var(--spacing-4);
  color: var(--color-text);
  font-size: var(--font-size-xl);
}

.instructions h3 {
  margin-top: var(--spacing-6);
  margin-bottom: var(--spacing-3);
  color: var(--color-text);
  font-size: var(--font-size-lg);
}

.instructions .intro {
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-5);
  line-height: 1.6;
}

/* Overrides for view-specific styling */
.test-item.completed {
  opacity: 0.7;
}

.test-check {
  background: #10b981;
  color: white;
  padding: 0.2rem 0.5rem;
  border-radius: var(--radius-sm);
}

.btn-link {
  width: 100%;
  text-align: center;
  margin-bottom: var(--spacing-3);
}

/* Session Comparison Box */
.session-comparison {
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid #ef4444;
  border-radius: var(--radius-md);
  padding: var(--spacing-3);
  margin: var(--spacing-3) 0;
}

.session-comparison.success {
  background: rgba(16, 185, 129, 0.1);
  border-color: #10b981;
}

.session-old,
.session-new {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  margin-bottom: var(--spacing-2);
}

.session-comparison .label {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  min-width: 120px;
}

.session-comparison code {
  background: var(--color-surface);
  padding: 0.2rem 0.4rem;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  word-break: break-all;
}

.comparison-note {
  margin: var(--spacing-2) 0 0 0;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  color: #ef4444;
}

.comparison-note.success {
  color: #10b981;
}

.comparison-note.warning {
  color: #f59e0b;
}

.completion-box {
  margin-top: var(--spacing-6);
  padding: var(--spacing-5);
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid #10b981;
  border-radius: var(--radius-md);
}

.completion-box h3 {
  color: #10b981;
  margin-top: 0;
  margin-bottom: var(--spacing-2);
}

.completion-box p {
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-4);
}

.completion-box h4 {
  color: var(--color-text);
  font-size: var(--font-size-sm);
  margin-bottom: var(--spacing-2);
}

.test-description a,
.step-description a {
  color: #DC382C;
  text-decoration: underline;
}

.instructions code {
  background: var(--color-surface);
  padding: 0.2rem 0.4rem;
  border-radius: var(--radius-sm);
  color: #DC382C;
  font-family: 'Courier New', monospace;
  font-size: 0.85em;
}

.instructions ul {
  margin-left: var(--spacing-5);
  color: var(--color-text-secondary);
}

.instructions ul li {
  margin-bottom: var(--spacing-2);
  line-height: 1.6;
}

/* Right Column - Details */
.details-column {
  position: sticky;
  top: var(--spacing-6);
}

.details-card {
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  padding: var(--spacing-6);
  box-shadow: var(--shadow-xl);
  border: 1px solid var(--color-border);
}

.header {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
  margin-bottom: var(--spacing-5);
  padding-bottom: var(--spacing-4);
  border-bottom: 1px solid var(--color-border);
}

.header h2 {
  margin: 0;
  font-size: var(--font-size-lg);
  color: var(--color-text);
}

.session-info {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-3);
  margin-bottom: var(--spacing-5);
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-1);
  padding: var(--spacing-3);
  background: var(--color-dark-800);
  border-radius: var(--radius-md);
}

.info-label {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.info-value {
  color: var(--color-text);
  font-family: 'Courier New', monospace;
  font-size: var(--font-size-sm);
  word-break: break-all;
}

.info-value.redis-enabled {
  color: #10b981;
}

.actions {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-3);
  margin-bottom: var(--spacing-4);
}

.footer-info {
  background: var(--color-dark-800);
  border-radius: var(--radius-md);
  padding: var(--spacing-3);
  text-align: center;
}

.footer-info p {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  line-height: 1.5;
}

.footer-info strong {
  color: #DC382C;
}

/* Override alert-info for this view */
.alert-info {
  background: #094771;
}

/* Redis Insight Box */
.redis-insight-box {
  margin: var(--spacing-5) 0;
  padding: var(--spacing-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-dark-800);
}

.redis-insight-box h4 {
  margin-bottom: var(--spacing-2);
  color: #4fc3f7;
  font-size: var(--font-size-base);
}

.redis-insight-box p {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  margin-bottom: var(--spacing-3);
}

.redis-insight-box code {
  background: var(--color-surface);
  color: #4fc3f7;
}

/* View-specific button overrides */
.btn-warning {
  margin-top: var(--spacing-3);
}

/* Responsive */
@media (max-width: 900px) {
  .welcome-container {
    grid-template-columns: 1fr;
  }

  .details-column {
    order: -1;
    position: static;
  }

  .actions {
    flex-direction: row;
  }

  .actions .btn {
    flex: 1;
  }
}
</style>
