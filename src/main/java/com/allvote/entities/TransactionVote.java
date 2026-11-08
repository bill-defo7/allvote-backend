package com.allvote.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.allvote.utiles.Operateur;
import com.allvote.utiles.StatutVote;

import jakarta.persistence.*;
import lombok.*;

/**
 * Un paiement de vote. Le taux de commission est FIGÉ au moment du vote :
 * changer le taux plus tard ne modifie pas l'historique.
 */
@Entity
@Table(name = "transactions_vote", indexes = {
        @Index(name = "idx_tv_statut", columnList = "statut, date_creation"),
        @Index(name = "idx_tv_organisateur", columnList = "id_organisateur"),
        @Index(name = "idx_tv_concours", columnList = "id_concours"),
        @Index(name = "idx_tv_pay_token", columnList = "pay_token")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idTransaction;

    /** Notre référence unique, envoyée à Aangaraa comme transaction_id. */
    @Basic(optional = false)
    @Column(unique = true, updatable = false)
    private UUID reference;

    @Basic(optional = false)
    private int nbVoix;

    @Basic(optional = false)
    private int prixUnitaire;

    /**
     * Montant TOTAL payé par le votant (FCFA) = valeur des voix + fraisService.
     * C'est ce montant qui est demandé au prestataire et contrôlé au retour.
     */
    @Basic(optional = false)
    private int montant;

    /**
     * Frais de service payés EN PLUS par le votant (FCFA), revenu AllVote.
     * "default 0" : les transactions antérieures (sans frais) restent valides lors de l'ajout de la colonne.
     */
    @Basic(optional = false)
    @Column(columnDefinition = "integer default 0 not null")
    private int fraisService;

    /** Taux des frais appliqué en % (figé au vote). null pour les transactions antérieures. */
    @Basic
    @Column(precision = 5, scale = 2)
    private BigDecimal tauxFrais;

    /** Taux appliqué en % (ex. 20.00). */
    @Basic(optional = false)
    @Column(precision = 5, scale = 2)
    private BigDecimal tauxCommission;

    /** Part AllVote. */
    @Basic(optional = false)
    private int montantCommission;

    /** Part reversée à l'organisateur = montant - montantCommission. */
    @Basic(optional = false)
    private int montantOrganisateur;

    @Basic(optional = false)
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Operateur operateur;

    @Basic(optional = false)
    @Column(length = 15)
    private String telephone;

    @Basic(optional = false)
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StatutVote statut = StatutVote.EN_ATTENTE;

    @Basic
    @Column(length = 120)
    private String payToken;

    @Basic
    @Column(length = 120)
    private String providerTxnId;

    @Basic
    private String message;

    @Basic(optional = false)
    private Instant dateCreation = Instant.now();

    @Basic(optional = false)
    private Instant dateMaj = Instant.now();

    @Basic
    private Instant dateConfirmation;

    @Basic
    private Instant derniereVerification;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_candidat")
    private Candidat candidat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_concours")
    private Concours concours;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_organisateur")
    private Organisateur organisateur;

    /** Super-admin qui a tranché une transaction A_VERIFIER / EXPIRE (traçabilité). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_resolu_par")
    private Utilisateur resoluPar;

    @Basic
    private Instant dateResolution;

    /** Reversement qui a payé cette transaction à l'organisateur (null = pas encore reversée). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_reversement")
    private Reversement reversement;
}
