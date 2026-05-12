package com.example.comic.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(example = "1")
    @NotNull(message = "comicId là bắt buộc.")
    private Long comicId;

    @Schema(example = "5")
    @NotNull(message = "chapterId là bắt buộc.")
    private Long chapterId;

    @Schema(example = "12")
    @NotNull(message = "lastPageRead là bắt buộc.")
    private Integer lastPageRead;

    @Schema(example = "2026-04-26T10:00:00Z")
    @NotNull(message = "clientUpdatedAt là bắt buộc.")
    private String clientUpdatedAt;
}
