package com.example.comic.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterCommentCreateRequest {

    @Schema(example = "Chapter này hay quá!")
    @NotBlank(message = "Nội dung bình luận không được để trống.")
    @Size(max = 1000, message = "Nội dung bình luận không vượt quá 1000 ký tự.")
    private String content;

    @Schema(example = "null")
    private Long parentId;
}
