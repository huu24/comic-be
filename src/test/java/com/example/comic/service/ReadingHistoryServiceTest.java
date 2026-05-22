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

    private ReadingHistoryService readingHistoryService;

    @BeforeEach
    void setUp() {
        readingHistoryService = new ReadingHistoryService(readingHistoryRepository, currentUserService, chapterRepository);
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
    void getByComicId_shouldThrowWhenHistoryMissing() {
        User user = user(1L);
        when(currentUserService.requireUser()).thenReturn(user);
        when(readingHistoryRepository.findByUserIdAndComicId(1L, 99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> readingHistoryService.getByComicId(99L));
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

    private static User user(Long id) {
        return User.builder().id(id).email("user@example.com").passwordHash("hash").fullName("User").build();
    }
}
