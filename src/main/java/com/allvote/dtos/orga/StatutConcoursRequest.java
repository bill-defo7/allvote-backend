package com.allvote.dtos.orga;

import com.allvote.utiles.StatutConcours;

/** OUVERT, SUSPENDU ou FERME pour forcer ; null pour revenir au mode automatique (selon les dates). */
public record StatutConcoursRequest(StatutConcours statutForce) {
}
