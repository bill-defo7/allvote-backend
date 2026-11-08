package com.allvote.dtos.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Email obligatoire") @Email(message = "Email invalide") @Size(max = 120) String email,
        @NotBlank(message = "Mot de passe obligatoire") @Size(max = 128) String password) {
}
