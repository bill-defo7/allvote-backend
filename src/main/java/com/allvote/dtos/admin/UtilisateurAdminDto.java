package com.allvote.dtos.admin;

import java.time.Instant;
import java.util.List;

public record UtilisateurAdminDto(Long idUtilisateur, String nom, String prenom, String email, String telephone,
        List<String> roles, boolean actif, boolean verrouille, Instant derniereConnexion, Instant dateCreation,
        Long idOrganisateur, String organisateurNom) {
}
