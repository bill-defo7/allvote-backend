package com.allvote.dtos.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.allvote.utiles.Validations;

public record ChangerMotDePasseRequest(
        @NotBlank(message = "Ancien mot de passe obligatoire") @Size(max = 128) String ancienMotDePasse,
        @NotBlank(message = "Nouveau mot de passe obligatoire")
        @Pattern(regexp = Validations.MOT_DE_PASSE,
                message = Validations.MESSAGE_MOT_DE_PASSE)
        String nouveauMotDePasse) {
}
