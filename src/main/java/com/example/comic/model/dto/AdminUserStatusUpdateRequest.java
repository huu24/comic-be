package com.example.comic.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserStatusUpdateRequest {

    @Schema(example = "ACTIVE")
    @NotNull(message = "Trạng thái là bắt buộc.")
    private String status;
}
