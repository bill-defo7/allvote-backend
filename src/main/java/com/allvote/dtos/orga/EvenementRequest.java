package com.allvote.dtos.orga;

import java.time.Instant;

import com.allvote.utiles.Validations;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Dates au format ISO UTC, ex. "2026-11-26T18:00:00Z". */
public record EvenementRequest(
        @NotBlank(message = "Titre obligatoire") @Size(min = 3, max = 150) String titre,
        @Size(max = 2000) String description,
        @Size(max = 150) String lieu,
        Instant dateDebut,
        Instant dateFin,
        @Pattern(regexp = Validations.URL_IMAGE, message = "Image invalide : téléversez-la d'abord") String afficheUrl) {
}
