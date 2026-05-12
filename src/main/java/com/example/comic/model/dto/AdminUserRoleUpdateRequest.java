package com.example.comic.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(example = "ADMIN")
    @NotBlank(message = "Vai trò là bắt buộc.")
    private String role;
}
