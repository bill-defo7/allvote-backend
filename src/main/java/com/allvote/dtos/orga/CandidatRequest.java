package com.allvote.dtos.orga;

import com.allvote.utiles.TypeCandidat;
import com.allvote.utiles.Validations;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** numero facultatif : numéro suivant de la catégorie si absent. */
public record CandidatRequest(
        @NotBlank(message = "Nom obligatoire") @Size(max = 120) String nom,
        @Size(max = 80) String sousTitre,
        @Size(max = 280, message = "280 caractères maximum") String description,
        @Pattern(regexp = Validations.URL_IMAGE, message = "Image invalide : téléversez-la d'abord") String photoUrl,
        TypeCandidat typeCandidat,
        @Min(1) @Max(999) Integer numero) {
}
