package com.allvote.config;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.allvote.entities.*;
import com.allvote.repositories.*;
import com.allvote.utiles.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Données indispensables, créées au démarrage si elles n'existent pas encore :
 *  1. permissions et rôles
 *  2. paramètres de la plateforme (commission 20 %)
 *  3. compte super-admin (depuis ALLVOTE_SUPERADMIN_EMAIL / ALLVOTE_SUPERADMIN_PASSWORD)
 *  4. données de démonstration (si ALLVOTE_DONNEES_DEMO=true et base vide)
 * Ré-exécutable sans risque : rien n'est créé en double.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InitialisationDonnees implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ParametrePlateformeRepository parametreRepository;
    private final OrganisateurRepository organisateurRepository;
    private final EvenementRepository evenementRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties props;

    @Override
    @Transactional
    public void run(String... args) {
        Map<String, Permission> permissions = creerPermissions();
        creerRoles(permissions);
        creerParametres();
        creerSuperAdmin();
        if (props.donneesDemo() && organisateurRepository.count() == 0) {
            creerDonneesDemo();
        }
    }

    // =================================================================
    // 1. Permissions & rôles
    // =================================================================
    private Map<String, Permission> creerPermissions() {
        Map<String, String> liste = new LinkedHashMap<>();
        liste.put(Permissions.EVENEMENT_GERER, "Créer et modifier des événements, concours, catégories");
        liste.put(Permissions.EVENEMENT_PUBLIER, "Publier / dépublier un événement");
        liste.put(Permissions.CANDIDAT_GERER, "Ajouter et modifier des candidats");
        liste.put(Permissions.TRANSACTION_VOIR, "Voir les paiements de vote");
        liste.put(Permissions.FINANCE_VOIR, "Voir les montants, commissions et reversements");
        liste.put(Permissions.MEMBRE_GERER, "Gérer les membres de l'organisation");
        liste.put(Permissions.PROFIL_GERER, "Modifier le profil de l'organisation");
        liste.put(Permissions.ORGANISATEUR_VALIDER, "Valider, refuser, suspendre des organisateurs");
        liste.put(Permissions.REVERSEMENT_GERER, "Enregistrer les reversements aux organisateurs");
        liste.put(Permissions.UTILISATEUR_GERER, "Activer / désactiver des comptes");
        liste.put(Permissions.PLATEFORME_PARAMETRER, "Modifier les paramètres (commission…)");

        Map<String, Permission> res = new LinkedHashMap<>();
        liste.forEach((code, desc) -> res.put(code, permissionRepository.findByCode(code)
                .orElseGet(() -> permissionRepository.save(new Permission(null, code, desc)))));
        return res;
    }

    private void creerRoles(Map<String, Permission> p) {
        creerRole(Roles.SUPER_ADMIN, "Administrateur de la plateforme AllVote", List.copyOf(p.values()));
        creerRole(Roles.ORGA_ADMIN, "Responsable d'une organisation", List.of(
                p.get(Permissions.EVENEMENT_GERER), p.get(Permissions.EVENEMENT_PUBLIER),
                p.get(Permissions.CANDIDAT_GERER), p.get(Permissions.TRANSACTION_VOIR),
                p.get(Permissions.FINANCE_VOIR), p.get(Permissions.MEMBRE_GERER),
                p.get(Permissions.PROFIL_GERER)));
        creerRole(Roles.ORGA_MEMBRE, "Membre d'une organisation (sans accès à l'argent)", List.of(
                p.get(Permissions.EVENEMENT_GERER), p.get(Permissions.CANDIDAT_GERER)));
    }

    private void creerRole(String nom, String description, List<Permission> perms) {
        Role role = roleRepository.findByNomRole(nom).orElseGet(() -> {
            Role r = new Role();
            r.setNomRole(nom);
            r.setDescription(description);
            return r;
        });
        role.getPermissions().clear();
        role.getPermissions().addAll(perms);   // garde les permissions à jour si la liste évolue
        roleRepository.save(role);
    }

    // =================================================================
    // 2. Paramètres
    // =================================================================
    private void creerParametres() {
        if (!parametreRepository.existsById(ParametrePlateforme.TAUX_COMMISSION_DEFAUT)) {
            parametreRepository.save(new ParametrePlateforme(ParametrePlateforme.TAUX_COMMISSION_DEFAUT,
                    "20", "Commission AllVote en % sur chaque vote payé"));
            log.info("Paramètre créé : commission par défaut = 20 %");
        }
        if (!parametreRepository.existsById(ParametrePlateforme.FRAIS_VOTANT)) {
            parametreRepository.save(new ParametrePlateforme(ParametrePlateforme.FRAIS_VOTANT,
                    "5", "Frais de service en % payés en plus par le votant (frais du prestataire de paiement)"));
            log.info("Paramètre créé : frais de service votant = 5 %");
        }
    }

    // =================================================================
    // 3. Super-admin
    // =================================================================
    private void creerSuperAdmin() {
        if (utilisateurRepository.existsByRoles_NomRole(Roles.SUPER_ADMIN)) {
            return;
        }
        String email = props.superAdmin().email();
        String pass = props.superAdmin().password();
        if (email == null || email.isBlank() || pass == null || pass.isBlank()) {
            log.warn("Aucun super-admin : définissez ALLVOTE_SUPERADMIN_EMAIL et ALLVOTE_SUPERADMIN_PASSWORD dans .env puis redémarrez.");
            return;
        }
        if (pass.length() < 12) {
            throw new IllegalStateException("ALLVOTE_SUPERADMIN_PASSWORD doit contenir au moins 12 caractères.");
        }
        Utilisateur u = new Utilisateur();
        u.setNom("AllVote");
        u.setPrenom("Super-admin");
        u.setEmail(email.trim().toLowerCase());
        u.setMotDePasse(passwordEncoder.encode(pass));
        u.getRoles().add(roleRepository.findByNomRole(Roles.SUPER_ADMIN).orElseThrow());
        utilisateurRepository.save(u);
        log.info("Super-admin « {} » créé. Vous pouvez retirer ALLVOTE_SUPERADMIN_PASSWORD du fichier .env.", u.getEmail());
    }

    // =================================================================
    // 4. Démonstration (2 organisateurs, 2 événements, 3 concours)
    // =================================================================
    private void creerDonneesDemo() {
        Instant now = Instant.now();

        // ---- Organisateur 1 : Département GI ----
        Organisateur gi = organisateur("Département de Génie Informatique — ESTLC", "Ambam",
                "Département GI (GSI) de l'École Supérieure de Transport, de Logistique et de Commerce, Université d'Ebolowa.",
                "gi.estlc@allvote.cm");
        Evenement soiree = evenement(gi, "Soirée de Parrainage GI 2026", "Salle Polyvalente Horizon, Ambam",
                "« Héritage numérique : transmettre pour mieux bâtir ». La soirée officielle du département GI.",
                now.plus(Duration.ofDays(20)));

        Concours missMaster = concours(soiree, "Miss & Master GI 2026",
                "Élisez la Miss et le Master qui représenteront le département.", now.minus(Duration.ofDays(3)), now.plus(Duration.ofDays(18)));
        categorie(missMaster, "Miss", 1, TypeCandidat.PERSONNE, new Object[][]{
                {"ORNELLA MBA", "GSI 2", "Passionnée de développement web et de design.", 412},
                {"LARISSA NGO", "GSI 1", "Étudiante engagée, je crois en l'entraide entre aînés et nouveaux.", 268},
                {"CHRISTELLE EYENGA", "GSI 3", "Amoureuse de la data et de l'élégance.", 155}});
        categorie(missMaster, "Master", 2, TypeCandidat.PERSONNE, new Object[][]{
                {"STEVE ABENA", "GSI 3", "Développeur backend et leader associatif.", 367},
                {"YANN MVONDO", "GSI 2", "Fan de cybersécurité et de sport.", 214},
                {"BRICE ESSOMBA", "GSI 3", "Calme et déterminé, je crois à la transmission.", 128}});

        Concours projets = concours(soiree, "Meilleur projet GI",
                "Votez pour le projet étudiant le plus innovant.", now.minus(Duration.ofDays(1)), now.plus(Duration.ofDays(18)));
        categorie(projets, "Meilleur projet", 1, TypeCandidat.PROJET, new Object[][]{
                {"SmartCampus", "GSI 3", "Application de gestion intelligente du campus.", 96},
                {"LogiTrack", "GSI 2", "Suivi en temps réel des livraisons.", 74},
                {"AgriBot", "GSI 3", "Assistant agricole par SMS.", 51}});
        evenementRepository.save(soiree);

        // ---- Organisateur 2 : Club sport ----
        Organisateur sport = organisateur("Club Sport ESTLC", "Ambam",
                "Association sportive des étudiants de l'ESTLC.", "sport.estlc@allvote.cm");
        Evenement tournoi = evenement(sport, "Tournoi inter-filières 2026", "Stade municipal d'Ambam",
                "Le grand tournoi de football entre les filières de l'ESTLC.", now.plus(Duration.ofDays(30)));
        Concours joueurs = concours(tournoi, "Meilleurs joueurs du tournoi",
                "Élisez le meilleur joueur et la meilleure joueuse.", now.plus(Duration.ofDays(5)), now.plus(Duration.ofDays(30)));
        categorie(joueurs, "Meilleur joueur", 1, TypeCandidat.PERSONNE, new Object[][]{
                {"KEVIN TCHOUA", "Équipe GSI", "Attaquant, 7 buts en phase de poules.", 0},
                {"PAUL ONANA", "Équipe TTL", "Milieu infatigable.", 0}});
        categorie(joueurs, "Meilleure joueuse", 2, TypeCandidat.PERSONNE, new Object[][]{
                {"SANDRA BELLA", "Équipe Commerce", "Capitaine et défenseure solide.", 0},
                {"DANIELLE ABO", "Équipe GSI", "Gardienne de but, 3 arrêts décisifs en finale.", 0}});
        evenementRepository.save(tournoi);

        log.info("Données de démonstration créées : 2 organisateurs, 2 événements, 3 concours.");
    }

    private Organisateur organisateur(String nom, String ville, String description, String email) {
        Organisateur o = new Organisateur();
        o.setNom(nom);
        o.setSlug(Slugs.slugifier(nom));
        o.setVille(ville);
        o.setDescription(description);
        o.setEmail(email);
        o.setStatut(StatutOrganisateur.VALIDE);
        o.setDateValidation(Instant.now());
        return organisateurRepository.save(o);
    }

    private Evenement evenement(Organisateur o, String titre, String lieu, String description, Instant debut) {
        Evenement e = new Evenement();
        e.setOrganisateur(o);
        e.setTitre(titre);
        e.setSlug(Slugs.slugifier(titre));
        e.setLieu(lieu);
        e.setDescription(description);
        e.setDateDebut(debut);
        e.setDateFin(debut.plus(Duration.ofHours(6)));
        e.setStatut(StatutEvenement.PUBLIE);
        return e;
    }

    private Concours concours(Evenement e, String titre, String description, Instant ouverture, Instant fermeture) {
        Concours c = new Concours();
        c.setEvenement(e);
        c.setTitre(titre);
        c.setDescription(description);
        c.setPrixVoix(100);
        c.setDateOuverture(ouverture);
        c.setDateFermeture(fermeture);
        e.getConcours().add(c);
        return c;
    }

    /** lignes : {nom, sousTitre, description, votes} */
    private void categorie(Concours c, String nom, int ordre, TypeCandidat type, Object[][] lignes) {
        Categorie k = new Categorie();
        k.setConcours(c);
        k.setNom(nom);
        k.setOrdre(ordre);
        int numero = 1;
        for (Object[] l : lignes) {
            Candidat x = new Candidat();
            x.setCategorie(k);
            x.setNumero(numero++);
            x.setNom((String) l[0]);
            x.setSousTitre((String) l[1]);
            x.setDescription((String) l[2]);
            x.setVotes((Integer) l[3]);
            x.setTypeCandidat(type);
            k.getCandidats().add(x);
        }
        c.getCategories().add(k);
    }
}
