package com.example.comic.repository;

import com.example.comic.model.ReadingHistory;
import com.example.comic.model.id.ReadingHistoryId;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReadingHistoryRepository extends JpaRepository<ReadingHistory, ReadingHistoryId> {
    Optional<ReadingHistory> findByUserIdAndComicId(Long userId, Long comicId);

    Page<ReadingHistory> findByUserId(Long userId, Pageable pageable);

    void deleteByComicId(Long comicId);
}
