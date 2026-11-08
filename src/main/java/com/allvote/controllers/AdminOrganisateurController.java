package com.allvote.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.allvote.dtos.admin.CommissionRequest;
import com.allvote.dtos.admin.MotifRequest;
import com.allvote.dtos.admin.OrganisateurAdminDto;
import com.allvote.dtos.commun.PageDto;
import com.allvote.services.AdminOrganisateurService;
import com.allvote.utiles.StatutOrganisateur;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/organisateurs")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ORGANISATEUR_VALIDER')")
public class AdminOrganisateurController {

    private final AdminOrganisateurService organisateurService;

    /** ?statut=EN_ATTENTE&page=0&taille=20 */
    @GetMapping
    public PageDto<OrganisateurAdminDto> lister(@RequestParam(required = false) StatutOrganisateur statut,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int taille) {
        return organisateurService.lister(statut, page, taille);
    }

    @GetMapping("/{id}")
    public OrganisateurAdminDto detail(@PathVariable Long id) {
        return organisateurService.detail(id);
    }

    @PostMapping("/{id}/valider")
    public OrganisateurAdminDto valider(@PathVariable Long id) {
        return organisateurService.valider(id);
    }

    @PostMapping("/{id}/refuser")
    public OrganisateurAdminDto refuser(@PathVariable Long id, @Valid @RequestBody MotifRequest request) {
        return organisateurService.refuser(id, request);
    }

    @PostMapping("/{id}/suspendre")
    public OrganisateurAdminDto suspendre(@PathVariable Long id, @Valid @RequestBody MotifRequest request) {
        return organisateurService.suspendre(id, request);
    }

    @PostMapping("/{id}/reactiver")
    public OrganisateurAdminDto reactiver(@PathVariable Long id) {
        return organisateurService.reactiver(id);
    }

    /** {"tauxCommission":10} ; {"tauxCommission":null} = taux par défaut */
    @PutMapping("/{id}/commission")
    @PreAuthorize("hasAuthority('PLATEFORME_PARAMETRER')")
    public OrganisateurAdminDto commission(@PathVariable Long id, @Valid @RequestBody CommissionRequest request) {
        return organisateurService.modifierCommission(id, request);
    }
}
