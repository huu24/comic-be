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
public class ChapterSummaryResponse {
    private Long id;
    private Integer chapterNumber;
    private String title;
    private Instant createdAt;
}
