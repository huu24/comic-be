package com.example.comic.repository;

import com.example.comic.model.Comic;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComicRepository extends JpaRepository<Comic, Long> {
  List<Comic> findTop5ByOrderByTotalRatingsDescUpdatedAtDesc();

    @Query(
        value = """
        SELECT DISTINCT c.*
        FROM comics c
        LEFT JOIN comic_categories cc ON cc.comic_id = c.id
        WHERE (:keyword IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.author) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:categoryId IS NULL OR cc.category_id = :categoryId)
          AND (:originalLanguage IS NULL OR LOWER(c.original_language) = LOWER(:originalLanguage))
          AND (:comicStatus IS NULL OR LOWER(c.status) = LOWER(:comicStatus))
        ORDER BY c.updated_at DESC
        """,
        countQuery = """
        SELECT COUNT(DISTINCT c.id)
        FROM comics c
        LEFT JOIN comic_categories cc ON cc.comic_id = c.id
        WHERE (:keyword IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.author) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:categoryId IS NULL OR cc.category_id = :categoryId)
          AND (:originalLanguage IS NULL OR LOWER(c.original_language) = LOWER(:originalLanguage))
          AND (:comicStatus IS NULL OR LOWER(c.status) = LOWER(:comicStatus))
        """,
        nativeQuery = true
    )
    Page<Comic> search(
        @Param("keyword") String keyword,
        @Param("categoryId") Long categoryId,
        @Param("originalLanguage") String originalLanguage,
        @Param("comicStatus") String comicStatus,
        Pageable pageable
    );
}
