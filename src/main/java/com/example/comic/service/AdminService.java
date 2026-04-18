package com.example.comic.service;

import com.example.comic.exception.NotFoundException;
import com.example.comic.exception.PermissionDeniedException;
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
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final UserRepository userRepository;
    private final ComicRepository comicRepository;
    private final ChapterRepository chapterRepository;
    private final ChapterPageRepository chapterPageRepository;
    private final ComicRatingRepository comicRatingRepository;
    private final ReadingHistoryRepository readingHistoryRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public PageDataResponse<AdminUserSummaryResponse> getUsers(String keyword, int page, int size) {
        currentUserService.requireAdmin();

        Pageable pageable = PageRequest.of(normalizePage(page), normalizeSize(size));
        Page<User> users = userRepository.searchByKeyword(normalizeKeyword(keyword), pageable);

        List<AdminUserSummaryResponse> content = users
            .getContent()
            .stream()
            .map(user ->
                AdminUserSummaryResponse
                    .builder()
                    .id(user.getId())
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .role(user.getRole() == null ? null : user.getRole().name())
                    .status(user.getStatus() == null ? null : user.getStatus().name())
                    .createdAt(user.getCreatedAt())
                    .build()
            )
            .toList();

        return PageDataResponse
            .<AdminUserSummaryResponse>builder()
            .content(content)
            .pageNo(users.getNumber())
            .pageSize(users.getSize())
            .totalElements(users.getTotalElements())
            .totalPages(users.getTotalPages())
            .last(users.isLast())
            .build();
    }

    @Transactional
    public AdminUserSummaryResponse updateUserStatus(Long userId, String targetStatus) {
        User admin = currentUserService.requireAdmin();
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng."));

        if (admin.getId().equals(user.getId())) {
            throw new PermissionDeniedException("Bạn không thể tự khóa/mở khóa tài khoản của chính mình.");
        }

        UserStatus status = parseStatus(targetStatus);
        user.setStatus(status);
        User saved = userRepository.save(user);
        return toSummary(saved);
    }

    @Transactional
    public AdminUserSummaryResponse updateUserRole(Long userId, String targetRole) {
        User admin = currentUserService.requireAdmin();
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng."));

        if (admin.getId().equals(user.getId())) {
            throw new PermissionDeniedException("Bạn không thể tự thay đổi quyền của chính mình.");
        }

        user.setRole(parseRole(targetRole));
        User saved = userRepository.save(user);
        return toSummary(saved);
    }

    @Transactional(readOnly = true)
    public AdminDashboardSummaryResponse getDashboardSummary() {
        currentUserService.requireAdmin();

        List<AdminTopComicResponse> topComics = comicRepository
            .findTop5ByOrderByTotalRatingsDescUpdatedAtDesc()
            .stream()
            .map(comic ->
                AdminTopComicResponse
                    .builder()
                    .id(comic.getId())
                    .title(comic.getTitle())
                    .averageRating(comic.getAverageRating() == null ? 0D : comic.getAverageRating())
                    .totalRatings(comic.getTotalRatings() == null ? 0 : comic.getTotalRatings())
                    .build()
            )
            .toList();

        return AdminDashboardSummaryResponse
            .builder()
            .totalUsers(userRepository.count())
            .activeUsers(userRepository.countByStatus(UserStatus.ACTIVE))
            .lockedUsers(userRepository.countByStatus(UserStatus.LOCKED))
            .totalComics(comicRepository.count())
            .totalChapters(chapterRepository.count())
            .totalPages(chapterPageRepository.count())
            .totalRatings(comicRatingRepository.count())
            .totalReadingHistories(readingHistoryRepository.count())
            .topComics(topComics)
            .build();
    }

    private AdminUserSummaryResponse toSummary(User user) {
        return AdminUserSummaryResponse
            .builder()
            .id(user.getId())
            .fullName(user.getFullName())
            .email(user.getEmail())
            .role(user.getRole() == null ? null : user.getRole().name())
            .status(user.getStatus() == null ? null : user.getStatus().name())
            .createdAt(user.getCreatedAt())
            .build();
    }

    private UserStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ. Chỉ chấp nhận ACTIVE hoặc LOCKED.");
        }
        String normalized = raw.trim().toUpperCase();
        if (!"ACTIVE".equals(normalized) && !"LOCKED".equals(normalized)) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ. Chỉ chấp nhận ACTIVE hoặc LOCKED.");
        }
        return UserStatus.valueOf(normalized);
    }

    private UserRole parseRole(String raw) {
        UserRole role = UserRole.from(raw);
        if (role == UserRole.GUEST) {
            throw new IllegalArgumentException("Vai trò không hợp lệ. Chỉ chấp nhận MEMBER hoặc ADMIN.");
        }
        return role;
    }

    private int normalizePage(int page) {
        return Math.max(DEFAULT_PAGE, page);
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String normalized = keyword.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
