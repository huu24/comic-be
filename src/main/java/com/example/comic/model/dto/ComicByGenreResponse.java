package com.example.comic.model.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComicByGenreResponse {

    private String genre;
    private List<ComicGenreBookResponse> books;
}
