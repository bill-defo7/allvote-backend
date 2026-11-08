package com.allvote.services.impl;

import java.time.Duration;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.allvote.dtos.auth.ChangerMotDePasseRequest;
import com.allvote.dtos.auth.LoginRequest;
import com.allvote.dtos.auth.LoginResponse;
import com.allvote.dtos.auth.UtilisateurDto;
import com.allvote.entities.Organisateur;
import com.allvote.entities.Utilisateur;
import com.allvote.exceptions.ApiException;
import com.allvote.mappers.UtilisateurMapper;
import com.allvote.repositories.UtilisateurRepository;
import com.allvote.security.JwtService;
import com.allvote.services.AuthService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private static final int MAX_ECHECS = 5;
    private static final Duration DUREE_VERROU = Duration.ofMinutes(15);
    private static final String IDENTIFIANTS_INCORRECTS = "Email ou mot de passe incorrect.";

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    /** Hash factice : même temps de réponse que l'email existe ou non (empêche de deviner les comptes). */
    private final String hashFactice;

    public AuthServiceImpl(UtilisateurRepository utilisateurRepository, PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.hashFactice = passwordEncoder.encode("allvote-compte-inexistant");
    }

    @Override
    @Transactional(noRollbackFor = ApiException.class)   // le compteur d'échecs doit être enregistré même en erreur
    public LoginResponse login(LoginRequest r) {
        Utilisateur u = utilisateurRepository.findByEmailIgnoreCase(r.email().trim()).orElse(null);
        if (u == null) {
            passwordEncoder.matches(r.password(), hashFactice);
            throw ApiException.unauthorized(IDENTIFIANTS_INCORRECTS);
        }
        Instant now = Instant.now();
        if (u.getVerrouilleJusqua() != null && now.isBefore(u.getVerrouilleJusqua())) {
            long minutes = Math.max(1, Duration.between(now, u.getVerrouilleJusqua()).toMinutes() + 1);
            throw new ApiException(HttpStatus.LOCKED,
                    "Trop d'essais. Compte verrouillé, réessayez dans " + minutes + " minute(s).");
        }
        if (!passwordEncoder.matches(r.password(), u.getMotDePasse())) {
            int echecs = u.getTentativesEchouees() + 1;
            if (echecs >= MAX_ECHECS) {
                u.setTentativesEchouees(0);
                u.setVerrouilleJusqua(now.plus(DUREE_VERROU));
                log.warn("Compte {} verrouillé après {} échecs de connexion", u.getIdUtilisateur(), MAX_ECHECS);
            } else {
                u.setTentativesEchouees(echecs);
            }
            throw ApiException.unauthorized(IDENTIFIANTS_INCORRECTS);
        }
        u.setTentativesEchouees(0);
        u.setVerrouilleJusqua(null);

        // Statuts vérifiés APRÈS le mot de passe : on ne révèle rien à qui ne connaît pas le mot de passe.
        if (!u.isActif()) {
            throw ApiException.forbidden("Ce compte est désactivé.");
        }
        Organisateur o = u.getOrganisateur();
        if (o != null) {
            switch (o.getStatut()) {
                case REFUSE -> throw ApiException.forbidden("Inscription refusée"
                        + (o.getMotifRefus() != null ? " : " + o.getMotifRefus() : "."));
                case SUSPENDU -> throw ApiException.forbidden("Votre organisation est suspendue. Contactez le support AllVote.");
                default -> { /* EN_ATTENTE : connexion permise, l'espace affiche « en attente de validation » */ }
            }
        }
        u.setDerniereConnexion(now);
        JwtService.JetonAcces jeton = jwtService.generer(u);
        return new LoginResponse(jeton.token(), jeton.expiresAt(), UtilisateurMapper.versDto(u));
    }

    @Override
    @Transactional(readOnly = true)
    public UtilisateurDto moi(Long idUtilisateur) {
        return UtilisateurMapper.versDto(charger(idUtilisateur));
    }

    @Override
    @Transactional
    public void changerMotDePasse(Long idUtilisateur, ChangerMotDePasseRequest r) {
        Utilisateur u = charger(idUtilisateur);
        if (!passwordEncoder.matches(r.ancienMotDePasse(), u.getMotDePasse())) {
            throw ApiException.badRequest("Ancien mot de passe incorrect.");
        }
        if (r.ancienMotDePasse().equals(r.nouveauMotDePasse())) {
            throw ApiException.badRequest("Le nouveau mot de passe doit être différent de l'ancien.");
        }
        u.setMotDePasse(passwordEncoder.encode(r.nouveauMotDePasse()));
    }

    private Utilisateur charger(Long id) {
        return utilisateurRepository.findById(id).orElseThrow(() -> ApiException.unauthorized("Compte introuvable."));
    }
}
