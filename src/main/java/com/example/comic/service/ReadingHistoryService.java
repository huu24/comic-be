package com.example.comic.service;

import com.example.comic.exception.NotFoundException;
import com.example.comic.model.Chapter;
import com.example.comic.model.ReadingHistory;
import com.example.comic.model.User;
import com.example.comic.model.dto.MessageStatusResponse;
import com.example.comic.model.dto.MessageResponse;
import com.example.comic.model.dto.ReadingHistoryResponse;
import com.example.comic.model.dto.ReadingHistorySyncRequest;
import com.example.comic.repository.ChapterRepository;
import com.example.comic.repository.ReadingHistoryRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReadingHistoryService {

    private final ReadingHistoryRepository readingHistoryRepository;
    private final CurrentUserService currentUserService;
    private final ChapterRepository chapterRepository;

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
}
