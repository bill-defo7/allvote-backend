package com.allvote.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.allvote.dtos.orga.*;
import com.allvote.security.UtilisateurConnecte;
import com.allvote.services.OrgaEvenementService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orga/evenements")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('EVENEMENT_GERER')")
public class OrgaEvenementController {

    private final OrgaEvenementService evenementService;

    @GetMapping
    public List<EvenementOrgaDto> lister() {
        return evenementService.lister(UtilisateurConnecte.idOrganisateur());
    }

    @GetMapping("/{idEvenement}")
    public EvenementOrgaDetailDto detail(@PathVariable Long idEvenement) {
        return evenementService.detail(UtilisateurConnecte.idOrganisateur(), idEvenement);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EvenementOrgaDetailDto creer(@Valid @RequestBody EvenementRequest request) {
        return evenementService.creer(UtilisateurConnecte.idOrganisateur(), request);
    }

    @PutMapping("/{idEvenement}")
    public EvenementOrgaDetailDto modifier(@PathVariable Long idEvenement, @Valid @RequestBody EvenementRequest request) {
        return evenementService.modifier(UtilisateurConnecte.idOrganisateur(), idEvenement, request);
    }

    /** {"statut":"PUBLIE"} | "BROUILLON" | "ARCHIVE" */
    @PatchMapping("/{idEvenement}/statut")
    @PreAuthorize("hasAuthority('EVENEMENT_PUBLIER')")
    public EvenementOrgaDetailDto statut(@PathVariable Long idEvenement, @Valid @RequestBody StatutEvenementRequest request) {
        return evenementService.changerStatut(UtilisateurConnecte.idOrganisateur(), idEvenement, request);
    }

    @DeleteMapping("/{idEvenement}")
    public ResponseEntity<Void> supprimer(@PathVariable Long idEvenement) {
        evenementService.supprimer(UtilisateurConnecte.idOrganisateur(), idEvenement);
        return ResponseEntity.noContent().build();
    }
}
