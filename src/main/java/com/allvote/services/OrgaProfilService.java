package com.allvote.services;

import com.allvote.dtos.orga.ProfilOrganisateurDto;
import com.allvote.dtos.orga.ProfilOrganisateurRequest;

public interface OrgaProfilService {

    ProfilOrganisateurDto profil(Long idOrganisateur);

    ProfilOrganisateurDto modifier(Long idOrganisateur, ProfilOrganisateurRequest request);
}
