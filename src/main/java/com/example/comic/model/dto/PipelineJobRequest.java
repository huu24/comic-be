package com.example.comic.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineJobRequest {
    private String job_id;
    private String chapter_id;
    private String page_id;
    private String image_url;
    private String source_lang;
    private List<String> target_langs;
    private String comic_type;
    private boolean skip_translate;
    private String webhook_url;
}