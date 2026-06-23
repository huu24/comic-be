package com.example.comic.listener;

import com.example.comic.event.PageReadEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ViewCounterListener {

    private final StringRedisTemplate redisTemplate;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handle(
            PageReadEvent event
    ) {

        String key =
                "comic:view:"
                        + event.getComicId();

        redisTemplate.opsForValue()
                .increment(key);
    }
}