<template>
  <main class="redis-insight-shell">
    <header class="redis-insight-shell__top-bar">
      <div class="redis-insight-shell__top-bar-content">
        <div class="redis-insight-shell__brand" aria-label="Redis workshop">
          <RedisWordmark
            class="redis-insight-shell__logo"
          />
          <h1>Distributed Session Management with Redis</h1>
        </div>

        <nav class="redis-insight-shell__header-actions" aria-label="Redis Insight navigation">
          <router-link
            class="redis-insight-shell__header-action"
            :to="backToWorkshopRoute"
          >
            Back to Workshop
          </router-link>
          <router-link
            class="redis-insight-shell__header-action"
            :to="codeEditorRoute"
          >
            Code Editor
          </router-link>
          <a
            class="redis-insight-shell__header-action"
            :href="workshopHubUrl"
          >
            Back to Hub
          </a>
        </nav>
      </div>
    </header>

    <section class="redis-insight-shell__body" aria-label="Redis Insight">
      <iframe
        class="redis-insight-shell__frame"
        :src="redisInsightFrameUrl"
        title="Redis Insight"
      ></iframe>
    </section>
  </main>
</template>

<script>
import { getBasePath, getWorkshopHubUrl } from '../utils/basePath';
import RedisWordmark from '../components/RedisWordmark.vue';

function getSameOriginRedisInsightUrl() {
  const basePath = getBasePath();
  const normalizedBasePath = basePath && basePath !== '/' ? basePath : '';

  return `${window.location.origin}${normalizedBasePath}/redis-insight/`;
}

export default {
  name: 'RedisInsightView',
  components: {
    RedisWordmark
  },
  computed: {
    backToWorkshopRoute() {
      const route = this.$route.query.returnTo;
      if (typeof route === 'string' && /^\/[0-3]$/.test(route)) {
        return route;
      }

      return '/2';
    },
    codeEditorRoute() {
      return `/4?returnTo=${encodeURIComponent(this.backToWorkshopRoute)}`;
    },
    redisInsightFrameUrl() {
      return getSameOriginRedisInsightUrl();
    },
    workshopHubUrl() {
      return getWorkshopHubUrl();
    }
  }
};
</script>

<style scoped>
.redis-insight-shell {
  min-height: 100vh;
  background: var(--color-background, #0A151B);
  color: var(--color-text, #e2e8f0);
}

.redis-insight-shell__top-bar {
  align-items: center;
  background-color: var(--color-dark-800, #0D1A22);
  border-bottom: 1px solid var(--color-border, rgba(71, 85, 105, 0.5));
  display: flex;
  height: 72px;
}

.redis-insight-shell__top-bar-content {
  align-items: center;
  display: flex;
  gap: var(--spacing-4, 1rem);
  justify-content: space-between;
  padding: 0 var(--spacing-6, 1.5rem);
  width: 100%;
}

.redis-insight-shell__brand {
  align-items: center;
  display: flex;
  gap: var(--spacing-4, 1rem);
  min-width: 0;
}

.redis-insight-shell__logo {
  flex: 0 0 auto;
  height: 1.75rem;
  width: 5.25rem;
}

.redis-insight-shell__brand h1 {
  color: var(--color-text, #e2e8f0);
  font-size: clamp(1.125rem, 2vw, 1.5rem);
  font-weight: var(--font-weight-semibold, 600);
  letter-spacing: -0.01em;
  line-height: 1.2;
  margin: 0;
  min-width: 0;
}

.redis-insight-shell__header-actions {
  align-items: center;
  display: flex;
  flex: 0 0 auto;
  gap: var(--spacing-2, 0.5rem);
}

.redis-insight-shell__header-action {
  align-items: center;
  background: var(--color-restart-bg, rgba(59, 130, 246, 0.2));
  border: 1px solid var(--color-restart-border, rgba(59, 130, 246, 0.4));
  border-radius: var(--radius-lg, 8px);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03);
  color: var(--color-restart-text, #93c5fd);
  display: inline-flex;
  font-size: var(--font-size-sm, 0.875rem);
  font-weight: var(--font-weight-semibold, 600);
  justify-content: center;
  line-height: 1;
  min-height: 2.5rem;
  padding: 0.65rem 0.95rem;
  text-decoration: none;
  transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease;
}

.redis-insight-shell__header-action:hover {
  background: rgba(59, 130, 246, 0.28);
  border-color: rgba(59, 130, 246, 0.58);
  color: #bfdbfe;
}

.redis-insight-shell__body {
  height: calc(100vh - 72px);
  min-height: 0;
  padding: var(--spacing-4, 1rem);
}

.redis-insight-shell__frame {
  background: #0A151B;
  border: 1px solid var(--color-border, rgba(71, 85, 105, 0.5));
  border-radius: var(--radius-xl, 0.75rem);
  height: 100%;
  width: 100%;
}

@media (max-width: 720px) {
  .redis-insight-shell__top-bar {
    height: auto;
    min-height: 72px;
  }

  .redis-insight-shell__top-bar-content {
    align-items: flex-start;
    flex-direction: column;
    padding: var(--spacing-3, 0.75rem) var(--spacing-4, 1rem);
  }

  .redis-insight-shell__header-actions {
    flex-wrap: wrap;
    width: 100%;
  }

  .redis-insight-shell__body {
    height: calc(100vh - 120px);
  }
}
</style>
