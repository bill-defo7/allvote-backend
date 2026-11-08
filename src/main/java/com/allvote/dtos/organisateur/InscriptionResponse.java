package com.allvote.dtos.organisateur;

import com.allvote.utiles.StatutOrganisateur;

public record InscriptionResponse(String slug, StatutOrganisateur statut, String message) {
}
