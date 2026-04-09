package com.example.comic.model.dto;

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

    @NotBlank(message = "Email là bắt buộc.")
    @Email(message = "Định dạng email không hợp lệ.")
    private String email;

    @NotBlank(message = "Mật khẩu là bắt buộc.")
    private String password;
}
