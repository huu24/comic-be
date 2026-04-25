package com.example.comic.service;

import com.example.comic.exception.NotFoundException;
import com.example.comic.model.Chapter;
import com.example.comic.model.ChapterComment;
import com.example.comic.model.User;
import com.example.comic.model.dto.ChapterCommentCreateRequest;
import com.example.comic.model.dto.ChapterCommentResponse;
import com.example.comic.model.dto.PageDataResponse;
import com.example.comic.repository.ChapterCommentRepository;
import com.example.comic.repository.ChapterRepository;
import com.example.comic.repository.UserRepository;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChapterCommentServiceTest {

    @Mock
    private ChapterCommentRepository chapterCommentRepository;

    @Mock
    private ChapterRepository chapterRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfanityFilterService profanityFilterService;

    private ChapterCommentService chapterCommentService;

    @BeforeEach
    void setUp() {
        chapterCommentService = new ChapterCommentService(
            chapterCommentRepository,
            chapterRepository,
            currentUserService,
            userRepository,
            profanityFilterService
        );
    }

    @Test
    void getComments_shouldReturnThreadedReplies() {
        Chapter chapter = Chapter.builder().id(10L).comicId(1L).chapterNumber(1).build();
        ChapterComment parent = ChapterComment.builder().id(100L).chapterId(10L).userId(1L).content("Parent").createdAt(Instant.parse("2025-01-01T00:00:00Z")).build();
        ChapterComment reply = ChapterComment.builder().id(101L).chapterId(10L).userId(2L).content("Reply").parentId(100L).createdAt(Instant.parse("2025-01-01T00:01:00Z")).build();
        User parentUser = User.builder().id(1L).email("a@example.com").fullName("Parent User").build();
        User replyUser = User.builder().id(2L).email("b@example.com").fullName("Reply User").build();

        when(chapterRepository.findById(10L)).thenReturn(Optional.of(chapter));
        when(chapterCommentRepository.findByChapterIdAndParentIdIsNullOrderByCreatedAtDesc(eq(10L), any())).thenReturn(new PageImpl<>(List.of(parent), PageRequest.of(0, 20), 1));
        when(chapterCommentRepository.findByParentIdInOrderByCreatedAtAsc(List.of(100L))).thenReturn(List.of(reply));
        when(userRepository.findAllById(any())).thenReturn(List.of(parentUser, replyUser));

        PageDataResponse<ChapterCommentResponse> response = chapterCommentService.getComments(10L, 0, 20);

        assertEquals(1, response.getContent().size());
        assertEquals("Parent User", response.getContent().get(0).getFullName());
        assertEquals(1, response.getContent().get(0).getReplies().size());
        assertEquals("Reply User", response.getContent().get(0).getReplies().get(0).getFullName());
    }

    @Test
    void getComments_shouldReturnAnonymousWhenUserMissing() {
        Chapter chapter = Chapter.builder().id(10L).comicId(1L).chapterNumber(1).build();
        ChapterComment parent = ChapterComment.builder().id(100L).chapterId(10L).userId(99L).content("Parent").createdAt(Instant.parse("2025-01-01T00:00:00Z")).build();

        when(chapterRepository.findById(10L)).thenReturn(Optional.of(chapter));
        when(chapterCommentRepository.findByChapterIdAndParentIdIsNullOrderByCreatedAtDesc(eq(10L), any())).thenReturn(new PageImpl<>(List.of(parent), PageRequest.of(0, 20), 1));
        when(chapterCommentRepository.findByParentIdInOrderByCreatedAtAsc(List.of(100L))).thenReturn(List.of());
        when(userRepository.findAllById(any())).thenReturn(List.of());

        PageDataResponse<ChapterCommentResponse> response = chapterCommentService.getComments(10L, 0, 20);

        assertEquals("Ẩn danh", response.getContent().get(0).getFullName());
    }

    @Test
    void getComments_shouldThrowWhenChapterMissing() {
        when(chapterRepository.findById(10L)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(NotFoundException.class, () -> chapterCommentService.getComments(10L, 0, 20));
    }

    @Test
    void create_shouldSanitizeAndSaveComment() {
        User current = User.builder().id(1L).email("user@example.com").fullName("User").build();
        Chapter chapter = Chapter.builder().id(10L).comicId(1L).build();
        when(currentUserService.requireUser()).thenReturn(current);
        when(chapterRepository.findById(10L)).thenReturn(Optional.of(chapter));
        when(profanityFilterService.sanitize("bad text")).thenReturn("*** text");
        when(chapterCommentRepository.save(any(ChapterComment.class))).thenAnswer(invocation -> {
            ChapterComment comment = invocation.getArgument(0);
            comment.setId(500L);
            return comment;
        });

        ChapterCommentResponse response = chapterCommentService.create(
            10L,
            ChapterCommentCreateRequest.builder().content(" bad text ").build()
        );

        assertEquals(500L, response.getId());
        assertEquals("*** text", response.getContent());
        assertNull(response.getParentId());

        ArgumentCaptor<ChapterComment> captor = ArgumentCaptor.forClass(ChapterComment.class);
        verify(chapterCommentRepository).save(captor.capture());
        assertEquals("*** text", captor.getValue().getContent());
    }

    @Test
    void create_shouldRejectMissingChapterAndMissingParent() {
        User current = User.builder().id(1L).email("user@example.com").fullName("User").build();
        when(currentUserService.requireUser()).thenReturn(current);
        when(chapterRepository.findById(10L)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(
            NotFoundException.class,
            () -> chapterCommentService.create(10L, ChapterCommentCreateRequest.builder().content("Hello").build())
        );

        when(chapterRepository.findById(10L)).thenReturn(Optional.of(Chapter.builder().id(10L).comicId(1L).build()));
        when(chapterCommentRepository.findById(200L)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(
            NotFoundException.class,
            () -> chapterCommentService.create(
                10L,
                ChapterCommentCreateRequest.builder().content("Hello").parentId(200L).build()
            )
        );
    }

    @Test
    void getComments_shouldHandleNoParentComments() {
        Chapter chapter = Chapter.builder().id(10L).comicId(1L).chapterNumber(1).build();
        when(chapterRepository.findById(10L)).thenReturn(Optional.of(chapter));
        when(chapterCommentRepository.findByChapterIdAndParentIdIsNullOrderByCreatedAtDesc(eq(10L), any()))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        when(userRepository.findAllById(any())).thenReturn(List.of());

        PageDataResponse<ChapterCommentResponse> response = chapterCommentService.getComments(10L, 0, 20);

        assertEquals(0, response.getContent().size());
        verify(chapterCommentRepository, org.mockito.Mockito.never()).findByParentIdInOrderByCreatedAtAsc(any());
    }

    @Test
    void getComments_shouldUseDefaultSizeWhenSizeIsNonPositive() {
        Chapter chapter = Chapter.builder().id(10L).comicId(1L).chapterNumber(1).build();
        when(chapterRepository.findById(10L)).thenReturn(Optional.of(chapter));
        when(chapterCommentRepository.findByChapterIdAndParentIdIsNullOrderByCreatedAtDesc(eq(10L), any()))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        when(userRepository.findAllById(any())).thenReturn(List.of());

        PageDataResponse<ChapterCommentResponse> response = chapterCommentService.getComments(10L, 0, 0);

        assertEquals(20, response.getPageSize());
    }
}
