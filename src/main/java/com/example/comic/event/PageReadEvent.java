package com.example.comic.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class PageReadEvent {

    private Long userId;
    private Long comicId;
    private Long chapterId;
    private Long pageId;
    private Instant readAt;
}