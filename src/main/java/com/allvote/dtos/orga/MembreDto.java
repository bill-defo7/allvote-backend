package com.allvote.dtos.orga;

import java.time.Instant;
import java.util.List;

public record MembreDto(Long idUtilisateur, String nom, String prenom, String email, String telephone,
        List<String> roles, boolean actif, Instant derniereConnexion, Instant dateCreation) {
}
