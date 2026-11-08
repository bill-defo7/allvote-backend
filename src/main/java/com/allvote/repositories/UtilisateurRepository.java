package com.allvote.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.allvote.entities.Utilisateur;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    Optional<Utilisateur> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByRoles_NomRole(String nomRole);

    List<Utilisateur> findByOrganisateur_IdOrganisateurOrderByDateCreationAsc(Long idOrganisateur);

    Optional<Utilisateur> findByIdUtilisateurAndOrganisateur_IdOrganisateur(Long idUtilisateur, Long idOrganisateur);

    /** Utilisateur + son organisation en une requête (utilisé à chaque appel authentifié). */
    @Query("select u from Utilisateur u left join fetch u.organisateur where u.idUtilisateur = :id")
    Optional<Utilisateur> findAvecOrganisateur(@Param("id") Long id);

    // ------------------------- Super-admin -------------------------

    @EntityGraph(attributePaths = {"organisateur"})
    Page<Utilisateur> findAllBy(Pageable pageable);

    @EntityGraph(attributePaths = {"organisateur"})
    Page<Utilisateur> findByEmailContainingIgnoreCaseOrNomContainingIgnoreCase(String email, String nom, Pageable pageable);
}
