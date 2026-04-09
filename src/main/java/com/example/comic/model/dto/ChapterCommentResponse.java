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
public class ChapterCommentResponse {
    private Long id;
    private Long userId;
    private String fullName;
    private String avatarUrl;
    private String content;
    private Long parentId;
    private Instant createdAt;
    private List<ChapterCommentResponse> replies;
}
