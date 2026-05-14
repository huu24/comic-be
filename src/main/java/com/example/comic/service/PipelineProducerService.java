package com.example.comic.service;

import com.example.comic.model.dto.PipelineJobRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PipelineProducerService {

    private final RabbitTemplate rabbitTemplate;

    @Value("${application.rabbitmq.manga-exchange}")
    private String exchange;

    @Value("${application.rabbitmq.manga-routing-key}")
    private String routingKey;

    public void sendToPipeline(PipelineJobRequest request) {
        rabbitTemplate.convertAndSend(exchange, routingKey, request);
    }
}