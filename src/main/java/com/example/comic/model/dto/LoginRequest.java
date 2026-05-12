package com.example.comic.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @Schema(example = "hoangconghuu24@gmail.com")
    @NotBlank(message = "Email là bắt buộc.")
    @Email(message = "Định dạng email không hợp lệ.")
    private String email;

    @Schema(example = "Password123")
    @NotBlank(message = "Mật khẩu là bắt buộc.")
    private String password;
}
