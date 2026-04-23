package com.example.comic.controller;

import com.example.comic.model.dto.PageDetailResponse;
import com.example.comic.model.dto.StatusDataResponse;
import com.example.comic.service.PageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pages")
@RequiredArgsConstructor
public class PageController {

    private final PageService pageService;

    @GetMapping("/{pageId}")
    public ResponseEntity<StatusDataResponse<PageDetailResponse>> getPageDetail(
            @PathVariable Long pageId,
            @RequestParam String lang) {
        return ResponseEntity.ok(
                StatusDataResponse.<PageDetailResponse>builder()
                        .status("success")
                        .data(pageService.getPageDetail(pageId, lang))
                        .build());
    }
}
