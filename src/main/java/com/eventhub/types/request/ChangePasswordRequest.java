package com.eventhub.types.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @NotBlank private String currentPassword;
    @NotBlank @Size(min = 8, max = 16) private String newPassword;
    @NotBlank private String newPasswordConfirm;
}
