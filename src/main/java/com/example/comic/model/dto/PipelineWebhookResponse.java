package com.example.comic.model.dto;

import lombok.Data;
import java.util.Map;

@Data
public class PipelineWebhookResponse {
    private String job_id;
    private String status; // COMPLETED hoặc FAILED
    private String page_id;
    private String chapter_id;
    private String error; // Dùng khi status là FAILED
    private PipelineResult result;

    @Data
    public static class PipelineResult {
        private String cleaned_img_url;
        private PipelineMetadata metadata;
    }

    @Data
    public static class PipelineMetadata {
        private String original_url;
        private Map<String, String> translations; // mapping: lang -> translation_url
    }
}