<template>
  <div class="search-demo">
    <div class="main-container">
      <div class="workshop-panel">
        <div class="workshop-header">
          <h2>
            <img
              src="@/assets/logo/small.png"
              alt="Redis Logo"
              width="28"
              height="28"
              class="logo-small"
            />
            Test Your Search
          </h2>
        </div>

        <div class="workshop-content">
          <div v-if="contentLoading" class="content-status">
            Loading workshop instructions...
          </div>

          <div v-else-if="contentError" class="content-status content-status--error">
            {{ contentError }}
          </div>

          <WorkshopContentRenderer
            v-else-if="content"
            :content="content"
            :active-stage-id="currentTestStageId"
            :show-title="false"
            :show-summary="false"
            :action-handlers="actionHandlers"
            @render-error="handleRenderIssues"
          />
        </div>
      </div>

      <div class="search-section">
        <div class="search-header">
          <form @submit.prevent="searchMovies" class="search-form">
            <div class="form-row-inline">
              <div class="form-group">
                <label for="title">Title</label>
                <input type="text" id="title" v-model="searchParams.title" placeholder="e.g., Matrix" />
              </div>
              <div class="form-group">
                <label for="extract">Description</label>
                <input type="text" id="extract" v-model="searchParams.extract" placeholder="e.g., science fiction" />
              </div>
              <div class="form-group">
                <label for="actors">Actor</label>
                <input type="text" id="actors" v-model="searchParams.actors" placeholder="e.g., Keanu Reeves" />
              </div>
              <div class="form-group small">
                <label for="year">Year</label>
                <input type="number" id="year" v-model.number="searchParams.year" placeholder="1999" />
              </div>
              <div class="form-group small">
                <label for="genres">Genre</label>
                <select id="genres" v-model="searchParams.genres">
                  <option value="">All</option>
                  <option v-for="genre in availableGenres" :key="genre" :value="genre">{{ genre }}</option>
                </select>
              </div>
              <button type="submit" class="btn btn-primary" :disabled="loading">
                {{ loading ? 'Searching...' : 'Search' }}
              </button>
            </div>
          </form>

          <div v-if="error" class="alert alert-error">{{ error }}</div>
        </div>

        <div class="results-container">
          <div v-if="movies.length > 0" class="results-section">
            <h3>Results ({{ movies.length }} movies found in {{ searchTime }}ms)</h3>
            <div class="movies-grid">
              <div v-for="movie in movies" :key="movie.id" class="movie-card">
                <img
                  v-if="movie.thumbnail && !movie.imageError"
                  :src="movie.thumbnail"
                  :alt="movie.title"
                  class="movie-thumbnail"
                  @error="movie.imageError = true"
                />
                <div v-else class="movie-thumbnail-placeholder">No Image</div>
                <div class="movie-info">
                  <h4>{{ movie.title }}</h4>
                  <p class="movie-year">{{ movie.year }}</p>
                  <p class="movie-genres">{{ movie.genres?.join(', ') }}</p>
                  <p class="movie-cast">{{ movie.cast?.slice(0, 3).join(', ') }}</p>
                  <p class="movie-extract">{{ movie.extract }}</p>
                </div>
              </div>
            </div>
          </div>
          <div v-else-if="searched && !loading" class="no-results">
            <p>No movies found. Try different search criteria.</p>
          </div>
          <div v-else class="no-results">
            <p>Enter search criteria and click Search to find movies.</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getApiUrl, getRedisInsightUrl } from '../utils/basePath';
import { WorkshopContentRenderer } from '../utils/components';
import {
  fetchWorkshopContent,
  getContentStageIndex,
  resolveContentStageId
} from '../utils/workshopContent';

const STORAGE_KEY = 'fullTextSearchWorkshop';

const LEGACY_TEST_STAGE_MAP = {
  1: 'title-search',
  2: 'description-search',
  3: 'fuzzy-match',
  4: 'combined-filters',
  5: 'actor-search',
  6: 'redis-insight',
  7: 'complete'
};

export default {
  name: 'SearchDemo',
  components: {
    WorkshopContentRenderer
  },
  data() {
    return {
      content: null,
      contentLoading: true,
      contentError: '',
      currentTestStageId: '',
      searchParams: { title: '', extract: '', actors: '', year: null, genres: '' },
      movies: [],
      availableGenres: [],
      searchTime: null,
      loading: false,
      error: null,
      searched: false,
      restartingLab: false
    };
  },
  computed: {
    redisInsightUrl() {
      return getRedisInsightUrl();
    },
    actionHandlers() {
      return {
        setStage: ({ args }) => this.setTestStage(args.stageId),
        openRedisInsight: () => this.openRedisInsight(),
        restartLab: () => this.restartLab(),
        resetProgress: () => this.restartTests(),
        openRoute: ({ args }) => this.openRoute(args.route),
        openEditor: () => this.openRoute('/editor')
      };
    }
  },
  async mounted() {
    await Promise.all([
      this.loadContent(),
      this.fetchGenres()
    ]);
  },
  methods: {
    async loadContent() {
      this.contentLoading = true;
      this.contentError = '';

      try {
        this.content = await fetchWorkshopContent('search-demo');
        this.currentTestStageId = this.restoreSavedStageId();
      } catch (error) {
        console.error('Error loading search demo content:', error);
        this.contentError = error.message || 'Failed to load workshop instructions.';
      } finally {
        this.contentLoading = false;
      }
    },
    restoreSavedStageId() {
      const fallbackStageId = resolveContentStageId(this.content);
      const saved = localStorage.getItem(STORAGE_KEY);

      if (!saved) {
        return fallbackStageId;
      }

      try {
        const data = JSON.parse(saved);
        if (typeof data.testStageId === 'string') {
          return resolveContentStageId(this.content, data.testStageId);
        }

        if (typeof data.testStep === 'number') {
          return resolveContentStageId(this.content, LEGACY_TEST_STAGE_MAP[data.testStep]);
        }
      } catch (error) {
        return fallbackStageId;
      }

      return fallbackStageId;
    },
    saveTestStage() {
      const saved = localStorage.getItem(STORAGE_KEY);
      let data = {};

      if (saved) {
        try {
          data = JSON.parse(saved);
        } catch (error) {
          data = {};
        }
      }

      data.testStageId = this.currentTestStageId;
      data.testStep = getContentStageIndex(this.content, this.currentTestStageId) + 1;
      localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
    },
    setTestStage(stageId) {
      this.currentTestStageId = resolveContentStageId(this.content, stageId);
      this.saveTestStage();
    },
    restartTests() {
      this.setTestStage(resolveContentStageId(this.content));
    },
    openRedisInsight() {
      window.open(this.redisInsightUrl, '_blank', 'noopener');
    },
    openRoute(route) {
      if (!route) {
        return;
      }

      this.$router.push(route).catch(() => {});
    },
    handleRenderIssues(issues) {
      if (issues?.length) {
        console.warn('SearchDemo content render issues:', issues);
      }
    },
    async fetchGenres() {
      try {
        const response = await fetch(getApiUrl('/api/genres'));
        if (response.ok) {
          const data = await response.json();
          this.availableGenres = data.genres || [];
        }
      } catch (error) {
        console.error('Error fetching genres:', error);
      }
    },
    hasFilters() {
      return this.searchParams.title
        || this.searchParams.extract
        || this.searchParams.actors
        || this.searchParams.year
        || this.searchParams.genres;
    },
    async searchMovies() {
      if (!this.hasFilters()) {
        this.movies = [];
        this.searched = false;
        return;
      }

      this.loading = true;
      this.error = null;
      this.searched = true;

      try {
        const params = new URLSearchParams();
        if (this.searchParams.title) params.append('title', this.searchParams.title);
        if (this.searchParams.extract) params.append('text', this.searchParams.extract);
        if (this.searchParams.actors) params.append('cast', this.searchParams.actors);
        if (this.searchParams.year) params.append('year', this.searchParams.year);
        if (this.searchParams.genres) params.append('genres', this.searchParams.genres);

        const response = await fetch(getApiUrl(`/api/search?${params}`));
        if (response.ok) {
          const data = await response.json();
          this.movies = data.movies || [];
          this.searchTime = data.searchTime;
        } else {
          this.error = 'Failed to search movies';
        }
      } catch (error) {
        console.error('Error searching:', error);
        this.error = 'Error connecting to server';
      } finally {
        this.loading = false;
      }
    },
    async restartLab() {
      if (!confirm('Are you sure you want to restart the lab? This will restore all files to their original state. You will need to restart the application after this.')) {
        return;
      }

      this.restartingLab = true;
      try {
        const response = await fetch(getApiUrl('/api/editor/restore'), {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          credentials: 'include'
        });
        const data = await response.json();
        if (data.success) {
          localStorage.removeItem(STORAGE_KEY);
          this.currentTestStageId = resolveContentStageId(this.content);
          alert('Lab reset! Please go to the Workshop Hub and rebuild the app, then refresh this page to start from the beginning.');
        } else {
          alert('Error: ' + (data.error || 'Failed to restore files'));
        }
      } catch (error) {
        console.error('Error restarting lab:', error);
        alert('Failed to restore files. Please try again.');
      } finally {
        this.restartingLab = false;
      }
    }
  }
};
</script>

<style scoped>
.search-demo {
  margin: 0;
  padding: 0;
  background: #1a1a1a;
  overflow: hidden;
}

.main-container {
  display: flex;
  height: 100vh;
  width: 100vw;
}

.workshop-panel {
  width: 360px;
  min-width: 360px;
  background: #1e1e1e;
  border-right: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.workshop-header {
  background: #252526;
  padding: var(--spacing-4);
  border-bottom: 1px solid var(--color-border);
}

.workshop-header h2 {
  margin: 0;
  color: #dc382c;
  font-size: var(--font-size-lg);
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
}

.logo-small {
  display: inline-block;
}

.workshop-content {
  flex: 1;
  overflow-y: auto;
  padding: var(--spacing-5);
  color: #cccccc;
  font-size: var(--font-size-sm);
  line-height: 1.5;
}

.content-status {
  padding: var(--spacing-4);
  border-radius: var(--radius-md);
  background: #1f2937;
  border: 1px solid rgba(148, 163, 184, 0.2);
  color: #cbd5e1;
}

.content-status--error {
  border-color: rgba(220, 56, 44, 0.4);
  color: #fca5a5;
}

.search-section {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.search-header {
  padding: var(--spacing-5);
  border-bottom: 1px solid var(--color-border);
  background: #111827;
}

.search-form {
  width: 100%;
}

.form-row-inline {
  display: flex;
  gap: var(--spacing-3);
  align-items: end;
  flex-wrap: wrap;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-2);
  min-width: 180px;
}

.form-group.small {
  min-width: 110px;
}

.form-group label {
  font-size: 0.8rem;
  color: #cbd5e1;
}

.form-group input,
.form-group select {
  background: #0f172a;
  border: 1px solid rgba(148, 163, 184, 0.25);
  border-radius: var(--radius-md);
  color: #f8fafc;
  padding: 0.7rem 0.85rem;
}

.results-container {
  flex: 1;
  overflow-y: auto;
  padding: var(--spacing-5);
}

.results-section h3 {
  margin-bottom: var(--spacing-4);
}

.movies-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: var(--spacing-4);
}

.movie-card {
  background: #111827;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.movie-thumbnail {
  width: 100%;
  height: 220px;
  object-fit: cover;
  display: block;
}

.movie-thumbnail-placeholder {
  height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #1f2937;
  color: #94a3b8;
}

.movie-info {
  padding: var(--spacing-4);
}

.movie-info h4 {
  margin-bottom: var(--spacing-2);
}

.movie-year,
.movie-genres,
.movie-cast,
.movie-extract {
  margin-bottom: var(--spacing-2);
  color: #cbd5e1;
}

.no-results {
  color: #cbd5e1;
}

@media (max-width: 1024px) {
  .main-container {
    flex-direction: column;
    height: auto;
    min-height: 100vh;
  }

  .workshop-panel {
    width: 100%;
    min-width: 0;
    max-height: 45vh;
  }
}
</style>
