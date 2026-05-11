package com.example.comic.model.dto;

import com.example.comic.model.LibraryListType;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLibraryItemResponse {
    private Long id;
    private String title;
    private String author;
    private String coverImageUrl;
    private String originalLanguage;
    private String status;
    private String format;
    private Double averageRating;
    private LibraryListType listType;
    private Instant savedAt;
}
