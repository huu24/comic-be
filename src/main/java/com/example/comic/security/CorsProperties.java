package com.example.comic.security;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "application.cors")
public class CorsProperties {

    private List<String> allowedOrigins = List.of(
        "http://localhost:3000",
        "http://127.0.0.1:3000",
        "http://localhost:8080"
    );
}
