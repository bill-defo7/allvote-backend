package com.allvote.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.allvote.entities.Evenement;
import com.allvote.entities.Organisateur;
import com.allvote.utiles.StatutEvenement;
import com.allvote.utiles.StatutOrganisateur;

public interface EvenementRepository extends JpaRepository<Evenement, Long> {

    Optional<Evenement> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Evenement> findByOrganisateurAndStatutOrderByDateDebutDesc(Organisateur organisateur, StatutEvenement statut);

    /** Espace organisateur : SES événements uniquement. */
    List<Evenement> findByOrganisateur_IdOrganisateurOrderByDateCreationDesc(Long idOrganisateur);

    Optional<Evenement> findByIdEvenementAndOrganisateur_IdOrganisateur(Long idEvenement, Long idOrganisateur);

    /**
     * Événements visibles du public, avec recherche.
     * motif et ville sont toujours fournis sous la forme "%texte%" ("%%" = pas de filtre).
     */
    @Query(value = """
            select e from Evenement e join fetch e.organisateur o
            where e.statut = :statutEvenement and o.statut = :statutOrganisateur
              and (lower(e.titre) like :motif or lower(o.nom) like :motif)
              and (lower(coalesce(e.lieu, '')) like :ville or lower(coalesce(o.ville, '')) like :ville)
            """,
            countQuery = """
            select count(e) from Evenement e join e.organisateur o
            where e.statut = :statutEvenement and o.statut = :statutOrganisateur
              and (lower(e.titre) like :motif or lower(o.nom) like :motif)
              and (lower(coalesce(e.lieu, '')) like :ville or lower(coalesce(o.ville, '')) like :ville)
            """)
    Page<Evenement> rechercherPublics(@Param("statutEvenement") StatutEvenement statutEvenement,
            @Param("statutOrganisateur") StatutOrganisateur statutOrganisateur,
            @Param("motif") String motif,
            @Param("ville") String ville,
            Pageable pageable);

    // ------------------------- Super-admin -------------------------

    @EntityGraph(attributePaths = {"organisateur"})
    Page<Evenement> findAllBy(Pageable pageable);

    @EntityGraph(attributePaths = {"organisateur"})
    Page<Evenement> findByStatut(StatutEvenement statut, Pageable pageable);

    @EntityGraph(attributePaths = {"organisateur"})
    Page<Evenement> findByOrganisateur_IdOrganisateur(Long idOrganisateur, Pageable pageable);

    @EntityGraph(attributePaths = {"organisateur"})
    Page<Evenement> findByOrganisateur_IdOrganisateurAndStatut(Long idOrganisateur, StatutEvenement statut, Pageable pageable);

    long countByStatut(StatutEvenement statut);
}
