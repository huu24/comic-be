package com.example.comic.controller;

import com.example.comic.model.dto.ComicRatingRequest;
import com.example.comic.model.dto.ComicRatingResponse;
import com.example.comic.model.dto.ComicSummaryResponse;
import com.example.comic.model.dto.DataResponse;
import com.example.comic.model.dto.PageDataResponse;
import com.example.comic.model.dto.RateComicResponse;
import com.example.comic.service.ComicService;
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
@RequestMapping("/comics")
@RequiredArgsConstructor
public class ComicController {

    private final ComicService comicService;

    @GetMapping
    public ResponseEntity<DataResponse<PageDataResponse<ComicSummaryResponse>>> getComics(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(DataResponse.<PageDataResponse<ComicSummaryResponse>>builder().data(comicService.getComics(keyword, categoryId, page, size)).build());
    }

    @PutMapping("/{comicId}/ratings")
    public ResponseEntity<RateComicResponse> rateComic(
        @PathVariable Long comicId,
        @Valid @RequestBody ComicRatingRequest request
    ) {
        ComicRatingResponse data = comicService.rateComic(comicId, request.getScore());
        return ResponseEntity.ok(RateComicResponse.builder().message("Đánh giá thành công.").data(data).build());
    }
}
