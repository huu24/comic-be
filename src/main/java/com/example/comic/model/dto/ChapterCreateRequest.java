package com.example.comic.model.dto;

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

    @NotNull(message = "Số chương là bắt buộc.")
    @Positive(message = "Số chương phải lớn hơn 0.")
    private Integer chapterNumber;

    private String title;
}
