<template>
  <section
    class="workshop-tool-frame"
    :class="{ 'workshop-tool-frame--maximized': isMaximized }"
  >
    <header class="workshop-tool-frame__header">
      <p class="workshop-tool-frame__eyebrow">{{ eyebrow }}</p>
      <div class="workshop-tool-frame__actions">
        <slot name="actions"></slot>
        <button
          type="button"
          class="workshop-tool-frame__action workshop-tool-frame__action--icon"
          :aria-pressed="isMaximized ? 'true' : 'false'"
          :aria-label="isMaximized ? 'Restore frame' : 'Maximize frame'"
          :title="isMaximized ? 'Restore frame' : 'Maximize frame'"
          @click="toggleMaximized"
        >
          <svg
            v-if="isMaximized"
            class="workshop-tool-frame__icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
            aria-hidden="true"
          >
            <path d="M9 3v6H3" />
            <path d="M15 3v6h6" />
            <path d="M9 21v-6H3" />
            <path d="M15 21v-6h6" />
          </svg>
          <svg
            v-else
            class="workshop-tool-frame__icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
            aria-hidden="true"
          >
            <path d="M8 4H4v4" />
            <path d="M16 4h4v4" />
            <path d="M8 20H4v-4" />
            <path d="M20 16v4h-4" />
          </svg>
        </button>
      </div>
    </header>

    <div class="workshop-tool-frame__viewport">
      <iframe
        class="workshop-tool-frame__iframe"
        :src="src"
        :title="title"
        :allow="allow"
        @load="$emit('load', $event)"
        @error="$emit('error', $event)"
      ></iframe>
    </div>
  </section>
</template>

<script>
export default {
  name: 'WorkshopToolFrame',
  props: {
    eyebrow: { type: String, default: 'Tool' },
    src: { type: String, required: true },
    title: { type: String, default: 'Workshop tool' },
    allow: { type: String, default: '' }
  },
  emits: ['load', 'error'],
  data() {
    return {
      isMaximized: false,
      previousBodyOverflow: ''
    };
  },
  mounted() {
    window.addEventListener('keydown', this.handleKeydown);
  },
  beforeUnmount() {
    window.removeEventListener('keydown', this.handleKeydown);
    this.setBodyScrollLock(false);
  },
  methods: {
    handleKeydown(event) {
      if (event.key === 'Escape' && this.isMaximized) {
        this.isMaximized = false;
        this.setBodyScrollLock(false);
      }
    },
    setBodyScrollLock(locked) {
      if (locked) {
        if (!this.previousBodyOverflow) {
          this.previousBodyOverflow = document.body.style.overflow || '';
        }
        document.body.style.overflow = 'hidden';
        return;
      }

      document.body.style.overflow = this.previousBodyOverflow;
      this.previousBodyOverflow = '';
    },
    toggleMaximized() {
      this.isMaximized = !this.isMaximized;
      this.setBodyScrollLock(this.isMaximized);
    }
  }
};
</script>

<style scoped>
.workshop-tool-frame {
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  border: 1px solid var(--color-border, rgba(71, 85, 105, 0.5));
  border-radius: var(--radius-xl, 0.75rem);
  background: var(--color-dark-900, #0A151B);
  box-shadow: var(--card-shadow, 0 4px 6px -1px rgba(0, 0, 0, 0.3));
}

.workshop-tool-frame--maximized {
  position: fixed;
  inset: calc(72px + var(--spacing-4, 1rem)) var(--spacing-4, 1rem) var(--spacing-4, 1rem);
  z-index: 1000;
  width: auto;
  height: auto;
  min-height: 0;
  border-radius: var(--radius-xl, 0.75rem);
  box-shadow:
    0 0 0 100vmax var(--color-background, #0A151B),
    0 24px 80px rgba(0, 0, 0, 0.5);
}

.workshop-tool-frame__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-2, 0.5rem);
  min-height: 2.75rem;
  padding: var(--spacing-2, 0.5rem) var(--spacing-3, 0.75rem);
  border-bottom: 1px solid var(--color-border-light, rgba(71, 85, 105, 0.3));
  background: rgba(13, 26, 34, 0.95);
}

.workshop-tool-frame__eyebrow {
  margin: 0;
  color: var(--color-text-secondary, #94a3b8);
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: var(--font-weight-semibold, 600);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.workshop-tool-frame__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: var(--spacing-2, 0.5rem);
}

.workshop-tool-frame__action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 1.85rem;
  border: 1px solid var(--color-restart-border, rgba(59, 130, 246, 0.4));
  border-radius: var(--radius-lg, 8px);
  background: var(--color-restart-bg, rgba(59, 130, 246, 0.2));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03);
  color: var(--color-restart-text, #93c5fd);
  cursor: pointer;
  transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease;
}

.workshop-tool-frame__action--icon {
  width: 1.85rem;
  padding: 0;
}

.workshop-tool-frame__action:hover {
  background: rgba(59, 130, 246, 0.28);
  border-color: rgba(59, 130, 246, 0.58);
  color: #bfdbfe;
}

.workshop-tool-frame__icon {
  width: 1rem;
  height: 1rem;
  flex: 0 0 auto;
}

.workshop-tool-frame__viewport {
  position: relative;
  flex: 1;
  min-height: 0;
  background: var(--color-dark-900, #0A151B);
}

.workshop-tool-frame__iframe {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  border: 0;
  background: #0A151B;
}

@media (max-width: 720px) {
  .workshop-tool-frame--maximized {
    inset: calc(72px + var(--spacing-2, 0.5rem)) var(--spacing-2, 0.5rem) var(--spacing-2, 0.5rem);
  }
}
</style>
