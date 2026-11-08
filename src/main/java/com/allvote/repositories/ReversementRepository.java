package com.allvote.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.allvote.entities.Reversement;
import com.allvote.utiles.StatutReversement;

public interface ReversementRepository extends JpaRepository<Reversement, Long> {

    @Query("select coalesce(sum(r.montant), 0) from Reversement r where r.organisateur.idOrganisateur = :id and r.statut = :statut")
    long somme(@Param("id") Long idOrganisateur, @Param("statut") StatutReversement statut);

    @Query("select coalesce(sum(r.montant), 0) from Reversement r where r.statut = :statut")
    long sommeGlobale(@Param("statut") StatutReversement statut);

    boolean existsByOrganisateur_IdOrganisateurAndStatut(Long idOrganisateur, StatutReversement statut);

    @EntityGraph(attributePaths = {"organisateur", "effectuePar"})
    Page<Reversement> findAllBy(Pageable pageable);

    @EntityGraph(attributePaths = {"organisateur", "effectuePar"})
    Page<Reversement> findByStatut(StatutReversement statut, Pageable pageable);

    @EntityGraph(attributePaths = {"organisateur", "effectuePar"})
    Page<Reversement> findByOrganisateur_IdOrganisateur(Long idOrganisateur, Pageable pageable);

    @EntityGraph(attributePaths = {"organisateur", "effectuePar"})
    Page<Reversement> findByOrganisateur_IdOrganisateurAndStatut(Long idOrganisateur, StatutReversement statut, Pageable pageable);
}
