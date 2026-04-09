package com.example.comic.service;

import com.example.comic.exception.NotFoundException;
import com.example.comic.model.Chapter;
import com.example.comic.model.ChapterPage;
import com.example.comic.model.Comic;
import com.example.comic.model.ComicRating;
import com.example.comic.model.User;
import com.example.comic.model.dto.ChapterPageResponse;
import com.example.comic.model.dto.ComicRatingResponse;
import com.example.comic.model.dto.ComicSummaryResponse;
import com.example.comic.model.dto.PageDataResponse;
import com.example.comic.repository.ChapterPageRepository;
import com.example.comic.repository.ChapterRepository;
import com.example.comic.repository.ComicRatingRepository;
import com.example.comic.repository.ComicRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ComicService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final ComicRepository comicRepository;
    private final ChapterRepository chapterRepository;
    private final ChapterPageRepository chapterPageRepository;
    private final ComicRatingRepository comicRatingRepository;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public PageDataResponse<ComicSummaryResponse> getComics(String keyword, Long categoryId, int page, int size) {
        Pageable pageable = PageRequest.of(normalizePage(page), normalizeSize(size));
        Page<Comic> comics = comicRepository.search(keyword, categoryId, pageable);
        List<ComicSummaryResponse> content = comics
            .getContent()
            .stream()
            .map(c ->
                ComicSummaryResponse
                    .builder()
                    .id(c.getId())
                    .title(c.getTitle())
                    .coverImageUrl(c.getCoverImageUrl())
                    .format(c.getFormat())
                    .averageRating(c.getAverageRating() == null ? 0D : c.getAverageRating())
                    .build()
            )
            .toList();

        return PageDataResponse
            .<ComicSummaryResponse>builder()
            .content(content)
            .pageNo(comics.getNumber())
            .pageSize(comics.getSize())
            .totalElements(comics.getTotalElements())
            .totalPages(comics.getTotalPages())
            .last(comics.isLast())
            .build();
    }

    @Transactional(readOnly = true)
    public List<ChapterPageResponse> getChapterPages(Long chapterId) {
        Chapter chapter = chapterRepository
            .findById(chapterId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy chương truyện."));

        List<ChapterPage> pages = chapterPageRepository.findByChapterIdOrderByPageNumberAsc(chapter.getId());

        return pages
            .stream()
            .map(page ->
                ChapterPageResponse
                    .builder()
                    .id(page.getId())
                    .pageNumber(page.getPageNumber())
                    .imageUrl(page.getImageUrl())
                    .cleanedImageUrl(page.getCleanedImageUrl())
                    .aiMetadata(parseJson(page.getAiMetadata()))
                    .build()
            )
            .toList();
    }

    @Transactional
    public ComicRatingResponse rateComic(Long comicId, int score) {
        User current = currentUserService.requireUser();
        Comic comic = comicRepository
            .findById(comicId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ truyện."));

        ComicRating rating = comicRatingRepository
            .findByUserIdAndComicId(current.getId(), comicId)
            .orElse(
                ComicRating.builder().userId(current.getId()).comicId(comicId).build()
            );
        rating.setScore(score);
        comicRatingRepository.save(rating);

        Double avg = comicRatingRepository.avgScoreByComicId(comicId);
        long total = comicRatingRepository.countByComicId(comicId);

        comic.setAverageRating(avg);
        comic.setTotalRatings((int) total);
        comicRepository.save(comic);

        return ComicRatingResponse.builder().newAverageRating(avg).totalRatings(total).build();
    }

    private JsonNode parseJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(raw);
        } catch (Exception ex) {
            return null;
        }
    }

    private int normalizePage(int page) {
        return Math.max(DEFAULT_PAGE, page);
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
