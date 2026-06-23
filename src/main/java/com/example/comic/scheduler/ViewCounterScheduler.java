package com.example.comic.scheduler;

import com.example.comic.repository.ComicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class ViewCounterScheduler {

    private final StringRedisTemplate redisTemplate;
    private final ComicRepository comicRepository;

    @Scheduled(fixedRate = 300000)
    public void flushViews() {

        Set<String> keys =
                redisTemplate.keys(
                        "comic:view:*"
                );

        if(keys == null || keys.isEmpty()){
            return;
        }

        for(String key : keys){

            String comicIdStr =
                    key.replace(
                            "comic:view:",
                            ""
                    );

            Long comicId =
                    Long.valueOf(comicIdStr);

            String value =
                    redisTemplate.opsForValue()
                            .get(key);

            long views =
                    value == null
                            ? 0
                            : Long.parseLong(value);

            comicRepository
                    .increaseViews(
                            comicId,
                            views
                    );

            redisTemplate.delete(key);
        }

        log.info(
                "Flushed view counter"
        );
    }
}