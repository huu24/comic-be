package com.example.comic.service;

import com.example.comic.event.ComicDeletedEvent;
import com.example.comic.event.ComicSavedEvent;
import com.example.comic.model.Comic;
import com.example.comic.model.document.ComicDocument;
import com.example.comic.repository.search.ComicSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchSyncListener {

    private final ComicSearchRepository comicSearchRepository;

    @Async
    @EventListener
    public void onComicSaved(ComicSavedEvent event) {
        Comic comic = event.getComic();
        log.debug("Syncing comic {} to Elasticsearch", comic.getId());

        ComicDocument document = ComicDocument.builder()
                .id(comic.getId())
                .title(comic.getTitle())
                .description(comic.getDescription())
                .author(comic.getAuthor())
                .coverImageUrl(comic.getCoverImageUrl())
                .originalLanguage(comic.getOriginalLanguage())
                .format(comic.getFormat())
                .status(comic.getStatus())
                .averageRating(comic.getAverageRating())
                .build();

        comicSearchRepository.save(document);
        log.info("Comic {} synced to Elasticsearch successfully", comic.getId());
    }

    @Async
    @EventListener
    public void onComicDeleted(ComicDeletedEvent event) {
        Long comicId = event.getComicId();
        log.debug("Removing comic {} from Elasticsearch", comicId);

        try {
            comicSearchRepository.deleteById(comicId);
            log.info("Comic {} removed from Elasticsearch successfully", comicId);
        } catch (Exception ex) {
            log.warn("Failed to remove comic {} from Elasticsearch: {}", comicId, ex.getMessage());
        }
    }
}
