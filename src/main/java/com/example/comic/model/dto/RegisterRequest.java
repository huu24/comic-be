package com.example.comic.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @Schema(example = "hoangconghuu24@gmail.com")
    @NotBlank(message = "Email là bắt buộc.")
    @Email(message = "Định dạng email không hợp lệ.")
    private String email;

    @Schema(example = "SecurePass1")
    @NotBlank(message = "Mật khẩu là bắt buộc.")
    @Size(min = 8, message = "Mật khẩu tối thiểu 8 ký tự.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Mật khẩu phải bao gồm chữ và số.")
    private String password;

    @Schema(example = "Nguyen Van A")
    @NotBlank(message = "fullName là bắt buộc.")
    private String fullName;
}
