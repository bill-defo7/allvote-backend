package com.allvote.services;

import com.allvote.dtos.admin.CommissionRequest;
import com.allvote.dtos.admin.MotifRequest;
import com.allvote.dtos.admin.OrganisateurAdminDto;
import com.allvote.dtos.commun.PageDto;
import com.allvote.utiles.StatutOrganisateur;

/** Cycle de vie des organisateurs : EN_ATTENTE -> VALIDE / REFUSE ; VALIDE <-> SUSPENDU. */
public interface AdminOrganisateurService {

    PageDto<OrganisateurAdminDto> lister(StatutOrganisateur statut, int page, int taille);

    OrganisateurAdminDto detail(Long idOrganisateur);

    OrganisateurAdminDto valider(Long idOrganisateur);

    OrganisateurAdminDto refuser(Long idOrganisateur, MotifRequest request);

    OrganisateurAdminDto suspendre(Long idOrganisateur, MotifRequest request);

    OrganisateurAdminDto reactiver(Long idOrganisateur);

    OrganisateurAdminDto modifierCommission(Long idOrganisateur, CommissionRequest request);
}
