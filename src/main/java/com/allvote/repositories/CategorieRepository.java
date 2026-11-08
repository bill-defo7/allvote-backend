package com.allvote.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.allvote.entities.Categorie;

public interface CategorieRepository extends JpaRepository<Categorie, Long> {

    Optional<Categorie> findByIdCategorieAndConcours_Evenement_Organisateur_IdOrganisateur(Long idCategorie, Long idOrganisateur);
}
