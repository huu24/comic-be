package com.example.comic.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrendingScheduler {

    private final StringRedisTemplate redisTemplate;

    /**
     * Reset trending today
     * Mỗi ngày 00:00
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void resetTodayTrending() {

        redisTemplate.delete("comic:trending:" + LocalDate.now().minusDays(1));

        log.info("Reset trending today");
    }

    /**
     * Reset trending week
     * Mỗi thứ 2 lúc 00:00
     */
    @Scheduled(cron = "0 0 0 * * MON")
    public void resetWeekTrending() {

        redisTemplate.delete("comic:trending:week");

        log.info("Reset trending week");
    }

    /**
     * Reset trending month
     * Ngày đầu tháng lúc 00:00
     */
    @Scheduled(cron = "0 0 0 1 * *")
    public void resetMonthTrending() {

        redisTemplate.delete("comic:trending:month");

        log.info("Reset trending month");
    }
}