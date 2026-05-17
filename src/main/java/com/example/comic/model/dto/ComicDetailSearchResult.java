package com.example.comic.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComicDetailSearchResult {

    private Long id;
    private String title;
    private String author;
    private String coverImageUrl;
    private Double averageRating;
    private String listType;
}
