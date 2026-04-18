package com.example.comic.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComicCreateResponse {

    private Long id;
    private String title;
    private String description;
    private String author;
    private String coverImageUrl;
    private String originalLanguage;
    private String format;
    private String status;
}
