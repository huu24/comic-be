package com.example.comic.service;

import com.example.comic.exception.NotFoundException;
import com.example.comic.model.Chapter;
import com.example.comic.model.Comic;
import com.example.comic.model.ReadingHistory;
import com.example.comic.model.User;
import com.example.comic.model.dto.MessageStatusResponse;
import com.example.comic.model.dto.MessageResponse;
import com.example.comic.model.dto.PageDataResponse;
import com.example.comic.model.dto.ReadingHistoryResponse;
import com.example.comic.model.dto.ReadingHistorySyncRequest;
import com.example.comic.model.dto.UserReadingHistoryItemResponse;
import com.example.comic.repository.ChapterRepository;
import com.example.comic.repository.ComicRepository;
import com.example.comic.repository.ReadingHistoryRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReadingHistoryService {

    private final ReadingHistoryRepository readingHistoryRepository;
    private final CurrentUserService currentUserService;
    private final ChapterRepository chapterRepository;
    private final ComicRepository comicRepository;

    @Transactional
    public ReadingHistoryResponse getByComicId(Long comicId) {
        User user = currentUserService.requireUser();
        ReadingHistory history = readingHistoryRepository
            .findByUserIdAndComicId(user.getId(), comicId)
            .orElseGet(() -> {
                java.util.List<Chapter> chapters = chapterRepository.findByComicIdOrderByChapterNumberAsc(comicId);
                if (chapters.isEmpty()) {
                    throw new NotFoundException("Chưa có lịch sử đọc cho bộ truyện này.");
                }
                Chapter firstChapter = chapters.get(0);
                ReadingHistory newHistory = ReadingHistory
                    .builder()
                    .userId(user.getId())
                    .comicId(comicId)
                    .chapterId(firstChapter.getId())
                    .lastPageRead(0)
                    .updatedAt(Instant.now())
                    .build();
                return readingHistoryRepository.save(newHistory);
            });
        Chapter chapter = chapterRepository
            .findById(history.getChapterId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy Chapter ID: " + history.getChapterId() + " trong hệ thống."));

        return ReadingHistoryResponse
            .builder()
            .comicId(history.getComicId())
            .chapterNumber(chapter.getChapterNumber())
            .lastPageRead(history.getLastPageRead())
            .updatedAt(history.getUpdatedAt())
            .build();
    }

    @Transactional
    public Object sync(ReadingHistorySyncRequest request) {
        User user = currentUserService.requireUser();
        Instant clientTime = Instant.parse(request.getClientUpdatedAt());

        ReadingHistory history = readingHistoryRepository.findByUserIdAndComicId(user.getId(), request.getComicId()).orElse(null);

        if (history == null) {
            readingHistoryRepository.save(
                ReadingHistory
                    .builder()
                    .userId(user.getId())
                    .comicId(request.getComicId())
                    .chapterId(request.getChapterId())
                    .lastPageRead(request.getLastPageRead())
                    .updatedAt(clientTime)
                    .build()
            );
            return MessageResponse.builder().message("Tiến độ đọc đã được lưu.").build();
        }

        Chapter clientChapter = chapterRepository.findById(request.getChapterId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy Chapter ID: " + request.getChapterId() + " trong hệ thống."));
        Chapter serverChapter = chapterRepository.findById(history.getChapterId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy Chapter ID: " + history.getChapterId() + " trong hệ thống."));

        if (
            clientTime.isAfter(history.getUpdatedAt())
            && (
                clientChapter.getChapterNumber() > serverChapter.getChapterNumber()
                || (
                    clientChapter.getChapterNumber() == serverChapter.getChapterNumber()
                    && request.getLastPageRead() > history.getLastPageRead()
                )
            )
        ) {
            history.setChapterId(request.getChapterId());
            history.setLastPageRead(request.getLastPageRead());
            history.setUpdatedAt(clientTime);
            readingHistoryRepository.save(history);
            return MessageResponse.builder().message("Tiến độ đọc đã được lưu.").build();
        }

        return MessageStatusResponse.builder().message("Tiến độ trên Server mới hơn. Bỏ qua đồng bộ.").status("IGNORED").build();
    }

    @Transactional(readOnly = true)
    public PageDataResponse<UserReadingHistoryItemResponse> getReadingHistory(int page, int size) {
        User user = currentUserService.requireUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        
        Page<ReadingHistory> histories = readingHistoryRepository.findByUserId(user.getId(), pageable);
        
        Set<Long> comicIds = histories.getContent().stream()
            .map(ReadingHistory::getComicId)
            .collect(Collectors.toSet());
        Map<Long, Comic> comicMap = comicRepository.findAllById(comicIds).stream()
            .collect(Collectors.toMap(Comic::getId, Function.identity()));
            
        Set<Long> chapterIds = histories.getContent().stream()
            .map(ReadingHistory::getChapterId)
            .collect(Collectors.toSet());
        Map<Long, Chapter> chapterMap = chapterRepository.findAllById(chapterIds).stream()
            .collect(Collectors.toMap(Chapter::getId, Function.identity()));
            
        List<UserReadingHistoryItemResponse> content = histories.getContent().stream()
            .map(item -> {
                Comic comic = comicMap.get(item.getComicId());
                Chapter chapter = chapterMap.get(item.getChapterId());
                return UserReadingHistoryItemResponse.builder()
                    .comicId(item.getComicId())
                    .title(comic == null ? null : comic.getTitle())
                    .author(comic == null ? null : comic.getAuthor())
                    .coverImageUrl(comic == null ? null : comic.getCoverImageUrl())
                    .originalLanguage(comic == null ? null : comic.getOriginalLanguage())
                    .status(comic == null ? null : comic.getStatus())
                    .format(comic == null ? null : comic.getFormat())
                    .averageRating(comic == null || comic.getAverageRating() == null ? 0D : comic.getAverageRating())
                    .chapterNumber(chapter == null ? null : chapter.getChapterNumber())
                    .lastPageRead(item.getLastPageRead())
                    .updatedAt(item.getUpdatedAt())
                    .build();
            })
            .toList();

        return PageDataResponse.<UserReadingHistoryItemResponse>builder()
            .content(content)
            .pageNo(histories.getNumber())
            .pageSize(histories.getSize())
            .totalElements(histories.getTotalElements())
            .totalPages(histories.getTotalPages())
            .last(histories.isLast())
            .build();
    }
}
