package com.example.comic.repository;

import com.example.comic.model.ReadingHistory;
import com.example.comic.model.id.ReadingHistoryId;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReadingHistoryRepository extends JpaRepository<ReadingHistory, ReadingHistoryId> {
    Optional<ReadingHistory> findByUserIdAndComicId(Long userId, Long comicId);
}
