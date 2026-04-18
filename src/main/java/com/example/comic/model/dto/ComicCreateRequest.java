package com.example.comic.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComicCreateRequest {

    @NotBlank(message = "Tên truyện là bắt buộc.")
    private String title;

    private String description;

    private String author;

    private String coverImageUrl;

    private String originalLanguage;

    @NotBlank(message = "Định dạng truyện là bắt buộc.")
    private String format;

    private String status;
}
