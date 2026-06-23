package com.example.comic.service;

import com.example.comic.event.PageReadEvent;
import com.example.comic.exception.NotFoundException;
import com.example.comic.model.*;
import com.example.comic.model.dto.PageDetailResponse;
import com.example.comic.model.dto.PageImagesResponse;
import com.example.comic.repository.*;
import com.example.comic.security.SecurityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.Optional;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.security.SecurityUtil;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;

@Service
@RequiredArgsConstructor
public class PageService {

    private final ChapterPageRepository chapterPageRepository;
    private final PageCachedService pageCachedService;
    private final ApplicationEventPublisher eventPublisher;
    private final SecurityUtils securityUtils;
    private final UserRepository userRepository;
    private final ChapterRepository chapterRepository;
    private final ReadingHistoryRepository readingHistoryRepository;
    @Transactional
    public PageDetailResponse getPageDetail(Long pageId, String lang) {
        PageDetailResponse pageDetailResponse = pageCachedService.getPageDetailCached(pageId, lang);
        updateReadingHistory(pageId);
        return pageDetailResponse;
    }

    private void updateReadingHistory(Long pageId) {
        String email = securityUtils.getCurrentUserEmail();
        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() -> new EntityNotFoundException(email));
        Long userId = user.getId();
        ChapterPage chapterPage =
                chapterPageRepository
                        .findById(pageId)
                        .orElseThrow(() -> new EntityNotFoundException(String.valueOf(pageId)));
        Chapter chapter = chapterRepository.findById(chapterPage.getChapterId())
                .orElseThrow(() -> new EntityNotFoundException(String.valueOf(pageId)));
        Long comicId = chapter.getComicId();
        ReadingHistory history =
                readingHistoryRepository
                        .findByUserIdAndComicId(
                                userId,
                                comicId
                        )
                        .orElse(null);
        if (history == null) {
            history =
                    ReadingHistory.builder()
                            .userId(userId)
                            .comicId(comicId)
                            .chapterId(chapter.getId())
                            .lastPageRead(chapterPage.getPageNumber())
                            .build();
            readingHistoryRepository.save(history);
        } else {
            history.setChapterId(chapter.getId());
            history.setLastPageRead(chapterPage.getPageNumber());
            history.setUpdatedAt(Instant.now());
        }
        eventPublisher.publishEvent(
                new PageReadEvent(
                        userId,
                        comicId,
                        chapter.getId(),
                        pageId,
                        Instant.now()
                )
        );
    }





}
