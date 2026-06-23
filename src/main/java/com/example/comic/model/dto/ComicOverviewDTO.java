package com.example.comic.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComicOverviewDTO {
    private Long id;
    private String title;
    private String author;
    private String coverImageUrl;
    private String description;
    private Double averageRating;
    private Integer totalRatings;
    private String libraryType;
    private int views;
}
