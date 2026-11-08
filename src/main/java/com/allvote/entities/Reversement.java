package com.allvote.entities;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.allvote.utiles.StatutReversement;

import jakarta.persistence.*;
import lombok.*;

/** Argent versé par AllVote à un organisateur (part des votes, commission déduite). */
@Entity
@Table(name = "reversements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Reversement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idReversement;

    @Basic(optional = false)
    private long montant;

    @Basic(optional = false)
    private int nbTransactions;

    @Basic
    private Instant periodeDebut;

    @Basic
    private Instant periodeFin;

    @Basic(optional = false)
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StatutReversement statut = StatutReversement.A_PAYER;

    /** Référence de la transaction Mobile Money du reversement. */
    @Basic
    @Column(length = 120)
    private String referencePaiement;

    @Basic
    private Instant datePaiement;

    @Basic
    private String note;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_organisateur")
    private Organisateur organisateur;

    /** Super-admin qui a effectué le reversement (traçabilité). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur")
    private Utilisateur effectuePar;

    @OneToMany(mappedBy = "reversement")
    private List<TransactionVote> transactions = new ArrayList<>();
}
