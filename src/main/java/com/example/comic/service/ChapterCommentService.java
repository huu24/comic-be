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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChapterCommentService {

    private final ChapterCommentRepository chapterCommentRepository;
    private final ChapterRepository chapterRepository;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final ProfanityFilterService profanityFilterService;

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    @Transactional(readOnly = true)
    public PageDataResponse<ChapterCommentResponse> getComments(Long chapterId, int page, int size) {
        Chapter chapter = chapterRepository
            .findById(chapterId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy chương truyện."));

        Page<ChapterComment> parentComments = chapterCommentRepository.findByChapterIdAndParentIdIsNullOrderByCreatedAtDesc(
            chapter.getId(),
            PageRequest.of(normalizePage(page), normalizeSize(size))
        );

        List<Long> parentIds = parentComments.getContent().stream().map(ChapterComment::getId).toList();
        List<ChapterComment> replies = parentIds.isEmpty()
            ? List.of()
            : chapterCommentRepository.findByParentIdInOrderByCreatedAtAsc(parentIds);

        Map<Long, List<ChapterComment>> repliesMap = new HashMap<>();
        for (ChapterComment reply : replies) {
            repliesMap.computeIfAbsent(reply.getParentId(), k -> new ArrayList<>()).add(reply);
        }

        Map<Long, User> userMap = buildUserMap(parentComments.getContent(), replies);

        List<ChapterCommentResponse> content = parentComments
            .getContent()
            .stream()
            .map(parent ->
                toResponse(
                    parent,
                    userMap,
                    repliesMap.getOrDefault(parent.getId(), List.of()).stream().map(reply -> toResponse(reply, userMap)).toList()
                )
            )
            .toList();

        return PageDataResponse
            .<ChapterCommentResponse>builder()
            .content(content)
            .pageNo(parentComments.getNumber())
            .pageSize(parentComments.getSize())
            .totalElements(parentComments.getTotalElements())
            .totalPages(parentComments.getTotalPages())
            .last(parentComments.isLast())
            .build();
    }

    public ChapterCommentResponse create(Long chapterId, ChapterCommentCreateRequest request) {
        User current = currentUserService.requireUser();
        chapterRepository.findById(chapterId).orElseThrow(() -> new NotFoundException("Không tìm thấy chương truyện."));

        if (request.getParentId() != null) {
            chapterCommentRepository
                .findById(request.getParentId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bình luận cha."));
        }

        ChapterComment saved = chapterCommentRepository.save(
            ChapterComment
                .builder()
                .chapterId(chapterId)
                .userId(current.getId())
                .content(profanityFilterService.sanitize(request.getContent().trim()))
                .parentId(request.getParentId())
                .build()
        );

        return toResponse(saved, java.util.Map.of(current.getId(), current), List.of());
    }

    private Map<Long, User> buildUserMap(List<ChapterComment> parents, List<ChapterComment> replies) {
        Set<Long> userIds = new java.util.HashSet<>();
        parents.forEach(c -> userIds.add(c.getUserId()));
        replies.forEach(c -> userIds.add(c.getUserId()));
        return userRepository
            .findAllById(userIds)
            .stream()
            .collect(java.util.stream.Collectors.toMap(User::getId, java.util.function.Function.identity()));
    }

    private ChapterCommentResponse toResponse(ChapterComment comment, Map<Long, User> userMap) {
        return toResponse(comment, userMap, List.of());
    }

    private ChapterCommentResponse toResponse(
        ChapterComment comment,
        Map<Long, User> userMap,
        List<ChapterCommentResponse> replies
    ) {
        User user = userMap.get(comment.getUserId());
        return ChapterCommentResponse
            .builder()
            .id(comment.getId())
            .userId(comment.getUserId())
            .fullName(user == null ? "Ẩn danh" : user.getFullName())
            .avatarUrl(user == null ? null : user.getAvatarUrl())
            .content(comment.getContent())
            .parentId(comment.getParentId())
            .createdAt(comment.getCreatedAt())
            .replies(replies)
            .build();
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
}
