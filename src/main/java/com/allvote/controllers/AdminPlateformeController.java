package com.allvote.controllers;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.allvote.dtos.admin.CommissionDefautRequest;
import com.allvote.dtos.admin.EvenementAdminDto;
import com.allvote.dtos.admin.ParametreDto;
import com.allvote.dtos.admin.TableauBordDto;
import com.allvote.dtos.admin.UtilisateurAdminDto;
import com.allvote.dtos.commun.PageDto;
import com.allvote.dtos.orga.ActifRequest;
import com.allvote.dtos.orga.StatutEvenementRequest;
import com.allvote.security.UtilisateurConnecte;
import com.allvote.services.AdminPlateformeService;
import com.allvote.services.ParametreService;
import com.allvote.utiles.StatutEvenement;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Tableau de bord, paramètres, modération des événements, comptes. (Accès : rôle SUPER_ADMIN, voir SecurityConfig.) */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminPlateformeController {

    private final AdminPlateformeService plateformeService;
    private final ParametreService parametreService;

    @GetMapping("/tableau-de-bord")
    public TableauBordDto tableauBord() {
        return plateformeService.tableauBord();
    }

    // ------------------------- Paramètres -------------------------

    @GetMapping("/parametres")
    @PreAuthorize("hasAuthority('PLATEFORME_PARAMETRER')")
    public List<ParametreDto> parametres() {
        return parametreService.lister();
    }

    /** {"taux":15} : s'applique aux NOUVELLES transactions uniquement. */
    @PutMapping("/parametres/commission")
    @PreAuthorize("hasAuthority('PLATEFORME_PARAMETRER')")
    public ParametreDto commission(@Valid @RequestBody CommissionDefautRequest request) {
        return parametreService.modifierTauxDefaut(request.taux());
    }

    /** {"taux":5} : frais de service payés en plus par le votant ; NOUVELLES transactions uniquement. */
    @PutMapping("/parametres/frais")
    @PreAuthorize("hasAuthority('PLATEFORME_PARAMETRER')")
    public ParametreDto frais(@Valid @RequestBody CommissionDefautRequest request) {
        return parametreService.modifierFraisVotant(request.taux());
    }

    // ------------------------- Événements (modération) -------------------------

    @GetMapping("/evenements")
    @PreAuthorize("hasAuthority('ORGANISATEUR_VALIDER')")
    public PageDto<EvenementAdminDto> evenements(@RequestParam(required = false) StatutEvenement statut,
            @RequestParam(required = false) Long idOrganisateur,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int taille) {
        return plateformeService.evenements(statut, idOrganisateur, page, taille);
    }

    /** {"statut":"BROUILLON"} (retirer du public) ou {"statut":"ARCHIVE"} */
    @PatchMapping("/evenements/{id}/statut")
    @PreAuthorize("hasAuthority('ORGANISATEUR_VALIDER')")
    public EvenementAdminDto statutEvenement(@PathVariable Long id, @Valid @RequestBody StatutEvenementRequest request) {
        return plateformeService.changerStatutEvenement(id, request);
    }

    // ------------------------- Utilisateurs -------------------------

    @GetMapping("/utilisateurs")
    @PreAuthorize("hasAuthority('UTILISATEUR_GERER')")
    public PageDto<UtilisateurAdminDto> utilisateurs(@RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int taille) {
        return plateformeService.utilisateurs(q, page, taille);
    }

    @PatchMapping("/utilisateurs/{id}/actif")
    @PreAuthorize("hasAuthority('UTILISATEUR_GERER')")
    public UtilisateurAdminDto actif(@PathVariable Long id, @Valid @RequestBody ActifRequest request) {
        return plateformeService.activerUtilisateur(id, UtilisateurConnecte.idUtilisateur(), request);
    }

    @PostMapping("/utilisateurs/{id}/deverrouiller")
    @PreAuthorize("hasAuthority('UTILISATEUR_GERER')")
    public UtilisateurAdminDto deverrouiller(@PathVariable Long id) {
        return plateformeService.deverrouiller(id);
    }
}
