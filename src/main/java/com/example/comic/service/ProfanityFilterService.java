package com.example.comic.service;

import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ProfanityFilterService {

    private static final Set<String> BLOCKED_WORDS = Set.of("dm", "dmm", "vl", "vcl");

    public String sanitize(String content) {
        if (content == null || content.isBlank()) {
            return content;
        }
        String sanitized = content;
        for (String badWord : BLOCKED_WORDS) {
            sanitized = sanitized.replaceAll("(?i)\\b" + java.util.regex.Pattern.quote(badWord) + "\\b", "***");
        }
        return sanitized;
    }
}
