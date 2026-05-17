package com.example.comic.service;

import com.example.comic.model.document.ComicDocument;
import com.example.comic.model.dto.ComicDetailSearchResult;
import com.example.comic.model.dto.ComicSearchResult;
import com.example.comic.repository.search.ComicSearchRepository;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ComicSearchService {

    private static final int MIN_KEYWORD_LENGTH = 2;
    private static final int DEFAULT_RESULT_LIMIT = 10;
    private static final int MAX_RESULT_LIMIT = 50;

    private final ComicSearchRepository comicSearchRepository;

    @Cacheable(value = "comicSearch", key = "#keyword + '-' + #limit")
    public List<ComicSearchResult> searchComics(String keyword, int limit) {
        if (keyword == null || keyword.isBlank() || keyword.trim().length() < MIN_KEYWORD_LENGTH) {
            return Collections.emptyList();
        }

        String trimmed = keyword.trim().toLowerCase();
        int actualLimit = Math.clamp(limit, 1, MAX_RESULT_LIMIT);

        return comicSearchRepository
                .searchByKeyword(trimmed, PageRequest.of(0, actualLimit))
                .getContent()
                .stream()
                .map(this::toQuickSearchResult)
                .toList();
    }

    @Cacheable(value = "comicSearchDetail", key = "#keyword + '-' + #limit")
    public List<ComicDetailSearchResult> searchComicsDetail(String keyword, int limit) {
        if (keyword == null || keyword.isBlank() || keyword.trim().length() < MIN_KEYWORD_LENGTH) {
            return Collections.emptyList();
        }

        String trimmed = keyword.trim().toLowerCase();
        int actualLimit = Math.clamp(limit, 1, MAX_RESULT_LIMIT);

        return comicSearchRepository
                .searchByKeyword(trimmed, PageRequest.of(0, actualLimit))
                .getContent()
                .stream()
                .map(this::toDetailSearchResult)
                .toList();
    }

    private ComicSearchResult toQuickSearchResult(ComicDocument document) {
        return ComicSearchResult.builder()
                .id(document.getId())
                .title(document.getTitle())
                .build();
    }

    private ComicDetailSearchResult toDetailSearchResult(ComicDocument document) {
        return ComicDetailSearchResult.builder()
                .id(document.getId())
                .title(document.getTitle())
                .author(document.getAuthor())
                .coverImageUrl(document.getCoverImageUrl())
                .averageRating(document.getAverageRating())
                .listType(document.getFormat())
                .build();
    }
}
