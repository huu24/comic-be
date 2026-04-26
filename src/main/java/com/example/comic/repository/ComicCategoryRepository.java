package com.example.comic.repository;

import com.example.comic.model.ComicCategory;
import com.example.comic.model.id.ComicCategoryId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComicCategoryRepository extends JpaRepository<ComicCategory, ComicCategoryId> {
    List<ComicCategory> findByComicId(Long comicId);

    void deleteByCategoryId(Long categoryId);

    @Query("SELECT c.name FROM Category c JOIN ComicCategory cc ON c.id = cc.categoryId WHERE cc.comicId = :comicId")
    List<String> findCategoryNamesByComicId(@Param("comicId") Long comicId);
}
