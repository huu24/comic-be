package com.example.comic.model.dto;

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

    @NotNull(message = "score là bắt buộc.")
    @Min(value = 1, message = "score phải từ 1 đến 10.")
    @Max(value = 10, message = "score phải từ 1 đến 10.")
    private Integer score;
}
