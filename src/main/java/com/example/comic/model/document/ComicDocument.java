package com.example.comic.model.document;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "comics")
public class ComicDocument implements Serializable {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Keyword)
    private String author;

    @Field(type = FieldType.Keyword)
    private String coverImageUrl;

    @Field(type = FieldType.Keyword)
    private String originalLanguage;

    @Field(type = FieldType.Keyword)
    private String format;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Double)
    private Double averageRating;
}
