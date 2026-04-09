package com.example.comic.model;

import com.example.comic.model.id.ComicCategoryId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "comic_categories")
@IdClass(ComicCategoryId.class)
public class ComicCategory {

    @Id
    @Column(name = "comic_id")
    private Long comicId;

    @Id
    @Column(name = "category_id")
    private Long categoryId;
}
