<template>
  <div class="search-editor">
    <div class="main-container">
      <!-- Workshop Instructions Panel -->
      <div class="workshop-panel">
        <div class="workshop-header">
          <h2>
            <div class="logo-small">
              <img src="@/assets/logo/small.png" alt="Redis Logo" width="24" height="24" />
            </div>
            Full-Text Search Workshop
          </h2>
        </div>
        <div class="workshop-content">
          <div class="alert">
            <strong>Your Task:</strong> Configure Redis OM Spring to enable full-text search on movie data.
          </div>

          <h3>Instructions:</h3>
          <p class="note">
            Click the play button (▶) next to any step to automatically apply that change!
          </p>

          <h4>Step 1: Add Redis OM Dependencies</h4>
          <ol>
            <li class="step-with-button">
              <span class="step-content">Click on <code>build.gradle.kts</code> tab above</span>
              <div class="button-group">
                <span class="tooltip-wrapper" data-tooltip="Opens the Gradle build file where dependencies are managed">
                  <span class="info-icon">i</span>
                </span>
                <button class="play-btn" @click="loadFileStep('build.gradle.kts', 1)">▶</button>
              </div>
            </li>
            <li class="step-with-button">
              <span class="step-content">Uncomment the Redis OM Spring dependencies</span>
              <div class="button-group">
                <span class="tooltip-wrapper" data-tooltip="Redis OM Spring provides object mapping, repository support, and full-text search capabilities">
                  <span class="info-icon">i</span>
                </span>
                <button class="play-btn" @click="uncommentGradleDependencies">▶</button>
              </div>
            </li>
            <li class="step-with-button">
              <span class="step-content">Click <strong>Save Changes</strong></span>
              <div class="button-group">
                <button class="play-btn" @click="saveFile">▶</button>
              </div>
            </li>
          </ol>

          <h4>Step 2: Configure Redis Connection</h4>
          <ol start="4">
            <li class="step-with-button">
              <span class="step-content">Click on <code>application.properties</code> tab</span>
              <div class="button-group">
                <button class="play-btn" @click="loadFileStep('application.properties', 4)">▶</button>
              </div>
            </li>
            <li class="step-with-button">
              <span class="step-content">Uncomment the Redis host and port configuration</span>
              <div class="button-group">
                <span class="tooltip-wrapper" data-tooltip="Configure the connection to your Redis instance">
                  <span class="info-icon">i</span>
                </span>
                <button class="play-btn" @click="uncommentRedisConfig">▶</button>
              </div>
            </li>
            <li class="step-with-button">
              <span class="step-content">Click <strong>Save Changes</strong></span>
              <div class="button-group">
                <button class="play-btn" @click="saveFile">▶</button>
              </div>
            </li>
          </ol>

          <h4>Step 3: Add Search Annotations to Movie</h4>
          <ol start="7">
            <li class="step-with-button">
              <span class="step-content">Click on <code>Movie.java</code> tab</span>
              <div class="button-group">
                <button class="play-btn" @click="loadFileStep('Movie.java', 7)">▶</button>
              </div>
            </li>
            <li class="step-with-button">
              <span class="step-content">Add <code>@Document</code> annotation to the class</span>
              <div class="button-group">
                <span class="tooltip-wrapper" data-tooltip="@Document marks this class as a Redis document stored as JSON. Redis Query Engine will create an index for this document type.">
                  <span class="info-icon">i</span>
                </span>
                <button class="play-btn" @click="addDocumentAnnotation">▶</button>
              </div>
            </li>
            <li class="step-with-button">
              <span class="step-content">Add <code>@Id</code> annotation to the id field</span>
              <div class="button-group">
                <span class="tooltip-wrapper" data-tooltip="@Id marks the primary key field for the document. This is the unique identifier used to store and retrieve the document.">
                  <span class="info-icon">i</span>
                </span>
                <button class="play-btn" @click="addIdAnnotation">▶</button>
              </div>
            </li>
            <li class="step-with-button">
              <span class="step-content">Add <code>@Searchable</code> annotations to title and extract</span>
              <div class="button-group">
                <span class="tooltip-wrapper" data-tooltip="@Searchable enables full-text search with stemming and fuzzy matching. Use this for fields where users will search for words or phrases.">
                  <span class="info-icon">i</span>
                </span>
                <button class="play-btn" @click="addSearchableAnnotations">▶</button>
              </div>
            </li>
            <li class="step-with-button">
              <span class="step-content">Add <code>@Indexed</code> annotations to year, cast, and genres</span>
              <div class="button-group">
                <span class="tooltip-wrapper" data-tooltip="@Indexed enables exact filtering and sorting on the field. Use this for fields where users will filter by exact values (numbers, tags, categories).">
                  <span class="info-icon">i</span>
                </span>
                <button class="play-btn" @click="addIndexedAnnotations">▶</button>
              </div>
            </li>
            <li class="step-with-button">
              <span class="step-content">Click <strong>Save Changes</strong></span>
              <div class="button-group">
                <button class="play-btn" @click="saveFile">▶</button>
              </div>
            </li>
          </ol>

          <h4>Step 4: Enable Redis Document Repositories</h4>
          <ol start="10">
            <li class="step-with-button">
              <span class="step-content">Click on <code>FullTextSearchApplication.java</code> tab</span>
              <div class="button-group">
                <button class="play-btn" @click="loadFileStep('FullTextSearchApplication.java', 13)">▶</button>
              </div>
            </li>
            <li class="step-with-button">
              <span class="step-content">Uncomment <code>@EnableRedisDocumentRepositories</code> annotation</span>
              <div class="button-group">
                <span class="tooltip-wrapper" data-tooltip="This enables automatic index creation and repository implementation generation">
                  <span class="info-icon">i</span>
                </span>
                <button class="play-btn" @click="enableRedisRepositories">▶</button>
              </div>
            </li>
            <li class="step-with-button">
              <span class="step-content">Click <strong>Save Changes</strong></span>
              <div class="button-group">
                <button class="play-btn" @click="saveFile">▶</button>
              </div>
            </li>
          </ol>

          <h4>Step 5: Extend RedisDocumentRepository</h4>
          <ol start="13">
            <li class="step-with-button">
              <span class="step-content">Click on <code>MovieRepository.java</code> tab</span>
              <div class="button-group">
                <button class="play-btn" @click="loadFileStep('MovieRepository.java', 16)">▶</button>
              </div>
            </li>
            <li class="step-with-button">
              <span class="step-content">Uncomment the repository interface extension</span>
              <div class="button-group">
                <span class="tooltip-wrapper" data-tooltip="RedisDocumentRepository provides CRUD operations and query methods">
                  <span class="info-icon">i</span>
                </span>
                <button class="play-btn" @click="extendRedisRepository">▶</button>
              </div>
            </li>
            <li class="step-with-button">
              <span class="step-content">Click <strong>Save Changes</strong></span>
              <div class="button-group">
                <button class="play-btn" @click="saveFile">▶</button>
              </div>
            </li>
          </ol>

          <h4>Step 6: Enable MovieService</h4>
          <ol start="16">
            <li class="step-with-button">
              <span class="step-content">Click on <code>MovieService.java</code> tab</span>
              <div class="button-group">
                <button class="play-btn" @click="loadFileStep('MovieService.java', 19)">▶</button>
              </div>
            </li>
            <li class="step-with-button">
              <span class="step-content">Uncomment the <code>MovieRepository</code> field and constructor parameter</span>
              <div class="button-group">
                <span class="tooltip-wrapper" data-tooltip="Inject the MovieRepository so we can save movies to Redis">
                  <span class="info-icon">i</span>
                </span>
                <button class="play-btn" @click="enableMovieServiceRepository">▶</button>
              </div>
            </li>
            <li class="step-with-button">
              <span class="step-content">Uncomment the <code>loadAndSaveMovies()</code> implementation</span>
              <div class="button-group">
                <span class="tooltip-wrapper" data-tooltip="This method loads movies from JSON and saves them to Redis. Redis OM Spring automatically indexes them based on the @Document annotations.">
                  <span class="info-icon">i</span>
                </span>
                <button class="play-btn" @click="enableLoadAndSaveMovies">▶</button>
              </div>
            </li>
            <li class="step-with-button">
              <span class="step-content">Uncomment the <code>isDataLoaded()</code> implementation</span>
              <div class="button-group">
                <span class="tooltip-wrapper" data-tooltip="This method checks if movies are already in Redis to avoid reloading on every restart">
                  <span class="info-icon">i</span>
                </span>
                <button class="play-btn" @click="enableIsDataLoaded">▶</button>
              </div>
            </li>
            <li class="step-with-button">
              <span class="step-content">Click <strong>Save Changes</strong></span>
              <div class="button-group">
                <button class="play-btn" @click="saveFile">▶</button>
              </div>
            </li>
          </ol>

          <h4>Step 7: Enable SearchService</h4>
          <ol start="21">
            <li class="step-with-button">
              <span class="step-content">Click on <code>SearchService.java</code> tab</span>
              <div class="button-group">
                <button class="play-btn" @click="loadFileStep('SearchService.java', 21)">▶</button>
              </div>
            </li>
            <li class="step-with-button">
              <span class="step-content">Uncomment imports and inject <code>EntityStream</code> and <code>MovieRepository</code></span>
              <div class="button-group">
                <span class="tooltip-wrapper" data-tooltip="EntityStream is the core API for building Redis queries. Movie$ is auto-generated and provides type-safe field references.">
                  <span class="info-icon">i</span>
                </span>
                <button class="play-btn" @click="enableSearchServiceDependencies">▶</button>
              </div>
            </li>
            <li class="step-with-button">
              <span class="step-content">Uncomment the <code>searchMovies()</code> EntityStream implementation</span>
              <div class="button-group">
                <span class="tooltip-wrapper" data-tooltip="EntityStream builds server-side queries (NOT in-memory filtering). Use .containing() for full-text search and .eq() for exact matches.">
                  <span class="info-icon">i</span>
                </span>
                <button class="play-btn" @click="enableSearchMoviesImplementation">▶</button>
              </div>
            </li>
            <li class="step-with-button">
              <span class="step-content">Uncomment the <code>getAllGenres()</code> implementation</span>
              <div class="button-group">
                <span class="tooltip-wrapper" data-tooltip="This uses the custom repository method to fetch all unique genres from the indexed movies">
                  <span class="info-icon">i</span>
                </span>
                <button class="play-btn" @click="enableGetAllGenres">▶</button>
              </div>
            </li>
            <li class="step-with-button">
              <span class="step-content">Click <strong>Save Changes</strong></span>
              <div class="button-group">
                <button class="play-btn" @click="saveFile">▶</button>
              </div>
            </li>
          </ol>

          <h4 ref="testStep">Step 8: Restart and Test</h4>
          <ol start="26">
            <li>Go to the <a :href="workshopHubUrl" target="_blank" style="color: #569cd6;">Workshop Hub</a> and rebuild the app</li>
            <li><router-link to="/search" class="link">Return to search</router-link> to test!</li>
          </ol>

          <div v-if="workshopComplete" class="completion-banner">
            All steps completed! Restart the app to test your search.
          </div>
        </div>
      </div>

      <!-- Editor Panel -->
      <div class="editor-container">
        <div class="file-tabs">
          <button
            v-for="file in files"
            :key="file"
            :class="['file-tab', { active: currentFile === file }]"
            @click="loadFile(file)"
          >
            {{ file }}
          </button>
        </div>

        <div class="editor-panel">
          <div class="editor-header">
            <h3>{{ currentFile || 'Select a file to edit' }}</h3>
            <div class="editor-actions">
              <button @click="saveFile" class="btn btn-primary" :disabled="!currentFile">Save Changes</button>
              <button @click="reloadFile" class="btn btn-secondary" :disabled="!currentFile">Reload</button>
            </div>
          </div>
          <div class="code-editor-wrapper">
            <div class="editor-container-inner">
              <pre class="code-highlight"><code :class="languageClass" v-html="highlightedCode"></code></pre>
              <textarea
                ref="codeTextarea"
                v-model="fileContent"
                class="code-editor"
                :disabled="!currentFile"
                spellcheck="false"
                @scroll="syncScroll"
              ></textarea>
            </div>
          </div>
          <div v-if="statusMessage" :class="['status-message', statusType]">
            {{ statusMessage }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getBasePath } from "../utils/basePath";
import hljs from 'highlight.js/lib/core';
import java from 'highlight.js/lib/languages/java';
import kotlin from 'highlight.js/lib/languages/kotlin';
import properties from 'highlight.js/lib/languages/properties';

hljs.registerLanguage('java', java);
hljs.registerLanguage('kotlin', kotlin);
hljs.registerLanguage('properties', properties);

const STORAGE_KEY = 'fullTextSearchWorkshop';

export default {
  name: 'SearchEditor',
  data() {
    return {
      files: [
        'build.gradle.kts',
        'application.properties',
        'Movie.java',
        'FullTextSearchApplication.java',
        'MovieRepository.java',
        'MovieService.java',
        'SearchService.java'
      ],
      currentFile: null,
      fileContent: '',
      fileLanguage: '',
      statusMessage: '',
      statusType: '',
      currentStep: 1,
      workshopComplete: false,
      fileContents: {} // Cache for checking completion
    };
  },
  async mounted() {
    // Load saved progress
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved) {
      try {
        const data = JSON.parse(saved);
        this.currentStep = data.currentStep || 1;
      } catch (e) {
        console.error('Failed to load saved progress', e);
      }
    }
    // Check if workshop is already complete
    await this.checkWorkshopCompletion();
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
    languageClass() {
      const langMap = { 'java': 'language-java', 'kotlin': 'language-kotlin', 'properties': 'language-properties' };
      return langMap[this.fileLanguage] || '';
    },
    highlightedCode() {
      if (!this.fileContent) return '';
      try {
        if (this.fileLanguage && hljs.getLanguage(this.fileLanguage)) {
          return hljs.highlight(this.fileContent, { language: this.fileLanguage }).value;
        }
        return this.escapeHtml(this.fileContent);
      } catch (e) {
        return this.escapeHtml(this.fileContent);
      }
    }
  },
  methods: {
    escapeHtml(text) {
      const div = document.createElement('div');
      div.textContent = text;
      return div.innerHTML;
    },
    syncScroll() {
      const textarea = this.$refs.codeTextarea;
      const pre = textarea?.parentElement?.querySelector('.code-highlight');
      if (pre) {
        pre.scrollTop = textarea.scrollTop;
        pre.scrollLeft = textarea.scrollLeft;
      }
    },
    saveProgress(step) {
      this.currentStep = step;
      localStorage.setItem(STORAGE_KEY, JSON.stringify({ currentStep: step }));
    },
    async fetchFileContent(fileName) {
      try {
        const url = `${this.basePath}/api/editor/file/${fileName}`;
        const response = await fetch(url, { credentials: 'include' });
        const data = await response.json();
        return data.content || '';
      } catch (e) {
        return '';
      }
    },
    async checkWorkshopCompletion() {
      // Fetch all files to check completion status
      const filesToCheck = ['build.gradle.kts', 'application.properties', 'Movie.java',
        'FullTextSearchApplication.java', 'MovieRepository.java', 'MovieService.java', 'SearchService.java'];

      for (const file of filesToCheck) {
        this.fileContents[file] = await this.fetchFileContent(file);
      }

      // Check if all key implementations are complete
      const checks = [
        // build.gradle.kts - Redis OM Spring dependency uncommented
        !this.fileContents['build.gradle.kts']?.includes('// implementation("com.redis.om:redis-om-spring'),
        // application.properties - Redis config uncommented
        !this.fileContents['application.properties']?.includes('# spring.data.redis.host'),
        // Movie.java - @Document annotation added
        this.fileContents['Movie.java']?.includes('@Document'),
        // FullTextSearchApplication.java - @EnableRedisDocumentRepositories added
        this.fileContents['FullTextSearchApplication.java']?.includes('@EnableRedisDocumentRepositories'),
        // MovieRepository.java - extends RedisDocumentRepository
        this.fileContents['MovieRepository.java']?.includes('extends RedisDocumentRepository'),
        // MovieService.java - repository field uncommented
        this.fileContents['MovieService.java']?.includes('private final MovieRepository movieRepository;'),
        // SearchService.java - EntityStream implementation uncommented
        this.fileContents['SearchService.java']?.includes('private final EntityStream entityStream;')
      ];

      this.workshopComplete = checks.every(Boolean);

      if (this.workshopComplete) {
        this.saveProgress(26); // Test step
        this.$nextTick(() => {
          this.scrollToTestStep();
        });
      }
    },
    scrollToTestStep() {
      const testStep = this.$el.querySelector('h4:last-of-type');
      if (testStep) {
        testStep.scrollIntoView({ behavior: 'smooth', block: 'start' });
        this.showStatus('Workshop complete! Restart the app and test your search.', 'success');
      }
    },
    async loadFile(fileName) {
      try {
        const url = `${this.basePath}/api/editor/file/${fileName}`;
        const response = await fetch(url, { credentials: 'include' });
        const responseText = await response.text();
        let data;
        try {
          data = JSON.parse(responseText);
        } catch (e) {
          this.showStatus('Server returned invalid response', 'error');
          return;
        }
        if (data.error) {
          this.showStatus(data.error, 'error');
          return;
        }
        this.currentFile = fileName;
        this.fileContent = data.content;
        this.fileLanguage = data.language || this.getLanguageFromFile(fileName);
        this.showStatus('File loaded successfully', 'success');
      } catch (error) {
        this.showStatus('Failed to load file: ' + error.message, 'error');
      }
    },
    async saveFile() {
      if (!this.currentFile) return;
      try {
        const response = await fetch(`${this.basePath}/api/editor/file/${this.currentFile}`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ content: this.fileContent }),
          credentials: 'include'
        });
        const data = await response.json();
        if (data.error) {
          this.showStatus(data.error, 'error');
        } else {
          this.showStatus('File saved! Restart the app to see changes.', 'success');
        }
      } catch (error) {
        this.showStatus('Failed to save file: ' + error.message, 'error');
      }
    },
    reloadFile() {
      if (this.currentFile) this.loadFile(this.currentFile);
    },
    showStatus(message, type) {
      this.statusMessage = message;
      this.statusType = type;
      setTimeout(() => { this.statusMessage = ''; this.statusType = ''; }, 3000);
    },
    async loadFileStep(fileName, step = null) {
      await this.loadFile(fileName);
      this.showStatus(`Opened ${fileName}`, 'success');
      if (step) this.saveProgress(step);
    },
    uncommentGradleDependencies() {
      if (this.currentFile !== 'build.gradle.kts') {
        this.showStatus('Please open build.gradle.kts first!', 'error');
        return;
      }
      this.fileContent = this.fileContent.replace(
        '// implementation("com.redis.om:redis-om-spring:1.0.4")',
        'implementation("com.redis.om:redis-om-spring:1.0.4")'
      );
      this.fileContent = this.fileContent.replace(
        '// annotationProcessor("com.redis.om:redis-om-spring:1.0.4")',
        'annotationProcessor("com.redis.om:redis-om-spring:1.0.4")'
      );
      this.showStatus('Redis OM dependencies uncommented! Click Save!', 'success');
      this.saveProgress(2);
    },
    uncommentRedisConfig() {
      if (this.currentFile !== 'application.properties') {
        this.showStatus('Please open application.properties first!', 'error');
        return;
      }
      this.fileContent = this.fileContent.replace('# spring.data.redis.host=localhost', 'spring.data.redis.host=localhost');
      this.fileContent = this.fileContent.replace('# spring.data.redis.port=6379', 'spring.data.redis.port=6379');
      this.showStatus('Redis config uncommented! Click Save!', 'success');
      this.saveProgress(5);
    },
    addDocumentAnnotation() {
      if (this.currentFile !== 'Movie.java') {
        this.showStatus('Please open Movie.java first!', 'error');
        return;
      }
      // Uncomment Document import
      this.fileContent = this.fileContent.replace('// import com.redis.om.spring.annotations.Document;', 'import com.redis.om.spring.annotations.Document;');
      // Add @Document
      this.fileContent = this.fileContent.replace('// TODO: Add @Document annotation here\npublic class Movie', '@Document\npublic class Movie');
      this.showStatus('@Document annotation added! Click Save!', 'success');
      this.saveProgress(8);
    },
    addIdAnnotation() {
      if (this.currentFile !== 'Movie.java') {
        this.showStatus('Please open Movie.java first!', 'error');
        return;
      }
      // Uncomment Id import
      this.fileContent = this.fileContent.replace('// import org.springframework.data.annotation.Id;', 'import org.springframework.data.annotation.Id;');
      // Add @Id
      this.fileContent = this.fileContent.replace('// TODO: Add @Id annotation here\n    private String id', '@Id\n    private String id');
      this.showStatus('@Id annotation added! Click Save!', 'success');
      this.saveProgress(9);
    },
    addSearchableAnnotations() {
      if (this.currentFile !== 'Movie.java') {
        this.showStatus('Please open Movie.java first!', 'error');
        return;
      }
      // Uncomment Searchable import
      this.fileContent = this.fileContent.replace('// import com.redis.om.spring.annotations.Searchable;', 'import com.redis.om.spring.annotations.Searchable;');
      // Add @Searchable to title
      this.fileContent = this.fileContent.replace('// TODO: Add @Searchable annotation for full-text search on title\n    private String title', '@Searchable\n    private String title');
      // Add @Searchable to extract
      this.fileContent = this.fileContent.replace('// TODO: Add @Searchable annotation for full-text search on extract\n    private String extract', '@Searchable\n    private String extract');
      this.showStatus('@Searchable annotations added! Click Save!', 'success');
      this.saveProgress(10);
    },
    addIndexedAnnotations() {
      if (this.currentFile !== 'Movie.java') {
        this.showStatus('Please open Movie.java first!', 'error');
        return;
      }
      // Uncomment Indexed import
      this.fileContent = this.fileContent.replace('// import com.redis.om.spring.annotations.Indexed;', 'import com.redis.om.spring.annotations.Indexed;');
      // Add @Indexed to year
      this.fileContent = this.fileContent.replace('// TODO: Add @Indexed(sortable = true) for year filtering and sorting\n    private int year', '@Indexed(sortable = true)\n    private int year');
      // Add @Indexed to cast
      this.fileContent = this.fileContent.replace('// TODO: Add @Indexed for cast filtering\n    private List<String> cast', '@Indexed\n    private List<String> cast');
      // Add @Indexed to genres
      this.fileContent = this.fileContent.replace('// TODO: Add @Indexed for genres filtering\n    private List<String> genres', '@Indexed\n    private List<String> genres');
      this.showStatus('@Indexed annotations added! Click Save!', 'success');
      this.saveProgress(11);
    },
    getLanguageFromFile(fileName) {
      if (fileName.endsWith('.java')) return 'java';
      if (fileName.endsWith('.kts') || fileName.endsWith('.kt')) return 'kotlin';
      if (fileName.endsWith('.properties')) return 'properties';
      return '';
    },
    enableRedisRepositories() {
      if (this.currentFile !== 'FullTextSearchApplication.java') {
        this.showStatus('Please open FullTextSearchApplication.java first!', 'error');
        return;
      }
      // Uncomment import
      this.fileContent = this.fileContent.replace(
        '// import com.redis.om.spring.annotations.EnableRedisDocumentRepositories;',
        'import com.redis.om.spring.annotations.EnableRedisDocumentRepositories;'
      );
      // Uncomment annotation
      this.fileContent = this.fileContent.replace(
        '// TODO: Add @EnableRedisDocumentRepositories',
        '@EnableRedisDocumentRepositories'
      );
      this.showStatus('@EnableRedisDocumentRepositories added! Click Save!', 'success');
      this.saveProgress(14);
    },
    extendRedisRepository() {
      if (this.currentFile !== 'MovieRepository.java') {
        this.showStatus('Please open MovieRepository.java first!', 'error');
        return;
      }
      // Uncomment import
      this.fileContent = this.fileContent.replace(
        '// import com.redis.om.spring.repository.RedisDocumentRepository;',
        'import com.redis.om.spring.repository.RedisDocumentRepository;\nimport org.springframework.stereotype.Repository;'
      );
      // Replace interface declaration
      this.fileContent = this.fileContent.replace(
        '// TODO: Uncomment the line below\n// @Repository\n// public interface MovieRepository extends RedisDocumentRepository<Movie, String> {\npublic interface MovieRepository {',
        '@Repository\npublic interface MovieRepository extends RedisDocumentRepository<Movie, String> {'
      );
      // Uncomment getAllGenres method
      this.fileContent = this.fileContent.replace(
        '// Iterable<String> getAllGenres();',
        'Iterable<String> getAllGenres();'
      );
      this.showStatus('MovieRepository extended! Click Save!', 'success');
      this.saveProgress(17);
    },
    enableMovieServiceRepository() {
      if (this.currentFile !== 'MovieService.java') {
        this.showStatus('Please open MovieService.java first!', 'error');
        return;
      }
      // Uncomment field
      this.fileContent = this.fileContent.replace(
        '// TODO: Uncomment after implementing MovieRepository\n    // private final MovieRepository movieRepository;',
        'private final MovieRepository movieRepository;'
      );
      // Uncomment constructor parameter
      this.fileContent = this.fileContent.replace(
        'public MovieService(ObjectMapper objectMapper, ResourceLoader resourceLoader /* , MovieRepository movieRepository */) {\n        this.objectMapper = objectMapper;\n        this.resourceLoader = resourceLoader;\n        // this.movieRepository = movieRepository;',
        'public MovieService(ObjectMapper objectMapper, ResourceLoader resourceLoader, MovieRepository movieRepository) {\n        this.objectMapper = objectMapper;\n        this.resourceLoader = resourceLoader;\n        this.movieRepository = movieRepository;'
      );
      this.showStatus('MovieRepository injected! Click Save!', 'success');
      this.saveProgress(20);
    },
    enableLoadAndSaveMovies() {
      if (this.currentFile !== 'MovieService.java') {
        this.showStatus('Please open MovieService.java first!', 'error');
        return;
      }
      // Uncomment loadAndSaveMovies implementation
      this.fileContent = this.fileContent.replace(
        '// TODO: Uncomment after implementing MovieRepository\n        /*\n        Resource resource = resourceLoader.getResource("classpath:" + filePath);\n        try (InputStream is = resource.getInputStream()) {\n            List<Movie> movies = objectMapper.readValue(is, new TypeReference<>() {});\n            long startTime = System.currentTimeMillis();\n            movieRepository.saveAll(movies);\n            long elapsedMillis = System.currentTimeMillis() - startTime;\n            log.info("Saved {} movies in {} ms", movies.size(), elapsedMillis);\n        }\n        */\n        log.info("MovieService.loadAndSaveMovies() - Redis OM Spring not yet configured");',
        'Resource resource = resourceLoader.getResource("classpath:" + filePath);\n        try (InputStream is = resource.getInputStream()) {\n            List<Movie> movies = objectMapper.readValue(is, new TypeReference<>() {});\n            long startTime = System.currentTimeMillis();\n            movieRepository.saveAll(movies);\n            long elapsedMillis = System.currentTimeMillis() - startTime;\n            log.info("Saved {} movies in {} ms", movies.size(), elapsedMillis);\n        }'
      );
      this.showStatus('loadAndSaveMovies() enabled! Click Save!', 'success');
      this.saveProgress(21);
    },
    enableIsDataLoaded() {
      if (this.currentFile !== 'MovieService.java') {
        this.showStatus('Please open MovieService.java first!', 'error');
        return;
      }
      // Uncomment isDataLoaded implementation
      this.fileContent = this.fileContent.replace(
        '// TODO: Uncomment after implementing MovieRepository\n        // return movieRepository.count() > 0;\n        return false;',
        'return movieRepository.count() > 0;'
      );
      this.showStatus('isDataLoaded() enabled! Click Save!', 'success');
      this.saveProgress(22);
    },
    enableSearchServiceDependencies() {
      if (this.currentFile !== 'SearchService.java') {
        this.showStatus('Please open SearchService.java first!', 'error');
        return;
      }
      // Uncomment imports
      this.fileContent = this.fileContent.replace(
        '// import com.redis.workshop.search.domain.Movie$;',
        'import com.redis.workshop.search.domain.Movie$;'
      );
      this.fileContent = this.fileContent.replace(
        '// import com.redis.om.spring.search.stream.EntityStream;',
        'import com.redis.om.spring.search.stream.EntityStream;'
      );
      this.fileContent = this.fileContent.replace(
        '// import com.redis.om.spring.search.stream.SearchStream;',
        'import com.redis.om.spring.search.stream.SearchStream;'
      );
      this.fileContent = this.fileContent.replace(
          '// import static redis.clients.jedis.search.aggr.SortedField.SortOrder.DESC;',
          'import static redis.clients.jedis.search.aggr.SortedField.SortOrder.DESC;'
      );

      // Uncomment fields
      this.fileContent = this.fileContent.replace(
        '// TODO: Uncomment these fields after adding Redis OM Spring dependency\n    // private final EntityStream entityStream;\n    // private final MovieRepository movieRepository;',
        'private final EntityStream entityStream;\n    private final MovieRepository movieRepository;'
      );
      // Uncomment constructor
      this.fileContent = this.fileContent.replace(
        '// TODO: Update constructor to inject EntityStream and MovieRepository after adding Redis OM Spring\n    public SearchService(/* EntityStream entityStream, MovieRepository movieRepository */) {\n        // this.entityStream = entityStream;\n        // this.movieRepository = movieRepository;',
        'public SearchService(EntityStream entityStream, MovieRepository movieRepository) {\n        this.entityStream = entityStream;\n        this.movieRepository = movieRepository;'
      );
      this.showStatus('EntityStream and MovieRepository injected! Click Save!', 'success');
      this.saveProgress(23);
    },
    enableSearchMoviesImplementation() {
      if (this.currentFile !== 'SearchService.java') {
        this.showStatus('Please open SearchService.java first!', 'error');
        return;
      }
      // Uncomment search implementation
      const replacement =
        'SearchStream<Movie> stream = entityStream.of(Movie.class);\n\n' +
        '        // Build the search query with filters\n' +
        '        // containing() - full-text search on @Searchable fields\n' +
        '        // eq() - exact match on @Indexed fields\n' +
        '        List<Movie> matchedMovies = stream\n' +
        '                .filter(Movie$.TITLE.containing(title))\n' +
        '                .filter(Movie$.EXTRACT.containing(extract))\n' +
        '                .filter(Movie$.CAST.eq(actors))\n' +
        '                .filter(Movie$.YEAR.eq(year))\n' +
        '                .filter(Movie$.GENRES.eq(genres))\n' +
        '                .sorted(Movie$.YEAR, DESC)\n' +
        '                .collect(Collectors.toList());';
      const original = this.fileContent;
      let updated = original.replace(
        '// TODO: Implement search using EntityStream\n        // SearchStream<Movie> stream = entityStream.of(Movie.class);\n        //\n        // Build the search query with filters\n        // containing() - full-text search on @Searchable fields\n        // eq() - exact match on @Indexed fields\n        // List<Movie> matchedMovies = stream\n        //         .filter(Movie$.TITLE.containing(title))\n        //         .filter(Movie$.EXTRACT.containing(extract))\n        //         .filter(Movie$.CAST.eq(actors))\n        //         .filter(Movie$.YEAR.eq(year))\n        //         .filter(Movie$.GENRES.eq(genres))\n        //         .sorted(Movie$.YEAR, DESC)\n        //         .collect(Collectors.toList());\n\n        // Temporary: Return empty results until Redis OM Spring is configured\n        List<Movie> matchedMovies = new ArrayList<>();',
        replacement
      );
      if (updated === original) {
        const pattern = /\/\/ TODO: Implement search using EntityStream[\s\S]*?List<Movie> matchedMovies = new ArrayList<>\(\);/;
        updated = original.replace(pattern, replacement);
      }
      if (updated === original) {
        this.showStatus('Could not find the searchMovies() block to uncomment.', 'error');
        return;
      }
      this.fileContent = updated;
      this.showStatus('searchMovies() implementation enabled! Click Save!', 'success');
      this.saveProgress(24);
    },
    enableGetAllGenres() {
      if (this.currentFile !== 'SearchService.java') {
        this.showStatus('Please open SearchService.java first!', 'error');
        return;
      }
      // Uncomment getAllGenres implementation - replace the entire TODO block including the temporary code
      this.fileContent = this.fileContent.replace(
        '// TODO: Uncomment after implementing MovieRepository\n        // Iterable<String> genresIterable = movieRepository.getAllGenres();\n        // Set<String> allGenres = new HashSet<>();\n        // genresIterable.forEach(allGenres::add);\n\n        // Temporary: Return empty set until Redis OM Spring is configured\n        Set<String> allGenres = new HashSet<>();',
        'Iterable<String> genresIterable = movieRepository.getAllGenres();\n        Set<String> allGenres = new HashSet<>();\n        genresIterable.forEach(allGenres::add);'
      );
      this.showStatus('getAllGenres() enabled! Click Save!', 'success');
      this.saveProgress(25);
    }
  }
};
</script>

<style scoped>
.search-editor { margin: 0; padding: 0; background: #1a1a1a; overflow: hidden; }
.main-container { display: flex; height: 100vh; width: 100vw; }
.workshop-panel { width: 400px; background: #1e1e1e; border-right: 1px solid var(--color-border); display: flex; flex-direction: column; overflow: hidden; }
.workshop-header { background: #252526; padding: var(--spacing-4); border-bottom: 1px solid var(--color-border); }
.workshop-header h2 { margin: 0; color: #DC382C; font-size: var(--font-size-lg); display: flex; align-items: center; gap: var(--spacing-2); }
.logo-small { display: inline-block; }
.workshop-content { flex: 1; overflow-y: auto; padding: var(--spacing-6); color: #cccccc; font-size: var(--font-size-sm); line-height: 1.6; }
.workshop-content h3 { color: #fff; font-size: var(--font-size-base); margin-top: 0; }
.workshop-content h4 { color: #4fc3f7; font-size: var(--font-size-sm); margin-top: var(--spacing-6); margin-bottom: var(--spacing-3); font-weight: var(--font-weight-semibold); }
.workshop-content ol { padding-left: var(--spacing-6); }
.workshop-content li { margin-bottom: var(--spacing-3); }
.step-with-button { display: flex; align-items: flex-start; gap: var(--spacing-2); margin-bottom: var(--spacing-3); }
.step-content { flex: 1; min-width: 0; word-break: break-word; }
.step-content code { word-break: break-all; }
.step-content ul { margin-top: var(--spacing-2); padding-left: var(--spacing-5); }
.button-group { display: flex; align-items: center; gap: var(--spacing-2); flex-shrink: 0; }
.tooltip-wrapper { position: relative; display: flex; align-items: center; cursor: help; }
.tooltip-wrapper::after { content: attr(data-tooltip); position: absolute; bottom: 100%; right: 0; background: #1e1e1e; color: #d4d4d4; padding: 8px 12px; border-radius: 4px; font-size: 0.8rem; white-space: normal; width: 220px; text-align: left; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3); border: 1px solid #3c3c3c; opacity: 0; visibility: hidden; transition: opacity 0.2s, visibility 0.2s; z-index: 1000; margin-bottom: 8px; line-height: 1.4; }
.tooltip-wrapper:hover::after { opacity: 1; visibility: visible; }
.info-icon { display: flex; align-items: center; justify-content: center; width: 18px; height: 18px; min-width: 18px; background: transparent; border: 1.5px solid #569cd6; border-radius: 50%; color: #569cd6; font-size: 0.7rem; font-weight: bold; font-style: italic; font-family: serif; opacity: 0.7; transition: opacity var(--transition-base); }
.tooltip-wrapper:hover .info-icon { opacity: 1; }
.play-btn { width: 28px; height: 28px; min-width: 28px; background: #1e7e34; color: white; border: none; border-radius: 50%; cursor: pointer; font-size: 0.7rem; transition: all var(--transition-base); display: flex; align-items: center; justify-content: center; flex-shrink: 0; padding: 0; margin-top: 2px; }
.play-btn:hover { background: #28a745; transform: scale(1.1); }
.play-btn:active { transform: scale(0.95); }
.workshop-content code { background: #2d2d2d; padding: 0.2rem 0.4rem; border-radius: var(--radius-sm); color: #ce9178; font-size: 0.85rem; }
.workshop-content .alert { padding: var(--spacing-4); border-radius: var(--radius-md); margin-bottom: var(--spacing-4); background: #094771; border-left: 3px solid #569cd6; }
.note { margin-bottom: var(--spacing-4); color: #999; font-size: 0.85rem; }
.link { color: #569cd6; text-decoration: none; }
.link:hover { text-decoration: underline; }
.editor-container { flex: 1; display: flex; flex-direction: column; }
.file-tabs { display: flex; background: #252526; border-bottom: 1px solid var(--color-border); overflow-x: auto; }
.file-tab { padding: var(--spacing-3) var(--spacing-6); background: transparent; border: none; color: #969696; cursor: pointer; transition: all var(--transition-base); border-right: 1px solid var(--color-border); white-space: nowrap; font-size: var(--font-size-sm); }
.file-tab:hover { background: #2a2d2e; color: #cccccc; }
.file-tab.active { background: #1e1e1e; color: #ffffff; border-bottom: 2px solid #DC382C; }
.editor-panel { flex: 1; display: flex; flex-direction: column; }
.editor-header { background: #252526; padding: var(--spacing-3) var(--spacing-4); display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--color-border); }
.editor-header h3 { color: #cccccc; margin: 0; font-size: var(--font-size-sm); }
.editor-actions { display: flex; gap: var(--spacing-2); }
.editor-actions button { padding: var(--spacing-2) var(--spacing-4); font-size: var(--font-size-sm); }
.code-editor-wrapper { flex: 1; width: 100%; height: 100%; overflow: hidden; }
.editor-container-inner { position: relative; width: 100%; height: 100%; }
.code-highlight { position: absolute; top: 0; left: 0; right: 0; bottom: 0; margin: 0; padding: var(--spacing-4); background: #1e1e1e; font-family: 'Consolas', 'Monaco', 'Courier New', monospace; font-size: 14px; line-height: 1.5; overflow: auto; pointer-events: none; white-space: pre; color: #d4d4d4; }
.code-highlight code { font-family: inherit; font-size: inherit; line-height: inherit; background: transparent; padding: 0; }
.code-editor { position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: transparent; color: transparent; caret-color: #fff; border: none; padding: var(--spacing-4); font-family: 'Consolas', 'Monaco', 'Courier New', monospace; font-size: 14px; line-height: 1.5; resize: none; z-index: 1; }
.code-editor:focus { outline: none; }
.code-editor:disabled { opacity: 0.5; cursor: not-allowed; }
.status-message { position: fixed; bottom: 20px; right: 20px; padding: var(--spacing-4) var(--spacing-6); background: #252526; color: #fff; text-align: center; font-size: var(--font-size-sm); border-radius: var(--radius-md); box-shadow: var(--shadow-xl); z-index: 1000; transition: opacity var(--transition-base); }
.status-message.success { background: #1e7e34; }
.status-message.error { background: #bd2130; }
.btn { padding: var(--spacing-2) var(--spacing-4); border: none; border-radius: var(--radius-md); font-size: var(--font-size-sm); font-weight: var(--font-weight-semibold); cursor: pointer; transition: all var(--transition-base); }
.btn-primary { background: #DC382C; color: white; }
.btn-primary:hover:not(:disabled) { background: #c42f24; }
.btn-secondary { background: var(--color-dark-800); color: var(--color-text); border: 1px solid var(--color-border); }
.btn-secondary:hover:not(:disabled) { background: var(--color-border); }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
@media (max-width: 1024px) { .workshop-panel { width: 300px; } }
@media (max-width: 768px) { .main-container { flex-direction: column; } .workshop-panel { width: 100%; max-height: 40vh; } }
.code-highlight :deep(.hljs-keyword) { color: #569cd6; }
.code-highlight :deep(.hljs-built_in) { color: #4ec9b0; }
.code-highlight :deep(.hljs-type) { color: #4ec9b0; }
.code-highlight :deep(.hljs-literal) { color: #569cd6; }
.code-highlight :deep(.hljs-number) { color: #b5cea8; }
.code-highlight :deep(.hljs-string) { color: #ce9178; }
.code-highlight :deep(.hljs-comment) { color: #6a9955; }
.code-highlight :deep(.hljs-class) { color: #4ec9b0; }
.code-highlight :deep(.hljs-function) { color: #dcdcaa; }
.code-highlight :deep(.hljs-title) { color: #dcdcaa; }
.code-highlight :deep(.hljs-title.function_) { color: #dcdcaa; }
.code-highlight :deep(.hljs-title.class_) { color: #4ec9b0; }
.code-highlight :deep(.hljs-params) { color: #9cdcfe; }
.code-highlight :deep(.hljs-variable) { color: #9cdcfe; }
.code-highlight :deep(.hljs-attr) { color: #9cdcfe; }
.code-highlight :deep(.hljs-property) { color: #9cdcfe; }
.code-highlight :deep(.hljs-punctuation) { color: #d4d4d4; }
.code-highlight :deep(.hljs-operator) { color: #d4d4d4; }
.code-highlight :deep(.hljs-meta) { color: #c586c0; }
.code-highlight :deep(.hljs-decorator) { color: #c586c0; }
.code-highlight :deep(.hljs-annotation) { color: #c586c0; }
.completion-banner { margin-top: var(--spacing-6); padding: var(--spacing-4); background: #1e7e34; color: white; border-radius: var(--radius-md); text-align: center; font-weight: var(--font-weight-semibold); }
</style>
