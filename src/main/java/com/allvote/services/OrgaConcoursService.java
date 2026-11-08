package com.allvote.services;

import com.allvote.dtos.orga.*;

/** Concours, catégories et candidats de l'organisateur connecté. */
public interface OrgaConcoursService {

    ConcoursOrgaDetailDto detail(Long idOrganisateur, Long idConcours);

    ConcoursOrgaDetailDto creer(Long idOrganisateur, Long idEvenement, ConcoursRequest request);

    ConcoursOrgaDetailDto modifier(Long idOrganisateur, Long idConcours, ConcoursRequest request);

    ConcoursOrgaDetailDto changerStatut(Long idOrganisateur, Long idConcours, StatutConcoursRequest request);

    void supprimer(Long idOrganisateur, Long idConcours);

    // --- catégories ---
    ConcoursOrgaDetailDto ajouterCategorie(Long idOrganisateur, Long idConcours, CategorieRequest request);

    ConcoursOrgaDetailDto modifierCategorie(Long idOrganisateur, Long idCategorie, CategorieRequest request);

    ConcoursOrgaDetailDto ordonnerCategories(Long idOrganisateur, Long idConcours, OrdreRequest request);

    void supprimerCategorie(Long idOrganisateur, Long idCategorie);

    // --- candidats ---
    CandidatOrgaDto ajouterCandidat(Long idOrganisateur, Long idCategorie, CandidatRequest request);

    CandidatOrgaDto modifierCandidat(Long idOrganisateur, Long idCandidat, CandidatRequest request);

    CandidatOrgaDto activerCandidat(Long idOrganisateur, Long idCandidat, ActifRequest request);

    void supprimerCandidat(Long idOrganisateur, Long idCandidat);
}
