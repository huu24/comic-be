package com.example.comic.service;

import com.example.comic.exception.NotFoundException;
import com.example.comic.model.ReadingHistory;
import com.example.comic.model.User;
import com.example.comic.model.dto.MessageResponse;
import com.example.comic.model.dto.MessageStatusResponse;
import com.example.comic.model.dto.ReadingHistoryResponse;
import com.example.comic.model.dto.ReadingHistorySyncRequest;
import com.example.comic.repository.ReadingHistoryRepository;
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

    private ReadingHistoryService readingHistoryService;

    @BeforeEach
    void setUp() {
        readingHistoryService = new ReadingHistoryService(readingHistoryRepository, currentUserService);
    }

    @Test
    void getByComicId_shouldReturnHistory() {
        User user = user(1L);
        ReadingHistory history = ReadingHistory.builder().userId(1L).comicId(10L).chapterId(5L).lastPageRead(12).updatedAt(Instant.parse("2025-01-01T00:00:00Z")).build();
        when(currentUserService.requireUser()).thenReturn(user);
        when(readingHistoryRepository.findByUserIdAndComicId(1L, 10L)).thenReturn(Optional.of(history));

        ReadingHistoryResponse response = readingHistoryService.getByComicId(10L);

        assertEquals(10L, response.getComicId());
        assertEquals(5L, response.getChapterId());
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
        when(currentUserService.requireUser()).thenReturn(user);
        when(readingHistoryRepository.findByUserIdAndComicId(1L, 10L)).thenReturn(Optional.of(history));

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
        when(currentUserService.requireUser()).thenReturn(user);
        when(readingHistoryRepository.findByUserIdAndComicId(1L, 10L)).thenReturn(Optional.of(history));
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
