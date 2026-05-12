package com.example.comic.model.dto;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookOverviewDTO {
    private Long id;
    private String title;
    private String author;
    private String coverImageUrl;
    private String description;
    private Double averageRating;
    private Integer totalRatings;
    private List<ChapterSummaryResponse> chapters = new ArrayList<>();
}


