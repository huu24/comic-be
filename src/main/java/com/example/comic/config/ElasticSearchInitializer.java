package com.example.comic.config;

import com.example.comic.event.ComicSavedEvent;
import com.example.comic.model.Comic;
import com.example.comic.repository.ComicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticSearchInitializer {

    private final ComicRepository comicRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @EventListener(ApplicationReadyEvent.class)
    public void reindexAllComicsOnStartup() {
        log.info("Starting Elasticsearch re-index on startup...");
        List<Comic> allComics = comicRepository.findAll();
        for (Comic comic : allComics) {
            eventPublisher.publishEvent(new ComicSavedEvent(comic));
        }
        log.info("Elasticsearch re-index completed. Indexed {} comics.", allComics.size());
    }
}
