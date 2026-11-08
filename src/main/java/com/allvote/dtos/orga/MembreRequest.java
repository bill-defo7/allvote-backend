package com.allvote.dtos.orga;

import com.allvote.utiles.Validations;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Le responsable choisit un mot de passe provisoire et le transmet au membre (qui le changera). */
public record MembreRequest(
        @NotBlank(message = "Nom obligatoire") @Size(max = 80) String nom,
        @NotBlank(message = "Prénom obligatoire") @Size(max = 80) String prenom,
        @NotBlank(message = "Email obligatoire") @Email(message = "Email invalide") @Size(max = 120) String email,
        @Size(max = 20) String telephone,
        @NotBlank(message = "Mot de passe obligatoire")
        @Pattern(regexp = Validations.MOT_DE_PASSE, message = Validations.MESSAGE_MOT_DE_PASSE)
        String motDePasseProvisoire) {
}
