package com.example.comic.service;

import com.example.comic.exception.NotFoundException;
import com.example.comic.model.ChapterPage;
import com.example.comic.model.PageTranslation;
import com.example.comic.model.dto.PageDetailResponse;
import com.example.comic.repository.ChapterPageRepository;
import com.example.comic.repository.PageTranslationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PageServiceTest {

    private ChapterPageRepository chapterPageRepository;
    private PageTranslationRepository pageTranslationRepository;
    private MinioStorageService minioStorageService;
    private ObjectMapper objectMapper;
    private PageService pageService;

    @BeforeEach
    void setUp() {
        chapterPageRepository = mock(ChapterPageRepository.class);
        pageTranslationRepository = mock(PageTranslationRepository.class);
        minioStorageService = mock(MinioStorageService.class);
        objectMapper = new ObjectMapper();
        pageService = new PageService(
                chapterPageRepository,
                pageTranslationRepository,
                minioStorageService,
                objectMapper);
    }

    @Test
    void getPageDetail_shouldThrowWhenPageNotFound() {
        when(chapterPageRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> pageService.getPageDetail(99L, "vi"));
    }

    @Test
    void getPageDetail_shouldReturnWithoutTranslation() {
        ChapterPage page = ChapterPage.builder()
                .id(1L).chapterId(10L).pageNumber(5)
                .imageUrl("pages/1.png").cleanedImageUrl("cleaned/1.png")
                .originalMetadataUrl("metadata/original.json")
                .build();
        when(chapterPageRepository.findById(1L)).thenReturn(Optional.of(page));
        when(pageTranslationRepository.findByPageIdAndLang(1L, "vi")).thenReturn(Optional.empty());
        when(minioStorageService.downloadObjectAsString("metadata/original.json"))
                .thenReturn("{\"page_id\":\"page_01\",\"bubbles\":[{\"id\":1,\"original_text\":\"hello\"}]}");
        when(minioStorageService.resolvePublicUrl("pages/1.png")).thenReturn("http://cdn/pages/1.png");
        when(minioStorageService.resolvePublicUrl("cleaned/1.png")).thenReturn("http://cdn/cleaned/1.png");

        PageDetailResponse response = pageService.getPageDetail(1L, "vi");

        assertEquals(1L, response.getPageId());
        assertEquals(10L, response.getChapterId());
        assertEquals(5, response.getPageNumber());
        assertEquals("http://cdn/pages/1.png", response.getImages().getOriginalUrl());
        assertEquals("http://cdn/cleaned/1.png", response.getImages().getInpaintedUrl());
        assertNotNull(response.getBubbles());
        assertEquals(1, response.getBubbles().size());
        assertNull(response.getBubbles().get(0).get("full_translation"));
    }

    @Test
    void getPageDetail_shouldMergeTranslation() {
        ChapterPage page = ChapterPage.builder()
                .id(1L).chapterId(10L).pageNumber(5)
                .imageUrl("pages/1.png").cleanedImageUrl("cleaned/1.png")
                .originalMetadataUrl("metadata/original.json")
                .build();
        PageTranslation translation = PageTranslation.builder()
                .id(100L).pageId(1L).lang("vi")
                .translationMetadataUrl("metadata/translation_vi.json")
                .build();

        when(chapterPageRepository.findById(1L)).thenReturn(Optional.of(page));
        when(pageTranslationRepository.findByPageIdAndLang(1L, "vi")).thenReturn(Optional.of(translation));
        when(minioStorageService.downloadObjectAsString("metadata/original.json"))
                .thenReturn(
                        "{\"page_id\":\"page_01\",\"bubbles\":[{\"id\":1,\"original_text\":\"えー\",\"chunks\":[]}]}");
        when(minioStorageService.downloadObjectAsString("metadata/translation_vi.json"))
                .thenReturn(
                        "{\"page_id\":\"page_01\",\"bubbles\":[{\"id\":1,\"full_translation\":\"À này\",\"chunk_meanings\":[{\"c1\":\"À thì\"}]}]}");
        when(minioStorageService.resolvePublicUrl("pages/1.png")).thenReturn("http://cdn/pages/1.png");
        when(minioStorageService.resolvePublicUrl("cleaned/1.png")).thenReturn("http://cdn/cleaned/1.png");

        PageDetailResponse response = pageService.getPageDetail(1L, "vi");

        assertNotNull(response.getBubbles());
        assertEquals(1, response.getBubbles().size());
        assertEquals("À này", response.getBubbles().get(0).get("full_translation").asText());
        assertNotNull(response.getBubbles().get(0).get("chunk_meanings"));
        // original fields preserved
        assertEquals("えー", response.getBubbles().get(0).get("original_text").asText());
    }

    @Test
    void getPageDetail_shouldHandleNullOriginalMetadataUrl() {
        ChapterPage page = ChapterPage.builder()
                .id(1L).chapterId(10L).pageNumber(5)
                .imageUrl("pages/1.png").cleanedImageUrl(null)
                .originalMetadataUrl(null)
                .build();
        when(chapterPageRepository.findById(1L)).thenReturn(Optional.of(page));
        when(pageTranslationRepository.findByPageIdAndLang(1L, "vi")).thenReturn(Optional.empty());
        when(minioStorageService.resolvePublicUrl("pages/1.png")).thenReturn("http://cdn/pages/1.png");
        when(minioStorageService.resolvePublicUrl(null)).thenReturn(null);

        PageDetailResponse response = pageService.getPageDetail(1L, "vi");

        assertNull(response.getBubbles());
        assertEquals("http://cdn/pages/1.png", response.getImages().getOriginalUrl());
    }

    @Test
    void getPageDetail_shouldHandleMultipleBubblesAndPartialMatch() {
        ChapterPage page = ChapterPage.builder()
                .id(1L).chapterId(10L).pageNumber(1)
                .imageUrl("pages/1.png").cleanedImageUrl("cleaned/1.png")
                .originalMetadataUrl("metadata/original.json")
                .build();
        PageTranslation translation = PageTranslation.builder()
                .id(100L).pageId(1L).lang("en")
                .translationMetadataUrl("metadata/translation_en.json")
                .build();

        when(chapterPageRepository.findById(1L)).thenReturn(Optional.of(page));
        when(pageTranslationRepository.findByPageIdAndLang(1L, "en")).thenReturn(Optional.of(translation));
        when(minioStorageService.downloadObjectAsString("metadata/original.json"))
                .thenReturn("{\"bubbles\":[{\"id\":1,\"original_text\":\"A\"},{\"id\":2,\"original_text\":\"B\"}]}");
        when(minioStorageService.downloadObjectAsString("metadata/translation_en.json"))
                .thenReturn("{\"bubbles\":[{\"id\":2,\"full_translation\":\"B translated\"}]}");
        when(minioStorageService.resolvePublicUrl("pages/1.png")).thenReturn("http://cdn/pages/1.png");
        when(minioStorageService.resolvePublicUrl("cleaned/1.png")).thenReturn("http://cdn/cleaned/1.png");

        PageDetailResponse response = pageService.getPageDetail(1L, "en");

        assertEquals(2, response.getBubbles().size());
        // bubble id=1 should NOT have translation
        assertNull(response.getBubbles().get(0).get("full_translation"));
        // bubble id=2 should have translation
        assertEquals("B translated", response.getBubbles().get(1).get("full_translation").asText());
    }
}
