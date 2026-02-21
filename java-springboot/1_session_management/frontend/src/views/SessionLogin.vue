<template>
  <div class="session-login">
    <div class="login-container">
      <!-- Left Column: Workshop Information -->
      <div class="info-column">
        <div class="info-header">
          <img src="@/assets/logo/small.png" alt="Redis Logo" width="40" height="40" />
          <h1>Session Management Workshop</h1>
        </div>

        <div class="info-section">
          <h2>What is Distributed Session Management?</h2>
          <p>Distributed session management stores user session data in a centralized location accessible by all application instances. Instead of keeping sessions in local memory, sessions are persisted externally so any server can handle any user request.</p>
        </div>

        <div class="info-section">
          <h2>Why is it Necessary?</h2>
          <p>In modern cloud environments, applications run across multiple instances for scalability and reliability. Without distributed sessions:</p>
          <ul>
            <li>Users lose their session if routed to a different server</li>
            <li>Scaling horizontally becomes problematic</li>
            <li>Server restarts cause all users to be logged out</li>
          </ul>
        </div>

        <div class="info-section">
          <h2>Why Use Redis?</h2>
          <p>Redis is ideal for session management because:</p>
          <ul>
            <li><strong>Speed:</strong> In-memory storage with sub-millisecond latency</li>
            <li><strong>TTL Support:</strong> Automatic session expiration</li>
            <li><strong>High Availability:</strong> Built-in replication and clustering</li>
            <li><strong>Simple Integration:</strong> Native Spring Session support</li>
          </ul>
        </div>

        <div class="info-section">
          <h2>Getting Started</h2>
          <ol>
            <li><strong>Login</strong> using the demo credentials on the right</li>
            <li><strong>Explore Stage 1:</strong> See your session stored in memory</li>
            <li><strong>Continue to Stage 2:</strong> Enable Redis-backed sessions</li>
          </ol>
        </div>
      </div>

      <!-- Right Column: Login Form -->
      <div class="login-column">
        <div class="login-card">
          <h2>Sign In</h2>
          <p class="subtitle">Access the workshop</p>

          <div v-if="error" class="alert alert-error">
            {{ error }}
          </div>
          <div v-if="success" class="alert alert-success">
            {{ success }}
          </div>

          <form :action="loginUrl" method="POST" class="login-form">
            <div class="form-group">
              <label for="username">Username</label>
              <input
                type="text"
                id="username"
                name="username"
                value="user"
                placeholder="Enter username"
                required
                autofocus
              >
            </div>

            <div class="form-group">
              <label for="password">Password</label>
              <input
                type="password"
                id="password"
                name="password"
                value="password"
                placeholder="Enter password"
                required
              >
            </div>

            <button type="submit" class="btn btn-primary">
              Sign In
            </button>
          </form>

          <div class="demo-credentials">
            <p><strong>Demo Credentials:</strong></p>
            <p>Username: <code>user</code> / Password: <code>password</code></p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getBasePath } from "../utils/basePath";

export default {
  name: 'SessionLogin',
  data() {
    return {
      error: null,
      success: null
    };
  },
  computed: {
    loginUrl() {
      // Use BASE_URL which is set from VUE_APP_BASE_PATH during build
      return `${getBasePath()}/login`;
    }
  },
  mounted() {
    // Check for query parameters from Spring Security redirects
    const params = new URLSearchParams(window.location.search);
    if (params.get('error') !== null) {
      this.error = 'Invalid username or password';
    } else if (params.get('logout') !== null) {
      this.success = 'You have been logged out successfully';
    } else if (params.get('expired') !== null) {
      this.error = 'Your session has expired';
    } else if (params.get('invalid') !== null) {
      this.error = 'Invalid session';
    }
  }
};
</script>

<style scoped>
.session-login {
  min-height: 100vh;
  background: linear-gradient(135deg, var(--color-dark-900) 0%, var(--color-dark-800) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-6);
}

.login-container {
  width: 100%;
  max-width: 1100px;
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: var(--spacing-8);
  align-items: center;
}

/* Left Column - Info */
.info-column {
  padding: var(--spacing-6);
}

.info-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-4);
  margin-bottom: var(--spacing-8);
}

.info-header h1 {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text);
  margin: 0;
}

.info-section {
  margin-bottom: var(--spacing-6);
}

.info-section h2 {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: #DC382C;
  margin-bottom: var(--spacing-3);
}

.info-section p {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.7;
  margin-bottom: var(--spacing-3);
}

.info-section ul,
.info-section ol {
  margin-left: var(--spacing-5);
  margin-bottom: var(--spacing-3);
}

.info-section li {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.7;
  margin-bottom: var(--spacing-2);
}

.info-section li strong {
  color: var(--color-text);
}

/* Right Column - Login */
.login-column {
  display: flex;
  justify-content: center;
}

.login-card {
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  padding: var(--spacing-8);
  box-shadow: var(--shadow-xl);
  border: 1px solid var(--color-border);
  width: 100%;
}

.login-card h2 {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text);
  margin-bottom: var(--spacing-2);
}

.subtitle {
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-6);
  font-size: var(--font-size-sm);
}

.alert {
  padding: var(--spacing-3) var(--spacing-4);
  border-radius: var(--radius-md);
  margin-bottom: var(--spacing-5);
  font-size: var(--font-size-sm);
}

.alert-error {
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid #ef4444;
  color: #ef4444;
}

.alert-success {
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid #10b981;
  color: #10b981;
}

.login-form {
  margin-bottom: var(--spacing-6);
}

.form-group {
  margin-bottom: var(--spacing-5);
}

.form-group label {
  display: block;
  margin-bottom: var(--spacing-2);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
}

.form-group input {
  width: 100%;
  padding: var(--spacing-3) var(--spacing-4);
  background: var(--color-dark-800);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text);
  font-size: var(--font-size-sm);
  transition: all var(--transition-base);
}

.form-group input:focus {
  outline: none;
  border-color: #DC382C;
  box-shadow: 0 0 0 3px rgba(220, 56, 44, 0.1);
}

.btn {
  width: 100%;
  padding: var(--spacing-3) var(--spacing-6);
  border: none;
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  cursor: pointer;
  transition: all var(--transition-base);
}

.btn-primary {
  background: #DC382C;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #c42f24;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.demo-credentials {
  background: var(--color-dark-800);
  border-radius: var(--radius-md);
  padding: var(--spacing-4);
  text-align: center;
}

.demo-credentials p {
  margin: var(--spacing-1) 0;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.demo-credentials code {
  background: var(--color-surface);
  padding: 0.2rem 0.5rem;
  border-radius: var(--radius-sm);
  color: #DC382C;
  font-family: 'Courier New', monospace;
}

/* Responsive - Stack on mobile */
@media (max-width: 900px) {
  .login-container {
    grid-template-columns: 1fr;
    max-width: 500px;
  }

  .info-column {
    order: 2;
    padding: var(--spacing-4);
  }

  .login-column {
    order: 1;
  }

  .login-card {
    padding: var(--spacing-6);
  }

  .info-header {
    margin-bottom: var(--spacing-6);
  }
}
</style>
