package com.example.comic.repository.search;

import com.example.comic.model.document.ComicDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ComicSearchRepository extends ElasticsearchRepository<ComicDocument, Long> {
}
