package com.example.comic.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyEmailOtpRequest {

    @Schema(example = "hoangconghuu24@gmail.com")
    @NotBlank(message = "Email là bắt buộc.")
    @Email(message = "Định dạng email không hợp lệ.")
    private String email;

    @Schema(example = "123456")
    @NotBlank(message = "OTP là bắt buộc.")
    @Pattern(regexp = "^\\d{6}$", message = "OTP phải gồm đúng 6 chữ số.")
    private String otp;
}
