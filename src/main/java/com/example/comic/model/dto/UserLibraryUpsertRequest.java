package com.example.comic.model.dto;

import com.example.comic.model.LibraryListType;
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

    @NotNull(message = "comicId là bắt buộc.")
    private Long comicId;

    @NotNull(message = "listType là bắt buộc.")
    private LibraryListType listType;
}
