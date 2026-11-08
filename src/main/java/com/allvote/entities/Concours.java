package com.allvote.entities;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.allvote.utiles.StatutConcours;

import jakarta.persistence.*;
import lombok.*;

/** Un vote au sein d'un événement : sa période (compte à rebours), son prix par voix, ses catégories. */
@Entity
@Table(name = "concours")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Concours {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idConcours;

    @Basic(optional = false)
    @Column(length = 150)
    private String titre;

    @Basic
    @Column(length = 2000)
    private String description;

    @Basic
    private String afficheUrl;

    /** Prix d'une voix en FCFA. */
    @Basic(optional = false)
    private int prixVoix;

    @Basic(optional = false)
    private boolean scoresPublics = true;

    @Basic(optional = false)
    private Instant dateOuverture;

    @Basic(optional = false)
    private Instant dateFermeture;

    /** Forçage manuel (OUVERT / SUSPENDU / FERME). null = automatique selon les dates. */
    @Basic
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StatutConcours statutForce;

    @Basic(optional = false)
    private boolean brouillon;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_evenement")
    private Evenement evenement;

    @OneToMany(mappedBy = "concours", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordre ASC, idCategorie ASC")
    private List<Categorie> categories = new ArrayList<>();

    /** Statut réel à l'instant T (le serveur fait foi, pas le navigateur). */
    public StatutConcours statutEffectif(Instant now) {
        if (brouillon) {
            return StatutConcours.BROUILLON;
        }
        if (statutForce != null) {
            return statutForce;
        }
        if (now.isBefore(dateOuverture)) {
            return StatutConcours.PLANIFIE;
        }
        if (now.isBefore(dateFermeture)) {
            return StatutConcours.OUVERT;
        }
        return StatutConcours.FERME;
    }

    /** Voix visibles si autorisé, ou toujours une fois le concours clôturé. */
    public boolean votesVisibles(Instant now) {
        return scoresPublics || statutEffectif(now) == StatutConcours.FERME;
    }
}
