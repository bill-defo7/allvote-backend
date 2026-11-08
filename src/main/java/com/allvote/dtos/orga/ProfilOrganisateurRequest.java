package com.allvote.dtos.orga;

import com.allvote.utiles.Operateur;
import com.allvote.utiles.Validations;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Le slug ne change jamais (les liens déjà partagés restent valides). */
public record ProfilOrganisateurRequest(
        @NotBlank(message = "Nom obligatoire") @Size(min = 3, max = 120) String nom,
        @Size(max = 2000) String description,
        @Pattern(regexp = Validations.URL_IMAGE, message = "Image invalide : téléversez-la d'abord") String logoUrl,
        @NotBlank(message = "Email obligatoire") @Email(message = "Email invalide") @Size(max = 120) String email,
        @Size(max = 20) String telephone,
        @Size(max = 80) String ville,
        @Size(max = 20) String numeroReversement,
        Operateur operateurReversement) {
}
