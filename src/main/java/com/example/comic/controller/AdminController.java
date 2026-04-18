package com.example.comic.controller;

import com.example.comic.model.dto.AdminDashboardSummaryResponse;
import com.example.comic.model.dto.AdminUserRoleUpdateRequest;
import com.example.comic.model.dto.AdminUserStatusUpdateRequest;
import com.example.comic.model.dto.AdminUserSummaryResponse;
import com.example.comic.model.dto.DataResponse;
import com.example.comic.model.dto.PageDataResponse;
import com.example.comic.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<DataResponse<PageDataResponse<AdminUserSummaryResponse>>> getUsers(
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
            DataResponse
                .<PageDataResponse<AdminUserSummaryResponse>>builder()
                .data(adminService.getUsers(keyword, page, size))
                .build()
        );
    }

    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<DataResponse<AdminUserSummaryResponse>> updateUserStatus(
        @PathVariable Long userId,
        @Valid @RequestBody AdminUserStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(
            DataResponse
                .<AdminUserSummaryResponse>builder()
                .data(adminService.updateUserStatus(userId, request.getStatus()))
                .build()
        );
    }

    @PatchMapping("/users/{userId}/role")
    public ResponseEntity<DataResponse<AdminUserSummaryResponse>> updateUserRole(
        @PathVariable Long userId,
        @Valid @RequestBody AdminUserRoleUpdateRequest request
    ) {
        return ResponseEntity.ok(
            DataResponse
                .<AdminUserSummaryResponse>builder()
                .data(adminService.updateUserRole(userId, request.getRole()))
                .build()
        );
    }

    @GetMapping("/dashboard/summary")
    public ResponseEntity<DataResponse<AdminDashboardSummaryResponse>> getDashboardSummary() {
        return ResponseEntity.ok(
            DataResponse.<AdminDashboardSummaryResponse>builder().data(adminService.getDashboardSummary()).build()
        );
    }
}
