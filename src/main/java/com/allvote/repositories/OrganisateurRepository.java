package com.allvote.repositories;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;

import com.allvote.entities.Organisateur;
import com.allvote.utiles.StatutOrganisateur;

public interface OrganisateurRepository extends JpaRepository<Organisateur, Long> {

    Optional<Organisateur> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Page<Organisateur> findByStatut(StatutOrganisateur statut, Pageable pageable);

    long countByStatut(StatutOrganisateur statut);
}
