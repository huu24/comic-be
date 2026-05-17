package com.example.comic.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ComicDeletedEvent {

    private final Long comicId;
}
