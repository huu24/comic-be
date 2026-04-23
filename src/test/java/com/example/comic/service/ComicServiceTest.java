package com.example.comic.service;

import com.example.comic.exception.AlreadyExistsException;
import com.example.comic.exception.NotFoundException;
import com.example.comic.model.Chapter;
import com.example.comic.model.ChapterPage;
import com.example.comic.model.Comic;
import com.example.comic.model.ComicRating;
import com.example.comic.model.User;
import com.example.comic.model.dto.ChapterCreateRequest;
import com.example.comic.model.dto.ChapterCreateResponse;
import com.example.comic.model.dto.ChapterPageResponse;
import com.example.comic.model.dto.ComicCreateRequest;
import com.example.comic.model.dto.ComicCreateResponse;
import com.example.comic.model.dto.ComicRatingResponse;
import com.example.comic.model.dto.ComicSummaryResponse;
import com.example.comic.model.dto.PageDataResponse;
import com.example.comic.repository.ChapterPageRepository;
import com.example.comic.repository.ChapterRepository;
import com.example.comic.repository.ComicRatingRepository;
import com.example.comic.repository.ComicRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ComicServiceTest {

    private ComicRepository comicRepository;
    private ChapterRepository chapterRepository;
    private ChapterPageRepository chapterPageRepository;
    private ComicRatingRepository comicRatingRepository;
    private CurrentUserService currentUserService;
    private MinioStorageService minioStorageService;
    private ComicService comicService;

    @BeforeEach
    void setUp() {
        comicRepository = mock(ComicRepository.class);
        chapterRepository = mock(ChapterRepository.class);
        chapterPageRepository = mock(ChapterPageRepository.class);
        comicRatingRepository = mock(ComicRatingRepository.class);
        currentUserService = mock(CurrentUserService.class);
        minioStorageService = mock(MinioStorageService.class);
        comicService = new ComicService(
                comicRepository,
                chapterRepository,
                chapterPageRepository,
                comicRatingRepository,
                currentUserService,
                minioStorageService);
    }

    @Test
    void createComic_shouldTrimAndPersist() {
        User admin = user(1L, "admin@example.com");
        when(currentUserService.requireAdmin()).thenReturn(admin);
        when(comicRepository.save(any(Comic.class))).thenAnswer(invocation -> {
            Comic comic = invocation.getArgument(0);
            comic.setId(11L);
            return comic;
        });

        ComicCreateResponse response = comicService.createComic(
                ComicCreateRequest.builder()
                        .title("  One Piece  ")
                        .description("  Epic  ")
                        .author("  Oda  ")
                        .coverImageUrl("  cover.png  ")
                        .originalLanguage("  Japanese  ")
                        .format("  MANGA  ")
                        .status("  ACTIVE  ")
                        .build());

        assertEquals(11L, response.getId());
        assertEquals("One Piece", response.getTitle());
        assertEquals("Epic", response.getDescription());
        assertEquals("Oda", response.getAuthor());
        assertEquals("cover.png", response.getCoverImageUrl());
        assertEquals("Japanese", response.getOriginalLanguage());
        assertEquals("MANGA", response.getFormat());
        assertEquals("ACTIVE", response.getStatus());
    }

    @Test
    void createChapter_shouldReturnCreatedResponse() {
        when(currentUserService.requireAdmin()).thenReturn(user(1L, "admin@example.com"));
        when(comicRepository.findById(10L)).thenReturn(
                Optional.of(Comic.builder().id(10L).title("Comic").format("MANGA").status("ACTIVE").build()));
        when(chapterRepository.existsByComicIdAndChapterNumber(10L, 3)).thenReturn(false);
        when(chapterRepository.save(any(Chapter.class))).thenAnswer(invocation -> {
            Chapter chapter = invocation.getArgument(0);
            chapter.setId(77L);
            return chapter;
        });

        ChapterCreateResponse response = comicService.createChapter(10L,
                ChapterCreateRequest.builder().chapterNumber(3).title("  Start  ").build());

        assertEquals(77L, response.getId());
        assertEquals(10L, response.getComicId());
        assertEquals(3, response.getChapterNumber());
        assertEquals("Start", response.getTitle());
    }

    @Test
    void createChapter_shouldThrowWhenComicMissingOrDuplicate() {
        when(currentUserService.requireAdmin()).thenReturn(user(1L, "admin@example.com"));
        when(comicRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> comicService.createChapter(10L, ChapterCreateRequest.builder().chapterNumber(1).build()));

        when(comicRepository.findById(10L)).thenReturn(
                Optional.of(Comic.builder().id(10L).title("Comic").format("MANGA").status("ACTIVE").build()));
        when(chapterRepository.existsByComicIdAndChapterNumber(10L, 1)).thenReturn(true);

        assertThrows(AlreadyExistsException.class,
                () -> comicService.createChapter(10L, ChapterCreateRequest.builder().chapterNumber(1).build()));
    }

    @Test
    void getComics_shouldNormalizeFiltersAndMapResponse() {
        when(comicRepository.search(eq("Naruto"), eq(2L), eq("jp"), eq("active"), any()))
                .thenReturn(
                        new PageImpl<>(
                                List.of(Comic.builder().id(1L).title("Naruto").format("MANGA").status("ACTIVE")
                                        .averageRating(null).build()),
                                PageRequest.of(0, 20),
                                1));

        PageDataResponse<ComicSummaryResponse> response = comicService.getComics("  Naruto  ", 2L, "  jp  ",
                "  active  ", -1, 0);

        assertEquals(1, response.getContent().size());
        assertEquals(0D, response.getContent().get(0).getAverageRating());
        assertEquals(0, response.getPageNo());
        assertEquals(20, response.getPageSize());
        verify(comicRepository).search(eq("Naruto"), eq(2L), eq("jp"), eq("active"), any());
    }

    @Test
    void getChapterPages_shouldMapUrlsAndMetadata() {
        when(chapterRepository.findById(5L)).thenReturn(Optional.of(Chapter.builder().id(5L).comicId(1L).build()));
        when(chapterPageRepository.findByChapterIdOrderByPageNumberAsc(5L))
                .thenReturn(
                        List.of(
                                ChapterPage.builder().id(9L).chapterId(5L).pageNumber(1).imageUrl("pages/1.png")
                                        .cleanedImageUrl("cleaned/1.png").originalMetadataUrl("metadata/original.json")
                                        .build()));
        when(minioStorageService.resolvePublicUrl("pages/1.png")).thenReturn("http://cdn/pages/1.png");
        when(minioStorageService.resolvePublicUrl("cleaned/1.png")).thenReturn("http://cdn/cleaned/1.png");
        when(minioStorageService.resolvePublicUrl("metadata/original.json"))
                .thenReturn("http://cdn/metadata/original.json");

        List<ChapterPageResponse> pages = comicService.getChapterPages(5L);

        assertEquals(1, pages.size());
        assertEquals("http://cdn/pages/1.png", pages.get(0).getImageUrl());
        assertEquals("http://cdn/metadata/original.json", pages.get(0).getOriginalMetadataUrl());
    }

    @Test
    void getChapterPages_shouldThrowWhenChapterMissing() {
        when(chapterRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> comicService.getChapterPages(5L));
    }

    @Test
    void uploadChapterPages_shouldValidatePersistAndReturnResponses() {
        when(currentUserService.requireAdmin()).thenReturn(user(1L, "admin@example.com"));
        when(chapterRepository.findById(5L)).thenReturn(Optional.of(Chapter.builder().id(5L).comicId(1L).build()));
        when(chapterPageRepository.existsByChapterIdAndPageNumberBetween(5L, 1, 2)).thenReturn(false);
        when(minioStorageService.uploadComicPage(eq(5L), eq(1), any())).thenReturn("pages/1.png");
        when(minioStorageService.uploadComicPage(eq(5L), eq(2), any())).thenReturn("pages/2.png");
        when(chapterPageRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(minioStorageService.resolvePublicUrl(any()))
                .thenAnswer(invocation -> "public:" + invocation.getArgument(0));

        List<ChapterPageResponse> responses = comicService.uploadChapterPages(
                5L,
                1,
                List.of(
                        new MockMultipartFile("files", "a.png", "image/png", "aaa".getBytes()),
                        new MockMultipartFile("files", "b.jpg", "image/jpeg", "bbb".getBytes())));

        assertEquals(2, responses.size());
        assertEquals("public:pages/1.png", responses.get(0).getImageUrl());
        verify(chapterPageRepository).saveAll(any());
    }

    @Test
    void uploadChapterPages_shouldRejectInvalidInputsAndDuplicates() {
        when(currentUserService.requireAdmin()).thenReturn(user(1L, "admin@example.com"));
        when(chapterRepository.findById(5L)).thenReturn(Optional.of(Chapter.builder().id(5L).comicId(1L).build()));

        assertThrows(IllegalArgumentException.class, () -> comicService.uploadChapterPages(5L, 1, List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> comicService.uploadChapterPages(5L, 1,
                        List.of(new MockMultipartFile("files", "bad.txt", "text/plain", "x".getBytes()))));
        when(chapterPageRepository.existsByChapterIdAndPageNumberBetween(5L, 1, 1)).thenReturn(true);
        assertThrows(
                AlreadyExistsException.class,
                () -> comicService.uploadChapterPages(5L, 1,
                        List.of(new MockMultipartFile("files", "a.png", "image/png", "aaa".getBytes()))));
    }

    @Test
    void deleteChapterPage_shouldDeleteStorageAndDatabase() {
        when(currentUserService.requireAdmin()).thenReturn(user(1L, "admin@example.com"));
        when(chapterPageRepository.findById(9L)).thenReturn(
                Optional.of(ChapterPage.builder().id(9L).chapterId(5L).imageUrl("pages/1.png")
                        .cleanedImageUrl("cleaned/1.png").build()));

        comicService.deleteChapterPage(9L);

        verify(minioStorageService).deleteObject("pages/1.png");
        verify(minioStorageService).deleteObject("cleaned/1.png");
        verify(chapterPageRepository).delete(any(ChapterPage.class));
    }

    @Test
    void deleteChapterPages_shouldDeleteAllAndRejectMissingChapter() {
        when(currentUserService.requireAdmin()).thenReturn(user(1L, "admin@example.com"));
        when(chapterRepository.findById(5L)).thenReturn(Optional.of(Chapter.builder().id(5L).comicId(1L).build()));
        when(chapterPageRepository.findByChapterIdOrderByPageNumberAsc(5L)).thenReturn(
                List.of(
                        ChapterPage.builder().id(1L).imageUrl("pages/1.png").cleanedImageUrl("cleaned/1.png").build(),
                        ChapterPage.builder().id(2L).imageUrl("pages/2.png").cleanedImageUrl(null).build()));

        comicService.deleteChapterPages(5L);

        verify(chapterPageRepository).deleteByChapterId(5L);
        verify(minioStorageService).deleteObject("pages/1.png");
        verify(minioStorageService).deleteObject(null);

        when(chapterRepository.findById(6L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> comicService.deleteChapterPages(6L));
    }

    @Test
    void rateComic_shouldUpdateExistingRatingAndComic() {
        User current = user(3L, "user@example.com");
        when(currentUserService.requireUser()).thenReturn(current);
        when(comicRepository.findById(11L)).thenReturn(
                Optional.of(Comic.builder().id(11L).title("Comic").format("MANGA").status("ACTIVE").build()));
        when(comicRatingRepository.findByUserIdAndComicId(3L, 11L))
                .thenReturn(Optional.of(ComicRating.builder().userId(3L).comicId(11L).score(4).build()));
        when(comicRatingRepository.avgScoreByComicId(11L)).thenReturn(4.5);
        when(comicRatingRepository.countByComicId(11L)).thenReturn(10L);
        when(comicRepository.save(any(Comic.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(comicRatingRepository.save(any(ComicRating.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ComicRatingResponse response = comicService.rateComic(11L, 5);

        assertEquals(4.5, response.getNewAverageRating());
        assertEquals(10L, response.getTotalRatings());
        verify(comicRatingRepository).save(any(ComicRating.class));
        verify(comicRepository).save(any(Comic.class));
    }

    @Test
    void rateComic_shouldThrowWhenComicMissing() {
        when(currentUserService.requireUser()).thenReturn(user(3L, "user@example.com"));
        when(comicRepository.findById(11L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> comicService.rateComic(11L, 5));
    }

    private static User user(Long id, String email) {
        return User.builder().id(id).email(email).passwordHash("hash").fullName("User").build();
    }
}
