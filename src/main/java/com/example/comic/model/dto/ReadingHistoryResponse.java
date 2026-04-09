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
public class ReadingHistoryResponse {
    private Long comicId;
    private Long chapterId;
    private Integer lastPageRead;
    private Instant updatedAt;
}
