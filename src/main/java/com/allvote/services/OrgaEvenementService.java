package com.allvote.services;

import java.util.List;

import com.allvote.dtos.orga.*;

public interface OrgaEvenementService {

    List<EvenementOrgaDto> lister(Long idOrganisateur);

    EvenementOrgaDetailDto detail(Long idOrganisateur, Long idEvenement);

    EvenementOrgaDetailDto creer(Long idOrganisateur, EvenementRequest request);

    EvenementOrgaDetailDto modifier(Long idOrganisateur, Long idEvenement, EvenementRequest request);

    EvenementOrgaDetailDto changerStatut(Long idOrganisateur, Long idEvenement, StatutEvenementRequest request);

    void supprimer(Long idOrganisateur, Long idEvenement);
}
