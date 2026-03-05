# Workshop 2: Full-Text Search with Redis

Learn how to implement full-text search using Redis Query Engine and Redis OM Spring.

## What You'll Learn

- How Redis Query Engine provides search capabilities
- How to use Redis OM Spring for indexing and querying
- Building text search with filtering and sorting

## Run with Docker

```bash
# From repository root
cd java-springboot/workshop-hub

# Start Redis + Workshop
docker compose -f docker-compose.local.yml --profile infrastructure up -d
docker compose -f docker-compose.local.yml --profile workshop-2_full_text_search up -d
```

Open **http://localhost:8081**

## Workshop Flow

| Stage | What You Do |
|-------|-------------|
| 1. See the Problem | Try searching movies without an index |
| 2. Fix It | Add Redis OM annotations and implement search |
| 3. Verify | Search, filter, and sort movies |

## Your Tasks

### 1. `Movie.java` - Add Redis OM annotations
```java
@Document
public class Movie {
    @Id
    private String id;
    
    @Searchable
    private String title;
    
    @Searchable
    private String plot;
    
    @Indexed
    private Integer year;
    
    @Indexed
    private String genre;
}
```

### 2. `MovieRepository.java` - Extend Redis OM repository
```java
public interface MovieRepository extends RedisDocumentRepository<Movie, String> {
    List<Movie> findByTitleContaining(String title);
    List<Movie> findByGenre(String genre);
    List<Movie> findByYearBetween(Integer start, Integer end);
}
```

### 3. `MovieService.java` - Implement search methods
```java
public List<Movie> searchMovies(String query) {
    return movieRepository.findByTitleContaining(query);
}
```

## View Index in Redis Insight

Open **http://localhost:5540** and run:
```
FT._LIST
FT.INFO MovieIdx
```

## Stopping

```bash
docker compose -f docker-compose.local.yml --profile workshop-2_full_text_search down
```

## Resources

- [Redis Query Engine Docs](https://redis.io/docs/interact/search-and-query/)
- [Redis OM Spring](https://github.com/redis/redis-om-spring)

