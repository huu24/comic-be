package com.example.comic.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.example.comic.repository.search")
@ConditionalOnProperty(prefix = "spring.elasticsearch", name = "uris")
public class ElasticsearchConfig {
}
