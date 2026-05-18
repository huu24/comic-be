package com.example.comic.service;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.example.comic.model.document.ComicDocument;
import com.example.comic.model.dto.ComicDetailSearchResult;
import com.example.comic.model.dto.ComicSearchResult;
import com.example.comic.model.dto.PageDataResponse;
import com.example.comic.repository.search.ComicSearchRepository;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ComicSearchService {

    private static final int MIN_KEYWORD_LENGTH = 1;
    private static final int DEFAULT_RESULT_LIMIT = 20;
    private static final int MAX_RESULT_LIMIT = 50;
    private static final int CHAR_COUNT_THRESHOLD = 3;

    private final ComicSearchRepository comicSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    @Cacheable(value = "comicSearch", key = "#keyword + '-' + #limit")
    public List<ComicSearchResult> searchComics(String keyword, int limit) {
        if (keyword == null || keyword.isBlank() || keyword.trim().length() < MIN_KEYWORD_LENGTH) {
            return Collections.emptyList();
        }

        String trimmed = keyword.trim().toLowerCase();
        int actualLimit = Math.clamp(limit, 1, MAX_RESULT_LIMIT);

        Page<ComicDocument> page = executeSearch(trimmed, PageRequest.of(0, actualLimit));

        return page.getContent()
                .stream()
                .map(this::toQuickSearchResult)
                .toList();
    }

    @Cacheable(value = "comicSearchDetail", key = "#keyword + '-' + #limit")
    public PageDataResponse<ComicDetailSearchResult> searchComicsDetail(String keyword, int limit) {
        if (keyword == null || keyword.isBlank() || keyword.trim().length() < MIN_KEYWORD_LENGTH) {
            return PageDataResponse.<ComicDetailSearchResult>builder()
                    .content(Collections.emptyList())
                    .pageNo(0)
                    .pageSize(0)
                    .totalElements(0)
                    .totalPages(0)
                    .last(true)
                    .build();
        }

        String trimmed = keyword.trim().toLowerCase();
        int actualLimit = Math.clamp(limit, 1, MAX_RESULT_LIMIT);

        Page<ComicDocument> page = executeSearch(trimmed, PageRequest.of(0, actualLimit));

        List<ComicDetailSearchResult> content = page.getContent()
                .stream()
                .map(this::toDetailSearchResult)
                .toList();

        return PageDataResponse
                .<ComicDetailSearchResult>builder()
                .content(content)
                .pageNo(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    private Page<ComicDocument> executeSearch(String keyword, PageRequest pageable) {
        int charCount = keyword.trim().length();

        NativeQuery nativeQuery;
        if (charCount < CHAR_COUNT_THRESHOLD) {
            nativeQuery = buildShortKeywordQuery(keyword, pageable);
        } else {
            nativeQuery = buildFuzzyQuery(keyword, pageable);
        }

        SearchHits<ComicDocument> searchHits = elasticsearchOperations.search(nativeQuery, ComicDocument.class);

        List<ComicDocument> content = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();

        return new PageImpl<>(content, pageable, searchHits.getTotalHits());
    }

    private NativeQuery buildShortKeywordQuery(String keyword, PageRequest pageable) {
        Query matchPhrasePrefix = Query.of(q -> q
                .matchPhrasePrefix(mpp -> mpp
                        .field("title")
                        .query(keyword)
                        .maxExpansions(10)
                        .boost(3.0f)
                )
        );

        Query matchTitle = Query.of(q -> q
                .match(m -> m
                        .field("title")
                        .query(keyword)
                        .boost(2.0f)
                )
        );

        return NativeQuery.builder()
                .withQuery(q -> q
                        .bool(b -> b
                                .should(matchPhrasePrefix)
                                .should(matchTitle)
                                .minimumShouldMatch("1")
                        )
                )
                .withPageable(pageable)
                .build();
    }

    private NativeQuery buildFuzzyQuery(String keyword, PageRequest pageable) {
        Query matchPhrasePrefix = Query.of(q -> q
                .matchPhrasePrefix(mpp -> mpp
                        .field("title")
                        .query(keyword)
                        .maxExpansions(10)
                        .boost(4.0f)
                )
        );

        Query matchTitle = Query.of(q -> q
                .match(m -> m
                        .field("title")
                        .query(keyword)
                        .fuzziness("AUTO")
                        .minimumShouldMatch("70%")
                        .boost(3.0f)
                )
        );

        Query matchDescription = Query.of(q -> q
                .match(m -> m
                        .field("description")
                        .query(keyword)
                        .fuzziness("AUTO")
                        .minimumShouldMatch("70%")
                        .boost(0.5f)
                )
        );

        return NativeQuery.builder()
                .withQuery(q -> q
                        .bool(b -> b
                                .should(matchPhrasePrefix)
                                .should(matchTitle)
                                .should(matchDescription)
                                .minimumShouldMatch("1")
                        )
                )
                .withPageable(pageable)
                .build();
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
