package com.allvote.dtos.orga;

import com.allvote.utiles.StatutEvenement;

import jakarta.validation.constraints.NotNull;

public record StatutEvenementRequest(@NotNull(message = "Statut obligatoire") StatutEvenement statut) {
}
