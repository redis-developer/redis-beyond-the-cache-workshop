<template>
  <div class="search-home">
    <div class="welcome-container" :class="{ 'search-mode': currentStage === 4 }">
      <!-- Left Column: Instructions -->
      <div class="instructions-column">
        <!-- STAGE 1: Introduction -->
        <div v-if="currentStage === 1" class="instructions">
          <div class="section-header">
            <img src="@/assets/logo/small.png" alt="Redis Logo" width="32" height="32" />
            <h2>Secondary Indexes &amp; Full-Text Search</h2>
          </div>

          <h3>What is Redis Query Engine?</h3>
          <p>Redis Query Engine adds indexing and querying capabilities to Redis. It lets you create secondary indexes on JSON documents and Hashes, then search and filter that data using the Redis Query Engine query language.</p>          <h3>What are Secondary Indexes?</h3>
          <p>A secondary index allows you to query data by fields other than the primary key. Redis Query Engine supports:</p>
          <ul>
            <li><strong>TAG</strong> - Exact match (genre, status, category)</li>
            <li><strong>NUMERIC</strong> - Range queries (year between 1990-2000)</li>
            <li><strong>TEXT</strong> - Full-text search</li>
            <li><strong>GEO</strong> - Location-based queries</li>
            <li><strong>VECTOR</strong> - Similarity search on embeddings</li>
          </ul>

          <h3>What is Full-Text Search?</h3>
          <p>Full-text search (TEXT index) goes beyond exact matching:</p>
          <ul>
            <li><strong>Stemming</strong> - "running" matches "run", "runs", "ran"</li>
            <li><strong>Fuzzy matching</strong> - "Matirx" still finds "Matrix"</li>
            <li><strong>Relevance scoring</strong> - Best matches ranked first</li>
          </ul>

          <h3>Why are They Necessary?</h3>
          <p>Redis is a key-value store. You can retrieve data instantly by key, but querying by other fields (year, genre, description) requires scanning all records. Secondary indexes solve this by creating lookup structures on those fields.</p>

          <h3>Why Use Redis?</h3>
          <ul>
            <li><strong>Speed</strong> - Sub-millisecond queries. Data and indexes live in memory.</li>
            <li><strong>Simplicity</strong> - Data and search index in one database. No syncing, no ETL, no separate infrastructure.</li>
            <li><strong>Real-time</strong> - Indexes update immediately when data changes. No reindexing delay.</li>
          </ul>

          <h3>Getting Started</h3>
          <ol>
            <li><strong>Continue</strong> to see how secondary indexes work with a concrete example</li>
            <li><strong>Implement</strong> indexes using Redis OM Spring annotations</li>
            <li><strong>Test</strong> your Movie Search Engine</li>
          </ol>

          <button @click="nextStage" class="btn btn-primary">Continue →</button>
        </div>

        <!-- STAGE 2: Understanding the Problem -->
        <div v-if="currentStage === 2" class="instructions">
          <div class="section-header">
            <img src="@/assets/logo/small.png" alt="Redis Logo" width="32" height="32" />
            <h2>How Indexes Work</h2>
          </div>

          <h3>Our Movie Data</h3>
          <p>Each movie is stored as a JSON document:</p>
          <div class="code-block">
            <pre>{
  "id": "tt0133093",
  "title": "The Matrix",
  "year": 1999,
  "genres": ["Action", "Sci-Fi"],
  "cast": ["Keanu Reeves", "Laurence Fishburne"],
  "extract": "A computer hacker learns about the true nature of reality..."
}</pre>
          </div>

          <h3>Queries We Want to Support</h3>
          <ul>
            <li>"matrix" → finds "The Matrix" (text search on title)</li>
            <li>"hacker" → finds movies with "hacker" in description (text search on extract)</li>
            <li>"Keanu Reeves" → finds all his movies (tag filter on cast)</li>
            <li>1999 → finds movies from that year (numeric filter on year)</li>
            <li>Combine: action movies from the 1990s</li>
          </ul>

          <h3>Index Structure</h3>
          <p>Redis Query Engine builds indexes that map field values to document keys:</p>
          <div class="code-block">
            <pre>Title Index (TEXT):
  "matrix" → [movie:tt0133093, movie:tt0234215]
  "inception" → [movie:tt1375666]

Year Index (NUMERIC):
  1999 → [movie:tt0133093, movie:tt0234215]
  2010 → [movie:tt1375666]

Cast Index (TAG):
  "Keanu Reeves" → [movie:tt0133093, movie:tt0234215]</pre>
          </div>

          <h3>Index Types for This Workshop</h3>
          <ul>
            <li><strong>TEXT</strong> on <code>title</code> and <code>extract</code> - full-text search with stemming</li>
            <li><strong>TAG</strong> on <code>genres</code> and <code>cast</code> - exact match filtering</li>
            <li><strong>NUMERIC</strong> on <code>year</code> - range queries</li>
          </ul>

          <div class="button-group">
            <button @click="prevStage" class="btn btn-secondary">← Back</button>
            <button @click="nextStage" class="btn btn-primary">Continue →</button>
          </div>
        </div>

        <!-- STAGE 3: Implementation Steps -->
        <div v-if="currentStage === 3" class="instructions">
          <div class="section-header">
            <img src="@/assets/logo/small.png" alt="Redis Logo" width="32" height="32" />
            <h2>Enable Full-Text Search</h2>
          </div>

          <div class="alert alert-info">
            <strong>Option 1: Use the In-Browser Code Editor</strong>
            <p style="margin: 0.5rem 0 0 0;">
              <router-link to="/editor" class="editor-link">Open Code Editor →</router-link>
            </p>
          </div>

          <p class="intro"><strong>Option 2: Edit files in your IDE</strong></p>

          <div class="step-item">
            <h4>Step 1: Add Dependencies</h4>
            <p class="step-description">Open <code>build.gradle.kts</code> and uncomment the Redis OM Spring dependencies: <code>redis-om-spring</code> implementation and annotation processor.</p>
          </div>

          <div class="step-item">
            <h4>Step 2: Annotate the Movie Entity</h4>
            <p class="step-description">Open <code>Movie.java</code>, uncomment the imports, and add annotations: <code>@Document</code> on the class, <code>@Id</code> on id, <code>@Searchable</code> on title/extract, and <code>@Indexed</code> on year/cast/genres.</p>
          </div>

          <div class="step-item">
            <h4>Step 3: Configure Redis Connection</h4>
            <p class="step-description">Open <code>application.properties</code> and uncomment the Redis host and port configuration.</p>
          </div>

          <div class="step-item">
            <h4>Step 4: Enable Document Repositories</h4>
            <p class="step-description">Open <code>FullTextSearchApplication.java</code> and uncomment the <code>@EnableRedisDocumentRepositories</code> import and annotation.</p>
          </div>

          <div class="step-item">
            <h4>Step 5: Implement the Repository</h4>
            <p class="step-description">Open <code>MovieRepository.java</code> and uncomment the RedisDocumentRepository import and change the interface to extend it.</p>
          </div>

          <div class="step-item">
            <h4>Step 6: Implement MovieService</h4>
            <p class="step-description">Open <code>MovieService.java</code> and uncomment the MovieRepository field, constructor parameter, and the loadAndSaveMovies/isDataLoaded implementations.</p>
          </div>

          <div class="step-item">
            <h4>Step 7: Implement SearchService</h4>
            <p class="step-description">Open <code>SearchService.java</code> and uncomment the imports, fields, constructor, and the search/getAllGenres implementations.</p>
          </div>

          <div class="step-item">
            <h4>Step 8: Rebuild &amp; Restart</h4>
            <p class="step-description">Go to the Workshop Hub and click "Rebuild &amp; Restart" to compile your code and start the application with Redis Query Engine enabled.</p>
          </div>

          <div class="button-group">
            <button @click="prevStage" class="btn btn-secondary">← Back</button>
            <router-link to="/search" class="btn btn-primary">Test Your Implementation →</router-link>
          </div>
        </div>

        <!-- STAGE 4: Removed - redirects to /search instead -->
        <div v-if="false" class="instructions">
          <div class="section-header">
            <img src="@/assets/logo/small.png" alt="Redis Logo" width="32" height="32" />
            <h2>Test & Explore</h2>
          </div>

          <div class="alert alert-success">
            <strong>Great work!</strong> Your full-text search implementation is complete. Now let's test it!
          </div>

          <h3>Try the Search</h3>
          <p>Use the search interface on the right to test your implementation:</p>
          <ul>
            <li>Search for "matrix" in the title</li>
            <li>Try a typo: "matirx" (fuzzy matching)</li>
            <li>Search for "hacker" in the description</li>
            <li>Filter by actor: "Keanu Reeves"</li>
            <li>Filter by year: 1999</li>
            <li>Combine multiple filters</li>
          </ul>

          <h3>Explore with Redis Insight</h3>
          <p>Want to see what's happening in Redis? Use Redis Insight to explore:</p>
          <ol>
            <li>Open Redis Insight (if available in your workshop environment)</li>
            <li>Connect to <code>localhost:6379</code></li>
            <li>Look for keys starting with <code>com.redis.workshop.search.domain.Movie:</code></li>
            <li>Click on "Indexes" to see the secondary indexes created by Redis Query Engine</li>
            <li>You'll see indexes for title, extract, year, cast, and genres</li>
          </ol>

          <h3>What You've Learned</h3>
          <ul>
            <li>How full-text search works with indexes</li>
            <li>The difference between primary keys and secondary indexes</li>
            <li>How Redis Query Engine creates indexes on JSON documents</li>
            <li>How to use Redis OM Spring annotations (@Document, @Searchable, @Indexed)</li>
            <li>How EntityStream builds server-side queries (not in-memory filtering)</li>
            <li>How to combine full-text search with exact filters</li>
          </ul>

          <h3>Next Steps</h3>
          <p>Try extending the application:</p>
          <ul>
            <li>Add a new searchable field (e.g., director)</li>
            <li>Implement autocomplete suggestions</li>
            <li>Add pagination to handle large result sets</li>
            <li>Experiment with different search operators (AND, OR, NOT)</li>
          </ul>

          <div class="alert alert-info" style="margin-top: 1.5rem;">
            <strong>Want to try again?</strong> Restore all files to their original state and restart the workshop.
          </div>

          <div class="button-group">
            <button @click="prevStage" class="btn btn-secondary">← Back</button>
            <button @click="restartLab" class="btn btn-warning" :disabled="restartingLab">
              {{ restartingLab ? 'Restoring files...' : 'Restart Lab' }}
            </button>
            <button @click="currentStage = 1" class="btn btn-primary">Start Over</button>
          </div>
        </div>
      </div>

      <!-- Right Column: Removed - search interface is now on /search page -->
      <div class="search-column" v-if="false">
        <div class="search-container">
          <h3>Movie Search</h3>

          <div class="search-form">
            <div class="form-group">
              <label>Title</label>
              <input
                v-model="searchParams.title"
                type="text"
                class="form-control"
                placeholder="e.g., Matrix"
              />
            </div>

            <div class="form-group">
              <label>Description</label>
              <input
                v-model="searchParams.extract"
                type="text"
                class="form-control"
                placeholder="e.g., hacker"
              />
            </div>

            <div class="form-group">
              <label>Actor</label>
              <input
                v-model="actorInput"
                type="text"
                class="form-control"
                placeholder="e.g., Keanu Reeves"
              />
            </div>

            <div class="form-group">
              <label>Year</label>
              <input
                v-model.number="searchParams.year"
                type="number"
                class="form-control"
                placeholder="e.g., 1999"
              />
            </div>

            <div class="form-group">
              <label>Genre</label>
              <select v-model="genreInput" class="form-control">
                <option value="">All Genres</option>
                <option v-for="genre in genres" :key="genre" :value="genre">
                  {{ genre }}
                </option>
              </select>
            </div>

            <button @click="search" class="btn btn-primary btn-block">
              Search Movies
            </button>
          </div>

          <div v-if="searchResults" class="search-results">
            <div class="results-header">
              <strong>{{ searchResults.count }} movies found</strong>
              <span class="search-time">({{ searchResults.searchTime }}ms)</span>
            </div>

            <div
              v-for="movie in searchResults.movies"
              :key="movie.id"
              class="movie-card"
            >
              <h4>{{ movie.title }} ({{ movie.year }})</h4>
              <p class="movie-genres">
                <span
                  v-for="genre in movie.genres"
                  :key="genre"
                  class="genre-tag"
                >
                  {{ genre }}
                </span>
              </p>
              <p class="movie-cast">
                <strong>Cast:</strong> {{ movie.cast.join(", ") }}
              </p>
              <p class="movie-extract">{{ movie.extract }}</p>
            </div>

            <div v-if="searchResults.count === 0" class="no-results">
              No movies found. Try different search criteria.
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Custom Modal -->
    <div v-if="modal.show" class="modal-overlay" @click.self="closeModal">
      <div class="modal-container">
        <div class="modal-header">
          <h3>{{ modal.title }}</h3>
        </div>
        <div class="modal-body">
          <p v-for="(line, index) in modal.message.split('\n')" :key="index">{{ line }}</p>
        </div>
        <div class="modal-footer">
          <button v-if="modal.type === 'confirm'" @click="closeModal" class="btn btn-outline">Cancel</button>
          <button @click="confirmModal" class="btn btn-primary">{{ modal.type === 'confirm' ? 'Confirm' : 'OK' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from "axios";
import { getBasePath } from "../utils/basePath";

export default {
  name: "SearchHome",
  data() {
    return {
      currentStage: 1,
      searchParams: {
        title: "",
        extract: "",
        actors: [],
        year: null,
        genres: [],
      },
      actorInput: "",
      genreInput: "",
      genres: [],
      searchResults: null,
      restartingLab: false,
      modal: {
        show: false,
        type: 'alert',
        title: '',
        message: '',
        onConfirm: null
      }
    };
  },
  mounted() {
    this.loadGenres();
  },
  computed: {
    basePath() {
      return getBasePath();
    }
  },
  methods: {
    nextStage() {
      if (this.currentStage < 4) {
        this.currentStage++;
      }
    },
    prevStage() {
      if (this.currentStage > 1) {
        this.currentStage--;
      }
    },
    async loadGenres() {
      try {
        const response = await axios.get(`${this.basePath}/api/genres`);
        const genresData = response.data.genres || response.data || [];
        this.genres = Array.isArray(genresData) ? genresData : [];
      } catch (error) {
        console.error("Error loading genres:", error);
      }
    },
    async search() {
      try {
        // Build actors array
        const actors = this.actorInput
          ? this.actorInput.split(",").map((a) => a.trim())
          : [];

        // Build genres array
        const genres = this.genreInput ? [this.genreInput] : [];

        const params = {
          title: this.searchParams.title || "",
          text: this.searchParams.extract || "",
          cast: actors.join(","),
          year: this.searchParams.year || "",
          genres: genres.join(","),
        };

        const response = await axios.get(`${this.basePath}/api/search`, { params });
        this.searchResults = response.data;
      } catch (error) {
        console.error("Error searching movies:", error);
        alert("Error searching movies. Make sure Redis is running and the application is properly configured.");
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
    closeModal() {
      this.modal.show = false;
      this.modal.onConfirm = null;
    },
    confirmModal() {
      if (this.modal.onConfirm) {
        this.modal.onConfirm();
      }
      this.closeModal();
    },
    async restartLab() {
      this.showModal('confirm', 'Restart Lab', 'Are you sure you want to restart the lab? This will restore all files to their original state and reset your progress. You will need to restart the application after this.', async () => {
        this.restartingLab = true;
        try {
          const response = await axios.post(`${this.basePath}/api/editor/restore`);

          if (response.data.success) {
            this.currentStage = 1;
            this.searchResults = null;
            this.showModal('alert', 'Lab Reset', 'Lab reset! Please go to the Workshop Hub and rebuild & restart the application.\n\nThen refresh this page to start from Stage 1.');
          } else {
            this.showModal('alert', 'Error', 'Error: ' + (response.data.error || 'Failed to restore files'));
          }
        } catch (error) {
          console.error('Error restarting lab:', error);
          this.showModal('alert', 'Error', 'Failed to restore files. Please try again.');
        } finally {
          this.restartingLab = false;
        }
      });
    },
  },
};
</script>


<style scoped>
.search-home {
  min-height: 100vh;
  background: linear-gradient(135deg, var(--color-dark-900) 0%, var(--color-dark-800) 100%);
  padding: var(--spacing-6);
}

.welcome-container {
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  gap: var(--spacing-6);
}

.instructions-column {
  flex: 1;
  min-width: 0;
}

.search-column {
  flex: 0 0 400px;
  position: sticky;
  top: var(--spacing-6);
  height: fit-content;
}

/* Search mode: make search column larger */
.search-mode .instructions-column {
  flex: 0 0 350px;
}

.search-mode .search-column {
  flex: 1;
  min-width: 0;
}

.instructions {
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  padding: var(--spacing-6);
  box-shadow: var(--shadow-xl);
  border: 1px solid var(--color-border);
}

.section-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
  margin-bottom: var(--spacing-5);
  padding-bottom: var(--spacing-4);
  border-bottom: 1px solid var(--color-border);
}

.instructions h2 {
  color: var(--color-text);
  margin: 0;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
}

.instructions h3 {
  color: #DC382C;
  margin-top: var(--spacing-6);
  margin-bottom: var(--spacing-4);
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
}

.instructions h4 {
  color: var(--color-text);
  margin-top: var(--spacing-5);
  margin-bottom: var(--spacing-3);
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
}

.instructions p {
  line-height: 1.6;
  margin-bottom: var(--spacing-4);
  color: var(--color-text-secondary);
}

.instructions ul,
.instructions ol {
  margin-bottom: var(--spacing-4);
  padding-left: var(--spacing-6);
  color: var(--color-text-secondary);
}

.instructions li {
  margin-bottom: var(--spacing-2);
  line-height: 1.6;
}

.instructions code {
  background: var(--color-dark-700);
  padding: 2px 6px;
  border-radius: 3px;
  font-family: "Courier New", monospace;
  font-size: 0.9em;
  color: #DC382C;
}

.code-block {
  background: var(--color-dark-800);
  border-left: 4px solid #DC382C;
  padding: var(--spacing-4);
  margin: var(--spacing-4) 0;
  border-radius: var(--radius-md);
  overflow-x: auto;
}

.code-block pre {
  margin: 0;
  font-family: "Courier New", monospace;
  font-size: 0.9em;
  line-height: 1.5;
  color: var(--color-text);
}

.alert {
  padding: var(--spacing-4);
  border-radius: var(--radius-md);
  margin-bottom: var(--spacing-5);
  border: 1px solid;
}

.alert-info {
  background: #094771;
  border-color: #17a2b8;
  color: var(--color-text);
}

.alert-success {
  background: rgba(16, 185, 129, 0.1);
  border-color: #10b981;
  color: var(--color-text);
}

.alert-warning {
  background: rgba(245, 158, 11, 0.1);
  border-color: #f59e0b;
  color: var(--color-text);
}

.alert strong {
  color: var(--color-text);
}

.editor-link {
  color: #4fc3f7;
  text-decoration: none;
  font-weight: var(--font-weight-medium);
}

.editor-link:hover {
  text-decoration: underline;
}

.info-box {
  background: rgba(23, 162, 184, 0.1);
  border: 1px solid #17a2b8;
  border-radius: var(--radius-md);
  padding: var(--spacing-4);
  margin: var(--spacing-5) 0;
}

.info-box h4 {
  margin-top: 0;
  color: var(--color-text);
}

.step-item {
  margin: var(--spacing-6) 0;
  padding: var(--spacing-5);
  background: var(--color-dark-800);
  border-radius: var(--radius-md);
  border-left: 4px solid #DC382C;
}

.step-item h4 {
  margin-top: 0;
  color: var(--color-text);
  font-weight: var(--font-weight-semibold);
}

.button-group {
  display: flex;
  gap: var(--spacing-3);
  margin-top: var(--spacing-6);
}

.btn {
  padding: var(--spacing-3) var(--spacing-5);
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  transition: all 0.2s;
}

.btn-primary {
  background: #DC382C;
  color: white;
}

.btn-primary:hover {
  background: #c42f24;
}

.btn-secondary {
  background: var(--color-dark-800);
  color: var(--color-text);
  border: 1px solid var(--color-border);
}

.btn-secondary:hover {
  background: var(--color-border);
}

.btn-warning {
  background: #f59e0b;
  color: white;
}

.btn-warning:hover {
  background: #d97706;
}

.btn-block {
  width: 100%;
}

/* Search Interface */
.search-container {
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  padding: var(--spacing-6);
  box-shadow: var(--shadow-xl);
  border: 1px solid var(--color-border);
}

.search-container h3 {
  color: var(--color-text);
  margin-bottom: var(--spacing-5);
  font-size: var(--font-size-lg);
}

.search-form {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-4);
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

.form-control {
  background: var(--color-dark-800);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--spacing-3);
  color: var(--color-text);
  font-size: var(--font-size-base);
}

.form-control:focus {
  outline: none;
  border-color: #DC382C;
}

.form-control::placeholder {
  color: var(--color-text-secondary);
  opacity: 0.5;
}

select.form-control {
  cursor: pointer;
}

/* Search Results */
.search-results {
  margin-top: var(--spacing-6);
}

.results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-4);
}

.results-header h4 {
  color: var(--color-text);
  margin: 0;
}

.results-count {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.movie-card {
  background: var(--color-dark-800);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--spacing-4);
  margin-bottom: var(--spacing-3);
  transition: all 0.2s;
}

.movie-card:hover {
  border-color: #DC382C;
  transform: translateY(-2px);
}

.movie-card h5 {
  color: var(--color-text);
  margin: 0 0 var(--spacing-2) 0;
  font-size: var(--font-size-base);
}

.movie-card p {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  margin: var(--spacing-1) 0;
  line-height: 1.5;
}

.genre-tag {
  display: inline-block;
  background: rgba(220, 56, 44, 0.2);
  color: #DC382C;
  padding: 0.25rem 0.5rem;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  margin-right: var(--spacing-2);
  margin-top: var(--spacing-1);
}

.no-results {
  text-align: center;
  padding: var(--spacing-6);
  color: var(--color-text-secondary);
}

/* Responsive */
@media (max-width: 968px) {
  .welcome-container {
    flex-direction: column;
  }

  .search-column {
    flex: 1;
    position: static;
  }
}

/* Modal Styles */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
}

.modal-container {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  max-width: 450px;
  width: 90%;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
}

.modal-header {
  padding: var(--spacing-4) var(--spacing-5);
  border-bottom: 1px solid var(--color-border);
}

.modal-header h3 {
  margin: 0;
  font-size: var(--font-size-lg);
  color: var(--color-text);
}

.modal-body {
  padding: var(--spacing-5);
}

.modal-body p {
  margin: 0 0 var(--spacing-2) 0;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.modal-body p:last-child {
  margin-bottom: 0;
}

.modal-footer {
  padding: var(--spacing-4) var(--spacing-5);
  border-top: 1px solid var(--color-border);
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-3);
}
</style>
