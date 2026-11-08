package com.allvote.services.impl;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.allvote.dtos.admin.CommissionRequest;
import com.allvote.dtos.admin.MotifRequest;
import com.allvote.dtos.admin.OrganisateurAdminDto;
import com.allvote.dtos.commun.PageDto;
import com.allvote.entities.Organisateur;
import com.allvote.exceptions.ApiException;
import com.allvote.mappers.AdminMapper;
import com.allvote.repositories.OrganisateurRepository;
import com.allvote.services.AdminOrganisateurService;
import com.allvote.services.ParametreService;
import com.allvote.utiles.StatutOrganisateur;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Effet immédiat d'une suspension / d'un refus : les membres sont déconnectés (CompteActifFilter),
 * les événements disparaissent du public et les votes sont bloqués.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminOrganisateurServiceImpl implements AdminOrganisateurService {

    private final OrganisateurRepository organisateurRepository;
    private final ParametreService parametreService;

    @Override
    @Transactional(readOnly = true)
    public PageDto<OrganisateurAdminDto> lister(StatutOrganisateur statut, int page, int taille) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(taille, 1), 100),
                Sort.by(Sort.Direction.DESC, "dateCreation"));
        Page<Organisateur> p = statut != null ? organisateurRepository.findByStatut(statut, pr) : organisateurRepository.findAll(pr);
        var defaut = parametreService.tauxCommissionDefaut();
        return PageDto.de(p, p.getContent().stream().map(o -> AdminMapper.organisateur(o, defaut)).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public OrganisateurAdminDto detail(Long id) {
        return vers(charger(id));
    }

    @Override
    public OrganisateurAdminDto valider(Long id) {
        Organisateur o = charger(id);
        if (o.getStatut() != StatutOrganisateur.EN_ATTENTE && o.getStatut() != StatutOrganisateur.REFUSE) {
            throw ApiException.conflict("Seule une organisation en attente ou refusée peut être validée (statut actuel : "
                    + o.getStatut() + ").");
        }
        o.setStatut(StatutOrganisateur.VALIDE);
        o.setDateValidation(Instant.now());
        o.setMotifRefus(null);
        log.info("Organisateur {} validé", o.getSlug());
        return vers(o);
    }

    @Override
    public OrganisateurAdminDto refuser(Long id, MotifRequest r) {
        Organisateur o = charger(id);
        if (o.getStatut() != StatutOrganisateur.EN_ATTENTE) {
            throw ApiException.conflict("Seule une organisation en attente peut être refusée (utilisez « suspendre »).");
        }
        o.setStatut(StatutOrganisateur.REFUSE);
        o.setMotifRefus(r.motif().trim());
        log.info("Organisateur {} refusé", o.getSlug());
        return vers(o);
    }

    @Override
    public OrganisateurAdminDto suspendre(Long id, MotifRequest r) {
        Organisateur o = charger(id);
        if (o.getStatut() != StatutOrganisateur.VALIDE) {
            throw ApiException.conflict("Seule une organisation validée peut être suspendue.");
        }
        o.setStatut(StatutOrganisateur.SUSPENDU);
        o.setMotifRefus(r.motif().trim());       // motif du dernier refus / de la dernière suspension
        log.warn("Organisateur {} SUSPENDU : {}", o.getSlug(), o.getMotifRefus());
        return vers(o);
    }

    @Override
    public OrganisateurAdminDto reactiver(Long id) {
        Organisateur o = charger(id);
        if (o.getStatut() != StatutOrganisateur.SUSPENDU) {
            throw ApiException.conflict("Seule une organisation suspendue peut être réactivée.");
        }
        o.setStatut(StatutOrganisateur.VALIDE);
        o.setMotifRefus(null);
        log.info("Organisateur {} réactivé", o.getSlug());
        return vers(o);
    }

    @Override
    public OrganisateurAdminDto modifierCommission(Long id, CommissionRequest r) {
        Organisateur o = charger(id);
        o.setTauxCommission(r.tauxCommission());   // null = taux par défaut ; les transactions passées gardent leur taux
        log.info("Commission de {} : {}", o.getSlug(), r.tauxCommission() == null ? "taux par défaut" : r.tauxCommission() + " %");
        return vers(o);
    }

    private Organisateur charger(Long id) {
        return organisateurRepository.findById(id).orElseThrow(() -> ApiException.notFound("Organisation introuvable."));
    }

    private OrganisateurAdminDto vers(Organisateur o) {
        return AdminMapper.organisateur(o, parametreService.tauxCommissionDefaut());
    }
}
