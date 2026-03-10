package com.redis.workshop.search.frontend.infrastructure;

import com.redis.workshop.infrastructure.WorkshopConfig;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class FullTextSearchWorkshopConfig implements WorkshopConfig {

    private static final Map<String, String> EDITABLE_FILES = Map.ofEntries(
        Map.entry("build.gradle.kts", "build.gradle.kts"),
        Map.entry("application.properties", "src/main/resources/application.properties"),
        Map.entry("Movie.java", "src/main/java/com/redis/workshop/search/domain/Movie.java"),
        Map.entry("FullTextSearchApplication.java", "src/main/java/com/redis/workshop/search/FullTextSearchApplication.java"),
        Map.entry("MovieRepository.java", "src/main/java/com/redis/workshop/search/repository/MovieRepository.java"),
        Map.entry("MovieService.java", "src/main/java/com/redis/workshop/search/service/MovieService.java"),
        Map.entry("SearchService.java", "src/main/java/com/redis/workshop/search/service/SearchService.java")
    );

    private static final Map<String, String> ORIGINAL_CONTENTS = Map.ofEntries(
        Map.entry("build.gradle.kts", """
            plugins {
                java
                id("org.springframework.boot")
                id("io.spring.dependency-management")
            }

            group = "com.redis.workshop"
            version = "0.0.1-SNAPSHOT"

            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }

            repositories {
                mavenCentral()
            }

            dependencies {
                implementation("org.springframework.boot:spring-boot-starter-web")

                // TODO: Add Redis OM Spring dependency for full-text search
                // implementation("com.redis.om:redis-om-spring:1.0.4")
                // annotationProcessor("com.redis.om:redis-om-spring:1.0.4")

                testImplementation("org.springframework.boot:spring-boot-starter-test")
                testRuntimeOnly("org.junit.platform:junit-platform-launcher")
            }

            tasks.withType<Test> {
                useJUnitPlatform()
            }
            """),
        Map.entry("application.properties", """
            # Application name
            spring.application.name=full-text-search-api

            # Server configuration
            server.port=${SERVER_PORT:18081}

            # TODO: Configure Redis connection
            # spring.data.redis.host=localhost
            # spring.data.redis.port=6379

            # Logging
            logging.level.com.redis.workshop.search=INFO
            logging.level.com.redis.om.spring=INFO
            """),
        Map.entry("Movie.java", """
            package com.redis.workshop.search.domain;

            // TODO: Uncomment these imports after adding Redis OM Spring dependency
            // import com.redis.om.spring.annotations.Document;
            // import com.redis.om.spring.annotations.Indexed;
            // import com.redis.om.spring.annotations.Searchable;
            // import org.springframework.data.annotation.Id;

            import java.util.List;

            /**
             * Movie entity representing a document stored in Redis.
             *
             * TODO: Add annotations to enable Redis Query Engine indexing:
             * - @Document: Marks this class as a Redis document (stored as JSON)
             * - @Searchable: Enables full-text search on the field (stemming, fuzzy matching)
             * - @Indexed: Marks the field for exact filtering and sorting
             */
            // TODO: Add @Document annotation here
            public class Movie {

                // TODO: Add @Id annotation here
                private String id;

                // TODO: Add @Searchable annotation for full-text search on title
                private String title;

                // TODO: Add @Indexed(sortable = true) for year filtering and sorting
                private int year;

                // TODO: Add @Indexed for cast filtering
                private List<String> cast;

                // TODO: Add @Indexed for genres filtering
                private List<String> genres;

                private String href;

                // TODO: Add @Searchable annotation for full-text search on extract
                private String extract;

                private String thumbnail;
                private int thumbnailWidth;
                private int thumbnailHeight;

                public String getId() { return id; }
                public void setId(String id) { this.id = id; }
                public String getTitle() { return title; }
                public void setTitle(String title) { this.title = title; }
                public int getYear() { return year; }
                public void setYear(int year) { this.year = year; }
                public List<String> getCast() { return cast; }
                public void setCast(List<String> cast) { this.cast = cast; }
                public List<String> getGenres() { return genres; }
                public void setGenres(List<String> genres) { this.genres = genres; }
                public String getHref() { return href; }
                public void setHref(String href) { this.href = href; }
                public String getExtract() { return extract; }
                public void setExtract(String extract) { this.extract = extract; }
                public String getThumbnail() { return thumbnail; }
                public void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }
                public int getThumbnailWidth() { return thumbnailWidth; }
                public void setThumbnailWidth(int w) { this.thumbnailWidth = w; }
                public int getThumbnailHeight() { return thumbnailHeight; }
                public void setThumbnailHeight(int h) { this.thumbnailHeight = h; }
            }
            """),
        Map.entry("FullTextSearchApplication.java", """
            package com.redis.workshop.search;

            // TODO: Uncomment this import after adding Redis OM Spring dependency
            // import com.redis.om.spring.annotations.EnableRedisDocumentRepositories;
            import com.redis.workshop.search.service.MovieService;
            import org.springframework.boot.CommandLineRunner;
            import org.springframework.boot.SpringApplication;
            import org.springframework.boot.autoconfigure.SpringBootApplication;
            import org.springframework.context.annotation.Bean;

            /**
             * Main application class for the Full-Text Search Workshop.
             *
             * TODO: Add @EnableRedisDocumentRepositories annotation to enable:
             * - Automatic index creation based on @Document annotations
             * - Repository implementation generation
             * - EntityStream for fluent search queries
             */
            @SpringBootApplication
            // TODO: Add @EnableRedisDocumentRepositories
            public class FullTextSearchApplication {

                public static void main(String[] args) {
                    SpringApplication.run(FullTextSearchApplication.class, args);
                }

                @Bean
                CommandLineRunner loadData(MovieService movieService) {
                    return args -> {
                        if (movieService.isDataLoaded()) {
                            System.out.println("Data already loaded. Skipping data load.");
                            return;
                        }
                        movieService.loadAndSaveMovies("movies.json");
                    };
                }
            }
            """),
        Map.entry("MovieRepository.java", """
            package com.redis.workshop.search.repository;

            import com.redis.workshop.search.domain.Movie;
            // TODO: Uncomment this import after adding Redis OM Spring dependency
            // import com.redis.om.spring.repository.RedisDocumentRepository;

            /**
             * Repository interface for Movie documents.
             *
             * TODO: Uncomment the @Repository annotation and extend RedisDocumentRepository to get:
             * - CRUD operations (save, findById, findAll, delete, etc.)
             * - Query methods based on method names
             * - Integration with Redis Query Engine for searching
             */
            // TODO: Uncomment the line below
            // @Repository
            // public interface MovieRepository extends RedisDocumentRepository<Movie, String> {
            public interface MovieRepository {

                /**
                 * Gets all unique genres from the indexed movies.
                 * This method is automatically implemented by Redis OM Spring.
                 */
                // Iterable<String> getAllGenres();
            }
            """),
        Map.entry("MovieService.java", """
            package com.redis.workshop.search.service;

            // import com.fasterxml.jackson.core.type.TypeReference;
            import com.fasterxml.jackson.databind.ObjectMapper;
            // import com.redis.workshop.search.domain.Movie;
            // import com.redis.workshop.search.repository.MovieRepository;
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;
            // import org.springframework.core.io.Resource;
            import org.springframework.core.io.ResourceLoader;
            import org.springframework.stereotype.Service;

            // import java.io.InputStream;
            // import java.util.List;

            /**
             * Service for loading and managing movie data.
             *
             * This service handles:
             * - Loading movies from a JSON file
             * - Saving movies to Redis (which automatically indexes them)
             */
            @Service
            public class MovieService {

                private static final Logger log = LoggerFactory.getLogger(MovieService.class);

                private final ObjectMapper objectMapper;
                private final ResourceLoader resourceLoader;
                // TODO: Uncomment after implementing MovieRepository
                // private final MovieRepository movieRepository;

                public MovieService(ObjectMapper objectMapper, ResourceLoader resourceLoader /* , MovieRepository movieRepository */) {
                    this.objectMapper = objectMapper;
                    this.resourceLoader = resourceLoader;
                    // this.movieRepository = movieRepository;
                }

                /**
                 * Loads movies from a JSON file and saves them to Redis.
                 * Redis OM Spring automatically indexes the movies based on the annotations.
                 */
                public void loadAndSaveMovies(String filePath) throws Exception {
                    // TODO: Uncomment after implementing MovieRepository
                    /*
                    Resource resource = resourceLoader.getResource("classpath:" + filePath);
                    try (InputStream is = resource.getInputStream()) {
                        List<Movie> movies = objectMapper.readValue(is, new TypeReference<>() {});
                        long startTime = System.currentTimeMillis();
                        movieRepository.saveAll(movies);
                        long elapsedMillis = System.currentTimeMillis() - startTime;
                        log.info("Saved {} movies in {} ms", movies.size(), elapsedMillis);
                    }
                    */
                    log.info("MovieService.loadAndSaveMovies() - Redis OM Spring not yet configured");
                }

                /**
                 * Checks if movie data has already been loaded into Redis.
                 */
                public boolean isDataLoaded() {
                    // TODO: Uncomment after implementing MovieRepository
                    // return movieRepository.count() > 0;
                    return false;
                }
            }
            """),
        Map.entry("SearchService.java", """
            package com.redis.workshop.search.service;

            import com.redis.workshop.search.domain.Movie;
            // import com.redis.workshop.search.repository.MovieRepository;
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;
            import org.springframework.stereotype.Service;

            import java.util.*;
            import java.util.stream.Collectors;

            // TODO: Uncomment these imports after adding Redis OM Spring dependency
            // import com.redis.workshop.search.domain.Movie$;
            // import com.redis.om.spring.search.stream.EntityStream;
            // import com.redis.om.spring.search.stream.SearchStream;

            // import static redis.clients.jedis.search.aggr.SortedField.SortOrder.DESC;

            /**
             * Service for performing full-text search operations on movies.
             *
             * Uses Redis OM Spring's EntityStream API for building search queries.
             * The Movie$ class is auto-generated by the annotation processor and provides
             * type-safe field references for query building.
             */
            @Service
            public class SearchService {

                private static final Logger logger = LoggerFactory.getLogger(SearchService.class);
                // TODO: Uncomment these fields after adding Redis OM Spring dependency
                // private final EntityStream entityStream;
                // private final MovieRepository movieRepository;

                // TODO: Update constructor to inject EntityStream and MovieRepository after adding Redis OM Spring
                public SearchService(/* EntityStream entityStream, MovieRepository movieRepository */) {
                    // this.entityStream = entityStream;
                    // this.movieRepository = movieRepository;
                }

                public Map<String, Object> searchMovies(
                        String title,
                        String extract,
                        List<String> actors,
                        Integer year,
                        List<String> genres
                ) {
                    logger.info("Searching - title: {}, extract: {}, cast: {}, year: {}, genres: {}",
                            title, extract, actors, year, genres);

                    long startTime = System.currentTimeMillis();

                    // TODO: Implement search using EntityStream
                    // SearchStream<Movie> stream = entityStream.of(Movie.class);
                    //
                    // Build the search query with filters
                    // containing() - full-text search on @Searchable fields
                    // eq() - exact match on @Indexed fields
                    // List<Movie> matchedMovies = stream
                    //         .filter(Movie$.TITLE.containing(title))
                    //         .filter(Movie$.EXTRACT.containing(extract))
                    //         .filter(Movie$.CAST.eq(actors))
                        //         .filter(Movie$.YEAR.eq(year))
                        //         .filter(Movie$.GENRES.eq(genres))
                        //         .sorted(Movie$.YEAR, DESC)
                        //         .collect(Collectors.toList());

                    // Temporary: Return empty results until Redis OM Spring is configured
                    List<Movie> matchedMovies = new ArrayList<>();
                    long searchTime = System.currentTimeMillis() - startTime;

                    logger.info("Search completed in {} ms, found {} movies", searchTime, matchedMovies.size());

                    Map<String, Object> result = new HashMap<>();
                    result.put("movies", matchedMovies);
                    result.put("count", matchedMovies.size());
                    result.put("searchTime", searchTime);

                    return result;
                }

                public Set<String> getAllGenres() {
                    logger.info("Fetching all unique genres");
                    long startTime = System.currentTimeMillis();

                    // TODO: Uncomment after implementing MovieRepository
                    // Iterable<String> genresIterable = movieRepository.getAllGenres();
                    // Set<String> allGenres = new HashSet<>();
                    // genresIterable.forEach(allGenres::add);

                    // Temporary: Return empty set until Redis OM Spring is configured
                    Set<String> allGenres = new HashSet<>();
                    long fetchTime = System.currentTimeMillis() - startTime;

                    logger.info("Fetched {} unique genres in {} ms", allGenres.size(), fetchTime);

                    return allGenres;
                }
            }
            """)
    );

    @Override
    public Map<String, String> getEditableFiles() {
        return EDITABLE_FILES;
    }

    @Override
    public String getOriginalContent(String fileName) {
        return ORIGINAL_CONTENTS.get(fileName);
    }

    @Override
    public String getModuleName() {
        return "2_full_text_search";
    }

    @Override
    public String getWorkshopTitle() {
        return "Full-Text Search with Redis";
    }

    @Override
    public String getWorkshopDescription() {
        return "Build a Redis-backed movie search app with the SPA, editor, and backend APIs split into separate services.";
    }
}
