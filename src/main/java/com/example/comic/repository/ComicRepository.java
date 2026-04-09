package com.example.comic.repository;

import com.example.comic.model.Comic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComicRepository extends JpaRepository<Comic, Long> {
    @Query(
        value = """
        SELECT DISTINCT c.*
        FROM comics c
        LEFT JOIN comic_categories cc ON cc.comic_id = c.id
        WHERE (:keyword IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.author) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:categoryId IS NULL OR cc.category_id = :categoryId)
        """,
        countQuery = """
        SELECT COUNT(DISTINCT c.id)
        FROM comics c
        LEFT JOIN comic_categories cc ON cc.comic_id = c.id
        WHERE (:keyword IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.author) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:categoryId IS NULL OR cc.category_id = :categoryId)
        """,
        nativeQuery = true
    )
    Page<Comic> search(@Param("keyword") String keyword, @Param("categoryId") Long categoryId, Pageable pageable);
}
