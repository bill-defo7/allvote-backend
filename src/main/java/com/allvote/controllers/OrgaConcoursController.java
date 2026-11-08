package com.allvote.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.allvote.dtos.orga.*;
import com.allvote.security.UtilisateurConnecte;
import com.allvote.services.OrgaConcoursService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Concours, catégories et candidats de l'organisation connectée. */
@RestController
@RequestMapping("/api/orga")
@RequiredArgsConstructor
public class OrgaConcoursController {

    private final OrgaConcoursService concoursService;

    private static Long org() {
        return UtilisateurConnecte.idOrganisateur();
    }

    // ------------------------- Concours -------------------------

    @PostMapping("/evenements/{idEvenement}/concours")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EVENEMENT_GERER')")
    public ConcoursOrgaDetailDto creer(@PathVariable Long idEvenement, @Valid @RequestBody ConcoursRequest request) {
        return concoursService.creer(org(), idEvenement, request);
    }

    @GetMapping("/concours/{idConcours}")
    @PreAuthorize("hasAnyAuthority('EVENEMENT_GERER', 'CANDIDAT_GERER')")
    public ConcoursOrgaDetailDto detail(@PathVariable Long idConcours) {
        return concoursService.detail(org(), idConcours);
    }

    @PutMapping("/concours/{idConcours}")
    @PreAuthorize("hasAuthority('EVENEMENT_GERER')")
    public ConcoursOrgaDetailDto modifier(@PathVariable Long idConcours, @Valid @RequestBody ConcoursRequest request) {
        return concoursService.modifier(org(), idConcours, request);
    }

    /** {"statutForce":"SUSPENDU"} ; {"statutForce":null} = retour au mode automatique */
    @PatchMapping("/concours/{idConcours}/statut")
    @PreAuthorize("hasAuthority('EVENEMENT_PUBLIER')")
    public ConcoursOrgaDetailDto statut(@PathVariable Long idConcours, @RequestBody StatutConcoursRequest request) {
        return concoursService.changerStatut(org(), idConcours, request);
    }

    @DeleteMapping("/concours/{idConcours}")
    @PreAuthorize("hasAuthority('EVENEMENT_GERER')")
    public ResponseEntity<Void> supprimer(@PathVariable Long idConcours) {
        concoursService.supprimer(org(), idConcours);
        return ResponseEntity.noContent().build();
    }

    // ------------------------- Catégories -------------------------

    @PostMapping("/concours/{idConcours}/categories")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EVENEMENT_GERER')")
    public ConcoursOrgaDetailDto ajouterCategorie(@PathVariable Long idConcours, @Valid @RequestBody CategorieRequest request) {
        return concoursService.ajouterCategorie(org(), idConcours, request);
    }

    /** {"ids":[3,1,2]} : nouvel ordre d'affichage */
    @PutMapping("/concours/{idConcours}/categories/ordre")
    @PreAuthorize("hasAuthority('EVENEMENT_GERER')")
    public ConcoursOrgaDetailDto ordonner(@PathVariable Long idConcours, @Valid @RequestBody OrdreRequest request) {
        return concoursService.ordonnerCategories(org(), idConcours, request);
    }

    @PutMapping("/categories/{idCategorie}")
    @PreAuthorize("hasAuthority('EVENEMENT_GERER')")
    public ConcoursOrgaDetailDto modifierCategorie(@PathVariable Long idCategorie, @Valid @RequestBody CategorieRequest request) {
        return concoursService.modifierCategorie(org(), idCategorie, request);
    }

    @DeleteMapping("/categories/{idCategorie}")
    @PreAuthorize("hasAuthority('EVENEMENT_GERER')")
    public ResponseEntity<Void> supprimerCategorie(@PathVariable Long idCategorie) {
        concoursService.supprimerCategorie(org(), idCategorie);
        return ResponseEntity.noContent().build();
    }

    // ------------------------- Candidats -------------------------

    @PostMapping("/categories/{idCategorie}/candidats")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('CANDIDAT_GERER')")
    public CandidatOrgaDto ajouterCandidat(@PathVariable Long idCategorie, @Valid @RequestBody CandidatRequest request) {
        return concoursService.ajouterCandidat(org(), idCategorie, request);
    }

    @PutMapping("/candidats/{idCandidat}")
    @PreAuthorize("hasAuthority('CANDIDAT_GERER')")
    public CandidatOrgaDto modifierCandidat(@PathVariable Long idCandidat, @Valid @RequestBody CandidatRequest request) {
        return concoursService.modifierCandidat(org(), idCandidat, request);
    }

    /** {"actif":false} */
    @PatchMapping("/candidats/{idCandidat}/actif")
    @PreAuthorize("hasAuthority('CANDIDAT_GERER')")
    public CandidatOrgaDto activerCandidat(@PathVariable Long idCandidat, @Valid @RequestBody ActifRequest request) {
        return concoursService.activerCandidat(org(), idCandidat, request);
    }

    @DeleteMapping("/candidats/{idCandidat}")
    @PreAuthorize("hasAuthority('CANDIDAT_GERER')")
    public ResponseEntity<Void> supprimerCandidat(@PathVariable Long idCandidat) {
        concoursService.supprimerCandidat(org(), idCandidat);
        return ResponseEntity.noContent().build();
    }
}
