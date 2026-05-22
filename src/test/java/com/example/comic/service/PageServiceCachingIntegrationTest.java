package com.example.comic.service;

import com.example.comic.model.ChapterPage;
import com.example.comic.model.dto.PageDetailResponse;
import com.example.comic.repository.ChapterPageRepository;
import com.example.comic.repository.PageTranslationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@SpringBootTest
class PageServiceCachingIntegrationTest {

    @Autowired
    private PageService pageService;

    @MockitoBean
    private ChapterPageRepository chapterPageRepository;

    @MockitoBean
    private PageTranslationRepository pageTranslationRepository;

    @MockitoBean
    private MinioStorageService minioStorageService;

    @MockitoBean
    private org.springframework.mail.javamail.JavaMailSender javaMailSender;

    @Test
    void getPageDetail_shouldCacheResults() {
        ChapterPage mockPage = ChapterPage.builder()
                .id(1L)
                .chapterId(10L)
                .pageNumber(1)
                .imageUrl("images/page1.png")
                .cleanedImageUrl("images/page1_clean.png")
                .originalMetadataUrl("metadata/page1.json")
                .build();

        when(chapterPageRepository.findById(1L)).thenReturn(Optional.of(mockPage));
        when(pageTranslationRepository.findByPageIdAndLang(1L, "vi")).thenReturn(Optional.empty());
        when(minioStorageService.downloadObjectAsString("metadata/page1.json")).thenReturn("{\"bubbles\":[]}");
        when(minioStorageService.resolvePublicUrl(any())).thenReturn("http://localhost:9000/comic/images/page1.png");

        // First call - should invoke repositories & storage service
        PageDetailResponse response1 = pageService.getPageDetail(1L, "vi");
        assertNotNull(response1);

        // Second call - should fetch from cache and NOT invoke repositories/storage service again
        PageDetailResponse response2 = pageService.getPageDetail(1L, "vi");
        assertNotNull(response2);

        // Verify that findById was only called once (due to caching)
        verify(chapterPageRepository, times(1)).findById(1L);
    }
}
