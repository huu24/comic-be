package com.example.comic.service;

import com.example.comic.exception.UnauthenticatedException;
import com.example.comic.model.User;
import com.example.comic.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User requireUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (
            authentication == null ||
            !authentication.isAuthenticated() ||
            authentication instanceof AnonymousAuthenticationToken ||
            authentication.getName() == null
        ) {
            throw new UnauthenticatedException("Vui lòng đăng nhập để sử dụng tính năng này.");
        }

        return userRepository
            .findByEmail(authentication.getName())
            .orElseThrow(() -> new UnauthenticatedException("Vui lòng đăng nhập để sử dụng tính năng này."));
    }
}
