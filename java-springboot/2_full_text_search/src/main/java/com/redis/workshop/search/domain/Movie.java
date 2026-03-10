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
