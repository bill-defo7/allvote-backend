#!/usr/bin/env bash
# =====================================================================
#  Tests de l'étape 4c-2 (votes, paiement SIMULÉ, finances, partage). Lancer : bash tests/test-4c2.sh
#  Prérequis : ALLVOTE_PAYMENT_MODE=simulated, appli démarrée, compte club.test@gmail.com (étape 4b).
#  Le script valide temporairement "Club Test" dans la base (à la place du super-admin, étape 4d)
#  puis le remet EN_ATTENTE à la fin.
# =====================================================================
API=${API:-http://localhost:8081}
PSQL="docker exec allvote-postgres psql -U ${ALLVOTE_DB_USER:-allvote} -d allvote -qtAc"
ok=0; ko=0
verifier() { if [ "$2" = "$3" ]; then echo "  OK  $1 ($3)"; ok=$((ok+1)); else echo "  KO  $1 : attendu $2, obtenu $3  $4"; ko=$((ko+1)); fi; }
champ() { python3 -c "import sys,json; d=json.load(sys.stdin); print($1)"; }
appel() {
  local m=$1 u=$2 d=$3 tmp; tmp=$(mktemp)
  if [ -n "$d" ]; then CODE=$(curl -s -o "$tmp" -w '%{http_code}' -X "$m" "$API$u" -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d "$d")
  else CODE=$(curl -s -o "$tmp" -w '%{http_code}' -X "$m" "$API$u" -H "Authorization: Bearer $TOKEN"); fi
  CORPS=$(cat "$tmp"); rm -f "$tmp"
}
public() {   # sans jeton
  local m=$1 u=$2 d=$3 tmp; tmp=$(mktemp)
  if [ -n "$d" ]; then CODE=$(curl -s -o "$tmp" -w '%{http_code}' -X "$m" "$API$u" -H 'Content-Type: application/json' -d "$d")
  else CODE=$(curl -s -o "$tmp" -w '%{http_code}' -X "$m" "$API$u"); fi
  CORPS=$(cat "$tmp"); rm -f "$tmp"
}
attendre() {   # attendre REFERENCE -> STATUT final (max ~40 s)
  local s=EN_ATTENTE i
  for i in $(seq 1 20); do
    sleep 2; public GET /api/public/votes/$1; s=$(echo "$CORPS" | champ "d['statut']")
    [ "$s" != "EN_ATTENTE" ] && break
  done; echo "$s"
}
voix() { public GET /api/public/concours/$CO; echo "$CORPS" | champ "[x['votes'] for k in d['categories'] for x in k['candidats'] if x['idCandidat']==$1][0]"; }
date_iso() { date -u -d "$1" +%Y-%m-%dT%H:%M:%SZ; }

echo "== 0. Préparation : Club Test validé (simulation de l'étape 4d), commission par défaut"
$PSQL "update organisateurs set statut='VALIDE', date_validation=now(), taux_commission=null where slug='club-test'"
TOKEN=$(curl -s -X POST "$API/api/auth/login" -H 'Content-Type: application/json' \
  -d '{"email":"club.test@gmail.com","password":"ClubTest2026"}' | champ "d.get('token','')")
[ -n "$TOKEN" ] && verifier "connexion Club Test" 1 1 || { verifier "connexion Club Test" 1 0; exit 1; }

echo "== 1. Correctif booléens"
appel POST /api/orga/evenements "{\"titre\":\"Soirée Test Votes\",\"lieu\":\"Ebolowa\",\"dateDebut\":\"$(date_iso '+10 days')\"}"
verifier "créer l'événement" 201 "$CODE" "$CORPS"; EV=$(echo "$CORPS" | champ "d['idEvenement']"); SLUG=$(echo "$CORPS" | champ "d['slug']")
appel POST /api/orga/evenements/$EV/concours "{\"titre\":\"Vote test\",\"prixVoix\":100,\"dateOuverture\":\"$(date_iso '-1 hour')\",\"dateFermeture\":\"$(date_iso '+5 days')\"}"
verifier "concours sans scoresPublics ni brouillon" 201 "$CODE" "$CORPS"; CO=$(echo "$CORPS" | champ "d['idConcours']")
echo "     scoresPublics=$(echo "$CORPS" | champ "d['scoresPublics']") brouillon=$(echo "$CORPS" | champ "d['brouillon']") statut=$(echo "$CORPS" | champ "d['statut']")"
appel POST /api/orga/evenements/$EV/concours "{\"titre\":\"Dates inversées\",\"prixVoix\":100,\"dateOuverture\":\"$(date_iso '+5 days')\",\"dateFermeture\":\"$(date_iso '+1 day')\"}"
verifier "dates inversées refusées" 400 "$CODE"; echo "     message : $(echo "$CORPS" | champ "d['message']")"
appel PATCH /api/orga/candidats/1/actif '{}'; verifier "actif absent -> 400 clair (ou 404)" 1 "$([ "$CODE" = 400 ] || [ "$CODE" = 404 ] && echo 1 || echo 0)"

appel POST /api/orga/concours/$CO/categories '{"nom":"Meilleur talent"}'; K=$(echo "$CORPS" | champ "d['categories'][0]['idCategorie']")
appel POST /api/orga/categories/$K/candidats '{"nom":"CANDIDAT A"}'; A=$(echo "$CORPS" | champ "d['idCandidat']")
appel POST /api/orga/categories/$K/candidats '{"nom":"CANDIDAT B"}'; B=$(echo "$CORPS" | champ "d['idCandidat']")
appel PATCH /api/orga/evenements/$EV/statut '{"statut":"PUBLIE"}'; verifier "publication (organisation validée)" 200 "$CODE" "$CORPS"

echo "== 2. Votes refusés"
V="\"idConcours\":$CO,\"idCandidat\":$A,\"operateur\":\"MTN\",\"telephone\":\"681507245\""
public POST /api/public/votes "{$V,\"nbVoix\":0}";      verifier "0 voix" 400 "$CODE"
public POST /api/public/votes "{$V,\"nbVoix\":5000}";   verifier "5000 voix (max 1000)" 400 "$CODE"
public POST /api/public/votes "{$V,\"nbVoix\":1,\"website\":\"http://spam\"}"; verifier "robot (honeypot)" 400 "$CODE"
public POST /api/public/votes "{\"idConcours\":$CO,\"idCandidat\":$A,\"nbVoix\":1,\"operateur\":\"WAVE\",\"telephone\":\"681507245\"}"; verifier "opérateur inconnu" 400 "$CODE"
public POST /api/public/votes "{\"idConcours\":$CO,\"idCandidat\":$A,\"nbVoix\":1,\"operateur\":\"MTN\",\"telephone\":\"12345\"}"; verifier "numéro invalide" 400 "$CODE"
public POST /api/public/votes "{\"idConcours\":1,\"idCandidat\":$A,\"nbVoix\":1,\"operateur\":\"MTN\",\"telephone\":\"681507245\"}"; verifier "candidat d'un autre concours" 400 "$CODE"
public POST /api/public/votes '{"idConcours":3,"idCandidat":10,"nbVoix":1,"operateur":"ORANGE","telephone":"691234567"}'; verifier "concours pas encore ouvert (tournoi)" 409 "$CODE"

# L'anti-spam autorise 10 votes / minute / IP : on attend la minute suivante pour la suite.
ATTENTE=$((61 - 10#$(date +%S))); echo "     (pause anti-spam de ${ATTENTE} s…)"; sleep $ATTENTE

echo "== 3. Vote réussi (paiement simulé validé après ~8 s)"
AVANT=$(voix $A)
public POST /api/public/votes "{$V,\"nbVoix\":5}"; verifier "vote 5 voix" 201 "$CODE" "$CORPS"
echo "     montant=$(echo "$CORPS" | champ "d['montant']") FCFA  -> $(echo "$CORPS" | champ "d['message']")"
R1=$(echo "$CORPS" | champ "d['reference']")
verifier "paiement confirmé" CONFIRME "$(attendre $R1)"
verifier "voix créditées (+5)" $((AVANT+5)) "$(voix $A)"
public GET /api/public/votes/$R1; verifier "re-vérification : pas de double crédit" $((AVANT+5)) "$(voix $A)"

echo "== 4. Paiement refusé (numéro finissant par 000)"
public POST /api/public/votes "{\"idConcours\":$CO,\"idCandidat\":$B,\"nbVoix\":3,\"operateur\":\"ORANGE\",\"telephone\":\"690000000\"}"
R2=$(echo "$CORPS" | champ "d['reference']")
verifier "paiement échoué" ECHOUE "$(attendre $R2)"
verifier "aucune voix créditée à B" 0 "$(voix $B)"

echo "== 5. Faux webhook : ne crédite rien"
public POST /api/public/votes "{\"idConcours\":$CO,\"idCandidat\":$B,\"nbVoix\":50,\"operateur\":\"MTN\",\"telephone\":\"680000000\"}"
R3=$(echo "$CORPS" | champ "d['reference']")
public POST /api/payments/aangaraa/webhook "{\"transaction_id\":\"$R3\",\"status\":\"SUCCESSFUL\",\"amount\":5000}"
verifier "webhook accepté (200) mais ignoré" 200 "$CODE"
public GET /api/public/votes/$R3; verifier "toujours EN_ATTENTE" EN_ATTENTE "$(echo "$CORPS" | champ "d['statut']")"
verifier "faux paiement finalement refusé" ECHOUE "$(attendre $R3)"
verifier "B a toujours 0 voix" 0 "$(voix $B)"

echo "== 6. Commission figée : passage à 10 % pour Club Test"
$PSQL "update organisateurs set taux_commission=10 where slug='club-test'"
public POST /api/public/votes "{$V,\"nbVoix\":2}"; R4=$(echo "$CORPS" | champ "d['reference']")
verifier "vote 2 voix confirmé" CONFIRME "$(attendre $R4)"

echo "== 7. Transactions et finances de l'organisateur"
appel GET /api/orga/transactions; verifier "liste des transactions" 200 "$CODE"
verifier "4 transactions" 4 "$(echo "$CORPS" | champ "d['total']")"
echo "$CORPS" | champ "'\n'.join('     %-9s %4s voix %6s F  commission %4s F (%s %%)  tel %s' % (t['statut'], t['nbVoix'], t['montant'], t['montantCommission'], t['tauxCommission'], t['telephone']) for t in d['contenu'])"
appel GET "/api/orga/transactions?statut=CONFIRME"; verifier "filtre CONFIRME" 2 "$(echo "$CORPS" | champ "d['total']")"
appel GET /api/orga/finances; verifier "finances" 200 "$CODE" "$CORPS"
verifier "encaissé (500 + 200)" 700 "$(echo "$CORPS" | champ "d['encaisse']")"
verifier "commission (100 à 20 % + 20 à 10 %)" 120 "$(echo "$CORPS" | champ "d['commission']")"
verifier "part organisateur" 580 "$(echo "$CORPS" | champ "d['partOrganisateur']")"
verifier "reste à reverser" 580 "$(echo "$CORPS" | champ "d['resteAReverser']")"
verifier "taux actuel" 10.0 "$(echo "$CORPS" | champ "d['tauxCommissionActuel']")"
TOKEN_SAUVE=$TOKEN; TOKEN=""
appel GET /api/orga/finances; verifier "finances sans jeton" 401 "$CODE"
TOKEN=$TOKEN_SAUVE

echo "== 8. Liens de partage"
public GET /share/concours/$CO/candidat/$A; verifier "partage candidat" 200 "$CODE"
echo "     $(echo "$CORPS" | grep -o '<meta property="og:title" content="[^"]*"')"
public GET /share/evenements/$SLUG; verifier "partage événement" 200 "$CODE"

echo "== 9. Nettoyage"
appel DELETE /api/orga/evenements/$EV; verifier "suppression refusée (paiements existants)" 409 "$CODE"
appel PATCH /api/orga/evenements/$EV/statut '{"statut":"ARCHIVE"}'; verifier "archivage" 200 "$CODE"
public GET /api/public/evenements/$SLUG; verifier "événement archivé invisible" 404 "$CODE"
$PSQL "update organisateurs set statut='EN_ATTENTE', date_validation=null, taux_commission=null where slug='club-test'"

echo; echo "Résultat : $ok OK, $ko KO"
