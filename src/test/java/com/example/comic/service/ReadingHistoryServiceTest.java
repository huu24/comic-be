package com.example.comic.service;

import com.example.comic.exception.NotFoundException;
import com.example.comic.model.ReadingHistory;
import com.example.comic.model.User;
import com.example.comic.model.dto.MessageResponse;
import com.example.comic.model.dto.MessageStatusResponse;
import com.example.comic.model.dto.ReadingHistoryResponse;
import com.example.comic.model.dto.ReadingHistorySyncRequest;
import com.example.comic.repository.ReadingHistoryRepository;
import com.example.comic.repository.ChapterRepository;
import com.example.comic.model.Comic;
import com.example.comic.model.dto.PageDataResponse;
import com.example.comic.model.dto.UserReadingHistoryItemResponse;
import com.example.comic.repository.ComicRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadingHistoryServiceTest {

    @Mock
    private ReadingHistoryRepository readingHistoryRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ChapterRepository chapterRepository;

    @Mock
    private ComicRepository comicRepository;

    private ReadingHistoryService readingHistoryService;

    @BeforeEach
    void setUp() {
        readingHistoryService = new ReadingHistoryService(readingHistoryRepository, currentUserService, chapterRepository, comicRepository);
    }

    @Test
    void getByComicId_shouldReturnHistory() {
        User user = user(1L);
        ReadingHistory history = ReadingHistory.builder().userId(1L).comicId(10L).chapterId(5L).lastPageRead(12).updatedAt(Instant.parse("2025-01-01T00:00:00Z")).build();
        com.example.comic.model.Chapter chapter = com.example.comic.model.Chapter.builder().id(5L).chapterNumber(3).build();
        when(currentUserService.requireUser()).thenReturn(user);
        when(readingHistoryRepository.findByUserIdAndComicId(1L, 10L)).thenReturn(Optional.of(history));
        when(chapterRepository.findById(5L)).thenReturn(Optional.of(chapter));

        ReadingHistoryResponse response = readingHistoryService.getByComicId(10L);

        assertEquals(10L, response.getComicId());
        assertEquals(3, response.getChapterNumber());
        assertEquals(12, response.getLastPageRead());
    }

    @Test
    void sync_shouldCreateNewHistoryWhenMissing() {
        User user = user(1L);
        when(currentUserService.requireUser()).thenReturn(user);
        when(readingHistoryRepository.findByUserIdAndComicId(1L, 10L)).thenReturn(Optional.empty());
        when(readingHistoryRepository.save(any(ReadingHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Object response = readingHistoryService.sync(
            ReadingHistorySyncRequest.builder()
                .comicId(10L)
                .chapterId(5L)
                .lastPageRead(12)
                .clientUpdatedAt("2025-01-02T00:00:00Z")
                .build()
        );

        assertInstanceOf(MessageResponse.class, response);
        assertEquals("Tiến độ đọc đã được lưu.", ((MessageResponse) response).getMessage());
        verify(readingHistoryRepository).save(any(ReadingHistory.class));
    }

    @Test
    void sync_shouldIgnoreOlderClientTime() {
        User user = user(1L);
        ReadingHistory history = ReadingHistory.builder().userId(1L).comicId(10L).chapterId(5L).lastPageRead(12).updatedAt(Instant.parse("2025-01-02T00:00:00Z")).build();
        com.example.comic.model.Chapter clientChapter = com.example.comic.model.Chapter.builder().id(6L).chapterNumber(4).build();
        com.example.comic.model.Chapter serverChapter = com.example.comic.model.Chapter.builder().id(5L).chapterNumber(3).build();
        when(currentUserService.requireUser()).thenReturn(user);
        when(readingHistoryRepository.findByUserIdAndComicId(1L, 10L)).thenReturn(Optional.of(history));
        when(chapterRepository.findById(6L)).thenReturn(Optional.of(clientChapter));
        when(chapterRepository.findById(5L)).thenReturn(Optional.of(serverChapter));

        Object response = readingHistoryService.sync(
            ReadingHistorySyncRequest.builder()
                .comicId(10L)
                .chapterId(6L)
                .lastPageRead(20)
                .clientUpdatedAt("2025-01-01T00:00:00Z")
                .build()
        );

        assertInstanceOf(MessageStatusResponse.class, response);
        assertEquals("IGNORED", ((MessageStatusResponse) response).getStatus());
    }

    @Test
    void getByComicId_shouldThrowWhenHistoryAndChaptersMissing() {
        User user = user(1L);
        when(currentUserService.requireUser()).thenReturn(user);
        when(readingHistoryRepository.findByUserIdAndComicId(1L, 99L)).thenReturn(Optional.empty());
        when(chapterRepository.findByComicIdOrderByChapterNumberAsc(99L)).thenReturn(java.util.Collections.emptyList());

        assertThrows(NotFoundException.class, () -> readingHistoryService.getByComicId(99L));
    }

    @Test
    void getByComicId_shouldCreateNewHistoryWhenMissingButChaptersExist() {
        User user = user(1L);
        com.example.comic.model.Chapter firstChapter = com.example.comic.model.Chapter.builder().id(100L).chapterNumber(1).build();
        when(currentUserService.requireUser()).thenReturn(user);
        when(readingHistoryRepository.findByUserIdAndComicId(1L, 99L)).thenReturn(Optional.empty());
        when(chapterRepository.findByComicIdOrderByChapterNumberAsc(99L)).thenReturn(java.util.List.of(firstChapter));
        when(readingHistoryRepository.save(any(ReadingHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chapterRepository.findById(100L)).thenReturn(Optional.of(firstChapter));

        ReadingHistoryResponse response = readingHistoryService.getByComicId(99L);

        assertEquals(99L, response.getComicId());
        assertEquals(1, response.getChapterNumber());
        assertEquals(0, response.getLastPageRead());
        verify(readingHistoryRepository).save(any(ReadingHistory.class));
    }

    @Test
    void sync_shouldUpdateWhenClientTimeIsNewer() {
        User user = user(1L);
        ReadingHistory history = ReadingHistory.builder().userId(1L).comicId(10L).chapterId(5L).lastPageRead(12).updatedAt(Instant.parse("2025-01-01T00:00:00Z")).build();
        com.example.comic.model.Chapter clientChapter = com.example.comic.model.Chapter.builder().id(6L).chapterNumber(4).build();
        com.example.comic.model.Chapter serverChapter = com.example.comic.model.Chapter.builder().id(5L).chapterNumber(3).build();
        when(currentUserService.requireUser()).thenReturn(user);
        when(readingHistoryRepository.findByUserIdAndComicId(1L, 10L)).thenReturn(Optional.of(history));
        when(chapterRepository.findById(6L)).thenReturn(Optional.of(clientChapter));
        when(chapterRepository.findById(5L)).thenReturn(Optional.of(serverChapter));
        when(readingHistoryRepository.save(any(ReadingHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Object response = readingHistoryService.sync(
            ReadingHistorySyncRequest.builder()
                .comicId(10L)
                .chapterId(6L)
                .lastPageRead(20)
                .clientUpdatedAt("2025-01-02T00:00:00Z")
                .build()
        );

        assertInstanceOf(MessageResponse.class, response);
        assertEquals("Tiến độ đọc đã được lưu.", ((MessageResponse) response).getMessage());
        assertEquals(6L, history.getChapterId());
        assertEquals(20, history.getLastPageRead());
        verify(readingHistoryRepository).save(history);
    }

    @Test
    void getReadingHistory_shouldReturnPaginatedHistoriesWithComicAndChapterDetails() {
        User user = user(1L);
        when(currentUserService.requireUser()).thenReturn(user);

        ReadingHistory history = ReadingHistory.builder()
            .userId(1L)
            .comicId(10L)
            .chapterId(5L)
            .lastPageRead(2)
            .updatedAt(Instant.parse("2025-01-01T00:00:00Z"))
            .build();
        
        Pageable pageable = PageRequest.of(0, 10, Sort.by("updatedAt").descending());
        Page<ReadingHistory> page = new PageImpl<>(java.util.List.of(history), pageable, 1);
        when(readingHistoryRepository.findByUserId(1L, pageable)).thenReturn(page);

        Comic comic = Comic.builder()
            .id(10L)
            .title("Comic Title")
            .author("Author")
            .coverImageUrl("http://cover.jpg")
            .originalLanguage("ja")
            .status("ONGOING")
            .format("MANGA")
            .averageRating(4.5)
            .build();
        when(comicRepository.findAllById(java.util.Set.of(10L))).thenReturn(java.util.List.of(comic));

        com.example.comic.model.Chapter chapter = com.example.comic.model.Chapter.builder()
            .id(5L)
            .chapterNumber(2)
            .build();
        when(chapterRepository.findAllById(java.util.Set.of(5L))).thenReturn(java.util.List.of(chapter));

        PageDataResponse<UserReadingHistoryItemResponse> response = readingHistoryService.getReadingHistory(0, 10);

        assertEquals(1, response.getContent().size());
        UserReadingHistoryItemResponse item = response.getContent().get(0);
        assertEquals(10L, item.getComicId());
        assertEquals("Comic Title", item.getTitle());
        assertEquals(2, item.getChapterNumber());
        assertEquals(2, item.getLastPageRead());
    }

    private static User user(Long id) {
        return User.builder().id(id).email("user@example.com").passwordHash("hash").fullName("User").build();
    }
}
