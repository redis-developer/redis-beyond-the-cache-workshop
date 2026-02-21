package com.redis.workshop.search.controller;

import com.redis.workshop.search.service.SearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * REST controller for movie search operations.
 * 
 * Provides endpoints for:
 * - Full-text search on movies
 * - Retrieving all available genres
 */
@RestController
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * Search for movies using full-text search and filters.
     * 
     * @param title  Text to search in movie titles
     * @param text   Text to search in movie descriptions
     * @param cast   List of actors to filter by
     * @param year   Year to filter by
     * @param genres List of genres to filter by
     * @return Search results with movies, count, and search time
     */
    @GetMapping("/api/search")
    public Map<String, Object> search(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<String> cast,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) List<String> genres
    ) {
        return searchService.searchMovies(title, text, cast, year, genres);
    }

    /**
     * Get all unique genres available in the movie database.
     * 
     * @return Map containing genres, count, and fetch time
     */
    @GetMapping("/api/genres")
    public Map<String, Object> getAllGenres() {
        long startTime = System.currentTimeMillis();

        Set<String> genres = searchService.getAllGenres();

        long fetchTime = System.currentTimeMillis() - startTime;

        Map<String, Object> result = new HashMap<>();
        result.put("genres", genres);
        result.put("count", genres.size());
        result.put("fetchTime", fetchTime);

        return result;
    }
}

