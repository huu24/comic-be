package com.example.comic.repository;

import com.example.comic.model.ChapterPage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChapterPageRepository extends JpaRepository<ChapterPage, Long> {
    List<ChapterPage> findByChapterIdOrderByPageNumberAsc(Long chapterId);

    List<ChapterPage> findByChapterIdAndPageNumber(Long chapterId, Integer pageNumber);

    boolean existsByChapterIdAndPageNumberBetween(Long chapterId, Integer startPageNumber, Integer endPageNumber);

    void deleteByChapterId(Long chapterId);
}
