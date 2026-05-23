package com.example.comic.controller;

import com.example.comic.model.dto.DataResponse;
import com.example.comic.model.dto.PageDataResponse;
import com.example.comic.model.dto.ReadingHistoryResponse;
import com.example.comic.model.dto.ReadingHistorySyncRequest;
import com.example.comic.model.dto.UserReadingHistoryItemResponse;
import com.example.comic.service.ReadingHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reading-histories")
@RequiredArgsConstructor
public class ReadingHistoryController {

    private final ReadingHistoryService readingHistoryService;

    @GetMapping
    public ResponseEntity<DataResponse<PageDataResponse<UserReadingHistoryItemResponse>>> getReadingHistory(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
            DataResponse.<PageDataResponse<UserReadingHistoryItemResponse>>builder()
                .data(readingHistoryService.getReadingHistory(page, size))
                .build()
        );
    }

    @GetMapping("/comics/{comicId}")
    public ResponseEntity<DataResponse<ReadingHistoryResponse>> getByComicId(@PathVariable Long comicId) {
        return ResponseEntity.ok(
            DataResponse.<ReadingHistoryResponse>builder().data(readingHistoryService.getByComicId(comicId)).build()
        );
    }

    @PutMapping
    public ResponseEntity<?> sync(@Valid @RequestBody ReadingHistorySyncRequest request) {
        return ResponseEntity.ok(readingHistoryService.sync(request));
    }
}
