<template>
  <div class="search-demo">
    <div class="main-container">
      <!-- Left Sidebar: Tutorial Info -->
      <div class="workshop-panel">
        <div class="workshop-header">
          <h2><img src="@/assets/logo/small.png" alt="Redis Logo" width="28" height="28" class="logo-small" /> Test Your Search</h2>
        </div>
        <div class="workshop-content">
          <h3>Congratulations!</h3>
          <p>You've successfully implemented full-text search with Redis. Now let's test what you built.</p>

          <!-- Test Step 1: Basic Search -->
          <div v-if="testStep === 1" class="test-step">
            <h4>Test 1: Basic Title Search</h4>
            <p>Type <code>matrix</code> in the <strong>Title</strong> field and click Search.</p>
            <p class="observation"><strong>Observe:</strong> Look at the search time in the results header. Redis Query Engine typically returns results in just a few milliseconds - even when searching through thousands of movies!</p>
            <button @click="completeStep(1)" class="btn btn-primary">Completed - Next Test</button>
          </div>

          <!-- Test Step 2: Full-Text Search -->
          <div v-else-if="testStep === 2" class="test-step">
            <h4>Test 2: Full-Text Description Search</h4>
            <p>Clear the Title field. Type <code>space adventure</code> in the <strong>Description</strong> field.</p>
            <p class="observation"><strong>Observe:</strong> Full-text search finds movies where the description contains these words, even if they're not adjacent. This is the power of <code>@Searchable</code> fields with stemming and relevance scoring.</p>
            <button @click="completeStep(2)" class="btn btn-primary">Completed - Next Test</button>
          </div>

          <!-- Test Step 3: Fuzzy Matching -->
          <div v-else-if="testStep === 3" class="test-step">
            <h4>Test 3: Fuzzy Text Matching</h4>
            <p>Clear all fields. Type <code>Marty discovers his friend was trapped in 1885</code> in the <strong>Description</strong> field.</p>
            <p class="observation"><strong>Observe:</strong> This matches "Marty McFly (Fox) discovers that his friend Dr. Emmett 'Doc' Brown (Lloyd), trapped in 1885" - even though the search text isn't an exact substring! Full-text search tokenizes and matches individual words, ignoring extra words in between.</p>
            <button @click="completeStep(3)" class="btn btn-primary">Completed - Next Test</button>
          </div>

          <!-- Test Step 4: Combined Filters -->
          <div v-else-if="testStep === 4" class="test-step">
            <h4>Test 4: Combined Filters</h4>
            <p>Select a <strong>Genre</strong> (e.g., "sci-fi") AND enter a <strong>Year</strong> (e.g., 1999).</p>
            <p class="observation"><strong>Observe:</strong> Multiple filters combine with AND logic. The <code>@Indexed</code> fields (year, genres) enable exact match filtering, while <code>@Searchable</code> fields enable full-text search. You can mix both!</p>
            <button @click="completeStep(4)" class="btn btn-primary">Completed - Next Test</button>
          </div>

          <!-- Test Step 5: Actor Search -->
          <div v-else-if="testStep === 5" class="test-step">
            <h4>Test 5: Actor Search</h4>
            <p>Clear all fields. Type an actor name like <code>Keanu Reeves</code> in the <strong>Actor</strong> field.</p>
            <p class="observation"><strong>Observe:</strong> The cast field is <code>@Indexed</code>, enabling exact filtering on array elements. Redis can efficiently search within arrays stored in JSON documents.</p>
            <button @click="completeStep(5)" class="btn btn-primary">Completed - Next Test</button>
          </div>

          <!-- Test Step 6: Redis Insight -->
          <div v-else-if="testStep === 6" class="test-step">
            <h4>Test 6: Explore the Index in Redis Insight</h4>
            <p>Open <a :href="redisInsightUrl" target="_blank" class="link">Redis Insight</a>, click on <strong>Workbench</strong>, and run:</p>
            <p><code>FT.INFO "com.redis.workshop.search.domain.MovieIdx"</code></p>
            <p class="observation"><strong>Observe the index structure:</strong></p>
            <ul>
              <li><strong>TEXT fields</strong> (title, extract) - Enable full-text search with stemming</li>
              <li><strong>NUMERIC fields</strong> (year) - Enable range queries and sorting (note SORTABLE is enabled)</li>
              <li><strong>TAG fields</strong> (cast, genres, id) - Enable exact match filtering on discrete values</li>
              <li><strong>SEPARATOR "|"</strong> on TAG fields - Allows multiple values in a single field</li>
              <li><strong>Number of docs/terms</strong> at the bottom - Shows how many documents are indexed</li>
            </ul>
            <button @click="completeStep(6)" class="btn btn-primary">Completed - Finish</button>
          </div>

          <!-- All Tests Complete -->
          <div v-else class="test-complete">
            <h4>All Tests Complete!</h4>
            <div class="alert alert-success">You've explored the key features of Redis Query Engine.</div>

            <h4>Key Takeaways</h4>
            <ul>
              <li><code>@Searchable</code> - Full-text search with stemming and fuzzy matching</li>
              <li><code>@Indexed</code> - Exact filtering and sorting</li>
              <li>EntityStream builds server-side queries (not in-memory filtering)</li>
              <li>Millisecond response times even with large datasets</li>
            </ul>

            <div class="button-row">
              <button @click="restartTests" class="btn btn-primary">Restart Tests</button>
              <button @click="restartLab" class="btn btn-warning" :disabled="restartingLab">
                {{ restartingLab ? 'Restoring files...' : 'Restart Lab' }}
              </button>
            </div>
          </div>

          <div class="nav-links">
            <router-link to="/" class="nav-link">← Back to Workshop Home</router-link>
            <router-link to="/editor" class="nav-link">Open Code Editor →</router-link>
          </div>
        </div>
      </div>

      <!-- Right Section: Search -->
      <div class="search-section">
        <div class="search-header">
          <form @submit.prevent="searchMovies" class="search-form">
            <div class="form-row-inline">
              <div class="form-group">
                <label for="title">Title</label>
                <input type="text" id="title" v-model="searchParams.title" placeholder="e.g., Matrix">
              </div>
              <div class="form-group">
                <label for="extract">Description</label>
                <input type="text" id="extract" v-model="searchParams.extract" placeholder="e.g., science fiction">
              </div>
              <div class="form-group">
                <label for="actors">Actor</label>
                <input type="text" id="actors" v-model="searchParams.actors" placeholder="e.g., Keanu Reeves">
              </div>
              <div class="form-group small">
                <label for="year">Year</label>
                <input type="number" id="year" v-model.number="searchParams.year" placeholder="1999">
              </div>
              <div class="form-group small">
                <label for="genres">Genre</label>
                <select id="genres" v-model="searchParams.genres">
                  <option value="">All</option>
                  <option v-for="genre in availableGenres" :key="genre" :value="genre">{{ genre }}</option>
                </select>
              </div>
              <button type="submit" class="btn btn-primary" :disabled="loading">{{ loading ? 'Searching...' : 'Search' }}</button>
            </div>
          </form>
          <div v-if="error" class="alert alert-error">{{ error }}</div>
        </div>

        <!-- Results -->
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
import { getBasePath } from "../utils/basePath";

const STORAGE_KEY = 'fullTextSearchWorkshop';

export default {
  name: 'SearchDemo',
  data() {
    return {
      searchParams: { title: '', extract: '', actors: '', year: null, genres: '' },
      movies: [],
      availableGenres: [],
      searchTime: null,
      loading: false,
      error: null,
      searched: false,
      restartingLab: false,
      testStep: 1
    };
  },
  computed: {
    basePath() {
      return getBasePath();
    },
    redisInsightUrl() {
      return 'http://localhost:5540';
    }
  },
  async mounted() {
    this.loadTestStep();
    await this.fetchGenres();
  },
  methods: {
    loadTestStep() {
      const saved = localStorage.getItem(STORAGE_KEY);
      if (saved) {
        try {
          const data = JSON.parse(saved);
          this.testStep = data.testStep || 1;
        } catch (e) {
          this.testStep = 1;
        }
      }
    },
    saveTestStep() {
      const saved = localStorage.getItem(STORAGE_KEY);
      let data = {};
      if (saved) {
        try { data = JSON.parse(saved); } catch (e) { data = {}; }
      }
      data.testStep = this.testStep;
      localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
    },
    completeStep(step) {
      this.testStep = step + 1;
      this.saveTestStep();
    },
    restartTests() {
      this.testStep = 1;
      this.saveTestStep();
    },
    async fetchGenres() {
      try {
        const response = await fetch(`${this.basePath}/api/genres`);
        if (response.ok) {
          const data = await response.json();
          this.availableGenres = data.genres || [];
        }
      } catch (error) {
        console.error('Error fetching genres:', error);
      }
    },
    hasFilters() {
      return this.searchParams.title || this.searchParams.extract || this.searchParams.actors || this.searchParams.year || this.searchParams.genres;
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
        const response = await fetch(`${this.basePath}/api/search?${params}`);
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
    truncate(text, length) {
      if (!text) return '';
      return text.length > length ? text.substring(0, length) + '...' : text;
    },
    async restartLab() {
      if (!confirm('Are you sure you want to restart the lab? This will restore all files to their original state. You will need to restart the application after this.')) {
        return;
      }
      this.restartingLab = true;
      try {
        const response = await fetch(`${this.basePath}/api/editor/restore`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          credentials: 'include'
        });
        const data = await response.json();
        if (data.success) {
          localStorage.removeItem(STORAGE_KEY);
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
.search-demo { margin: 0; padding: 0; background: #1a1a1a; overflow: hidden; }
.main-container { display: flex; height: 100vh; width: 100vw; }
.workshop-panel { width: 320px; min-width: 320px; background: #1e1e1e; border-right: 1px solid var(--color-border); display: flex; flex-direction: column; overflow: hidden; }
.workshop-header { background: #252526; padding: var(--spacing-4); border-bottom: 1px solid var(--color-border); }
.workshop-header h2 { margin: 0; color: #DC382C; font-size: var(--font-size-lg); display: flex; align-items: center; gap: var(--spacing-2); }
.logo-small { display: inline-block; }
.workshop-content { flex: 1; overflow-y: auto; padding: var(--spacing-5); color: #cccccc; font-size: var(--font-size-sm); line-height: 1.5; }
.workshop-content h3 { color: #fff; font-size: var(--font-size-base); margin-top: 0; margin-bottom: var(--spacing-2); }
.workshop-content h4 { color: #4fc3f7; font-size: var(--font-size-sm); margin-top: var(--spacing-5); margin-bottom: var(--spacing-2); font-weight: var(--font-weight-semibold); }
.workshop-content ul { padding-left: var(--spacing-5); margin-bottom: var(--spacing-3); }
.workshop-content li { margin-bottom: var(--spacing-1); }
.workshop-content code { background: #2d2d2d; padding: 0.15rem 0.3rem; border-radius: var(--radius-sm); color: #ce9178; font-size: 0.8rem; }
.workshop-content p { margin-bottom: var(--spacing-2); }
.link { color: #569cd6; text-decoration: none; }
.link:hover { text-decoration: underline; }
.note { color: #999; font-size: 0.85rem; margin-bottom: var(--spacing-3); }
.test-step { background: #252526; border-radius: var(--radius-md); padding: var(--spacing-4); margin-top: var(--spacing-4); }
.test-step h4 { margin-top: 0; color: #4fc3f7; }
.test-step .observation { background: rgba(79, 195, 247, 0.1); border-left: 3px solid #4fc3f7; padding: var(--spacing-3); margin: var(--spacing-3) 0; }
.test-step .btn { margin-top: var(--spacing-3); }
.test-complete { margin-top: var(--spacing-4); }
.test-complete h4 { margin-top: var(--spacing-4); }
.test-complete h4:first-child { margin-top: 0; }
.alert-success { background: rgba(34, 197, 94, 0.1); border: 1px solid #22c55e; border-radius: var(--radius-md); padding: var(--spacing-3); color: #22c55e; margin-bottom: var(--spacing-3); }
.button-row { display: flex; gap: var(--spacing-3); margin-top: var(--spacing-4); }
.nav-links { margin-top: var(--spacing-4); padding-top: var(--spacing-3); border-top: 1px solid var(--color-border); display: flex; flex-direction: column; gap: var(--spacing-2); }
.nav-link { color: var(--color-text-secondary); text-decoration: none; font-size: var(--font-size-sm); }
.nav-link:hover { color: #DC382C; }
.search-section { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.search-header { background: #252526; padding: var(--spacing-4); border-bottom: 1px solid var(--color-border); }
.search-form { margin: 0; }
.form-row-inline { display: flex; gap: var(--spacing-3); align-items: flex-end; flex-wrap: wrap; }
.form-row-inline .form-group { margin-bottom: 0; flex: 1; min-width: 120px; }
.form-row-inline .form-group.small { flex: 0 0 100px; min-width: 80px; }
.form-group label { display: block; margin-bottom: var(--spacing-1); font-size: var(--font-size-xs); font-weight: var(--font-weight-medium); color: var(--color-text-secondary); }
.form-group input, .form-group select { width: 100%; padding: var(--spacing-2); background: var(--color-dark-800); border: 1px solid var(--color-border); border-radius: var(--radius-md); color: var(--color-text); font-size: var(--font-size-sm); box-sizing: border-box; }
.form-group input:focus, .form-group select:focus { outline: none; border-color: #DC382C; }
.btn { padding: var(--spacing-2) var(--spacing-4); border: none; border-radius: var(--radius-md); font-size: var(--font-size-sm); font-weight: var(--font-weight-semibold); cursor: pointer; white-space: nowrap; }
.btn-primary { background: #DC382C; color: white; }
.btn-primary:hover:not(:disabled) { background: #c42f24; }
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-warning { background: #e67e22; color: white; }
.btn-warning:hover:not(:disabled) { background: #d35400; }
.btn-warning:disabled { opacity: 0.6; cursor: not-allowed; }
.alert-error { margin-top: var(--spacing-3); padding: var(--spacing-2); background: rgba(239, 68, 68, 0.1); border: 1px solid #ef4444; border-radius: var(--radius-md); color: #ef4444; font-size: var(--font-size-sm); }
.results-container { flex: 1; overflow-y: auto; padding: var(--spacing-4); }
.results-section h3 { color: var(--color-text); margin-bottom: var(--spacing-4); font-size: var(--font-size-base); }
.movies-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: var(--spacing-4); }
.movie-card { background: var(--color-surface); border-radius: var(--radius-lg); overflow: hidden; border: 1px solid var(--color-border); transition: transform 0.2s; display: flex; flex-direction: row; }
.movie-card:hover { transform: translateY(-2px); box-shadow: var(--shadow-lg); }
.movie-thumbnail { width: 150px; min-width: 150px; height: auto; min-height: 200px; object-fit: cover; }
.movie-thumbnail-placeholder { width: 150px; min-width: 150px; height: auto; min-height: 200px; background: var(--color-dark-800); display: flex; align-items: center; justify-content: center; color: var(--color-text-secondary); font-size: var(--font-size-sm); }
.movie-info { padding: var(--spacing-4); flex: 1; }
.movie-info h4 { color: var(--color-text); font-size: var(--font-size-base); margin: 0 0 var(--spacing-2); }
.movie-year { color: #DC382C; font-size: var(--font-size-sm); margin: 0 0 var(--spacing-2); font-weight: var(--font-weight-semibold); }
.movie-genres { color: var(--color-text-secondary); font-size: var(--font-size-sm); margin: 0 0 var(--spacing-2); }
.movie-cast { color: var(--color-text-secondary); font-size: var(--font-size-sm); margin: 0 0 var(--spacing-3); font-style: italic; }
.movie-extract { color: var(--color-text-secondary); font-size: var(--font-size-sm); line-height: 1.5; margin: 0; }
.no-results { text-align: center; padding: var(--spacing-6); color: var(--color-text-secondary); background: var(--color-surface); border-radius: var(--radius-lg); border: 1px solid var(--color-border); }
@media (max-width: 1800px) {
  .movies-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 1200px) {
  .movies-grid { grid-template-columns: 1fr; }
}
@media (max-width: 1024px) {
  .workshop-panel { width: 280px; min-width: 280px; }
  .form-row-inline { flex-wrap: wrap; }
}
@media (max-width: 768px) {
  .main-container { flex-direction: column; }
  .workshop-panel { width: 100%; min-width: 100%; max-height: 35vh; }
  .form-row-inline .form-group { min-width: 100px; }
  .movie-card { flex-direction: column; }
  .movie-thumbnail, .movie-thumbnail-placeholder { width: 100%; min-width: 100%; height: 180px; min-height: 180px; }
}
</style>
