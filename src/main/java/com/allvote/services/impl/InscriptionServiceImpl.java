package com.allvote.services.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.allvote.dtos.organisateur.InscriptionOrganisateurRequest;
import com.allvote.dtos.organisateur.InscriptionResponse;
import com.allvote.entities.Organisateur;
import com.allvote.entities.Utilisateur;
import com.allvote.exceptions.ApiException;
import com.allvote.repositories.OrganisateurRepository;
import com.allvote.repositories.RoleRepository;
import com.allvote.repositories.UtilisateurRepository;
import com.allvote.services.InscriptionService;
import com.allvote.utiles.Roles;
import com.allvote.utiles.Slugs;
import com.allvote.utiles.StatutOrganisateur;
import com.allvote.utiles.Telephones;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Une organisation s'inscrit -> statut EN_ATTENTE -> le super-admin la valide (étape 4d). */
@Slf4j
@Service
@RequiredArgsConstructor
public class InscriptionServiceImpl implements InscriptionService {

    private static final String MESSAGE = "Inscription reçue. Votre organisation sera vérifiée par l'équipe AllVote "
            + "avant de pouvoir publier des votes. Vous pouvez déjà vous connecter.";

    private final OrganisateurRepository organisateurRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public InscriptionResponse inscrire(InscriptionOrganisateurRequest r) {
        // Piège à robots rempli : on fait semblant d'accepter, sans rien enregistrer.
        if (r.website() != null && !r.website().isBlank()) {
            log.info("Inscription ignorée (champ anti-robot rempli)");
            return new InscriptionResponse(null, StatutOrganisateur.EN_ATTENTE, MESSAGE);
        }
        String email = r.email().trim().toLowerCase();
        if (utilisateurRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Un compte existe déjà avec cet email. Connectez-vous.");
        }
        String telephone = Telephones.normaliser(r.telephone());

        Organisateur o = new Organisateur();
        o.setNom(r.nomOrganisation().trim());
        o.setSlug(slugUnique(o.getNom()));
        o.setDescription(vide(r.description()));
        o.setVille(vide(r.ville()));
        o.setEmail(r.emailOrganisation() != null && !r.emailOrganisation().isBlank()
                ? r.emailOrganisation().trim().toLowerCase() : email);
        o.setTelephone(telephone);
        o.setStatut(StatutOrganisateur.EN_ATTENTE);
        organisateurRepository.save(o);

        Utilisateur u = new Utilisateur();
        u.setNom(r.nom().trim());
        u.setPrenom(r.prenom().trim());
        u.setEmail(email);
        u.setTelephone(telephone);
        u.setMotDePasse(passwordEncoder.encode(r.motDePasse()));
        u.setOrganisateur(o);
        u.getRoles().add(roleRepository.findByNomRole(Roles.ORGA_ADMIN).orElseThrow());
        utilisateurRepository.save(u);

        log.info("Nouvelle organisation inscrite : {} (id {}) — en attente de validation", o.getSlug(), o.getIdOrganisateur());
        return new InscriptionResponse(o.getSlug(), o.getStatut(), MESSAGE);
    }

    /** "club-sport", puis "club-sport-2", "club-sport-3"… si déjà pris. */
    private String slugUnique(String nom) {
        String base = Slugs.slugifier(nom);
        if (base.isBlank()) {
            base = "organisation";
        }
        String slug = base;
        int i = 2;
        while (organisateurRepository.existsBySlug(slug)) {
            slug = base + "-" + i++;
        }
        return slug;
    }

    private static String vide(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
