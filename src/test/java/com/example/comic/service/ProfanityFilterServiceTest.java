package com.example.comic.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProfanityFilterServiceTest {

    private final ProfanityFilterService profanityFilterService = new ProfanityFilterService();

    @Test
    void sanitize_shouldReplaceBlockedWords() {
        assertEquals("Hello *** world", profanityFilterService.sanitize("Hello dmm world"));
    }

    @Test
    void sanitize_shouldKeepCleanText() {
        assertEquals("Xin chao", profanityFilterService.sanitize("Xin chao"));
    }

    @Test
    void sanitize_shouldKeepNullAndBlankInput() {
        assertNull(profanityFilterService.sanitize(null));
        assertEquals("   ", profanityFilterService.sanitize("   "));
    }
}
