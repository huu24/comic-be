package com.example.comic.model.dto;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComicDetailResponse {
    private Long id;
    private String title;
    private String description;
    private String author;
    private String coverImageUrl;
    private String originalLanguage;
    private String format;
    private String status;
    private Double averageRating;
    private Integer totalRatings;
    private Instant createdAt;
    private Instant updatedAt;
    private List<String> categories;
}
