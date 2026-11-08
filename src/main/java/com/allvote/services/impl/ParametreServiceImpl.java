package com.allvote.services.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.allvote.dtos.admin.ParametreDto;
import com.allvote.entities.ParametrePlateforme;
import com.allvote.repositories.ParametrePlateformeRepository;
import com.allvote.services.ParametreService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParametreServiceImpl implements ParametreService {

    private static final BigDecimal COMMISSION_DEFAUT = new BigDecimal("20.00");
    private static final BigDecimal FRAIS_DEFAUT = new BigDecimal("5.00");

    private final ParametrePlateformeRepository parametreRepository;

    @Override
    @Transactional(readOnly = true)
    public BigDecimal tauxCommissionDefaut() {
        return lireTaux(ParametrePlateforme.TAUX_COMMISSION_DEFAUT, COMMISSION_DEFAUT);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal fraisVotantPourcentage() {
        return lireTaux(ParametrePlateforme.FRAIS_VOTANT, FRAIS_DEFAUT);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParametreDto> lister() {
        return parametreRepository.findAll().stream()
                .map(p -> new ParametreDto(p.getCle(), p.getValeur(), p.getDescription())).toList();
    }

    @Override
    @Transactional
    public ParametreDto modifierTauxDefaut(BigDecimal taux) {
        return ecrireTaux(ParametrePlateforme.TAUX_COMMISSION_DEFAUT, taux,
                "Commission AllVote en % sur chaque vote payé", "Commission par défaut");
    }

    @Override
    @Transactional
    public ParametreDto modifierFraisVotant(BigDecimal taux) {
        return ecrireTaux(ParametrePlateforme.FRAIS_VOTANT, taux,
                "Frais de service en % payés en plus par le votant (frais du prestataire de paiement)", "Frais de service votant");
    }

    // ------------------------------------------------------------------

    private BigDecimal lireTaux(String cle, BigDecimal defaut) {
        return parametreRepository.findById(cle)
                .map(p -> {
                    try {
                        return new BigDecimal(p.getValeur().trim());
                    } catch (NumberFormatException | NullPointerException e) {
                        log.error("Paramètre {} invalide : '{}' -> {} % utilisé", cle, p.getValeur(), defaut);
                        return defaut;
                    }
                })
                .orElse(defaut);
    }

    private ParametreDto ecrireTaux(String cle, BigDecimal taux, String description, String libelle) {
        ParametrePlateforme p = parametreRepository.findById(cle)
                .orElseGet(() -> new ParametrePlateforme(cle, null, description));
        p.setValeur(taux.setScale(2, RoundingMode.HALF_UP).toPlainString());
        parametreRepository.save(p);
        log.info("{} modifié(e) : {} %", libelle, p.getValeur());
        return new ParametreDto(p.getCle(), p.getValeur(), p.getDescription());
    }
}
