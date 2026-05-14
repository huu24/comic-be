package com.example.comic.service;

import com.example.comic.model.ChapterPage;
import com.example.comic.model.PageTranslation;
import com.example.comic.model.ProcessStatus;
import com.example.comic.model.dto.PipelineWebhookResponse;
import com.example.comic.repository.ChapterPageRepository;
import com.example.comic.repository.PageTranslationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final ChapterPageRepository chapterPageRepository;
    private final PageTranslationRepository pageTranslationRepository;

    @Transactional
    public void processPipelineResult(PipelineWebhookResponse response) {
        Long pageId = Long.parseLong(response.getPage_id());
        ChapterPage page = chapterPageRepository.findById(pageId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy trang với ID: " + pageId));

        if ("FAILED".equals(response.getStatus())) {
            log.error("Pipeline processing failed for page {}: {}", pageId, response.getError());
            page.setStatus(ProcessStatus.FAILED);
            chapterPageRepository.save(page);
            return;
        }

        // Cập nhật thông tin trang chính
        var result = response.getResult();
        page.setCleanedImageUrl(result.getCleaned_img_url());
        page.setOriginalMetadataUrl(result.getMetadata().getOriginal_url());
        page.setStatus(ProcessStatus.COMPLETED);
        chapterPageRepository.save(page);

        // Lưu thông tin dịch thuật đa ngôn ngữ
        if (result.getMetadata().getTranslations() != null) {
            List<PageTranslation> translations = new ArrayList<>();
            result.getMetadata().getTranslations().forEach((lang, url) -> {
                translations.add(PageTranslation.builder()
                        .pageId(pageId)
                        .lang(lang)
                        .translationMetadataUrl(url)
                        .build());
            });
            pageTranslationRepository.saveAll(translations);
        }
        
        log.info("Successfully updated pipeline results for page {}", pageId);
    }
}