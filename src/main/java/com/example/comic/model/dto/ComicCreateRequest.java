package com.example.comic.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(example = "One Piece")
    @NotBlank(message = "Tên truyện là bắt buộc.")
    private String title;

    @Schema(example = "Câu chuyện về Luffy và hành trình tìm kho báu One Piece.")
    private String description;

    @Schema(example = "Eiichiro Oda")
    private String author;

    @Schema(example = "https://example.com/covers/one-piece.jpg")
    private String coverImageUrl;

    @Schema(example = "ja")
    private String originalLanguage;

    @Schema(example = "MANGA")
    @NotBlank(message = "Định dạng truyện là bắt buộc.")
    private String format;

    @Schema(example = "ONGOING")
    private String status;
}
