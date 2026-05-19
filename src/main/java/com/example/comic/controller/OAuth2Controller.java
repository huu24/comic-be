package com.example.comic.controller;

import com.example.comic.model.dto.DataResponse;
import com.example.comic.model.dto.OAuth2ProviderResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/oauth2")
@RequiredArgsConstructor
public class OAuth2Controller {

    @GetMapping("/providers")
    public ResponseEntity<DataResponse<List<OAuth2ProviderResponse>>> getProviders() {
        List<OAuth2ProviderResponse> providers = List.of(
                OAuth2ProviderResponse.builder()
                        .provider("google")
                        .displayName("Google")
                        .authorizationUrl("/oauth2/authorization/google")
                        .build());
        return ResponseEntity.ok(
                DataResponse.<List<OAuth2ProviderResponse>>builder().data(providers).build());
    }
}
