package com.example.comic.event;

import com.example.comic.model.Comic;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ComicSavedEvent {

    private final Comic comic;
}
