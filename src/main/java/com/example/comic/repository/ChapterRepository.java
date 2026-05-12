package com.example.comic.repository;

import com.example.comic.model.Chapter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {
	boolean existsByComicIdAndChapterNumber(Long comicId, Integer chapterNumber);

	Page<Chapter> findByComicIdOrderByChapterNumberAsc(Long comicId, Pageable pageable);
    List<Chapter> findByComicIdOrderByChapterNumberAsc(Long comicId);
}
