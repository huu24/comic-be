package com.example.comic.repository;

import com.example.comic.model.ComicRating;
import com.example.comic.model.id.ComicRatingId;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComicRatingRepository extends JpaRepository<ComicRating, ComicRatingId> {
    Optional<ComicRating> findByUserIdAndComicId(Long userId, Long comicId);

    @Query("SELECT coalesce(avg(r.score), 0) FROM ComicRating r WHERE r.comicId = :comicId")
    Double avgScoreByComicId(@Param("comicId") Long comicId);

    long countByComicId(Long comicId);
}
