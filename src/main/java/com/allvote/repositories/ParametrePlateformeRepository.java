package com.allvote.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.allvote.entities.ParametrePlateforme;

/** Clé primaire = la clé texte du paramètre (ex. "taux_commission_defaut"). */
public interface ParametrePlateformeRepository extends JpaRepository<ParametrePlateforme, String> {
}
