package com.example.mylibrary.repository;

import com.example.mylibrary.model.Book;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    @Query("SELECT DISTINCT b FROM Book b LEFT JOIN b.tags t WHERE " +
            "(:keyword IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:bookTypes IS NULL OR b.bookType IN :bookTypes) AND " +
            "(:statuses IS NULL OR b.status IN :statuses) AND " +
            "(:genres IS NULL OR b.genre IN :genres) AND " +
            "(:tags IS NULL OR t IN :tags)")
    List<Book> findFilteredBooks(
            @Param("keyword") String keyword,
            @Param("bookTypes") List<String> bookTypes,
            @Param("statuses") List<String> statuses,
            @Param("genres") List<String> genres,
            @Param("tags") List<String> tags,
            Sort sort
    );
}