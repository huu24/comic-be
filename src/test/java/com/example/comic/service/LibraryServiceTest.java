package com.example.comic.service;

import com.example.comic.exception.NotFoundException;
import com.example.comic.model.Comic;
import com.example.comic.model.LibraryListType;
import com.example.comic.model.User;
import com.example.comic.model.UserLibrary;
import com.example.comic.model.dto.PageDataResponse;
import com.example.comic.model.dto.UserLibraryItemResponse;
import com.example.comic.model.dto.UserLibraryUpsertRequest;
import com.example.comic.repository.ComicRepository;
import com.example.comic.repository.UserLibraryRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibraryServiceTest {

    @Mock
    private UserLibraryRepository userLibraryRepository;

    @Mock
    private ComicRepository comicRepository;

    @Mock
    private CurrentUserService currentUserService;

    private LibraryService libraryService;

    @BeforeEach
    void setUp() {
        libraryService = new LibraryService(userLibraryRepository, comicRepository, currentUserService);
    }

    @Test
    void getLibraries_shouldMapComicInfo() {
        User user = user(1L);
        UserLibrary library = UserLibrary.builder().userId(1L).comicId(10L).listType(LibraryListType.READING).createdAt(Instant.parse("2025-01-01T00:00:00Z")).build();
        Comic comic = Comic.builder().id(10L).title("Comic A").coverImageUrl("cover.jpg").build();
        when(currentUserService.requireUser()).thenReturn(user);
        when(userLibraryRepository.findByUserId(1L, PageRequest.of(0, 20))).thenReturn(new PageImpl<>(List.of(library), PageRequest.of(0, 20), 1));
        when(comicRepository.findAllById(any())).thenReturn(List.of(comic));

        PageDataResponse<UserLibraryItemResponse> response = libraryService.getLibraries(null, 0, 20);

        assertEquals(1, response.getContent().size());
        assertEquals("Comic A", response.getContent().get(0).getTitle());
        assertEquals(LibraryListType.READING, response.getContent().get(0).getListType());
    }

    @Test
    void upsertLibrary_shouldCreateNewRecordWhenMissing() {
        User user = user(1L);
        when(currentUserService.requireUser()).thenReturn(user);
        when(comicRepository.findById(10L)).thenReturn(Optional.of(Comic.builder().id(10L).title("Comic A").build()));
        when(userLibraryRepository.findByUserIdAndComicId(1L, 10L)).thenReturn(Optional.empty());
        when(userLibraryRepository.save(any(UserLibrary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        libraryService.upsertLibrary(UserLibraryUpsertRequest.builder().comicId(10L).listType(LibraryListType.FAVORITE).build());

        ArgumentCaptor<UserLibrary> captor = ArgumentCaptor.forClass(UserLibrary.class);
        verify(userLibraryRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getUserId());
        assertEquals(LibraryListType.FAVORITE, captor.getValue().getListType());
        assertNotNull(captor.getValue().getCreatedAt());
    }

    @Test
    void removeFromLibrary_shouldDeleteEntry() {
        User user = user(1L);
        when(currentUserService.requireUser()).thenReturn(user);

        libraryService.removeFromLibrary(10L);

        verify(userLibraryRepository).deleteByUserIdAndComicId(1L, 10L);
    }

    @Test
    void getLibraries_shouldUseListTypeFilterAndHandleMissingComic() {
        User user = user(1L);
        UserLibrary library = UserLibrary.builder().userId(1L).comicId(10L).listType(LibraryListType.FAVORITE).createdAt(Instant.parse("2025-01-01T00:00:00Z")).build();
        when(currentUserService.requireUser()).thenReturn(user);
        when(userLibraryRepository.findByUserIdAndListType(1L, LibraryListType.FAVORITE, PageRequest.of(0, 100)))
            .thenReturn(new PageImpl<>(List.of(library), PageRequest.of(0, 100), 1));
        when(comicRepository.findAllById(any())).thenReturn(List.of());

        PageDataResponse<UserLibraryItemResponse> response = libraryService.getLibraries(LibraryListType.FAVORITE, -1, 1000);

        assertEquals(1, response.getContent().size());
        assertNull(response.getContent().get(0).getTitle());
    }

    @Test
    void upsertLibrary_shouldUpdateExistingRecord() {
        User user = user(1L);
        UserLibrary existing = UserLibrary.builder().userId(1L).comicId(10L).listType(LibraryListType.READING).createdAt(Instant.parse("2025-01-01T00:00:00Z")).build();
        when(currentUserService.requireUser()).thenReturn(user);
        when(comicRepository.findById(10L)).thenReturn(Optional.of(Comic.builder().id(10L).title("Comic A").build()));
        when(userLibraryRepository.findByUserIdAndComicId(1L, 10L)).thenReturn(Optional.of(existing));
        when(userLibraryRepository.save(any(UserLibrary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        libraryService.upsertLibrary(UserLibraryUpsertRequest.builder().comicId(10L).listType(LibraryListType.FAVORITE).build());

        assertEquals(LibraryListType.FAVORITE, existing.getListType());
        verify(userLibraryRepository).save(existing);
    }

    @Test
    void upsertLibrary_shouldThrowWhenComicMissing() {
        User user = user(1L);
        when(currentUserService.requireUser()).thenReturn(user);
        when(comicRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(
            NotFoundException.class,
            () -> libraryService.upsertLibrary(UserLibraryUpsertRequest.builder().comicId(10L).listType(LibraryListType.FAVORITE).build())
        );
    }

    private static User user(Long id) {
        return User.builder().id(id).email("user@example.com").passwordHash("hash").fullName("User").build();
    }
}
