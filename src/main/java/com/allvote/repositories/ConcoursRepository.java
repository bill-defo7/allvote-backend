package com.allvote.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.allvote.entities.Concours;

public interface ConcoursRepository extends JpaRepository<Concours, Long> {

    /** Concours seulement s'il appartient à cet organisateur (sinon vide -> 404). */
    Optional<Concours> findByIdConcoursAndEvenement_Organisateur_IdOrganisateur(Long idConcours, Long idOrganisateur);
}
