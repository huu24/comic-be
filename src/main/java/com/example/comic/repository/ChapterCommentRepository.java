package com.example.comic.repository;

import com.example.comic.model.ChapterComment;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChapterCommentRepository extends JpaRepository<ChapterComment, Long> {
    Page<ChapterComment> findByChapterIdAndParentIdIsNullOrderByCreatedAtDesc(Long chapterId, Pageable pageable);

    List<ChapterComment> findByParentIdInOrderByCreatedAtAsc(List<Long> parentIds);
}
