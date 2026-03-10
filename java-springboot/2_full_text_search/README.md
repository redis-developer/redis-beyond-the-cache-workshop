# Workshop 2: Full-Text Search with Redis

Learn how to add Redis Query Engine indexes to movie documents and query them with Redis OM Spring.

## What You'll Build

- A movie search API backed by Redis JSON documents
- Full-text search over `title` and `extract`
- Exact-match filters over `cast`, `genres`, and `year`
- A frontend that lets you edit the starter code and verify the results

## Run with Docker

```bash
cd java-springboot/workshop-hub

docker compose -f docker-compose.local.yml --profile infrastructure up -d
docker compose -f docker-compose.local.yml --profile workshop-2_full_text_search up -d
```

Open [http://localhost:8081](http://localhost:8081).

## Workshop Flow

| Stage | What You Do |
| --- | --- |
| 1. Intro | Learn how Redis Query Engine indexes movie fields |
| 2. Build | Uncomment dependencies and implement the Redis OM pieces |
| 3. Test | Search by title, description, actor, year, and genre |

## Editable Files and Tasks

### 1. `build.gradle.kts`

Uncomment the Redis OM Spring dependency and annotation processor:

```kotlin
implementation("com.redis.om:redis-om-spring:1.0.4")
annotationProcessor("com.redis.om:redis-om-spring:1.0.4")
```

### 2. `src/main/resources/application.properties`

Uncomment the Redis connection settings so the backend can connect to Redis:

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

### 3. `src/main/java/com/redis/workshop/search/domain/Movie.java`

The workshop uses the `extract` and `genres` fields from the starter dataset, not `plot` or `genre`.

```java
@Document
public class Movie {
    @Id
    private String id;

    @Searchable
    private String title;

    @Indexed(sortable = true)
    private int year;

    @Indexed
    private List<String> cast;

    @Indexed
    private List<String> genres;

    @Searchable
    private String extract;
}
```

### 4. `src/main/java/com/redis/workshop/search/FullTextSearchApplication.java`

Enable Redis document repositories so Redis OM Spring can create repositories, indexes, and the generated `Movie$` metamodel:

```java
@SpringBootApplication
@EnableRedisDocumentRepositories
public class FullTextSearchApplication {
}
```

### 5. `src/main/java/com/redis/workshop/search/repository/MovieRepository.java`

Extend `RedisDocumentRepository<Movie, String>` and expose the genre lookup used by the UI:

```java
@Repository
public interface MovieRepository extends RedisDocumentRepository<Movie, String> {
    Iterable<String> getAllGenres();
}
```

### 6. `src/main/java/com/redis/workshop/search/service/MovieService.java`

Inject `MovieRepository`, load `movies.json`, and save the documents into Redis so the indexes are populated.

### 7. `src/main/java/com/redis/workshop/search/service/SearchService.java`

Inject `EntityStream` and `MovieRepository`, then implement:

- `searchMovies(title, extract, actors, year, genres)`
- `getAllGenres()`

The `/api/search` endpoint accepts `title`, `text`, `cast`, `year`, and `genres`. The controller maps the `text` request parameter to the movie `extract` field.

## Verify in the UI

After rebuilding and restarting the workshop:

- Search for `matrix` by title
- Search for `space adventure` in the description field
- Filter by an actor such as `Keanu Reeves`
- Combine `genres=Sci-Fi` with `year=1999`

## Inspect the Index in Redis Insight

Open [http://localhost:5540](http://localhost:5540) and run:

```text
FT._LIST
FT.INFO "com.redis.workshop.search.domain.MovieIdx"
```

## Stop the Workshop

```bash
docker compose -f docker-compose.local.yml --profile workshop-2_full_text_search down
```

## Resources

- [Redis Query Engine Docs](https://redis.io/docs/interact/search-and-query/)
- [Redis OM Spring](https://github.com/redis/redis-om-spring)
