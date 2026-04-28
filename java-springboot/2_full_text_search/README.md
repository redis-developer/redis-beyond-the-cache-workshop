# Workshop 2: Full-Text Search with Redis

Learn how to add Redis Query Engine indexes to movie documents and query them with Redis OM Spring.

## What You'll Build

1. A movie search API backed by Redis JSON documents.
2. Full-text search over `title` and `extract`.
3. Exact-match filters over `cast`, `genres`, and `year`.
4. A frontend that lets you edit the starter code and verify the results.

## Release Runtime

The release path for this workshop uses immutable images and session scoped runtime injection:

1. Frontend port: `8081`
2. Backend port: `18081`
3. Redis Stack port: `6379`
4. Redis Insight port: `5540`

The frontend and backend images do not depend on a repo mount at runtime. Session specific backend and workspace paths are injected by the launcher.

## Local Development

These commands are for local maintainer use only.
The public platform path launches this workshop through the control plane and execution plane.

```bash
./scripts/run-workshop.sh up 2_full_text_search
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

Open the session scoped Redis Insight link when you launch this workshop from the platform.
For the local helper flow, open [http://localhost:5540](http://localhost:5540) and run:

```text
FT._LIST
FT.INFO "com.redis.workshop.search.domain.MovieIdx"
```

## Stop the Workshop

```bash
./scripts/run-workshop.sh down 2_full_text_search
```

## Resources

- [Redis Query Engine Docs](https://redis.io/docs/interact/search-and-query/)
- [Redis OM Spring](https://github.com/redis/redis-om-spring)
