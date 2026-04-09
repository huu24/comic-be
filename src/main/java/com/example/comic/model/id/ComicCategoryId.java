package com.example.comic.model.id;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ComicCategoryId implements Serializable {
    private Long comicId;
    private Long categoryId;
}
