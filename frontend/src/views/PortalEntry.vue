<template>
  <main class="portal-entry">
    <section class="portal-card">
      <p class="eyebrow">Redis workshops</p>

      <form class="portal-form" @submit.prevent="submit">
        <label for="portal-email">Email address</label>
        <input
          id="portal-email"
          class="portal-email-input"
          v-model.trim="email"
          autocomplete="email"
          :disabled="portalLoading"
          placeholder="learner@example.com"
          required
          type="email"
        >

        <label class="contact-consent" for="portal-marketing-contact">
          <input
            id="portal-marketing-contact"
            v-model="marketingAllowed"
            :disabled="portalLoading"
            type="checkbox"
          >
          <span class="contact-consent-switch" aria-hidden="true"></span>
          <span class="contact-consent-copy">Allow marketing and sales contact</span>
        </label>

        <p v-if="displayError" class="error-message">{{ displayError }}</p>

        <button type="submit" :disabled="portalLoading">
          {{ portalLoading ? 'Opening portal' : 'Continue' }}
        </button>
      </form>
    </section>
  </main>
</template>

<script>
import { mapActions, mapState } from 'vuex';

export default {
  name: 'PortalEntry',
  data() {
    return {
      email: '',
      marketingAllowed: true,
      formError: null
    };
  },
  computed: {
    ...mapState({
      portalLoading: state => state.portalLoading,
      portalError: state => state.portalError
    }),
    displayError() {
      return this.formError || this.portalError;
    }
  },
  methods: {
    ...mapActions(['loginToPortal']),
    async submit() {
      this.formError = null;
      const email = this.email.trim();

      if (!email) {
        this.formError = 'Enter an email address';
        return;
      }

      try {
        await this.loginToPortal({
          email,
          marketingAllowed: this.marketingAllowed
        });
      } catch (error) {
        this.formError = error.message;
      }
    }
  }
};
</script>

<style scoped>
.portal-entry {
  align-items: center;
  background:
    radial-gradient(circle at 20% 20%, rgba(255, 68, 56, 0.24), transparent 32rem),
    linear-gradient(135deg, #151515 0%, #080808 100%);
  display: flex;
  min-height: 100vh;
  padding: var(--spacing-6);
}

.portal-card {
  background-color: rgba(22, 22, 22, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: var(--radius-xl);
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.32);
  margin: 0 auto;
  max-width: 520px;
  padding: var(--spacing-8);
  width: 100%;
}

.eyebrow {
  color: #ff6b5f;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  letter-spacing: 0.08em;
  margin: 0 0 var(--spacing-3);
  text-transform: uppercase;
}

.portal-form {
  display: grid;
  gap: var(--spacing-3);
}

label {
  color: var(--color-text);
  font-weight: var(--font-weight-medium);
}

.portal-email-input {
  background-color: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: var(--radius-md);
  color: var(--color-text);
  font-size: var(--font-size-base);
  padding: var(--spacing-4);
}

.portal-email-input:focus {
  border-color: #ff4438;
  box-shadow: 0 0 0 3px rgba(255, 68, 56, 0.2);
  outline: none;
}

.contact-consent {
  align-items: center;
  color: var(--color-text);
  cursor: pointer;
  display: grid;
  font-weight: var(--font-weight-medium);
  gap: var(--spacing-3);
  grid-template-columns: 44px 1fr;
  margin: var(--spacing-1) 0;
}

.contact-consent input {
  opacity: 0;
  pointer-events: none;
  position: absolute;
}

.contact-consent-switch {
  background-color: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 999px;
  display: inline-flex;
  height: 24px;
  position: relative;
  transition: background-color 0.18s ease, border-color 0.18s ease;
  width: 44px;
}

.contact-consent-switch::before {
  background-color: #f4f4f5;
  border-radius: 50%;
  content: '';
  height: 18px;
  left: 2px;
  position: absolute;
  top: 2px;
  transition: transform 0.18s ease, background-color 0.18s ease;
  width: 18px;
}

.contact-consent input:checked + .contact-consent-switch {
  background-color: #ff4438;
  border-color: #ff4438;
}

.contact-consent input:checked + .contact-consent-switch::before {
  background-color: #0b0b0b;
  transform: translateX(20px);
}

.contact-consent input:focus-visible + .contact-consent-switch {
  box-shadow: 0 0 0 3px rgba(255, 68, 56, 0.2);
}

.contact-consent input:disabled + .contact-consent-switch,
.contact-consent input:disabled ~ .contact-consent-copy {
  cursor: not-allowed;
  opacity: 0.68;
}

button {
  background-color: #ff4438;
  border: 0;
  border-radius: var(--radius-md);
  color: #0b0b0b;
  font-weight: var(--font-weight-semibold);
  padding: var(--spacing-4);
}

button:disabled,
.portal-email-input:disabled {
  cursor: not-allowed;
  opacity: 0.68;
}

.error-message {
  background-color: rgba(239, 68, 68, 0.16);
  border: 1px solid rgba(239, 68, 68, 0.45);
  border-radius: var(--radius-md);
  color: #fecaca;
  margin: 0;
  padding: var(--spacing-3);
}

@media (max-width: 640px) {
  .portal-entry {
    padding: var(--spacing-4);
  }

  .portal-card {
    padding: var(--spacing-6);
  }
}
</style>
