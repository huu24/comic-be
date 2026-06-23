package com.example.comic.listener;

import com.example.comic.event.PageReadEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PageReadEventListener {

    @EventListener
    public void handle(PageReadEvent event){

        log.info(
            "User {} đọc comic {}",
            event.getUserId(),
            event.getComicId()
        );
    }
}