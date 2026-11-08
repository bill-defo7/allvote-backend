package com.allvote.services.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.allvote.dtos.orga.ProfilOrganisateurDto;
import com.allvote.dtos.orga.ProfilOrganisateurRequest;
import com.allvote.entities.Organisateur;
import com.allvote.mappers.OrgaMapper;
import com.allvote.services.OrgaProfilService;
import com.allvote.services.ParametreService;
import com.allvote.utiles.Telephones;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrgaProfilServiceImpl implements OrgaProfilService {

    private final AccesOrganisateur acces;
    private final ParametreService parametreService;

    @Override
    @Transactional(readOnly = true)
    public ProfilOrganisateurDto profil(Long idOrganisateur) {
        return vers(acces.organisateur(idOrganisateur));
    }

    @Override
    @Transactional
    public ProfilOrganisateurDto modifier(Long idOrganisateur, ProfilOrganisateurRequest r) {
        Organisateur o = acces.organisateur(idOrganisateur);
        o.setNom(r.nom().trim());
        o.setDescription(vide(r.description()));
        o.setLogoUrl(vide(r.logoUrl()));
        o.setEmail(r.email().trim().toLowerCase());
        o.setTelephone(Telephones.normaliser(r.telephone()));
        o.setVille(vide(r.ville()));
        o.setNumeroReversement(Telephones.normaliser(r.numeroReversement()));
        o.setOperateurReversement(r.operateurReversement());
        return vers(o);
    }

    private ProfilOrganisateurDto vers(Organisateur o) {
        return OrgaMapper.profil(o, o.getTauxCommission() != null ? o.getTauxCommission()
                : parametreService.tauxCommissionDefaut());
    }

    static String vide(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
