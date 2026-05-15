package com.example.comic.event;

import com.example.comic.model.Comic;

public class ComicSavedEvent {
    private Comic comic;

    public ComicSavedEvent(Comic comic) {
        this.comic = comic;
    }
}
