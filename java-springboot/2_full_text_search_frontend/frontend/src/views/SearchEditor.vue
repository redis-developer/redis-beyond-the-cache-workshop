<template>
  <WorkshopEditorLayout
    ref="layout"
    title="Full-Text Search Workshop"
    :files="files"
    @file-loaded="onFileLoaded"
  >
    <template #instructions>
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
        <li>Go to the <a :href="workshopHubUrl" target="_blank" class="link">Workshop Hub</a> and rebuild the app</li>
        <li><router-link to="/search" class="link">Return to search</router-link> to test!</li>
      </ol>

      <div v-if="workshopComplete" class="completion-banner">
        All steps completed! Restart the app to test your search.
      </div>
    </template>
  </WorkshopEditorLayout>
</template>

<script>
import { getApiUrl, getWorkshopHubUrl } from '../utils/basePath';
import { WorkshopEditorLayout } from '../utils/components';

const STORAGE_KEY = 'fullTextSearchWorkshop';

export default {
  name: 'SearchEditor',
  components: { WorkshopEditorLayout },
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
      currentStep: 1,
      workshopComplete: false,
      fileContents: {}
    };
  },
  async mounted() {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved) {
      try {
        const data = JSON.parse(saved);
        this.currentStep = data.currentStep || 1;
      } catch (e) {
        console.error('Failed to load saved progress', e);
      }
    }
    await this.checkWorkshopCompletion();
  },
  computed: {
    workshopHubUrl() {
      return getWorkshopHubUrl();
    }
  },
  methods: {
    ensureImport(content, commentedImport, importLine, anchorLine = null) {
      if (commentedImport && content.includes(commentedImport)) {
        return content.replace(commentedImport, importLine);
      }
      if (content.includes(importLine)) {
        return content;
      }
      if (anchorLine && content.includes(anchorLine)) {
        return content.replace(anchorLine, `${anchorLine}\n${importLine}`);
      }
      const packageMatch = content.match(/^(package [^\n]+;\n)/);
      if (packageMatch) {
        return content.replace(packageMatch[1], `${packageMatch[1]}\n${importLine}\n`);
      }
      return `${importLine}\n${content}`;
    },
    onFileLoaded({ fileName, content }) {
      this.currentFile = fileName;
      this.fileContent = content;
    },
    saveProgress(step) {
      this.currentStep = step;
      localStorage.setItem(STORAGE_KEY, JSON.stringify({ currentStep: step }));
    },
    async fetchFileContent(fileName) {
      try {
        const url = getApiUrl(`/api/editor/file/${fileName}`);
        const response = await fetch(url, { credentials: 'include' });
        const data = await response.json();
        return data.content || '';
      } catch (e) { return ''; }
    },
    async checkWorkshopCompletion() {
      const filesToCheck = ['build.gradle.kts', 'application.properties', 'Movie.java',
        'FullTextSearchApplication.java', 'MovieRepository.java', 'MovieService.java', 'SearchService.java'];
      for (const file of filesToCheck) { this.fileContents[file] = await this.fetchFileContent(file); }
      const checks = [
        !this.fileContents['build.gradle.kts']?.includes('// implementation("com.redis.om:redis-om-spring'),
        !this.fileContents['application.properties']?.includes('# spring.data.redis.host'),
        this.fileContents['Movie.java']?.includes('@Document'),
        this.fileContents['FullTextSearchApplication.java']?.includes('@EnableRedisDocumentRepositories'),
        this.fileContents['MovieRepository.java']?.includes('extends RedisDocumentRepository'),
        this.fileContents['MovieService.java']?.includes('private final MovieRepository movieRepository;'),
        this.fileContents['SearchService.java']?.includes('private final EntityStream entityStream;')
      ];
      this.workshopComplete = checks.every(Boolean);
      if (this.workshopComplete) {
        this.saveProgress(26);
        this.$nextTick(() => { this.scrollToTestStep(); });
      }
    },
    scrollToTestStep() {
      const testStep = this.$el.querySelector('h4:last-of-type');
      if (testStep) {
        testStep.scrollIntoView({ behavior: 'smooth', block: 'start' });
        this.$refs.layout.showStatus('Workshop complete! Restart the app and test your search.', 'success');
      }
    },
    async loadFileStep(fileName, step = null) {
      await this.$refs.layout.loadFile(fileName);
      if (step) this.saveProgress(step);
    },
    saveFile() { this.$refs.layout.save(); },
    getContent() { return this.$refs.layout.getCurrentContent(); },
    setContent(content) { this.$refs.layout.updateContent(content); },
    showStatus(msg, type) { this.$refs.layout.showStatus(msg, type); },
    uncommentGradleDependencies() {
      if (this.currentFile !== 'build.gradle.kts') { this.showStatus('Please open build.gradle.kts first!', 'error'); return; }
      let c = this.getContent();
      c = c.replace('// implementation("com.redis.om:redis-om-spring:1.0.4")', 'implementation("com.redis.om:redis-om-spring:1.0.4")');
      c = c.replace('// annotationProcessor("com.redis.om:redis-om-spring:1.0.4")', 'annotationProcessor("com.redis.om:redis-om-spring:1.0.4")');
      this.setContent(c);
      this.showStatus('Redis OM dependencies uncommented! Click Save!', 'success');
      this.saveProgress(2);
    },
    uncommentRedisConfig() {
      if (this.currentFile !== 'application.properties') { this.showStatus('Please open application.properties first!', 'error'); return; }
      let c = this.getContent();
      c = c.replace('# spring.data.redis.host=localhost', 'spring.data.redis.host=localhost');
      c = c.replace('# spring.data.redis.port=6379', 'spring.data.redis.port=6379');
      this.setContent(c);
      this.showStatus('Redis config uncommented! Click Save!', 'success');
      this.saveProgress(5);
    },
    addDocumentAnnotation() {
      if (this.currentFile !== 'Movie.java') { this.showStatus('Please open Movie.java first!', 'error'); return; }
      let c = this.getContent();
      c = c.replace('// import com.redis.om.spring.annotations.Document;', 'import com.redis.om.spring.annotations.Document;');
      c = c.replace('// TODO: Add @Document annotation here\npublic class Movie', '@Document\npublic class Movie');
      this.setContent(c);
      this.showStatus('@Document annotation added! Click Save!', 'success');
      this.saveProgress(8);
    },
    addIdAnnotation() {
      if (this.currentFile !== 'Movie.java') { this.showStatus('Please open Movie.java first!', 'error'); return; }
      let c = this.getContent();
      c = c.replace('// import org.springframework.data.annotation.Id;', 'import org.springframework.data.annotation.Id;');
      c = c.replace('// TODO: Add @Id annotation here\n    private String id', '@Id\n    private String id');
      this.setContent(c);
      this.showStatus('@Id annotation added! Click Save!', 'success');
      this.saveProgress(9);
    },
    addSearchableAnnotations() {
      if (this.currentFile !== 'Movie.java') { this.showStatus('Please open Movie.java first!', 'error'); return; }
      let c = this.getContent();
      c = c.replace('// import com.redis.om.spring.annotations.Searchable;', 'import com.redis.om.spring.annotations.Searchable;');
      c = c.replace('// TODO: Add @Searchable annotation for full-text search on title\n    private String title', '@Searchable\n    private String title');
      c = c.replace('// TODO: Add @Searchable annotation for full-text search on extract\n    private String extract', '@Searchable\n    private String extract');
      this.setContent(c);
      this.showStatus('@Searchable annotations added! Click Save!', 'success');
      this.saveProgress(10);
    },
    addIndexedAnnotations() {
      if (this.currentFile !== 'Movie.java') { this.showStatus('Please open Movie.java first!', 'error'); return; }
      let c = this.getContent();
      c = c.replace('// import com.redis.om.spring.annotations.Indexed;', 'import com.redis.om.spring.annotations.Indexed;');
      c = c.replace('// TODO: Add @Indexed(sortable = true) for year filtering and sorting\n    private int year', '@Indexed(sortable = true)\n    private int year');
      c = c.replace('// TODO: Add @Indexed for cast filtering\n    private List<String> cast', '@Indexed\n    private List<String> cast');
      c = c.replace('// TODO: Add @Indexed for genres filtering\n    private List<String> genres', '@Indexed\n    private List<String> genres');
      this.setContent(c);
      this.showStatus('@Indexed annotations added! Click Save!', 'success');
      this.saveProgress(11);
    },
    enableRedisRepositories() {
      if (this.currentFile !== 'FullTextSearchApplication.java') { this.showStatus('Please open FullTextSearchApplication.java first!', 'error'); return; }
      let c = this.getContent();
      c = c.replace('// import com.redis.om.spring.annotations.EnableRedisDocumentRepositories;', 'import com.redis.om.spring.annotations.EnableRedisDocumentRepositories;');
      c = c.replace('// TODO: Add @EnableRedisDocumentRepositories', '@EnableRedisDocumentRepositories');
      this.setContent(c);
      this.showStatus('@EnableRedisDocumentRepositories added! Click Save!', 'success');
      this.saveProgress(14);
    },
    extendRedisRepository() {
      if (this.currentFile !== 'MovieRepository.java') { this.showStatus('Please open MovieRepository.java first!', 'error'); return; }
      let c = this.getContent();
      c = c.replace('// import com.redis.om.spring.repository.RedisDocumentRepository;', 'import com.redis.om.spring.repository.RedisDocumentRepository;\nimport org.springframework.stereotype.Repository;');
      c = c.replace('// TODO: Uncomment the line below\n// @Repository\n// public interface MovieRepository extends RedisDocumentRepository<Movie, String> {\npublic interface MovieRepository {', '@Repository\npublic interface MovieRepository extends RedisDocumentRepository<Movie, String> {');
      c = c.replace('// Iterable<String> getAllGenres();', 'Iterable<String> getAllGenres();');
      this.setContent(c);
      this.showStatus('MovieRepository extended! Click Save!', 'success');
      this.saveProgress(17);
    },
    enableMovieServiceRepository() {
      if (this.currentFile !== 'MovieService.java') { this.showStatus('Please open MovieService.java first!', 'error'); return; }
      let c = this.getContent();
      c = this.ensureImport(
        c,
        '// import com.redis.workshop.search.repository.MovieRepository;',
        'import com.redis.workshop.search.repository.MovieRepository;',
        'import com.fasterxml.jackson.databind.ObjectMapper;'
      );
      c = c.replace('// TODO: Uncomment after implementing MovieRepository\n    // private final MovieRepository movieRepository;', 'private final MovieRepository movieRepository;');
      c = c.replace('public MovieService(ObjectMapper objectMapper, ResourceLoader resourceLoader /* , MovieRepository movieRepository */) {\n        this.objectMapper = objectMapper;\n        this.resourceLoader = resourceLoader;\n        // this.movieRepository = movieRepository;', 'public MovieService(ObjectMapper objectMapper, ResourceLoader resourceLoader, MovieRepository movieRepository) {\n        this.objectMapper = objectMapper;\n        this.resourceLoader = resourceLoader;\n        this.movieRepository = movieRepository;');
      this.setContent(c);
      this.showStatus('MovieRepository injected! Click Save!', 'success');
      this.saveProgress(20);
    },
    enableLoadAndSaveMovies() {
      if (this.currentFile !== 'MovieService.java') { this.showStatus('Please open MovieService.java first!', 'error'); return; }
      let c = this.getContent();
      c = this.ensureImport(
        c,
        '// import com.fasterxml.jackson.core.type.TypeReference;',
        'import com.fasterxml.jackson.core.type.TypeReference;',
        'import com.fasterxml.jackson.databind.ObjectMapper;'
      );
      c = this.ensureImport(
        c,
        '// import com.redis.workshop.search.domain.Movie;',
        'import com.redis.workshop.search.domain.Movie;',
        'import com.fasterxml.jackson.databind.ObjectMapper;'
      );
      c = this.ensureImport(
        c,
        '// import org.springframework.core.io.Resource;',
        'import org.springframework.core.io.Resource;',
        'import org.slf4j.LoggerFactory;'
      );
      c = this.ensureImport(
        c,
        '// import java.io.InputStream;',
        'import java.io.InputStream;',
        'import org.springframework.stereotype.Service;'
      );
      c = this.ensureImport(
        c,
        '// import java.util.List;',
        'import java.util.List;',
        'import org.springframework.stereotype.Service;'
      );
      c = c.replace('// TODO: Uncomment after implementing MovieRepository\n        /*\n        Resource resource = resourceLoader.getResource("classpath:" + filePath);\n        try (InputStream is = resource.getInputStream()) {\n            List<Movie> movies = objectMapper.readValue(is, new TypeReference<>() {});\n            long startTime = System.currentTimeMillis();\n            movieRepository.saveAll(movies);\n            long elapsedMillis = System.currentTimeMillis() - startTime;\n            log.info("Saved {} movies in {} ms", movies.size(), elapsedMillis);\n        }\n        */\n        log.info("MovieService.loadAndSaveMovies() - Redis OM Spring not yet configured");', 'Resource resource = resourceLoader.getResource("classpath:" + filePath);\n        try (InputStream is = resource.getInputStream()) {\n            List<Movie> movies = objectMapper.readValue(is, new TypeReference<>() {});\n            long startTime = System.currentTimeMillis();\n            movieRepository.saveAll(movies);\n            long elapsedMillis = System.currentTimeMillis() - startTime;\n            log.info("Saved {} movies in {} ms", movies.size(), elapsedMillis);\n        }');
      this.setContent(c);
      this.showStatus('loadAndSaveMovies() enabled! Click Save!', 'success');
      this.saveProgress(21);
    },
    enableIsDataLoaded() {
      if (this.currentFile !== 'MovieService.java') { this.showStatus('Please open MovieService.java first!', 'error'); return; }
      let c = this.getContent();
      c = c.replace('// TODO: Uncomment after implementing MovieRepository\n        // return movieRepository.count() > 0;\n        return false;', 'return movieRepository.count() > 0;');
      this.setContent(c);
      this.showStatus('isDataLoaded() enabled! Click Save!', 'success');
      this.saveProgress(22);
    },
    enableSearchServiceDependencies() {
      if (this.currentFile !== 'SearchService.java') { this.showStatus('Please open SearchService.java first!', 'error'); return; }
      let c = this.getContent();
      c = this.ensureImport(
        c,
        '// import com.redis.workshop.search.repository.MovieRepository;',
        'import com.redis.workshop.search.repository.MovieRepository;',
        'import com.redis.workshop.search.domain.Movie;'
      );
      c = c.replace('// import com.redis.workshop.search.domain.Movie$;', 'import com.redis.workshop.search.domain.Movie$;');
      c = c.replace('// import com.redis.om.spring.search.stream.EntityStream;', 'import com.redis.om.spring.search.stream.EntityStream;');
      c = c.replace('// import com.redis.om.spring.search.stream.SearchStream;', 'import com.redis.om.spring.search.stream.SearchStream;');
      c = c.replace('// import static redis.clients.jedis.search.aggr.SortedField.SortOrder.DESC;', 'import static redis.clients.jedis.search.aggr.SortedField.SortOrder.DESC;');
      c = c.replace('// TODO: Uncomment these fields after adding Redis OM Spring dependency\n    // private final EntityStream entityStream;\n    // private final MovieRepository movieRepository;', 'private final EntityStream entityStream;\n    private final MovieRepository movieRepository;');
      c = c.replace('// TODO: Update constructor to inject EntityStream and MovieRepository after adding Redis OM Spring\n    public SearchService(/* EntityStream entityStream, MovieRepository movieRepository */) {\n        // this.entityStream = entityStream;\n        // this.movieRepository = movieRepository;', 'public SearchService(EntityStream entityStream, MovieRepository movieRepository) {\n        this.entityStream = entityStream;\n        this.movieRepository = movieRepository;');
      this.setContent(c);
      this.showStatus('EntityStream and MovieRepository injected! Click Save!', 'success');
      this.saveProgress(23);
    },
    enableSearchMoviesImplementation() {
      if (this.currentFile !== 'SearchService.java') { this.showStatus('Please open SearchService.java first!', 'error'); return; }
      const replacement = 'SearchStream<Movie> stream = entityStream.of(Movie.class);\n\n        // Build the search query with filters\n        // containing() - full-text search on @Searchable fields\n        // eq() - exact match on @Indexed fields\n        List<Movie> matchedMovies = stream\n                .filter(Movie$.TITLE.containing(title))\n                .filter(Movie$.EXTRACT.containing(extract))\n                .filter(Movie$.CAST.eq(actors))\n                .filter(Movie$.YEAR.eq(year))\n                .filter(Movie$.GENRES.eq(genres))\n                .sorted(Movie$.YEAR, DESC)\n                .collect(Collectors.toList());';
      let c = this.getContent();
      const original = c;
      c = c.replace('// TODO: Implement search using EntityStream\n        // SearchStream<Movie> stream = entityStream.of(Movie.class);\n        //\n        // Build the search query with filters\n        // containing() - full-text search on @Searchable fields\n        // eq() - exact match on @Indexed fields\n        // List<Movie> matchedMovies = stream\n        //         .filter(Movie$.TITLE.containing(title))\n        //         .filter(Movie$.EXTRACT.containing(extract))\n        //         .filter(Movie$.CAST.eq(actors))\n        //         .filter(Movie$.YEAR.eq(year))\n        //         .filter(Movie$.GENRES.eq(genres))\n        //         .sorted(Movie$.YEAR, DESC)\n        //         .collect(Collectors.toList());\n\n        // Temporary: Return empty results until Redis OM Spring is configured\n        List<Movie> matchedMovies = new ArrayList<>();', replacement);
      if (c === original) {
        const pattern = /\/\/ TODO: Implement search using EntityStream[\s\S]*?List<Movie> matchedMovies = new ArrayList<>\(\);/;
        c = original.replace(pattern, replacement);
      }
      if (c === original) { this.showStatus('Could not find the searchMovies() block to uncomment.', 'error'); return; }
      this.setContent(c);
      this.showStatus('searchMovies() implementation enabled! Click Save!', 'success');
      this.saveProgress(24);
    },
    enableGetAllGenres() {
      if (this.currentFile !== 'SearchService.java') { this.showStatus('Please open SearchService.java first!', 'error'); return; }
      let c = this.getContent();
      c = c.replace('// TODO: Uncomment after implementing MovieRepository\n        // Iterable<String> genresIterable = movieRepository.getAllGenres();\n        // Set<String> allGenres = new HashSet<>();\n        // genresIterable.forEach(allGenres::add);\n\n        // Temporary: Return empty set until Redis OM Spring is configured\n        Set<String> allGenres = new HashSet<>();', 'Iterable<String> genresIterable = movieRepository.getAllGenres();\n        Set<String> allGenres = new HashSet<>();\n        genresIterable.forEach(allGenres::add);');
      this.setContent(c);
      this.showStatus('getAllGenres() enabled! Click Save!', 'success');
      this.saveProgress(25);
    }
  }
};
</script>

<style scoped>
/* All styling is now provided by WorkshopEditorLayout */
</style>
