package com.allvote.dtos.admin;

/** Ce que le prestataire répond MAINTENANT pour cette transaction (lecture seule, rien n'est modifié). */
public record VerificationPrestataireDto(String etat, Long montantPaye, int montantAttendu, String message) {
}
