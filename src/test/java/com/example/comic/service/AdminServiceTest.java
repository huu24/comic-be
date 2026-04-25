package com.example.comic.service;

import com.example.comic.exception.NotFoundException;
import com.example.comic.exception.PermissionDeniedException;
import com.example.comic.model.Comic;
import com.example.comic.model.User;
import com.example.comic.model.UserRole;
import com.example.comic.model.UserStatus;
import com.example.comic.model.dto.AdminDashboardSummaryResponse;
import com.example.comic.model.dto.AdminTopComicResponse;
import com.example.comic.model.dto.AdminUserSummaryResponse;
import com.example.comic.model.dto.PageDataResponse;
import com.example.comic.repository.ChapterPageRepository;
import com.example.comic.repository.ChapterRepository;
import com.example.comic.repository.ComicRatingRepository;
import com.example.comic.repository.ComicRepository;
import com.example.comic.repository.ReadingHistoryRepository;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ComicRepository comicRepository;

    @Mock
    private ChapterRepository chapterRepository;

    @Mock
    private ChapterPageRepository chapterPageRepository;

    @Mock
    private ComicRatingRepository comicRatingRepository;

    @Mock
    private ReadingHistoryRepository readingHistoryRepository;

    @Mock
    private CurrentUserService currentUserService;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(
            userRepository,
            comicRepository,
            chapterRepository,
            chapterPageRepository,
            comicRatingRepository,
            readingHistoryRepository,
            currentUserService
        );
    }

    @Test
    void getUsers_shouldReturnPagedSummaries() {
        User admin = user(1L, "admin@comic.local", UserRole.ADMIN, UserStatus.ACTIVE);
        User user = user(2L, "member@comic.local", UserRole.MEMBER, UserStatus.LOCKED);
        when(currentUserService.requireAdmin()).thenReturn(admin);
        when(userRepository.searchByKeyword(eq("john"), any())).thenReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 20), 1));

        PageDataResponse<AdminUserSummaryResponse> response = adminService.getUsers(" john ", 0, 20);

        assertEquals(1, response.getContent().size());
        assertEquals("member@comic.local", response.getContent().get(0).getEmail());
        assertEquals("LOCKED", response.getContent().get(0).getStatus());
    }

    @Test
    void updateUserStatus_shouldRejectSelfUpdate() {
        User admin = user(1L, "admin@comic.local", UserRole.ADMIN, UserStatus.ACTIVE);
        when(currentUserService.requireAdmin()).thenReturn(admin);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThrows(PermissionDeniedException.class, () -> adminService.updateUserStatus(1L, "LOCKED"));
    }

    @Test
    void updateUserStatus_shouldPersistNewStatus() {
        User admin = user(1L, "admin@comic.local", UserRole.ADMIN, UserStatus.ACTIVE);
        User target = user(2L, "member@comic.local", UserRole.MEMBER, UserStatus.ACTIVE);
        when(currentUserService.requireAdmin()).thenReturn(admin);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminUserSummaryResponse response = adminService.updateUserStatus(2L, "locked");

        assertEquals("LOCKED", response.getStatus());
        verify(userRepository).save(target);
    }

    @Test
    void updateUserRole_shouldRejectInvalidRoleForSelf() {
        User admin = user(1L, "admin@comic.local", UserRole.ADMIN, UserStatus.ACTIVE);
        when(currentUserService.requireAdmin()).thenReturn(admin);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThrows(PermissionDeniedException.class, () -> adminService.updateUserRole(1L, "MEMBER"));
    }

    @Test
    void updateUserRole_shouldPersistNewRole() {
        User admin = user(1L, "admin@comic.local", UserRole.ADMIN, UserStatus.ACTIVE);
        User target = user(2L, "member@comic.local", UserRole.MEMBER, UserStatus.ACTIVE);
        when(currentUserService.requireAdmin()).thenReturn(admin);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminUserSummaryResponse response = adminService.updateUserRole(2L, "admin");

        assertEquals("ADMIN", response.getRole());
        verify(userRepository).save(target);
    }

    @Test
    void updateUserStatus_shouldRejectInvalidStatusAndMissingUser() {
        User admin = user(1L, "admin@comic.local", UserRole.ADMIN, UserStatus.ACTIVE);
        when(currentUserService.requireAdmin()).thenReturn(admin);

        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> adminService.updateUserStatus(999L, "ACTIVE"));

        User target = user(2L, "member@comic.local", UserRole.MEMBER, UserStatus.ACTIVE);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        assertThrows(IllegalArgumentException.class, () -> adminService.updateUserStatus(2L, "disabled"));
        assertThrows(IllegalArgumentException.class, () -> adminService.updateUserStatus(2L, "   "));
    }

    @Test
    void updateUserRole_shouldRejectInvalidRoleAndMissingUser() {
        User admin = user(1L, "admin@comic.local", UserRole.ADMIN, UserStatus.ACTIVE);
        when(currentUserService.requireAdmin()).thenReturn(admin);

        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> adminService.updateUserRole(999L, "MEMBER"));

        User target = user(2L, "member@comic.local", UserRole.MEMBER, UserStatus.ACTIVE);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        assertThrows(IllegalArgumentException.class, () -> adminService.updateUserRole(2L, "GUEST"));
    }

    @Test
    void getUsers_shouldNormalizeInputAndHandleNullRoleStatus() {
        User admin = user(1L, "admin@comic.local", UserRole.ADMIN, UserStatus.ACTIVE);
        User user = user(2L, "member@comic.local", UserRole.MEMBER, UserStatus.LOCKED);
        user.setRole(null);
        user.setStatus(null);

        when(currentUserService.requireAdmin()).thenReturn(admin);
        when(userRepository.searchByKeyword(eq(null), any())).thenReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 100), 1));

        PageDataResponse<AdminUserSummaryResponse> response = adminService.getUsers("   ", -5, 999);

        assertEquals(1, response.getContent().size());
        assertNull(response.getContent().get(0).getRole());
        assertNull(response.getContent().get(0).getStatus());
    }

    @Test
    void getDashboardSummary_shouldAggregateCounters() {
        User admin = user(1L, "admin@comic.local", UserRole.ADMIN, UserStatus.ACTIVE);
        when(currentUserService.requireAdmin()).thenReturn(admin);
        when(comicRepository.findTop5ByOrderByTotalRatingsDescUpdatedAtDesc()).thenReturn(
            List.of(Comic.builder().id(10L).title("Comic A").averageRating(4.5).totalRatings(20).build())
        );
        when(userRepository.count()).thenReturn(100L);
        when(userRepository.countByStatus(UserStatus.ACTIVE)).thenReturn(90L);
        when(userRepository.countByStatus(UserStatus.LOCKED)).thenReturn(10L);
        when(comicRepository.count()).thenReturn(5L);
        when(chapterRepository.count()).thenReturn(12L);
        when(chapterPageRepository.count()).thenReturn(120L);
        when(comicRatingRepository.count()).thenReturn(300L);
        when(readingHistoryRepository.count()).thenReturn(40L);

        AdminDashboardSummaryResponse response = adminService.getDashboardSummary();

        assertEquals(100L, response.getTotalUsers());
        assertEquals(1, response.getTopComics().size());
        assertEquals("Comic A", response.getTopComics().get(0).getTitle());
    }

    @Test
    void getDashboardSummary_shouldDefaultNullTopComicRatingAndTotal() {
        User admin = user(1L, "admin@comic.local", UserRole.ADMIN, UserStatus.ACTIVE);
        when(currentUserService.requireAdmin()).thenReturn(admin);
        when(comicRepository.findTop5ByOrderByTotalRatingsDescUpdatedAtDesc())
            .thenReturn(List.of(Comic.builder().id(11L).title("Comic B").averageRating(null).totalRatings(null).build()));
        when(userRepository.count()).thenReturn(1L);
        when(userRepository.countByStatus(UserStatus.ACTIVE)).thenReturn(1L);
        when(userRepository.countByStatus(UserStatus.LOCKED)).thenReturn(0L);
        when(comicRepository.count()).thenReturn(1L);
        when(chapterRepository.count()).thenReturn(1L);
        when(chapterPageRepository.count()).thenReturn(1L);
        when(comicRatingRepository.count()).thenReturn(1L);
        when(readingHistoryRepository.count()).thenReturn(1L);

        AdminDashboardSummaryResponse response = adminService.getDashboardSummary();

        assertEquals(0D, response.getTopComics().get(0).getAverageRating());
        assertEquals(0, response.getTopComics().get(0).getTotalRatings());
    }

    @Test
    void getUsers_shouldUseDefaultSizeAndNullKeyword() {
        User admin = user(1L, "admin@comic.local", UserRole.ADMIN, UserStatus.ACTIVE);
        when(currentUserService.requireAdmin()).thenReturn(admin);
        when(userRepository.searchByKeyword(eq(null), any()))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        PageDataResponse<AdminUserSummaryResponse> response = adminService.getUsers(null, -2, 0);

        assertEquals(20, response.getPageSize());
        assertEquals(0, response.getContent().size());
    }

    @Test
    void updateUserStatus_shouldAcceptActiveAndNullStatusRoleInSummary() {
        User admin = user(1L, "admin@comic.local", UserRole.ADMIN, UserStatus.ACTIVE);
        User target = user(2L, "member@comic.local", UserRole.MEMBER, UserStatus.LOCKED);
        target.setRole(null);
        target.setStatus(null);
        when(currentUserService.requireAdmin()).thenReturn(admin);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminUserSummaryResponse response = adminService.updateUserStatus(2L, "ACTIVE");

        assertNull(response.getRole());
        assertEquals("ACTIVE", response.getStatus());
    }

    @Test
    void updateUserStatus_shouldRejectNullStatus() {
        User admin = user(1L, "admin@comic.local", UserRole.ADMIN, UserStatus.ACTIVE);
        User target = user(2L, "member@comic.local", UserRole.MEMBER, UserStatus.ACTIVE);
        when(currentUserService.requireAdmin()).thenReturn(admin);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        assertThrows(IllegalArgumentException.class, () -> adminService.updateUserStatus(2L, null));
    }

    @Test
    void updateUserRole_shouldReturnNullStatusWhenTargetStatusIsNull() {
        User admin = user(1L, "admin@comic.local", UserRole.ADMIN, UserStatus.ACTIVE);
        User target = user(2L, "member@comic.local", UserRole.MEMBER, UserStatus.ACTIVE);
        target.setStatus(null);
        when(currentUserService.requireAdmin()).thenReturn(admin);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminUserSummaryResponse response = adminService.updateUserRole(2L, "MEMBER");

        assertNull(response.getStatus());
    }

    private static User user(Long id, String email, UserRole role, UserStatus status) {
        return User
            .builder()
            .id(id)
            .email(email)
            .passwordHash("hash")
            .fullName("Test User")
            .role(role)
            .status(status)
            .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
            .build();
    }
}
