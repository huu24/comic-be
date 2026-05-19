package com.example.comic.service;

import com.example.comic.exception.PermissionDeniedException;
import com.example.comic.exception.UnauthenticatedException;
import com.example.comic.model.User;
import com.example.comic.model.UserRole;
import com.example.comic.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User requireUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = getEmail(authentication);
        if (email == null) {
            throw new UnauthenticatedException("Vui lòng đăng nhập để sử dụng tính năng này.");
        }

        return userRepository
            .findByEmail(email)
            .orElseThrow(() -> new UnauthenticatedException("Vui lòng đăng nhập để sử dụng tính năng này."));
    }

    public User requireAdmin() {
        User user = requireUser();
        if (user.getRole() != UserRole.ADMIN) {
            throw new PermissionDeniedException("Bạn không có quyền truy cập vào khu vực này.");
        }
        return user;
    }

    public UserRole resolveRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = getEmail(authentication);
        if (email == null) {
            return UserRole.GUEST;
        }

        return userRepository
            .findByEmail(email)
            .map(User::getRole)
            .orElse(UserRole.GUEST);
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String email = getEmail(authentication);
        if (email == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        return userRepository.findByEmail(email).orElse(null);
    }

    private String getEmail(Authentication authentication) {
        if (
            authentication == null ||
            !authentication.isAuthenticated() ||
            authentication instanceof AnonymousAuthenticationToken
        ) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else if (principal instanceof OAuth2User) {
            return ((OAuth2User) principal).getAttribute("email");
        }

        return authentication.getName();
    }
}
