package com.example.comic.model.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageDetailResponse {
    private Long pageId;
    private Long chapterId;
    private Integer pageNumber;
    private PageImagesResponse images;
    private JsonNode bubbles;
}
