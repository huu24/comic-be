package com.example.comic.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadingHistorySyncRequest {

    @NotNull(message = "comicId là bắt buộc.")
    private Long comicId;

    @NotNull(message = "chapterId là bắt buộc.")
    private Long chapterId;

    @NotNull(message = "lastPageRead là bắt buộc.")
    private Integer lastPageRead;

    @NotNull(message = "clientUpdatedAt là bắt buộc.")
    private String clientUpdatedAt;
}
