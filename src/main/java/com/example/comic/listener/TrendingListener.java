package com.example.comic.listener;

import com.example.comic.event.PageReadEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Component
@RequiredArgsConstructor
public class TrendingListener {

    private final StringRedisTemplate redisTemplate;

    @TransactionalEventListener(
            phase = AFTER_COMMIT
    )
    public void handle(
            PageReadEvent event
    ){
        String key =
                "comic:trending:"
                        + LocalDate.now();

        redisTemplate.opsForZSet()
                .incrementScore(
                        key,
                        event.getComicId().toString(),
                        1
                );
    }
}