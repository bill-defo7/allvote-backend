package com.allvote.repositories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.allvote.entities.Reversement;
import com.allvote.entities.TransactionVote;
import com.allvote.entities.Utilisateur;
import com.allvote.utiles.StatutVote;

public interface TransactionVoteRepository extends JpaRepository<TransactionVote, Long>,
        JpaSpecificationExecutor<TransactionVote> {

    @EntityGraph(attributePaths = {"candidat"})
    Optional<TransactionVote> findByReference(UUID reference);

    /** Une transaction existe -> on ne supprime plus (traçabilité de l'argent). */
    boolean existsByConcours_IdConcours(Long idConcours);

    boolean existsByConcours_Evenement_IdEvenement(Long idEvenement);

    boolean existsByCandidat_Categorie_IdCategorie(Long idCategorie);

    boolean existsByCandidat_IdCandidat(Long idCandidat);

    // ------------------------- Cycle de vie d'un paiement -------------------------

    /**
     * Transition d'état conditionnelle et atomique : ne s'applique QUE si la transaction est
     * encore dans l'état attendu. Garantit qu'un paiement n'est crédité qu'UNE fois, même si
     * webhook, tâche planifiée et front vérifient en même temps.
     */
    @Modifying
    @Query("""
            update TransactionVote t
               set t.statut = :nouveau, t.message = :message, t.dateMaj = :now, t.dateConfirmation = :dateConfirmation
             where t.idTransaction = :id and t.statut = :attendu
            """)
    int changerStatut(@Param("id") Long id, @Param("attendu") StatutVote attendu, @Param("nouveau") StatutVote nouveau,
            @Param("message") String message, @Param("now") Instant now,
            @Param("dateConfirmation") Instant dateConfirmation);

    @Modifying
    @Query("update TransactionVote t set t.payToken = :payToken, t.providerTxnId = :providerTxnId, t.dateMaj = :now where t.idTransaction = :id")
    int enregistrerPayToken(@Param("id") Long id, @Param("payToken") String payToken,
            @Param("providerTxnId") String providerTxnId, @Param("now") Instant now);

    @Modifying
    @Query("update TransactionVote t set t.derniereVerification = :now where t.idTransaction = :id")
    int marquerVerifie(@Param("id") Long id, @Param("now") Instant now);

    List<TransactionVote> findTop100ByStatutOrderByDateCreationAsc(StatutVote statut);

    // ------------------------- Espace organisateur -------------------------

    @EntityGraph(attributePaths = {"candidat", "concours"})
    Page<TransactionVote> findByOrganisateur_IdOrganisateur(Long idOrganisateur, Pageable pageable);

    @EntityGraph(attributePaths = {"candidat", "concours"})
    Page<TransactionVote> findByOrganisateur_IdOrganisateurAndStatut(Long idOrganisateur, StatutVote statut, Pageable pageable);

    @EntityGraph(attributePaths = {"candidat", "concours"})
    Page<TransactionVote> findByOrganisateur_IdOrganisateurAndConcours_IdConcours(Long idOrganisateur, Long idConcours, Pageable pageable);

    @EntityGraph(attributePaths = {"candidat", "concours"})
    Page<TransactionVote> findByOrganisateur_IdOrganisateurAndStatutAndConcours_IdConcours(Long idOrganisateur,
            StatutVote statut, Long idConcours, Pageable pageable);

    /**
     * Par concours : [idConcours, titre, nbTransactions, voix, valeur des voix, commission, part organisateur].
     * "valeur des voix" = montant payé - frais de service (les frais reviennent à AllVote, hors partage).
     */
    @Query("""
            select c.idConcours, c.titre, count(t), sum(t.nbVoix), sum(t.montant - t.fraisService), sum(t.montantCommission), sum(t.montantOrganisateur)
              from TransactionVote t join t.concours c
             where t.organisateur.idOrganisateur = :idOrganisateur and t.statut = :statut
             group by c.idConcours, c.titre
             order by c.idConcours
            """)
    List<Object[]> totauxParConcours(@Param("idOrganisateur") Long idOrganisateur, @Param("statut") StatutVote statut);

    /** Part organisateur confirmée mais pas encore incluse dans un reversement. */
    @Query("""
            select coalesce(sum(t.montantOrganisateur), 0) from TransactionVote t
             where t.organisateur.idOrganisateur = :idOrganisateur and t.statut = :statut and t.reversement is null
            """)
    long nonReverse(@Param("idOrganisateur") Long idOrganisateur, @Param("statut") StatutVote statut);

    long countByOrganisateur_IdOrganisateurAndStatut(Long idOrganisateur, StatutVote statut);

    // ------------------------- Super-admin -------------------------

    @EntityGraph(attributePaths = {"candidat"})
    Optional<TransactionVote> findWithCandidatByIdTransaction(Long idTransaction);

    /** Décision manuelle du super-admin, conditionnelle (ne s'applique que si le statut n'a pas changé). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update TransactionVote t
               set t.statut = :nouveau, t.message = :message, t.dateMaj = :now, t.dateConfirmation = :dateConfirmation,
                   t.resoluPar = :par, t.dateResolution = :now
             where t.idTransaction = :id and t.statut = :attendu
            """)
    int resoudre(@Param("id") Long id, @Param("attendu") StatutVote attendu, @Param("nouveau") StatutVote nouveau,
            @Param("message") String message, @Param("now") Instant now,
            @Param("dateConfirmation") Instant dateConfirmation, @Param("par") Utilisateur par);

    long countByStatut(StatutVote statut);

    /** [nombre, encaissé total, commission, part organisateurs, frais de service] pour un statut. */
    @Query("""
            select count(t), coalesce(sum(t.montant), 0), coalesce(sum(t.montantCommission), 0), coalesce(sum(t.montantOrganisateur), 0),
                   coalesce(sum(t.fraisService), 0)
              from TransactionVote t where t.statut = :statut
            """)
    List<Object[]> totaux(@Param("statut") StatutVote statut);

    @Query("select coalesce(sum(t.montantOrganisateur), 0) from TransactionVote t where t.statut = :statut and t.reversement is null")
    long nonReverseGlobal(@Param("statut") StatutVote statut);

    /** Soldes à reverser : [idOrganisateur, nom, slug, numeroReversement, operateurReversement, nbTransactions, montant] */
    @Query("""
            select o.idOrganisateur, o.nom, o.slug, o.numeroReversement, o.operateurReversement, count(t), sum(t.montantOrganisateur)
              from TransactionVote t join t.organisateur o
             where t.statut = :statut and t.reversement is null
             group by o.idOrganisateur, o.nom, o.slug, o.numeroReversement, o.operateurReversement
             order by sum(t.montantOrganisateur) desc
            """)
    List<Object[]> soldes(@Param("statut") StatutVote statut);

    /** Rattache d'un coup toutes les transactions confirmées non reversées à un reversement. */
    @Modifying
    @Query("""
            update TransactionVote t set t.reversement = :reversement
             where t.organisateur.idOrganisateur = :idOrganisateur and t.statut = :statut and t.reversement is null
            """)
    int attacher(@Param("reversement") Reversement reversement, @Param("idOrganisateur") Long idOrganisateur,
            @Param("statut") StatutVote statut);

    /** [nombre, montant, première confirmation, dernière confirmation] d'un reversement. */
    @Query("""
            select count(t), coalesce(sum(t.montantOrganisateur), 0), min(t.dateConfirmation), max(t.dateConfirmation)
              from TransactionVote t where t.reversement = :reversement
            """)
    List<Object[]> totauxReversement(@Param("reversement") Reversement reversement);

    @Modifying
    @Query("update TransactionVote t set t.reversement = null where t.reversement = :reversement")
    int detacher(@Param("reversement") Reversement reversement);
}
