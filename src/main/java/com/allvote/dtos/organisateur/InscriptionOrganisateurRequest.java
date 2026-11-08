package com.allvote.dtos.organisateur;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.allvote.utiles.Validations;

/**
 * Inscription d'une organisation + de son responsable (qui devient ORGA_ADMIN).
 * "website" est un piège à robots (champ caché côté front) : il doit rester vide.
 */
public record InscriptionOrganisateurRequest(
        // --- organisation ---
        @NotBlank(message = "Nom de l'organisation obligatoire") @Size(min = 3, max = 120) String nomOrganisation,
        @Size(max = 2000) String description,
        @Email(message = "Email de l'organisation invalide") @Size(max = 120) String emailOrganisation,
        @Size(max = 80) String ville,
        // --- responsable ---
        @NotBlank(message = "Nom obligatoire") @Size(max = 80) String nom,
        @NotBlank(message = "Prénom obligatoire") @Size(max = 80) String prenom,
        @NotBlank(message = "Email obligatoire") @Email(message = "Email invalide") @Size(max = 120) String email,
        @NotBlank(message = "Téléphone obligatoire") @Size(max = 20) String telephone,
        @NotBlank(message = "Mot de passe obligatoire")
        @Pattern(regexp = Validations.MOT_DE_PASSE,
                message = Validations.MESSAGE_MOT_DE_PASSE)
        String motDePasse,
        @NotNull(message = "Vous devez accepter les conditions d'utilisation")
        @AssertTrue(message = "Vous devez accepter les conditions d'utilisation") Boolean accepteCgu,
        // --- anti-spam ---
        @Size(max = 200) String website) {
}
