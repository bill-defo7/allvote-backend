package com.allvote.services;

import com.allvote.dtos.commun.PageDto;
import com.allvote.dtos.publique.*;

public interface PubliqueService {

    PageDto<EvenementResumeDto> evenements(String recherche, String ville, int page, int taille);

    EvenementDetailDto evenement(String slug);

    OrganisateurPublicDto organisateur(String slug);

    ConcoursDetailDto concours(Long idConcours);

    CandidatPageDto candidat(Long idConcours, Long idCandidat);
}
