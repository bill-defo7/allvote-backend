package com.allvote.services.impl;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.allvote.dtos.orga.ActifRequest;
import com.allvote.dtos.orga.MembreDto;
import com.allvote.dtos.orga.MembreRequest;
import com.allvote.entities.Role;
import com.allvote.entities.Utilisateur;
import com.allvote.exceptions.ApiException;
import com.allvote.mappers.OrgaMapper;
import com.allvote.repositories.RoleRepository;
import com.allvote.repositories.UtilisateurRepository;
import com.allvote.services.OrgaMembreService;
import com.allvote.utiles.Roles;
import com.allvote.utiles.Telephones;

import lombok.RequiredArgsConstructor;

/** Le responsable (ORGA_ADMIN) gère ses membres (ORGA_MEMBRE). Il ne peut ni se désactiver ni se supprimer. */
@Service
@RequiredArgsConstructor
@Transactional
public class OrgaMembreServiceImpl implements OrgaMembreService {

    private final AccesOrganisateur acces;
    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<MembreDto> lister(Long idOrganisateur) {
        return utilisateurRepository.findByOrganisateur_IdOrganisateurOrderByDateCreationAsc(idOrganisateur)
                .stream().map(OrgaMapper::membre).toList();
    }

    @Override
    public MembreDto ajouter(Long idOrganisateur, MembreRequest r) {
        String email = r.email().trim().toLowerCase();
        if (utilisateurRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Un compte existe déjà avec cet email.");
        }
        Utilisateur u = new Utilisateur();
        u.setNom(r.nom().trim());
        u.setPrenom(r.prenom().trim());
        u.setEmail(email);
        u.setTelephone(Telephones.normaliser(r.telephone()));
        u.setMotDePasse(passwordEncoder.encode(r.motDePasseProvisoire()));
        u.setOrganisateur(acces.organisateur(idOrganisateur));
        u.getRoles().add(roleRepository.findByNomRole(Roles.ORGA_MEMBRE).orElseThrow());
        return OrgaMapper.membre(utilisateurRepository.save(u));
    }

    @Override
    public MembreDto activer(Long idOrganisateur, Long idConnecte, Long idMembre, ActifRequest r) {
        Utilisateur u = membreModifiable(idOrganisateur, idConnecte, idMembre);
        u.setActif(r.actif());
        return OrgaMapper.membre(u);
    }

    @Override
    public void supprimer(Long idOrganisateur, Long idConnecte, Long idMembre) {
        utilisateurRepository.delete(membreModifiable(idOrganisateur, idConnecte, idMembre));
    }

    private Utilisateur membreModifiable(Long idOrganisateur, Long idConnecte, Long idMembre) {
        if (idMembre.equals(idConnecte)) {
            throw ApiException.badRequest("Vous ne pouvez pas modifier votre propre compte ici.");
        }
        Utilisateur u = utilisateurRepository.findByIdUtilisateurAndOrganisateur_IdOrganisateur(idMembre, idOrganisateur)
                .orElseThrow(() -> ApiException.notFound("Membre introuvable."));
        boolean estResponsable = u.getRoles().stream().map(Role::getNomRole).anyMatch(Roles.ORGA_ADMIN::equals);
        if (estResponsable) {
            throw ApiException.forbidden("Un responsable ne peut être modifié que par le support AllVote.");
        }
        return u;
    }
}
