package com.example.comic.controller;

import com.example.comic.model.dto.ChapterCommentCreateRequest;
import com.example.comic.model.dto.ChapterCommentResponse;
import com.example.comic.model.dto.ChapterPageResponse;
import com.example.comic.model.dto.DataResponse;
import com.example.comic.model.dto.PageDataResponse;
import com.example.comic.model.dto.StatusDataResponse;
import com.example.comic.service.ChapterCommentService;
import com.example.comic.service.ComicService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/chapters")
@RequiredArgsConstructor
public class ChapterController {

    private final ComicService comicService;
    private final ChapterCommentService chapterCommentService;

    @GetMapping("/{chapterId}/pages")
    public ResponseEntity<StatusDataResponse<List<ChapterPageResponse>>> getPages(@PathVariable Long chapterId) {
        return ResponseEntity.ok(
            StatusDataResponse.<List<ChapterPageResponse>>builder()
                .status("success")
                .data(comicService.getChapterPages(chapterId))
                .build()
        );
    }

    @PostMapping(value = "/{chapterId}/pages", consumes = "multipart/form-data")
    public ResponseEntity<DataResponse<List<ChapterPageResponse>>> uploadPages(
        @PathVariable Long chapterId,
        @RequestParam(defaultValue = "1") int startPageNumber,
        @RequestParam("files") List<MultipartFile> files
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            DataResponse.<List<ChapterPageResponse>>builder()
                .data(comicService.uploadChapterPages(chapterId, startPageNumber, files))
                .build()
        );
    }

    @DeleteMapping("/pages/{pageId}")
    public ResponseEntity<Void> deletePage(@PathVariable Long pageId) {
        comicService.deleteChapterPage(pageId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{chapterId}/pages")
    public ResponseEntity<Void> deletePagesByChapter(@PathVariable Long chapterId) {
        comicService.deleteChapterPages(chapterId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{chapterId}/comments")
    public ResponseEntity<DataResponse<PageDataResponse<ChapterCommentResponse>>> getComments(
        @PathVariable Long chapterId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
            DataResponse
                .<PageDataResponse<ChapterCommentResponse>>builder()
                .data(chapterCommentService.getComments(chapterId, page, size))
                .build()
        );
    }

    @PostMapping("/{chapterId}/comments")
    public ResponseEntity<DataResponse<ChapterCommentResponse>> createComment(
        @PathVariable Long chapterId,
        @Valid @RequestBody ChapterCommentCreateRequest request
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(DataResponse.<ChapterCommentResponse>builder().data(chapterCommentService.create(chapterId, request)).build());
    }
}
