package com.example.comic.controller;

import com.example.comic.model.LibraryListType;
import com.example.comic.model.dto.DataResponse;
import com.example.comic.model.dto.MessageResponse;
import com.example.comic.model.dto.PageDataResponse;
import com.example.comic.model.dto.UserLibraryItemResponse;
import com.example.comic.model.dto.UserLibraryUpsertRequest;
import com.example.comic.service.LibraryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user-libraries")
@RequiredArgsConstructor
public class UserLibraryController {

    private final LibraryService libraryService;

    @GetMapping
    public ResponseEntity<DataResponse<PageDataResponse<UserLibraryItemResponse>>> getLibraries(
        @RequestParam(required = false) LibraryListType listType,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
            DataResponse
                .<PageDataResponse<UserLibraryItemResponse>>builder()
                .data(libraryService.getLibraries(listType, page, size))
                .build()
        );
    }

    @PostMapping
    public ResponseEntity<MessageResponse> upsertLibrary(@Valid @RequestBody UserLibraryUpsertRequest request) {
        libraryService.upsertLibrary(request);
        return ResponseEntity.ok(MessageResponse.builder().message("Đã cập nhật tủ sách thành công.").build());
    }

    @DeleteMapping("/comics/{comicId}")
    public ResponseEntity<MessageResponse> remove(@PathVariable Long comicId) {
        libraryService.removeFromLibrary(comicId);
        return ResponseEntity.ok(MessageResponse.builder().message("Đã xóa truyện khỏi tủ sách.").build());
    }
}
