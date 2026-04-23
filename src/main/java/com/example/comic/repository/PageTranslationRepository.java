package com.example.comic.repository;

import com.example.comic.model.PageTranslation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PageTranslationRepository extends JpaRepository<PageTranslation, Long> {
    Optional<PageTranslation> findByPageIdAndLang(Long pageId, String lang);

    List<PageTranslation> findByPageId(Long pageId);

    void deleteByPageId(Long pageId);
}
