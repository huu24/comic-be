package com.example.comic.service;

import com.example.comic.exception.NotFoundException;
import com.example.comic.model.ReadingHistory;
import com.example.comic.model.User;
import com.example.comic.model.dto.MessageStatusResponse;
import com.example.comic.model.dto.MessageResponse;
import com.example.comic.model.dto.ReadingHistoryResponse;
import com.example.comic.model.dto.ReadingHistorySyncRequest;
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

    @Transactional(readOnly = true)
    public ReadingHistoryResponse getByComicId(Long comicId) {
        User user = currentUserService.requireUser();
        ReadingHistory history = readingHistoryRepository
            .findByUserIdAndComicId(user.getId(), comicId)
            .orElseThrow(() -> new NotFoundException("Chưa có lịch sử đọc cho bộ truyện này."));

        return ReadingHistoryResponse
            .builder()
            .comicId(history.getComicId())
            .chapterId(history.getChapterId())
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

        if (clientTime.isAfter(history.getUpdatedAt())) {
            history.setChapterId(request.getChapterId());
            history.setLastPageRead(request.getLastPageRead());
            history.setUpdatedAt(clientTime);
            readingHistoryRepository.save(history);
            return MessageResponse.builder().message("Tiến độ đọc đã được lưu.").build();
        }

        return MessageStatusResponse.builder().message("Tiến độ trên Server mới hơn. Bỏ qua đồng bộ.").status("IGNORED").build();
    }
}
