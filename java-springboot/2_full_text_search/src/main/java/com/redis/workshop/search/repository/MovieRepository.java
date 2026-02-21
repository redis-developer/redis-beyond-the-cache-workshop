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
