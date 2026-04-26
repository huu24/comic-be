package com.example.comic.repository;

import com.example.comic.model.ComicCategory;
import com.example.comic.model.id.ComicCategoryId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComicCategoryRepository extends JpaRepository<ComicCategory, ComicCategoryId> {
    List<ComicCategory> findByComicId(Long comicId);

    void deleteByCategoryId(Long categoryId);
}
