package com.allvote.dtos.orga;

import java.time.Instant;

import com.allvote.utiles.Validations;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * brouillon = true : invisible du public tant qu'il n'est pas prêt.
 * Champs facultatifs : scoresPublics (défaut true), brouillon (défaut false).
 */
public record ConcoursRequest(
        @NotBlank(message = "Titre obligatoire") @Size(min = 3, max = 150) String titre,
        @Size(max = 2000) String description,
        @Pattern(regexp = Validations.URL_IMAGE, message = "Image invalide : téléversez-la d'abord") String afficheUrl,
        @NotNull(message = "Prix obligatoire") @Min(value = 50, message = "50 FCFA minimum")
        @Max(value = 100000, message = "100 000 FCFA maximum") Integer prixVoix,
        Boolean scoresPublics,
        @NotNull(message = "Date d'ouverture obligatoire") Instant dateOuverture,
        @NotNull(message = "Date de fermeture obligatoire") Instant dateFermeture,
        Boolean brouillon) {
}
