package com.example.comic.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComicSummaryResponse {
    private Long id;
    private String title;
    private String coverImageUrl;
    private String format;
    private Double averageRating;
}
