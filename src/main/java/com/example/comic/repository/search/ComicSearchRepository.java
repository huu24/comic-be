package com.example.comic.repository.search;

import com.example.comic.model.document.ComicDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ComicSearchRepository extends ElasticsearchRepository<ComicDocument, Long> {

    @Query("{" +
            "  \"bool\": {" +
            "    \"should\": [" +
            "      { \"match_phrase_prefix\": { \"title\": { \"query\": \"?0\", \"max_expansions\": 10, \"boost\": 3.0 } } }," +
            "      { \"match\": { \"title\": { \"query\": \"?0\", \"fuzziness\": \"2\", \"boost\": 2.0 } } }," +
            "      { \"match\": { \"description\": { \"query\": \"?0\", \"fuzziness\": \"AUTO\", \"boost\": 0.5 } } }" +
            "    ]," +
            "    \"minimum_should_match\": 1" +
            "  }" +
            "}")
    Page<ComicDocument> searchByKeyword(String keyword, Pageable pageable);
}
