<template>
  <WorkshopEditorLayout
    ref="layout"
    title="Full-Text Search Workshop"
    :files="files"
    @file-loaded="onFileLoaded"
    @file-saved="onFileSaved"
  >
    <template #instructions>
      <div v-if="contentLoading" class="content-status">
        Loading workshop instructions...
      </div>

      <div v-else-if="contentError" class="content-status content-status--error">
        {{ contentError }}
      </div>

      <WorkshopContentRenderer
        v-else-if="content"
        :content="content"
        :show-title="false"
        :show-summary="false"
        :action-handlers="actionHandlers"
        @render-error="handleRenderIssues"
      />

      <div v-if="workshopComplete" class="completion-banner">
        All steps completed! Restart the app to test your search.
      </div>
    </template>
  </WorkshopEditorLayout>
</template>

<script>
import { getApiUrl, getWorkshopHubUrl } from '../utils/basePath';
import { WorkshopContentRenderer, WorkshopEditorLayout } from '../utils/components';
import { fetchWorkshopContent } from '../utils/workshopContent';

const STORAGE_KEY = 'fullTextSearchWorkshop';

const EDITOR_STEP_HANDLERS = {
  'search-editor.uncommentGradleDependencies': 'uncommentGradleDependencies',
  'search-editor.uncommentRedisConfig': 'uncommentRedisConfig',
  'search-editor.addDocumentAnnotation': 'addDocumentAnnotation',
  'search-editor.addIdAnnotation': 'addIdAnnotation',
  'search-editor.addSearchableAnnotations': 'addSearchableAnnotations',
  'search-editor.addIndexedAnnotations': 'addIndexedAnnotations',
  'search-editor.enableRedisRepositories': 'enableRedisRepositories',
  'search-editor.extendRedisRepository': 'extendRedisRepository',
  'search-editor.enableMovieServiceRepository': 'enableMovieServiceRepository',
  'search-editor.enableLoadAndSaveMovies': 'enableLoadAndSaveMovies',
  'search-editor.enableIsDataLoaded': 'enableIsDataLoaded',
  'search-editor.enableSearchServiceDependencies': 'enableSearchServiceDependencies',
  'search-editor.enableSearchMoviesImplementation': 'enableSearchMoviesImplementation',
  'search-editor.enableGetAllGenres': 'enableGetAllGenres'
};

export default {
  name: 'SearchEditor',
  components: {
    WorkshopContentRenderer,
    WorkshopEditorLayout
  },
  data() {
    return {
      content: null,
      contentLoading: true,
      contentError: '',
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
      } catch (error) {
        console.error('Failed to load saved progress', error);
      }
    }

    await Promise.all([
      this.loadContent(),
      this.checkWorkshopCompletion()
    ]);
  },
  computed: {
    workshopHubUrl() {
      return getWorkshopHubUrl();
    },
    actionHandlers() {
      return {
        openFile: ({ args }) => this.loadFileStep(args.file),
        saveFile: () => this.saveFile(),
        applyEditorStep: ({ args }) => this.applyEditorStep(args.stepId),
        openHub: () => this.openHub(),
        openRoute: ({ args }) => this.openRoute(args.route)
      };
    }
  },
  methods: {
    async loadContent() {
      this.contentLoading = true;
      this.contentError = '';

      try {
        this.content = await fetchWorkshopContent('search-editor');
      } catch (error) {
        console.error('Failed to load editor content:', error);
        this.contentError = error.message || 'Failed to load workshop instructions.';
      } finally {
        this.contentLoading = false;
      }
    },
    handleRenderIssues(issues) {
      if (issues?.length) {
        console.warn('SearchEditor content render issues:', issues);
      }
    },
    openHub() {
      window.open(this.workshopHubUrl, '_blank', 'noopener');
    },
    openRoute(route) {
      if (!route) {
        return;
      }

      this.$router.push(route).catch(() => {});
    },
    applyEditorStep(stepId) {
      const handlerName = EDITOR_STEP_HANDLERS[stepId];

      if (!handlerName || typeof this[handlerName] !== 'function') {
        this.showStatus(`Unknown editor step: ${stepId}`, 'error');
        return;
      }

      this[handlerName]();
    },
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
    onFileSaved() {
      this.checkWorkshopCompletion();
    },
    saveProgress(step) {
      this.currentStep = step;
      const saved = localStorage.getItem(STORAGE_KEY);
      let data = {};

      if (saved) {
        try {
          data = JSON.parse(saved);
        } catch (error) {
          data = {};
        }
      }

      data.currentStep = step;
      localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
    },
    async fetchFileContent(fileName) {
      try {
        const response = await fetch(getApiUrl(`/api/editor/file/${fileName}`), { credentials: 'include' });
        const data = await response.json();
        return data.content || '';
      } catch (error) {
        return '';
      }
    },
    async checkWorkshopCompletion() {
      const filesToCheck = [
        'build.gradle.kts',
        'application.properties',
        'Movie.java',
        'FullTextSearchApplication.java',
        'MovieRepository.java',
        'MovieService.java',
        'SearchService.java'
      ];

      for (const file of filesToCheck) {
        this.fileContents[file] = await this.fetchFileContent(file);
      }

      const checks = [
        !this.fileContents['build.gradle.kts']?.includes('// implementation("com.redis.om:redis-om-spring'),
        !this.fileContents['application.properties']?.includes('# spring.data.redis.host'),
        this.fileContents['Movie.java']?.includes('@Document'),
        this.fileContents['FullTextSearchApplication.java']?.includes('@EnableRedisDocumentRepositories'),
        this.fileContents['MovieRepository.java']?.includes('extends RedisDocumentRepository'),
        this.fileContents['MovieService.java']?.includes('private final MovieRepository movieRepository;'),
        this.fileContents['SearchService.java']?.includes('private final EntityStream entityStream;')
      ];

      const wasComplete = this.workshopComplete;
      this.workshopComplete = checks.every(Boolean);

      if (!wasComplete && this.workshopComplete) {
        this.saveProgress(26);
        this.$nextTick(() => {
          this.$refs.layout?.showStatus(
            'Workshop complete! Restart the app and test your search.',
            'success'
          );
        });
      }
    },
    async loadFileStep(fileName, step = null) {
      await this.$refs.layout.loadFile(fileName);
      if (step) {
        this.saveProgress(step);
      }
    },
    saveFile() {
      this.$refs.layout.save();
    },
    getContent() {
      return this.$refs.layout.getCurrentContent();
    },
    setContent(content) {
      this.$refs.layout.updateContent(content);
    },
    showStatus(message, type) {
      this.$refs.layout.showStatus(message, type);
    },
    uncommentGradleDependencies() {
      if (this.currentFile !== 'build.gradle.kts') {
        this.showStatus('Please open build.gradle.kts first!', 'error');
        return;
      }
      let content = this.getContent();
      content = content.replace(
        '// implementation("com.redis.om:redis-om-spring:1.0.4")',
        'implementation("com.redis.om:redis-om-spring:1.0.4")'
      );
      content = content.replace(
        '// annotationProcessor("com.redis.om:redis-om-spring:1.0.4")',
        'annotationProcessor("com.redis.om:redis-om-spring:1.0.4")'
      );
      this.setContent(content);
      this.showStatus('Redis OM dependencies uncommented! Click Save!', 'success');
      this.saveProgress(2);
    },
    uncommentRedisConfig() {
      if (this.currentFile !== 'application.properties') {
        this.showStatus('Please open application.properties first!', 'error');
        return;
      }
      let content = this.getContent();
      content = content.replace('# spring.data.redis.host=localhost', 'spring.data.redis.host=localhost');
      content = content.replace('# spring.data.redis.port=6379', 'spring.data.redis.port=6379');
      this.setContent(content);
      this.showStatus('Redis config uncommented! Click Save!', 'success');
      this.saveProgress(5);
    },
    addDocumentAnnotation() {
      if (this.currentFile !== 'Movie.java') {
        this.showStatus('Please open Movie.java first!', 'error');
        return;
      }
      let content = this.getContent();
      content = content.replace(
        '// import com.redis.om.spring.annotations.Document;',
        'import com.redis.om.spring.annotations.Document;'
      );
      content = content.replace(
        '// TODO: Add @Document annotation here\npublic class Movie',
        '@Document\npublic class Movie'
      );
      this.setContent(content);
      this.showStatus('@Document annotation added! Click Save!', 'success');
      this.saveProgress(8);
    },
    addIdAnnotation() {
      if (this.currentFile !== 'Movie.java') {
        this.showStatus('Please open Movie.java first!', 'error');
        return;
      }
      let content = this.getContent();
      content = content.replace('// import org.springframework.data.annotation.Id;', 'import org.springframework.data.annotation.Id;');
      content = content.replace('// TODO: Add @Id annotation here\n    private String id', '@Id\n    private String id');
      this.setContent(content);
      this.showStatus('@Id annotation added! Click Save!', 'success');
      this.saveProgress(9);
    },
    addSearchableAnnotations() {
      if (this.currentFile !== 'Movie.java') {
        this.showStatus('Please open Movie.java first!', 'error');
        return;
      }
      let content = this.getContent();
      content = content.replace(
        '// import com.redis.om.spring.annotations.Searchable;',
        'import com.redis.om.spring.annotations.Searchable;'
      );
      content = content.replace(
        '// TODO: Add @Searchable annotation for full-text search on title\n    private String title',
        '@Searchable\n    private String title'
      );
      content = content.replace(
        '// TODO: Add @Searchable annotation for full-text search on extract\n    private String extract',
        '@Searchable\n    private String extract'
      );
      this.setContent(content);
      this.showStatus('@Searchable annotations added! Click Save!', 'success');
      this.saveProgress(10);
    },
    addIndexedAnnotations() {
      if (this.currentFile !== 'Movie.java') {
        this.showStatus('Please open Movie.java first!', 'error');
        return;
      }
      let content = this.getContent();
      content = content.replace(
        '// import com.redis.om.spring.annotations.Indexed;',
        'import com.redis.om.spring.annotations.Indexed;'
      );
      content = content.replace(
        '// TODO: Add @Indexed(sortable = true) for year filtering and sorting\n    private int year',
        '@Indexed(sortable = true)\n    private int year'
      );
      content = content.replace(
        '// TODO: Add @Indexed for cast filtering\n    private List<String> cast',
        '@Indexed\n    private List<String> cast'
      );
      content = content.replace(
        '// TODO: Add @Indexed for genres filtering\n    private List<String> genres',
        '@Indexed\n    private List<String> genres'
      );
      this.setContent(content);
      this.showStatus('@Indexed annotations added! Click Save!', 'success');
      this.saveProgress(11);
    },
    enableRedisRepositories() {
      if (this.currentFile !== 'FullTextSearchApplication.java') {
        this.showStatus('Please open FullTextSearchApplication.java first!', 'error');
        return;
      }
      let content = this.getContent();
      content = content.replace(
        '// import com.redis.om.spring.annotations.EnableRedisDocumentRepositories;',
        'import com.redis.om.spring.annotations.EnableRedisDocumentRepositories;'
      );
      content = content.replace('// TODO: Add @EnableRedisDocumentRepositories', '@EnableRedisDocumentRepositories');
      this.setContent(content);
      this.showStatus('@EnableRedisDocumentRepositories added! Click Save!', 'success');
      this.saveProgress(14);
    },
    extendRedisRepository() {
      if (this.currentFile !== 'MovieRepository.java') {
        this.showStatus('Please open MovieRepository.java first!', 'error');
        return;
      }
      let content = this.getContent();
      content = content.replace(
        '// import com.redis.om.spring.repository.RedisDocumentRepository;',
        'import com.redis.om.spring.repository.RedisDocumentRepository;\nimport org.springframework.stereotype.Repository;'
      );
      content = content.replace(
        '// TODO: Uncomment the line below\n// @Repository\n// public interface MovieRepository extends RedisDocumentRepository<Movie, String> {\npublic interface MovieRepository {',
        '@Repository\npublic interface MovieRepository extends RedisDocumentRepository<Movie, String> {'
      );
      content = content.replace('// Iterable<String> getAllGenres();', 'Iterable<String> getAllGenres();');
      this.setContent(content);
      this.showStatus('MovieRepository extended! Click Save!', 'success');
      this.saveProgress(17);
    },
    enableMovieServiceRepository() {
      if (this.currentFile !== 'MovieService.java') {
        this.showStatus('Please open MovieService.java first!', 'error');
        return;
      }
      let content = this.getContent();
      content = this.ensureImport(
        content,
        '// import com.redis.workshop.search.repository.MovieRepository;',
        'import com.redis.workshop.search.repository.MovieRepository;',
        'import com.fasterxml.jackson.databind.ObjectMapper;'
      );
      content = content.replace(
        '// TODO: Uncomment after implementing MovieRepository\n    // private final MovieRepository movieRepository;',
        'private final MovieRepository movieRepository;'
      );
      content = content.replace(
        'public MovieService(ObjectMapper objectMapper, ResourceLoader resourceLoader /* , MovieRepository movieRepository */) {\n        this.objectMapper = objectMapper;\n        this.resourceLoader = resourceLoader;\n        // this.movieRepository = movieRepository;',
        'public MovieService(ObjectMapper objectMapper, ResourceLoader resourceLoader, MovieRepository movieRepository) {\n        this.objectMapper = objectMapper;\n        this.resourceLoader = resourceLoader;\n        this.movieRepository = movieRepository;'
      );
      this.setContent(content);
      this.showStatus('MovieRepository injected! Click Save!', 'success');
      this.saveProgress(20);
    },
    enableLoadAndSaveMovies() {
      if (this.currentFile !== 'MovieService.java') {
        this.showStatus('Please open MovieService.java first!', 'error');
        return;
      }
      let content = this.getContent();
      content = this.ensureImport(
        content,
        '// import com.fasterxml.jackson.core.type.TypeReference;',
        'import com.fasterxml.jackson.core.type.TypeReference;',
        'import com.fasterxml.jackson.databind.ObjectMapper;'
      );
      content = this.ensureImport(
        content,
        '// import com.redis.workshop.search.domain.Movie;',
        'import com.redis.workshop.search.domain.Movie;',
        'import com.fasterxml.jackson.databind.ObjectMapper;'
      );
      content = this.ensureImport(
        content,
        '// import org.springframework.core.io.Resource;',
        'import org.springframework.core.io.Resource;',
        'import org.slf4j.LoggerFactory;'
      );
      content = this.ensureImport(
        content,
        '// import java.io.InputStream;',
        'import java.io.InputStream;',
        'import org.springframework.stereotype.Service;'
      );
      content = this.ensureImport(
        content,
        '// import java.util.List;',
        'import java.util.List;',
        'import org.springframework.stereotype.Service;'
      );
      content = content.replace(
        '// TODO: Uncomment after implementing MovieRepository\n        /*\n        Resource resource = resourceLoader.getResource("classpath:" + filePath);\n        try (InputStream is = resource.getInputStream()) {\n            List<Movie> movies = objectMapper.readValue(is, new TypeReference<>() {});\n            long startTime = System.currentTimeMillis();\n            movieRepository.saveAll(movies);\n            long elapsedMillis = System.currentTimeMillis() - startTime;\n            log.info("Saved {} movies in {} ms", movies.size(), elapsedMillis);\n        }\n        */\n        log.info("MovieService.loadAndSaveMovies() - Redis OM Spring not yet configured");',
        'Resource resource = resourceLoader.getResource("classpath:" + filePath);\n        try (InputStream is = resource.getInputStream()) {\n            List<Movie> movies = objectMapper.readValue(is, new TypeReference<>() {});\n            long startTime = System.currentTimeMillis();\n            movieRepository.saveAll(movies);\n            long elapsedMillis = System.currentTimeMillis() - startTime;\n            log.info("Saved {} movies in {} ms", movies.size(), elapsedMillis);\n        }'
      );
      this.setContent(content);
      this.showStatus('loadAndSaveMovies() enabled! Click Save!', 'success');
      this.saveProgress(21);
    },
    enableIsDataLoaded() {
      if (this.currentFile !== 'MovieService.java') {
        this.showStatus('Please open MovieService.java first!', 'error');
        return;
      }
      let content = this.getContent();
      content = content.replace(
        '// TODO: Uncomment after implementing MovieRepository\n        // return movieRepository.count() > 0;\n        return false;',
        'return movieRepository.count() > 0;'
      );
      this.setContent(content);
      this.showStatus('isDataLoaded() enabled! Click Save!', 'success');
      this.saveProgress(22);
    },
    enableSearchServiceDependencies() {
      if (this.currentFile !== 'SearchService.java') {
        this.showStatus('Please open SearchService.java first!', 'error');
        return;
      }
      let content = this.getContent();
      content = this.ensureImport(
        content,
        '// import com.redis.workshop.search.repository.MovieRepository;',
        'import com.redis.workshop.search.repository.MovieRepository;',
        'import com.redis.workshop.search.domain.Movie;'
      );
      content = content.replace('// import com.redis.workshop.search.domain.Movie$;', 'import com.redis.workshop.search.domain.Movie$;');
      content = content.replace('// import com.redis.om.spring.search.stream.EntityStream;', 'import com.redis.om.spring.search.stream.EntityStream;');
      content = content.replace('// import com.redis.om.spring.search.stream.SearchStream;', 'import com.redis.om.spring.search.stream.SearchStream;');
      content = content.replace(
        '// import static redis.clients.jedis.search.aggr.SortedField.SortOrder.DESC;',
        'import static redis.clients.jedis.search.aggr.SortedField.SortOrder.DESC;'
      );
      content = content.replace(
        '// TODO: Uncomment these fields after adding Redis OM Spring dependency\n    // private final EntityStream entityStream;\n    // private final MovieRepository movieRepository;',
        'private final EntityStream entityStream;\n    private final MovieRepository movieRepository;'
      );
      content = content.replace(
        '// TODO: Update constructor to inject EntityStream and MovieRepository after adding Redis OM Spring\n    public SearchService(/* EntityStream entityStream, MovieRepository movieRepository */) {\n        // this.entityStream = entityStream;\n        // this.movieRepository = movieRepository;',
        'public SearchService(EntityStream entityStream, MovieRepository movieRepository) {\n        this.entityStream = entityStream;\n        this.movieRepository = movieRepository;'
      );
      this.setContent(content);
      this.showStatus('EntityStream and MovieRepository injected! Click Save!', 'success');
      this.saveProgress(23);
    },
    enableSearchMoviesImplementation() {
      if (this.currentFile !== 'SearchService.java') {
        this.showStatus('Please open SearchService.java first!', 'error');
        return;
      }
      const replacement = 'SearchStream<Movie> stream = entityStream.of(Movie.class);\n\n        // Build the search query with filters\n        // containing() - full-text search on @Searchable fields\n        // eq() - exact match on @Indexed fields\n        List<Movie> matchedMovies = stream\n                .filter(Movie$.TITLE.containing(title))\n                .filter(Movie$.EXTRACT.containing(extract))\n                .filter(Movie$.CAST.eq(actors))\n                .filter(Movie$.YEAR.eq(year))\n                .filter(Movie$.GENRES.eq(genres))\n                .sorted(Movie$.YEAR, DESC)\n                .collect(Collectors.toList());';
      let content = this.getContent();
      const original = content;
      content = content.replace(
        '// TODO: Implement search using EntityStream\n        // SearchStream<Movie> stream = entityStream.of(Movie.class);\n        //\n        // Build the search query with filters\n        // containing() - full-text search on @Searchable fields\n        // eq() - exact match on @Indexed fields\n        // List<Movie> matchedMovies = stream\n        //         .filter(Movie$.TITLE.containing(title))\n        //         .filter(Movie$.EXTRACT.containing(extract))\n        //         .filter(Movie$.CAST.eq(actors))\n        //         .filter(Movie$.YEAR.eq(year))\n        //         .filter(Movie$.GENRES.eq(genres))\n        //         .sorted(Movie$.YEAR, DESC)\n        //         .collect(Collectors.toList());\n\n        // Temporary: Return empty results until Redis OM Spring is configured\n        List<Movie> matchedMovies = new ArrayList<>();',
        replacement
      );
      if (content === original) {
        const pattern = /\/\/ TODO: Implement search using EntityStream[\s\S]*?List<Movie> matchedMovies = new ArrayList<>\(\);/;
        content = original.replace(pattern, replacement);
      }
      if (content === original) {
        this.showStatus('Could not find the searchMovies() block to uncomment.', 'error');
        return;
      }
      this.setContent(content);
      this.showStatus('searchMovies() implementation enabled! Click Save!', 'success');
      this.saveProgress(24);
    },
    enableGetAllGenres() {
      if (this.currentFile !== 'SearchService.java') {
        this.showStatus('Please open SearchService.java first!', 'error');
        return;
      }
      let content = this.getContent();
      content = content.replace(
        '// TODO: Uncomment after implementing MovieRepository\n        // Iterable<String> genresIterable = movieRepository.getAllGenres();\n        // Set<String> allGenres = new HashSet<>();\n        // genresIterable.forEach(allGenres::add);\n\n        // Temporary: Return empty set until Redis OM Spring is configured\n        Set<String> allGenres = new HashSet<>();',
        'Iterable<String> genresIterable = movieRepository.getAllGenres();\n        Set<String> allGenres = new HashSet<>();\n        genresIterable.forEach(allGenres::add);'
      );
      this.setContent(content);
      this.showStatus('getAllGenres() enabled! Click Save!', 'success');
      this.saveProgress(25);
    }
  }
};
</script>

<style scoped>
.content-status {
  margin-bottom: var(--spacing-4);
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

.completion-banner {
  margin-top: var(--spacing-6);
  padding: var(--spacing-4);
  background: #1e7e34;
  color: white;
  border-radius: var(--radius-md);
  text-align: center;
  font-weight: var(--font-weight-semibold);
}
</style>
