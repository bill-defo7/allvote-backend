package com.allvote.services;

import com.allvote.dtos.admin.EvenementAdminDto;
import com.allvote.dtos.admin.TableauBordDto;
import com.allvote.dtos.admin.UtilisateurAdminDto;
import com.allvote.dtos.commun.PageDto;
import com.allvote.dtos.orga.ActifRequest;
import com.allvote.dtos.orga.StatutEvenementRequest;
import com.allvote.utiles.StatutEvenement;

/** Tableau de bord, modération des événements, comptes utilisateurs. */
public interface AdminPlateformeService {

    TableauBordDto tableauBord();

    PageDto<EvenementAdminDto> evenements(StatutEvenement statut, Long idOrganisateur, int page, int taille);

    EvenementAdminDto changerStatutEvenement(Long idEvenement, StatutEvenementRequest request);

    PageDto<UtilisateurAdminDto> utilisateurs(String recherche, int page, int taille);

    UtilisateurAdminDto activerUtilisateur(Long idUtilisateur, Long idSuperAdmin, ActifRequest request);

    UtilisateurAdminDto deverrouiller(Long idUtilisateur);
}
