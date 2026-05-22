package com.example.comic.config;

import com.example.comic.event.ComicSavedEvent;
import com.example.comic.model.Comic;
import com.example.comic.repository.ComicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticSearchInitializer {

    private final ComicRepository comicRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void reindexAllComicsOnStartup() {
        log.info("Starting Elasticsearch re-index on startup...");
        int page = 0;
        int size = 100;
        long totalIndexed = 0;
        Page<Comic> comicPage;
        do {
            comicPage = comicRepository.findAll(PageRequest.of(page, size));
            for (Comic comic : comicPage.getContent()) {
                eventPublisher.publishEvent(new ComicSavedEvent(comic));
                totalIndexed++;
            }
            page++;
        } while (comicPage.hasNext());
        log.info("Elasticsearch re-index completed. Indexed {} comics.", totalIndexed);
    }
}
