<template>
  <main class="session-learner-app">
    <section v-if="state === 'loading'" class="app-card app-card--center">
      <p>Checking session state...</p>
    </section>

    <section v-else-if="state === 'unavailable'" class="app-card app-card--center">
      <h1>Backend is unavailable</h1>
      <p>The learner runtime may be restarting or rebuilding. Try again when the shell says the runtime is ready.</p>
      <button type="button" class="btn btn-secondary" @click="fetchSessionInfo">Retry</button>
    </section>

    <section v-else-if="state === 'unauthenticated'" class="app-grid">
      <div class="app-card app-card--intro">
        <img src="@/assets/logo/small.png" alt="Redis Logo" width="40" height="40" />
        <h1>Session Management App</h1>
        <p>
          This is the application surface for the workshop. Sign in, note your session id,
          then use the shell instructions to test what happens when the runtime restarts.
        </p>
      </div>

      <div class="app-card">
        <h2>Sign In</h2>
        <p class="subtitle">Use the demo credentials to enter the app.</p>

        <div v-if="loginError" class="alert alert-error">
          {{ loginError }}
        </div>

        <form class="login-form" @submit.prevent="login">
          <div class="form-group">
            <label for="username">Username</label>
            <input id="username" v-model="username" name="username" type="text" autocomplete="username" required />
          </div>

          <div class="form-group">
            <label for="password">Password</label>
            <input id="password" v-model="password" name="password" type="password" autocomplete="current-password" required />
          </div>

          <button type="submit" class="btn btn-primary" :disabled="loginBusy">
            {{ loginBusy ? 'Signing in...' : 'Sign In' }}
          </button>
        </form>

        <div class="demo-credentials">
          <p><strong>Demo Credentials:</strong></p>
          <p>Username: <code>user</code> / Password: <code>password</code></p>
        </div>
      </div>
    </section>

    <section v-else class="app-card session-card">
      <div class="session-card__header">
        <div>
          <p class="session-card__eyebrow">Authenticated app session</p>
          <h1>Session Details</h1>
        </div>
        <span class="storage-pill" :class="{ 'storage-pill--redis': redisEnabled }">
          {{ sessionInfo.storageType }}
        </span>
      </div>

      <div class="session-info">
        <div class="info-item">
          <span class="info-label">Session ID</span>
          <span class="info-value">{{ sessionInfo.sessionId }}</span>
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
        <button type="button" class="btn btn-secondary" @click="fetchSessionInfo">
          Refresh Session
        </button>
        <button type="button" class="btn btn-danger" @click="logout">
          Logout
        </button>
      </div>

      <div class="footer-info">
        <p v-if="redisEnabled"><strong>Tip:</strong> Redis is preserving session data across runtime restarts.</p>
        <p v-else><strong>Tip:</strong> In-memory sessions are lost when the application restarts.</p>
      </div>
    </section>
  </main>
</template>

<script>
import { getApiUrl, getBasePath } from '../utils/basePath';

export default {
  name: 'SessionLearnerApp',
  data() {
    return {
      state: 'loading',
      username: 'user',
      password: 'password',
      loginBusy: false,
      loginError: '',
      redisEnabled: false,
      sessionInfo: {
        sessionId: '',
        storageType: '',
        username: '',
        createdAt: ''
      }
    };
  },
  computed: {
    basePath() {
      return getBasePath();
    }
  },
  async mounted() {
    await this.fetchSessionInfo();
  },
  methods: {
    async fetchSessionInfo() {
      this.loginError = '';

      try {
        const response = await fetch(getApiUrl('/api/session-info'), {
          credentials: 'include'
        });

        if (response.status === 401 || response.status === 403) {
          this.state = 'unauthenticated';
          this.notifyShell('session-app-unauthenticated');
          return;
        }

        if (!response.ok) {
          this.state = 'unavailable';
          this.notifyShell('session-app-unavailable');
          return;
        }

        const data = await response.json();
        this.sessionInfo = {
          sessionId: data.sessionId,
          storageType: data.sessionStorage,
          username: data.username,
          createdAt: new Date(data.creationTime).toLocaleString()
        };
        this.redisEnabled = data.redisEnabled;
        this.state = 'authenticated';
        this.notifyShell('session-app-authenticated', {
          sessionId: data.sessionId,
          redisEnabled: data.redisEnabled,
          stage1Completed: data.stage1Completed
        });
      } catch (error) {
        console.error('Error fetching session info:', error);
        this.state = 'unavailable';
        this.notifyShell('session-app-unavailable');
      }
    },
    async login() {
      this.loginBusy = true;
      this.loginError = '';

      try {
        const body = new URLSearchParams();
        body.set('username', this.username);
        body.set('password', this.password);

        await fetch(`${this.basePath}/login`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
          },
          credentials: 'include',
          body
        });

        await this.fetchSessionInfo();

        if (this.state !== 'authenticated') {
          this.loginError = 'Invalid username or password';
        }
      } catch (error) {
        console.error('Login failed:', error);
        this.loginError = 'Backend is unavailable';
      } finally {
        this.loginBusy = false;
      }
    },
    async logout() {
      try {
        await fetch(`${this.basePath}/logout`, {
          method: 'POST',
          credentials: 'include'
        });
      } catch (error) {
        console.error('Logout failed:', error);
      } finally {
        this.state = 'unauthenticated';
        this.notifyShell('session-app-unauthenticated');
      }
    },
    notifyShell(type, payload = {}) {
      if (window.parent && window.parent !== window) {
        window.parent.postMessage({ type, payload }, window.location.origin);
      }
    }
  }
};
</script>

<style scoped>
.session-learner-app {
  min-height: 100vh;
  padding: var(--spacing-6);
  background:
    radial-gradient(circle at 0% 0%, rgba(220, 56, 44, 0.12), transparent 22rem),
    linear-gradient(135deg, var(--color-dark-900) 0%, var(--color-dark-800) 100%);
  color: var(--color-text);
}

.app-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(20rem, 26rem);
  gap: var(--spacing-6);
  align-items: center;
  min-height: calc(100vh - (var(--spacing-6) * 2));
  max-width: 64rem;
  margin: 0 auto;
}

.app-card {
  width: 100%;
  max-width: 52rem;
  margin: 0 auto;
  padding: var(--spacing-6);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  background: rgba(16, 26, 33, 0.96);
  box-shadow: var(--shadow-xl);
}

.app-card--center {
  display: grid;
  min-height: 20rem;
  place-items: center;
  text-align: center;
}

.app-card--intro {
  background: transparent;
  border-color: transparent;
  box-shadow: none;
}

.app-card h1,
.app-card h2 {
  margin: 0 0 var(--spacing-3);
  color: var(--color-text);
}

.app-card p,
.subtitle {
  color: var(--color-text-secondary);
  line-height: 1.7;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-4);
  margin-top: var(--spacing-5);
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-2);
}

.form-group label {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
}

.form-group input {
  padding: var(--spacing-3);
  border: 1px solid var(--input-border);
  border-radius: var(--radius-md);
  background: var(--input-bg);
  color: var(--color-text);
}

.demo-credentials {
  margin-top: var(--spacing-5);
  padding: var(--spacing-3);
  border-radius: var(--radius-md);
  background: var(--color-dark-800);
}

.demo-credentials p {
  margin: 0;
  font-size: var(--font-size-xs);
}

.session-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--spacing-4);
  margin-bottom: var(--spacing-5);
  padding-bottom: var(--spacing-4);
  border-bottom: 1px solid var(--color-border);
}

.session-card__eyebrow {
  margin: 0 0 var(--spacing-1);
  color: var(--color-primary-300);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.storage-pill {
  padding: 0.25rem 0.65rem;
  border: 1px solid rgba(251, 191, 36, 0.4);
  border-radius: 999px;
  background: rgba(251, 191, 36, 0.12);
  color: var(--color-warning);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
}

.storage-pill--redis {
  border-color: rgba(74, 222, 128, 0.4);
  background: rgba(74, 222, 128, 0.12);
  color: var(--color-success);
}

.session-info {
  display: grid;
  gap: var(--spacing-3);
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-1);
  padding: var(--spacing-3);
  border-radius: var(--radius-md);
  background: var(--color-dark-800);
}

.info-label {
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

.info-value {
  color: var(--color-text);
  font-family: var(--font-family-mono);
  font-size: var(--font-size-sm);
  word-break: break-all;
}

.actions {
  display: flex;
  gap: var(--spacing-3);
  margin-top: var(--spacing-5);
}

.footer-info {
  margin-top: var(--spacing-5);
  padding: var(--spacing-3);
  border-radius: var(--radius-md);
  background: var(--color-dark-800);
}

.footer-info p {
  margin: 0;
  font-size: var(--font-size-xs);
}

.footer-info strong {
  color: #DC382C;
}

@media (max-width: 820px) {
  .app-grid {
    grid-template-columns: 1fr;
  }

  .app-card--intro {
    padding-bottom: 0;
  }
}
</style>
