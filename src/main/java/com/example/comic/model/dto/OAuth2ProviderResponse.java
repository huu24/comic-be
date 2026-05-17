package com.example.comic.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2ProviderResponse {
    private String provider;
    private String displayName;
    private String authorizationUrl;
}
