package com.example.comic.model.dto;

import com.example.comic.model.LibraryListType;
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
public class UserLibraryUpsertRequest {

    @Schema(example = "1")
    @NotNull(message = "comicId là bắt buộc.")
    private Long comicId;

    @Schema(example = "FAVORITE")
    @NotNull(message = "listType là bắt buộc.")
    private LibraryListType listType;
}
