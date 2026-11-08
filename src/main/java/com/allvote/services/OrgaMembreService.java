package com.allvote.services;

import java.util.List;

import com.allvote.dtos.orga.ActifRequest;
import com.allvote.dtos.orga.MembreDto;
import com.allvote.dtos.orga.MembreRequest;

public interface OrgaMembreService {

    List<MembreDto> lister(Long idOrganisateur);

    MembreDto ajouter(Long idOrganisateur, MembreRequest request);

    MembreDto activer(Long idOrganisateur, Long idUtilisateurConnecte, Long idMembre, ActifRequest request);

    void supprimer(Long idOrganisateur, Long idUtilisateurConnecte, Long idMembre);
}
