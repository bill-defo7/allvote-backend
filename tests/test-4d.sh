#!/usr/bin/env bash
# =====================================================================
#  Tests de l'étape 4d (super-admin). Lancer depuis la racine du projet : bash tests/test-4d.sh
#  Le mot de passe super-admin est demandé (saisie masquée), jamais affiché ni enregistré.
# =====================================================================
API=${API:-http://localhost:8081}
PSQL="docker exec allvote-postgres psql -U allvote -d allvote -qtAc"
SA_EMAIL=$(grep -E '^ALLVOTE_SUPERADMIN_EMAIL=' .env | cut -d= -f2-)
ok=0; ko=0
verifier() { if [ "$2" = "$3" ]; then echo "  OK  $1 ($3)"; ok=$((ok+1)); else echo "  KO  $1 : attendu $2, obtenu $3  $4"; ko=$((ko+1)); fi; }
champ() { python3 -c "import sys,json; d=json.load(sys.stdin); print($1)"; }
req() {   # req JETON METHODE URL [JSON]
  local t=$1 m=$2 u=$3 d=$4 tmp; tmp=$(mktemp); local h=(); [ -n "$t" ] && h=(-H "Authorization: Bearer $t")
  if [ -n "$d" ]; then CODE=$(curl -s -o "$tmp" -w '%{http_code}' -X "$m" "$API$u" "${h[@]}" -H 'Content-Type: application/json' -d "$d")
  else CODE=$(curl -s -o "$tmp" -w '%{http_code}' -X "$m" "$API$u" "${h[@]}"); fi
  CORPS=$(cat "$tmp"); rm -f "$tmp"
}
connexion() { curl -s -X POST "$API/api/auth/login" -H 'Content-Type: application/json' -d "{\"email\":\"$1\",\"password\":\"$2\"}" | champ "d.get('token','')" 2>/dev/null; }
attendre() { local s=EN_ATTENTE i; for i in $(seq 1 20); do sleep 2; req "" GET /api/public/votes/$1; s=$(echo "$CORPS" | champ "d['statut']"); [ "$s" != "EN_ATTENTE" ] && break; done; echo "$s"; }
date_iso() { date -u -d "$1" +%Y-%m-%dT%H:%M:%SZ; }

read -s -p "Mot de passe super-admin ($SA_EMAIL) : " P; echo
SA=$(connexion "$SA_EMAIL" "$P"); unset P
OR=$(connexion club.test@gmail.com ClubTest2026)
echo "== 1. Accès"
verifier "connexion super-admin" 1 "$([ -n "$SA" ] && echo 1 || echo 0)"; [ -z "$SA" ] && exit 1
verifier "connexion Club Test" 1 "$([ -n "$OR" ] && echo 1 || echo 0)"
req "$OR" GET /api/admin/tableau-de-bord; verifier "organisateur interdit dans /api/admin" 403 "$CODE"
req "$SA" GET /api/orga/profil;          verifier "super-admin hors espace organisateur" 403 "$CODE"
req "$SA" GET /api/admin/tableau-de-bord; verifier "tableau de bord" 200 "$CODE" "$CORPS"
echo "     $(echo "$CORPS" | champ "'en attente=%s  encaissé=%s F  commission=%s F  à reverser=%s F  taux=%s %%' % (d['organisateursEnAttente'], d['encaisse'], d['commissionAllVote'], d['resteAReverser'], d['tauxCommissionDefaut'])")"

echo "== 2. Validation des organisateurs"
req "$SA" GET "/api/admin/organisateurs?statut=EN_ATTENTE"; verifier "liste EN_ATTENTE" 200 "$CODE"
ORG=$(echo "$CORPS" | champ "[o['idOrganisateur'] for o in d['contenu'] if o['slug']=='club-test'][0]")
echo "     Club Test : id=$ORG  responsable=$(echo "$CORPS" | champ "[o['responsable']['email'] for o in d['contenu'] if o['slug']=='club-test'][0]")"
req "$SA" POST /api/admin/organisateurs/$ORG/refuser '{}';  verifier "refus sans motif" 400 "$CODE"
req "$SA" POST /api/admin/organisateurs/$ORG/valider;      verifier "valider Club Test" 200 "$CODE" "$CORPS"
req "$SA" POST /api/admin/organisateurs/$ORG/valider;      verifier "valider deux fois" 409 "$CODE"
req "$SA" PUT /api/admin/organisateurs/$ORG/commission '{"tauxCommission":15}'; verifier "commission propre 15 %" 200 "$CODE"
verifier "taux appliqué" 15.0 "$(echo "$CORPS" | champ "float(d['tauxApplique'])")"
req "$SA" PUT /api/admin/organisateurs/$ORG/commission '{"tauxCommission":150}'; verifier "commission 150 % refusée" 400 "$CODE"

echo "== 3. Préparation d'un vote (Club Test publie lui-même, il est validé)"
req "$OR" POST /api/orga/evenements "{\"titre\":\"Soirée Test Admin\",\"dateDebut\":\"$(date_iso '+10 days')\"}"; EV=$(echo "$CORPS" | champ "d['idEvenement']")
req "$OR" POST /api/orga/evenements/$EV/concours "{\"titre\":\"Vote admin\",\"prixVoix\":100,\"dateOuverture\":\"$(date_iso '-1 hour')\",\"dateFermeture\":\"$(date_iso '+5 days')\"}"; CO=$(echo "$CORPS" | champ "d['idConcours']")
req "$OR" POST /api/orga/concours/$CO/categories '{"nom":"Catégorie test"}'; K=$(echo "$CORPS" | champ "d['categories'][0]['idCategorie']")
req "$OR" POST /api/orga/categories/$K/candidats '{"nom":"CANDIDAT ADMIN"}'; X=$(echo "$CORPS" | champ "d['idCandidat']")
req "$OR" PATCH /api/orga/evenements/$EV/statut '{"statut":"PUBLIE"}'; verifier "publication" 200 "$CODE" "$CORPS"
V="{\"idConcours\":$CO,\"idCandidat\":$X,\"operateur\":\"MTN\",\"telephone\":\"681507245\""
req "" POST /api/public/votes "$V,\"nbVoix\":4}"; R1=$(echo "$CORPS" | champ "d['reference']")
verifier "vote 4 voix confirmé" CONFIRME "$(attendre $R1)"

echo "== 4. Transactions litigieuses"
req "" POST /api/public/votes "$V,\"nbVoix\":6}"; R2=$(echo "$CORPS" | champ "d['reference']")
req "" POST /api/public/votes "$V,\"nbVoix\":9}"; R3=$(echo "$CORPS" | champ "d['reference']")
$PSQL "update transactions_vote set statut='A_VERIFIER', message='Montant incohérent (test)' where reference='$R2'"
$PSQL "update transactions_vote set statut='EXPIRE', message='Délai dépassé (test)' where reference='$R3'"
req "$SA" GET "/api/admin/transactions?statut=A_VERIFIER&idOrganisateur=$ORG"; verifier "filtre A_VERIFIER" 200 "$CODE"
T2=$(echo "$CORPS" | champ "[t['idTransaction'] for t in d['contenu'] if t['reference']=='$R2'][0]")
req "$SA" GET "/api/admin/transactions?statut=EXPIRE&idOrganisateur=$ORG"
T3=$(echo "$CORPS" | champ "[t['idTransaction'] for t in d['contenu'] if t['reference']=='$R3'][0]")
sleep 8
req "$SA" GET /api/admin/transactions/$T2/prestataire; verifier "interroger le prestataire" 200 "$CODE"
echo "     prestataire : $(echo "$CORPS" | champ "'%s, payé=%s / attendu=%s' % (d['etat'], d['montantPaye'], d['montantAttendu'])")"
req "$SA" POST /api/admin/transactions/$T2/confirmer '{}'; verifier "confirmer sans motif" 400 "$CODE"
req "$SA" POST /api/admin/transactions/$T2/confirmer '{"motif":"Paiement vérifié sur le relevé Aangaraa"}'
verifier "confirmation manuelle" CONFIRME "$(echo "$CORPS" | champ "d['statut']")"
echo "     tracé : par $(echo "$CORPS" | champ "d['resoluPar']") — $(echo "$CORPS" | champ "d['message']")"
req "$SA" POST /api/admin/transactions/$T2/confirmer '{"motif":"encore"}'; verifier "pas de double confirmation" 409 "$CODE"
req "$SA" POST /api/admin/transactions/$T3/rejeter '{"motif":"Aucun paiement chez le prestataire"}'
verifier "rejet manuel" ECHOUE "$(echo "$CORPS" | champ "d['statut']")"
req "" GET /api/public/concours/$CO
verifier "voix du candidat (4 + 6, pas les 9 rejetées)" 10 "$(echo "$CORPS" | champ "d['categories'][0]['candidats'][0]['votes']")"

echo "== 5. Reversements"
req "$SA" GET /api/admin/soldes; verifier "soldes" 200 "$CODE"
DU=$(echo "$CORPS" | champ "[s['montant'] for s in d if s['idOrganisateur']==$ORG][0]")
echo "     AllVote doit à Club Test : $DU FCFA"
req "$SA" POST /api/admin/organisateurs/$ORG/reversements '{"note":"Test"}'; verifier "créer le reversement" 201 "$CODE" "$CORPS"
RV=$(echo "$CORPS" | champ "d['idReversement']"); verifier "montant = solde" "$DU" "$(echo "$CORPS" | champ "d['montant']")"
req "$SA" POST /api/admin/organisateurs/$ORG/reversements '{}'; verifier "un seul reversement en attente" 409 "$CODE"
req "$OR" GET /api/orga/finances
verifier "côté organisateur : en cours" "$DU" "$(echo "$CORPS" | champ "d['reversementEnCours']")"
verifier "côté organisateur : reste 0" 0 "$(echo "$CORPS" | champ "d['resteAReverser']")"
req "$SA" DELETE /api/admin/reversements/$RV; verifier "annuler le reversement" 204 "$CODE"
req "$OR" GET /api/orga/finances; verifier "transactions libérées" "$DU" "$(echo "$CORPS" | champ "d['resteAReverser']")"
req "$SA" POST /api/admin/organisateurs/$ORG/reversements '{}'; RV=$(echo "$CORPS" | champ "d['idReversement']")
req "$SA" POST /api/admin/reversements/$RV/payer '{}'; verifier "payer sans référence" 400 "$CODE"
req "$SA" POST /api/admin/reversements/$RV/payer '{"referencePaiement":"MP261106.TEST.0001"}'
verifier "marquer payé" PAYE "$(echo "$CORPS" | champ "d['statut']")"
req "$SA" POST /api/admin/reversements/$RV/payer '{"referencePaiement":"X"}'; verifier "payer deux fois" 409 "$CODE"
req "$SA" DELETE /api/admin/reversements/$RV; verifier "annuler un reversement payé" 409 "$CODE"
req "$OR" GET /api/orga/finances; verifier "côté organisateur : déjà reversé" "$DU" "$(echo "$CORPS" | champ "d['dejaReverse']")"
req "$SA" GET "/api/admin/reversements?idOrganisateur=$ORG"; verifier "historique" 200 "$CODE"

echo "== 6. Suspension"
req "$SA" POST /api/admin/organisateurs/$ORG/suspendre '{"motif":"Test de suspension"}'; verifier "suspendre" SUSPENDU "$(echo "$CORPS" | champ "d['statut']")"
req "$OR" GET /api/orga/profil;    verifier "membres coupés immédiatement" 401 "$CODE"
req "" GET /api/public/concours/$CO; verifier "concours invisible du public" 404 "$CODE"
req "" POST /api/public/votes "$V,\"nbVoix\":1}"; verifier "vote impossible" 404 "$CODE"
req "$SA" POST /api/admin/organisateurs/$ORG/reactiver; verifier "réactiver" VALIDE "$(echo "$CORPS" | champ "d['statut']")"
req "$OR" GET /api/orga/profil;    verifier "accès rétabli (même jeton)" 200 "$CODE"

echo "== 7. Modération des événements"
req "$SA" GET "/api/admin/evenements?idOrganisateur=$ORG"; verifier "événements de Club Test" 200 "$CODE"
req "$SA" PATCH /api/admin/evenements/$EV/statut '{"statut":"PUBLIE"}'; verifier "le super-admin ne publie pas" 400 "$CODE"
req "$SA" PATCH /api/admin/evenements/$EV/statut '{"statut":"ARCHIVE"}'; verifier "archiver" 200 "$CODE"

echo "== 8. Comptes utilisateurs"
for i in 1 2 3 4 5; do connexion club.test@gmail.com mauvais-mdp-$i >/dev/null; done
req "" POST /api/auth/login '{"email":"club.test@gmail.com","password":"ClubTest2026"}'; verifier "verrouillé après 5 échecs" 423 "$CODE"
req "$SA" GET "/api/admin/utilisateurs?q=club.test"; verifier "recherche" 200 "$CODE"
U=$(echo "$CORPS" | champ "[u['idUtilisateur'] for u in d['contenu'] if u['email']=='club.test@gmail.com'][0]")
verifier "affiché verrouillé" True "$(echo "$CORPS" | champ "[u['verrouille'] for u in d['contenu'] if u['email']=='club.test@gmail.com'][0]")"
req "$SA" POST /api/admin/utilisateurs/$U/deverrouiller; verifier "déverrouiller" 200 "$CODE"
req "" POST /api/auth/login '{"email":"club.test@gmail.com","password":"ClubTest2026"}'; verifier "connexion à nouveau possible" 200 "$CODE"
req "$SA" GET /api/auth/moi; SAID=$(echo "$CORPS" | champ "d['idUtilisateur']")
req "$SA" PATCH /api/admin/utilisateurs/$SAID/actif '{"actif":false}'; verifier "se désactiver soi-même" 400 "$CODE"

echo "== 9. Paramètres"
req "$SA" GET /api/admin/parametres; verifier "paramètres" 200 "$CODE"
req "$SA" PUT /api/admin/parametres/commission '{"taux":120}'; verifier "taux 120 % refusé" 400 "$CODE"
req "$SA" PUT /api/admin/parametres/commission '{"taux":25}';  verifier "taux par défaut 25 %" 25.00 "$(echo "$CORPS" | champ "d['valeur']")"
req "$SA" PUT /api/admin/parametres/commission '{"taux":20}';  verifier "retour à 20 %" 20.00 "$(echo "$CORPS" | champ "d['valeur']")"

echo "== 10. Remise à zéro de Club Test (pour pouvoir relancer les scripts 4c)"
req "$SA" PUT /api/admin/organisateurs/$ORG/commission '{"tauxCommission":null}'
$PSQL "update organisateurs set statut='EN_ATTENTE', date_validation=null where slug='club-test'"

echo; echo "Résultat : $ok OK, $ko KO"
