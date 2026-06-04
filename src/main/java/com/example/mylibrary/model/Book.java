package com.example.mylibrary.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    private String genre;
    private String isbn;
    private Integer pagesCount;

    @Column(name = "publication_year")
    private Integer year;

    @Column(name = "cover_url")
    private String coverUrl;
    private String coverImage;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "book_tags", joinColumns = @JoinColumn(name = "book_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    @Column(name = "book_type")
    private String bookType;

    private String status;

    private Integer rating;

    @Column(length = 1000)
    private String review;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "added_at")
    private LocalDate addedAt;

    public Book() {
    }

    @PrePersist
    protected void onCreate() {
        this.addedAt = LocalDate.now();
    }

    @ManyToMany(mappedBy = "books")
    private List<com.example.mylibrary.model.Collection> collections;

    public List<Collection> getCollections() {return collections;}
    public void setCollections(List<com.example.mylibrary.model.Collection> collections) {this.collections = collections;}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public Integer getPagesCount() { return pagesCount; }
    public void setPagesCount(Integer pagesCount) { this.pagesCount = pagesCount; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public String getCoverImage() {return coverImage;}
    public void setCoverImage(String coverImage) {this.coverImage = coverImage;}

    public String getBookType() { return bookType; }
    public void setBookType(String bookType) { this.bookType = bookType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getReview() { return review; }
    public void setReview(String review) { this.review = review; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public LocalDate getAddedAt() { return addedAt; }
    public void setAddedAt(LocalDate addedAt) { this.addedAt = addedAt; }
}
