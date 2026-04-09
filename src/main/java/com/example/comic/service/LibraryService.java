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
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LibraryService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final UserLibraryRepository userLibraryRepository;
    private final ComicRepository comicRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public PageDataResponse<UserLibraryItemResponse> getLibraries(LibraryListType listType, int page, int size) {
        User user = currentUserService.requireUser();
        Pageable pageable = PageRequest.of(normalizePage(page), normalizeSize(size));

        Page<UserLibrary> libraries = listType == null
            ? userLibraryRepository.findByUserId(user.getId(), pageable)
            : userLibraryRepository.findByUserIdAndListType(user.getId(), listType, pageable);

        Set<Long> comicIds = libraries.getContent().stream().map(UserLibrary::getComicId).collect(java.util.stream.Collectors.toSet());
        Map<Long, Comic> comicMap = comicRepository
            .findAllById(comicIds)
            .stream()
            .collect(java.util.stream.Collectors.toMap(Comic::getId, Function.identity()));

        List<UserLibraryItemResponse> content = libraries
            .getContent()
            .stream()
            .map(item -> {
                Comic comic = comicMap.get(item.getComicId());
                return UserLibraryItemResponse
                    .builder()
                    .comicId(item.getComicId())
                    .title(comic == null ? null : comic.getTitle())
                    .coverImageUrl(comic == null ? null : comic.getCoverImageUrl())
                    .listType(item.getListType())
                        .savedAt(item.getCreatedAt())
                    .build();
            })
            .toList();

        return PageDataResponse
            .<UserLibraryItemResponse>builder()
            .content(content)
            .pageNo(libraries.getNumber())
            .pageSize(libraries.getSize())
            .totalElements(libraries.getTotalElements())
            .totalPages(libraries.getTotalPages())
            .last(libraries.isLast())
            .build();
    }

    public void upsertLibrary(UserLibraryUpsertRequest request) {
        User user = currentUserService.requireUser();
        comicRepository.findById(request.getComicId()).orElseThrow(() -> new NotFoundException("Không tìm thấy bộ truyện."));

        UserLibrary library = userLibraryRepository
            .findByUserIdAndComicId(user.getId(), request.getComicId())
            .orElse(UserLibrary.builder().userId(user.getId()).comicId(request.getComicId()).build());

        library.setListType(request.getListType());
        library.setCreatedAt(Instant.now());
        userLibraryRepository.save(library);
    }

    public void removeFromLibrary(Long comicId) {
        User user = currentUserService.requireUser();
        userLibraryRepository.deleteByUserIdAndComicId(user.getId(), comicId);
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
