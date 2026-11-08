package com.allvote.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.allvote.entities.Candidat;

public interface CandidatRepository extends JpaRepository<Candidat, Long> {

    /** Ajout atomique de voix : jamais de lecture-modification-écriture côté Java. */
    @Modifying
    @Query("update Candidat c set c.votes = c.votes + :nb where c.idCandidat = :id")
    int ajouterVoix(@Param("id") Long id, @Param("nb") long nb);

    /** Candidat + catégorie + concours + événement + organisateur en une requête (création d'un vote). */
    @Query("""
            select x from Candidat x
              join fetch x.categorie k join fetch k.concours c join fetch c.evenement e join fetch e.organisateur
             where x.idCandidat = :id
            """)
    Optional<Candidat> findPourVote(@Param("id") Long id);

    Optional<Candidat> findByIdCandidatAndCategorie_Concours_Evenement_Organisateur_IdOrganisateur(Long idCandidat, Long idOrganisateur);

    @Query("select coalesce(max(c.numero), 0) from Candidat c where c.categorie.idCategorie = :idCategorie")
    int numeroMax(@Param("idCategorie") Long idCategorie);

    boolean existsByCategorie_IdCategorieAndNumeroAndIdCandidatNot(Long idCategorie, int numero, Long idCandidat);

    boolean existsByCategorie_IdCategorieAndNumero(Long idCategorie, int numero);
}
