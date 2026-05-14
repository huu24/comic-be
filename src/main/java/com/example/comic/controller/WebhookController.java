package com.example.comic.controller;

import com.example.comic.model.dto.PipelineWebhookResponse;
import com.example.comic.service.WebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/processing-result")
    public ResponseEntity<Void> receiveProcessingResult(@RequestBody PipelineWebhookResponse response) {
        webhookService.processPipelineResult(response);
        return ResponseEntity.ok().build();
    }
}