package com.example.comic.service;

import com.example.comic.exception.NotFoundException;
import com.example.comic.model.ChapterPage;
import com.example.comic.model.PageTranslation;
import com.example.comic.model.dto.PageDetailResponse;
import com.example.comic.model.dto.PageImagesResponse;
import com.example.comic.repository.ChapterPageRepository;
import com.example.comic.repository.PageTranslationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PageService {

    private final ChapterPageRepository chapterPageRepository;
    private final PageTranslationRepository pageTranslationRepository;
    private final MinioStorageService minioStorageService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public PageDetailResponse getPageDetail(Long pageId, String lang) {
        ChapterPage page = chapterPageRepository
                .findById(pageId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trang truyện."));

        String originalMetadataUrl = page.getOriginalMetadataUrl();

        JsonNode originalBubbles = null;
        if (originalMetadataUrl != null && !originalMetadataUrl.isBlank()) {
            String originalJson = minioStorageService.downloadObjectAsString(originalMetadataUrl);
            JsonNode originalRoot = parseJson(originalJson);
            if (originalRoot != null) {
                originalBubbles = originalRoot.path("bubbles");
            }
        }

        Optional<PageTranslation> translationOpt = pageTranslationRepository.findByPageIdAndLang(pageId, lang);
        if (translationOpt.isPresent() && originalBubbles != null && originalBubbles.isArray()) {
            String translationJson = minioStorageService.downloadObjectAsString(
                    translationOpt.get().getTranslationMetadataUrl());
            JsonNode translationRoot = parseJson(translationJson);
            if (translationRoot != null) {
                mergeBubbles((ArrayNode) originalBubbles, translationRoot.path("bubbles"));
            }
        }

        return PageDetailResponse
                .builder()
                .pageId(page.getId())
                .chapterId(page.getChapterId())
                .pageNumber(page.getPageNumber())
                .images(
                        PageImagesResponse
                                .builder()
                                .originalUrl(minioStorageService.resolvePublicUrl(page.getImageUrl()))
                                .inpaintedUrl(minioStorageService.resolvePublicUrl(page.getCleanedImageUrl()))
                                .build())
                .bubbles(originalBubbles)
                .build();
    }

    /**
     * Merge translation bubbles into original bubbles by matching on "id" field.
     * For each matching bubble, adds "full_translation" and "chunk_meanings" from
     * the translation.
     */
    void mergeBubbles(ArrayNode originalBubbles, JsonNode translationBubbles) {
        if (translationBubbles == null || !translationBubbles.isArray()) {
            return;
        }

        for (JsonNode transBubble : translationBubbles) {
            int transId = transBubble.path("id").asInt(-1);
            if (transId < 0) {
                continue;
            }

            for (JsonNode origBubble : originalBubbles) {
                if (origBubble.path("id").asInt(-1) == transId && origBubble.isObject()) {
                    ObjectNode origObj = (ObjectNode) origBubble;
                    if (transBubble.has("full_translation")) {
                        origObj.set("full_translation", transBubble.get("full_translation"));
                    }
                    if (transBubble.has("chunk_meanings") && origObj.has("chunks")) {
                        JsonNode origChunks = origObj.get("chunks");
                        JsonNode transChunkMeanings = transBubble.get("chunk_meanings");
                        
                        if (origChunks.isArray() && transChunkMeanings.isArray()) {
                            for (JsonNode origChunk : origChunks) {
                                String chunkId = origChunk.path("chunk_id").asText();
                                for (JsonNode transChunk : transChunkMeanings) {
                                    if (chunkId.equals(transChunk.path("chunk_id").asText()) && origChunk.isObject()) {
                                        ObjectNode chunkObj = (ObjectNode) origChunk;
                                        if (transChunk.has("type")) {
                                            chunkObj.set("type", transChunk.get("type"));
                                        }
                                        if (transChunk.has("meaning")) {
                                            chunkObj.set("meaning", transChunk.get("meaning"));
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    }                   
                    break;
                }
            }
        }
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
}
