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
