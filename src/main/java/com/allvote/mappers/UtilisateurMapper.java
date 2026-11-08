package com.allvote.mappers;

import java.util.List;
import java.util.TreeSet;

import com.allvote.dtos.auth.OrganisateurCourtDto;
import com.allvote.dtos.auth.UtilisateurDto;
import com.allvote.entities.Organisateur;
import com.allvote.entities.Permission;
import com.allvote.entities.Role;
import com.allvote.entities.Utilisateur;

/** Entité -> DTO pour les comptes. À appeler dans une transaction (organisateur chargé à la demande). */
public final class UtilisateurMapper {

    private UtilisateurMapper() {
    }

    public static UtilisateurDto versDto(Utilisateur u) {
        List<String> roles = u.getRoles().stream().map(Role::getNomRole).sorted().toList();
        TreeSet<String> permissions = new TreeSet<>();
        u.getRoles().forEach(r -> r.getPermissions().stream().map(Permission::getCode).forEach(permissions::add));
        return new UtilisateurDto(u.getIdUtilisateur(), u.getNom(), u.getPrenom(), u.getEmail(), u.getTelephone(),
                roles, List.copyOf(permissions), organisateurCourt(u.getOrganisateur()), u.getDerniereConnexion());
    }

    public static OrganisateurCourtDto organisateurCourt(Organisateur o) {
        if (o == null) {
            return null;
        }
        return new OrganisateurCourtDto(o.getIdOrganisateur(), o.getNom(), o.getSlug(), o.getLogoUrl(),
                o.getStatut(), o.getMotifRefus());
    }
}
