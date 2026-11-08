# AllVote — Backend (Spring Boot)

Plateforme de votes multi-événements et multi-organisateurs.
**Spring Boot 4 · Java 21 · PostgreSQL · JWT · Aangaraa Pay**

## Structure
```
com.allvote
 ├─ entities/       Utilisateur, Role, Permission, Organisateur, Evenement, Concours,
 │                  Categorie, Candidat, TransactionVote, Reversement, ParametrePlateforme
 ├─ repositories/   un repository par entité
 ├─ dtos/           objets échangés avec le front (auth/, organisateur/, publique/, commun/)
 ├─ mappers/        entité -> DTO
 ├─ services/       interfaces  (+ services/impl/ : implémentations)
 ├─ controllers/    Auth (/api/auth), Publique (/api/public), Orga* (/api/orga)
 ├─ security/       SecurityConfig, JwtService, UtilisateurConnecte, filtres (compte actif, anti-spam)
 ├─ paiement/      PasserellePaiement (simulée / Aangaraa), ReconciliationPaiements (toutes les 30 s)
 ├─ exceptions/     ApiException, GlobalExceptionHandler
 ├─ utiles/         enums, Roles, Permissions, Slugs, Telephones
 ├─ config/         AppProperties, PasswordConfig, InitialisationDonnees
 └─ AllVoteApplication.java
```
(prochaine étape : adaptation du front React à AllVote)

## Variables d'environnement
Toutes préfixées `ALLVOTE_` (évite les conflits avec d'autres projets dans `~/.bashrc`).
Voir `.env.example`. `ALLVOTE_JWT_SECRET` est obligatoire : `openssl rand -base64 48`.

## Lancer
```bash
cp .env.example .env        # remplir ALLVOTE_DB_PASSWORD, ALLVOTE_JWT_SECRET, ALLVOTE_SUPERADMIN_*
docker compose up -d        # PostgreSQL sur 127.0.0.1:5434
./mvnw spring-boot:run      # API sur le port 8081
```

## Endpoints disponibles (étape 4b)
| Méthode | URL | Accès |
|---|---|---|
| POST | /api/auth/login | public (10/min/IP, verrou 15 min après 5 échecs) |
| GET  | /api/auth/moi | connecté |
| PUT  | /api/auth/mot-de-passe | connecté |
| POST | /api/public/organisateurs/inscription | public (5/min/IP) |
| GET  | /api/public/evenements?q=&ville=&page=&taille= | public |
| GET  | /api/public/evenements/{slug} | public |
| GET  | /api/public/organisateurs/{slug} | public |
| GET  | /api/public/concours/{id} | public |
| GET  | /api/public/concours/{id}/candidats/{idCandidat} | public |

## Espace organisateur (étape 4c-1) — jeton ORGA_ADMIN ou ORGA_MEMBRE
L'organisation vient TOUJOURS du jeton : une ressource d'un autre organisateur répond 404.
| Méthode | URL | Permission |
|---|---|---|
| GET / PUT | /api/orga/profil | PUT : PROFIL_GERER |
| POST | /api/orga/uploads (multipart `fichier`, JPG/PNG ≤ 5 Mo) | connecté |
| GET / POST | /api/orga/evenements | EVENEMENT_GERER |
| GET / PUT / DELETE | /api/orga/evenements/{id} | EVENEMENT_GERER |
| PATCH | /api/orga/evenements/{id}/statut `{"statut":"PUBLIE"}` | EVENEMENT_PUBLIER (+ organisation VALIDE) |
| POST | /api/orga/evenements/{id}/concours | EVENEMENT_GERER |
| GET / PUT / DELETE | /api/orga/concours/{id} | EVENEMENT_GERER |
| PATCH | /api/orga/concours/{id}/statut `{"statutForce":"SUSPENDU"}` | EVENEMENT_PUBLIER |
| POST | /api/orga/concours/{id}/categories | EVENEMENT_GERER |
| PUT | /api/orga/concours/{id}/categories/ordre `{"ids":[..]}` | EVENEMENT_GERER |
| PUT / DELETE | /api/orga/categories/{id} | EVENEMENT_GERER |
| POST | /api/orga/categories/{id}/candidats | CANDIDAT_GERER |
| PUT / DELETE | /api/orga/candidats/{id} | CANDIDAT_GERER |
| PATCH | /api/orga/candidats/{id}/actif `{"actif":false}` | CANDIDAT_GERER |
| GET / POST | /api/orga/membres | MEMBRE_GERER |
| PATCH / DELETE | /api/orga/membres/{id} (…/actif) | MEMBRE_GERER |

Dès qu'un paiement existe, la suppression est refusée (409) : on ferme / désactive à la place.
Tests automatiques : `bash tests/test-4c1.sh`

## Votes et paiement (étape 4c-2)
| Méthode | URL | Accès |
|---|---|---|
| POST | /api/public/votes `{idConcours, idCandidat, nbVoix, operateur: MTN/ORANGE, telephone}` | public (10/min/IP) |
| GET  | /api/public/votes/{reference} | public (le front interroge toutes les 3 s) |
| POST | /api/payments/aangaraa/webhook | Aangaraa (contenu ignoré, re-vérification serveur) |
| GET  | /api/orga/transactions?statut=&idConcours=&page=&taille= | TRANSACTION_VOIR |
| GET  | /api/orga/finances | FINANCE_VOIR |
| GET  | /share/concours/{id}/candidat/{idCandidat} · /share/evenements/{slug} | public (aperçu WhatsApp/Facebook) |

- Le prix est recalculé par le serveur ; la commission (taux de l'organisateur, sinon 20 %) est FIGÉE sur chaque transaction.
- Les voix ne sont créditées qu'une fois (mise à jour conditionnelle atomique) ; montant incohérent -> A_VERIFIER.
- Mode `simulated` : validé après ~8 s ; un numéro finissant par 000 simule un refus.

Tests automatiques : `bash tests/test-4c2.sh`

### Frais de service payés par le votant
- Paramètre `frais_votant_pourcentage` (5 % par défaut, créé au démarrage) — modifiable : `PUT /api/admin/parametres/frais {"taux":5}`.
- Frais = taux × valeur des voix, arrondi au franc supérieur ; **le votant paie valeur + frais** (1 voix à 100 F → 105 F).
- La commission AllVote est calculée sur la valeur des voix ; les frais reviennent à AllVote (frais du prestataire) et
  ne touchent pas la part de l'organisateur. Taux des frais figé sur chaque transaction (`frais_service`, `taux_frais`).
- `GET /api/public/tarifs` : taux affiché par le front avant paiement (le serveur recalcule toujours).
- Tests (mode simulé) : `bash tests/test-frais.sh`

## Super-admin (étape 4d) — rôle SUPER_ADMIN
| Méthode | URL | Rôle |
|---|---|---|
| GET | /api/admin/tableau-de-bord | vue d'ensemble |
| GET | /api/admin/organisateurs?statut= · /{id} | liste / détail |
| POST | /api/admin/organisateurs/{id}/valider · refuser · suspendre · reactiver | `{"motif":"…"}` pour refuser / suspendre |
| PUT | /api/admin/organisateurs/{id}/commission `{"tauxCommission":10}` (null = défaut) | taux propre |
| GET / PATCH | /api/admin/evenements · /{id}/statut (BROUILLON / ARCHIVE) | modération |
| GET | /api/admin/transactions?statut=&idOrganisateur=&idConcours= · /{id} · /{id}/prestataire | suivi |
| POST | /api/admin/transactions/{id}/confirmer · rejeter `{"motif":"…"}` | A_VERIFIER / EXPIRE uniquement, tracé |
| GET | /api/admin/soldes | ce que AllVote doit à chacun |
| POST | /api/admin/organisateurs/{id}/reversements | regroupe les paiements non reversés (A_PAYER) |
| POST / DELETE | /api/admin/reversements/{id}/payer `{"referencePaiement":"…"}` · /api/admin/reversements/{id} | payer / annuler |
| GET | /api/admin/reversements?statut=&idOrganisateur= | historique |
| GET / PATCH / POST | /api/admin/utilisateurs?q= · /{id}/actif · /{id}/deverrouiller | comptes |
| GET / PUT | /api/admin/parametres · /api/admin/parametres/commission `{"taux":20}` | commission par défaut |

Tests automatiques : `bash tests/test-4d.sh` (mot de passe super-admin demandé, saisie masquée)

Erreurs toujours au format `{"status":400,"message":"…","champs":{…}}`.

## Profils
- `dev` (par défaut) : Hibernate crée/modifie les tables depuis les entités (`ddl-auto: update`).
- `prod` : schéma figé par Flyway (`ddl-auto: validate`) — le script sera généré avant la mise en ligne.
