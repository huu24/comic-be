package com.example.comic.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComicRatingRequest {

    @Schema(example = "8")
    @NotNull(message = "score là bắt buộc.")
    @Min(value = 1, message = "score phải từ 1 đến 10.")
    @Max(value = 10, message = "score phải từ 1 đến 10.")
    private Integer score;
}
