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
