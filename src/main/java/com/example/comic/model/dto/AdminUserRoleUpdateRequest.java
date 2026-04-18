package com.example.comic.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserRoleUpdateRequest {

    @NotBlank(message = "Vai trò là bắt buộc.")
    private String role;
}
