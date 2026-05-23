package com.example.comic.model.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserReadingHistoryItemResponse {
    private Long comicId;
    private String title;
    private String author;
    private String coverImageUrl;
    private String originalLanguage;
    private String status;
    private String format;
    private Double averageRating;
    private Integer chapterNumber;
    private Integer lastPageRead;
    private Instant updatedAt;
}
