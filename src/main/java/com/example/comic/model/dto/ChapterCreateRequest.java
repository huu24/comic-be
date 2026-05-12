package com.example.comic.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterCreateRequest {

    @Schema(example = "1")
    @NotNull(message = "Số chương là bắt buộc.")
    @Positive(message = "Số chương phải lớn hơn 0.")
    private Integer chapterNumber;

    @Schema(example = "Khởi đầu chuyến phiêu lưu")
    private String title;
}
