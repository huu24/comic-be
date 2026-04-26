package com.example.comic.controller;

import com.example.comic.model.dto.ChapterCreateRequest;
import com.example.comic.model.dto.ChapterCreateResponse;
import com.example.comic.model.dto.ChapterSummaryResponse;
import com.example.comic.model.dto.ComicCreateRequest;
import com.example.comic.model.dto.ComicCreateResponse;
import com.example.comic.model.dto.ComicDetailResponse;
import com.example.comic.model.dto.ComicRatingRequest;
import com.example.comic.model.dto.ComicRatingResponse;
import com.example.comic.model.dto.ComicSummaryResponse;
import com.example.comic.model.dto.DataResponse;
import com.example.comic.model.dto.PageDataResponse;
import com.example.comic.model.dto.RateComicResponse;
import com.example.comic.service.ComicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/comics")
@RequiredArgsConstructor
public class ComicController {

    private final ComicService comicService;

    @PostMapping
    public ResponseEntity<DataResponse<ComicCreateResponse>> createComic(
            @Valid @RequestBody ComicCreateRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(DataResponse.<ComicCreateResponse>builder().data(comicService.createComic(request)).build());
    }

    @PostMapping("/{comicId}/chapters")
    public ResponseEntity<DataResponse<ChapterCreateResponse>> createChapter(
            @PathVariable Long comicId,
            @Valid @RequestBody ChapterCreateRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(DataResponse.<ChapterCreateResponse>builder().data(comicService.createChapter(comicId, request))
                        .build());
    }

    @GetMapping
    public ResponseEntity<DataResponse<PageDataResponse<ComicSummaryResponse>>> getComics(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String originalLanguage,
            @RequestParam(required = false) String comicStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                DataResponse
                        .<PageDataResponse<ComicSummaryResponse>>builder()
                        .data(comicService.getComics(keyword, categoryId, originalLanguage, comicStatus, page, size))
                        .build());
    }

    @GetMapping("/{comicId}")
    public ResponseEntity<DataResponse<ComicDetailResponse>> getComicDetail(@PathVariable Long comicId) {
        return ResponseEntity.ok(
                DataResponse.<ComicDetailResponse>builder().data(comicService.getComicDetail(comicId)).build());
    }

    @GetMapping("/{comicId}/chapters")
    public ResponseEntity<DataResponse<PageDataResponse<ChapterSummaryResponse>>> getChapters(
            @PathVariable Long comicId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                DataResponse
                        .<PageDataResponse<ChapterSummaryResponse>>builder()
                        .data(comicService.getChapters(comicId, page, size))
                        .build());
    }

    @PutMapping("/{comicId}/ratings")
    public ResponseEntity<RateComicResponse> rateComic(
            @PathVariable Long comicId,
            @Valid @RequestBody ComicRatingRequest request) {
        ComicRatingResponse data = comicService.rateComic(comicId, request.getScore());
        return ResponseEntity.ok(RateComicResponse.builder().message("Đánh giá thành công.").data(data).build());
    }
}
