package com.example.comic.model.dto;

import com.example.comic.model.LibraryListType;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLibraryItemResponse {
    private Long comicId;
    private String title;
    private String coverImageUrl;
    private LibraryListType listType;
    private Instant savedAt;
}
