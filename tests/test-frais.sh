#!/usr/bin/env bash
# =====================================================================
#  Frais de service payés par le votant (5 % par défaut). MODE SIMULÉ OBLIGATOIRE (aucun argent réel).
#  Lancer depuis la racine du projet : bash tests/test-frais.sh
# =====================================================================
API=${API:-http://localhost:8081}
PSQL="docker exec allvote-postgres psql -U allvote -d allvote -qtAc"
SA_EMAIL=$(grep -E '^ALLVOTE_SUPERADMIN_EMAIL=' .env | cut -d= -f2-)
MODE=$(grep -E '^ALLVOTE_PAYMENT_MODE=' .env | cut -d= -f2-)
if [ "$MODE" != "simulated" ]; then echo "⛔ ALLVOTE_PAYMENT_MODE=$MODE : ce test doit tourner en mode simulated (pas d'argent réel)."; exit 1; fi
ok=0; ko=0
verifier() { if [ "$2" = "$3" ]; then echo "  OK  $1 ($3)"; ok=$((ok+1)); else echo "  KO  $1 : attendu $2, obtenu $3  $4"; ko=$((ko+1)); fi; }
champ() { python3 -c "import sys,json; d=json.load(sys.stdin); print($1)"; }
req() { local t=$1 m=$2 u=$3 d=$4 tmp; tmp=$(mktemp); local h=(); [ -n "$t" ] && h=(-H "Authorization: Bearer $t")
  if [ -n "$d" ]; then CODE=$(curl -s -o "$tmp" -w '%{http_code}' -X "$m" "$API$u" "${h[@]}" -H 'Content-Type: application/json' -d "$d")
  else CODE=$(curl -s -o "$tmp" -w '%{http_code}' -X "$m" "$API$u" "${h[@]}"); fi; CORPS=$(cat "$tmp"); rm -f "$tmp"; }
connexion() { curl -s -X POST "$API/api/auth/login" -H 'Content-Type: application/json' -d "{\"email\":\"$1\",\"password\":\"$2\"}" | champ "d.get('token','')" 2>/dev/null; }
attendre() { local s=EN_ATTENTE i; for i in $(seq 1 20); do sleep 2; req "" GET /api/public/votes/$1; s=$(echo "$CORPS" | champ "d['statut']"); [ "$s" != "EN_ATTENTE" ] && break; done; echo "$s"; }
date_iso() { date -u -d "$1" +%Y-%m-%dT%H:%M:%SZ; }

read -s -p "Mot de passe super-admin ($SA_EMAIL) : " P; echo
SA=$(connexion "$SA_EMAIL" "$P"); unset P
OR=$(connexion club.test@gmail.com ClubTest2026)
[ -z "$SA" ] && { echo "Connexion super-admin impossible"; exit 1; }
[ -z "$OR" ] && { echo "Connexion Club Test impossible (mot de passe changé ?)"; exit 1; }

echo "== 1. Paramètre et tarif public"
req "" GET /api/public/tarifs; verifier "frais par défaut" 5.0 "$(echo "$CORPS" | champ "float(d['fraisVotantPourcentage'])")"
req "$SA" GET /api/admin/tableau-de-bord; FRAIS0=$(echo "$CORPS" | champ "d['fraisService']"); REV0=$(echo "$CORPS" | champ "d['revenuAllVote']")

echo "== 2. Préparation (Club Test validé par le super-admin)"
req "$SA" GET "/api/admin/organisateurs?statut=EN_ATTENTE"; ORG=$(echo "$CORPS" | champ "[o['idOrganisateur'] for o in d['contenu'] if o['slug']=='club-test'][0]" 2>/dev/null)
[ -n "$ORG" ] && req "$SA" POST /api/admin/organisateurs/$ORG/valider
req "$OR" POST /api/orga/evenements "{\"titre\":\"Test frais\",\"dateDebut\":\"$(date_iso '+10 days')\"}"; EV=$(echo "$CORPS" | champ "d['idEvenement']")
creer() { req "$OR" POST /api/orga/evenements/$EV/concours "{\"titre\":\"Concours $1 F\",\"prixVoix\":$1,\"dateOuverture\":\"$(date_iso '-1 hour')\",\"dateFermeture\":\"$(date_iso '+5 days')\"}"; local c; c=$(echo "$CORPS" | champ "d['idConcours']")
  req "$OR" POST /api/orga/concours/$c/categories '{"nom":"Cat"}'; local k; k=$(echo "$CORPS" | champ "d['categories'][0]['idCategorie']")
  req "$OR" POST /api/orga/categories/$k/candidats '{"nom":"CANDIDAT FRAIS"}'; echo "$c $(echo "$CORPS" | champ "d['idCandidat']")"; }
read C100 X100 <<< "$(creer 100)"; read C150 X150 <<< "$(creer 150)"
req "$OR" PATCH /api/orga/evenements/$EV/statut '{"statut":"PUBLIE"}'; verifier "publication" 200 "$CODE" "$CORPS"

echo "== 3. Calcul des frais (serveur)"
req "" POST /api/public/votes "{\"idConcours\":$C100,\"idCandidat\":$X100,\"nbVoix\":3,\"operateur\":\"MTN\",\"telephone\":\"681507245\"}"
verifier "3 voix à 100 F : total" 315 "$(echo "$CORPS" | champ "d['montant']")"
verifier "  dont frais" 15 "$(echo "$CORPS" | champ "d['fraisService']")"
verifier "  valeur des voix" 300 "$(echo "$CORPS" | champ "d['valeurVoix']")"
echo "     message : $(echo "$CORPS" | champ "d['message']")"; R1=$(echo "$CORPS" | champ "d['reference']")
req "" POST /api/public/votes "{\"idConcours\":$C150,\"idCandidat\":$X150,\"nbVoix\":1,\"operateur\":\"ORANGE\",\"telephone\":\"691234567\"}"
verifier "1 voix à 150 F : 7,5 arrondi à 8 -> total" 158 "$(echo "$CORPS" | champ "d['montant']")"; R2=$(echo "$CORPS" | champ "d['reference']")
verifier "paiement 315 F confirmé" CONFIRME "$(attendre $R1)"
verifier "paiement 158 F confirmé" CONFIRME "$(attendre $R2)"
req "" GET /api/public/concours/$C100; verifier "3 voix créditées" 3 "$(echo "$CORPS" | champ "d['categories'][0]['candidats'][0]['votes']")"

echo "== 4. Partage : les frais ne touchent pas la part de l'organisateur"
req "$OR" GET "/api/orga/transactions?idConcours=$C100"
verifier "organisateur voit la valeur des voix" 300 "$(echo "$CORPS" | champ "d['contenu'][0]['montant']")"
verifier "commission 20 % sur 300" 60 "$(echo "$CORPS" | champ "d['contenu'][0]['montantCommission']")"
verifier "part organisateur" 240 "$(echo "$CORPS" | champ "d['contenu'][0]['montantOrganisateur']")"
req "$SA" GET "/api/admin/transactions?idConcours=$C100"
verifier "super-admin : total payé" 315 "$(echo "$CORPS" | champ "d['contenu'][0]['montant']")"
verifier "super-admin : frais" 15 "$(echo "$CORPS" | champ "d['contenu'][0]['fraisService']")"
req "$SA" GET /api/admin/tableau-de-bord
verifier "frais encaissés (+15 +8)" $((FRAIS0+23)) "$(echo "$CORPS" | champ "d['fraisService']")"
verifier "revenu AllVote (+60+15 +30+8)" $((REV0+113)) "$(echo "$CORPS" | champ "d['revenuAllVote']")"

echo "== 5. Taux modifiable, figé par transaction"
req "$SA" PUT /api/admin/parametres/frais '{"taux":10}'; verifier "frais passés à 10 %" 10.00 "$(echo "$CORPS" | champ "d['valeur']")"
req "" POST /api/public/votes "{\"idConcours\":$C100,\"idCandidat\":$X100,\"nbVoix\":1,\"operateur\":\"MTN\",\"telephone\":\"681507245\"}"
verifier "1 voix à 100 F avec 10 %" 110 "$(echo "$CORPS" | champ "d['montant']")"
req "$SA" PUT /api/admin/parametres/frais '{"taux":0}'
req "" POST /api/public/votes "{\"idConcours\":$C100,\"idCandidat\":$X100,\"nbVoix\":1,\"operateur\":\"MTN\",\"telephone\":\"681507245\"}"
verifier "frais à 0 % : prix net" 100 "$(echo "$CORPS" | champ "d['montant']")"
req "$SA" PUT /api/admin/parametres/frais '{"taux":5}'; verifier "retour à 5 %" 5.00 "$(echo "$CORPS" | champ "d['valeur']")"
req "$SA" GET "/api/admin/transactions?idConcours=$C100"
verifier "la 1re transaction garde 5 %" 5.0 "$(echo "$CORPS" | champ "[float(t['tauxFrais']) for t in d['contenu'] if t['montant']==315][0]")"
req "$SA" PUT /api/admin/parametres/frais '{"taux":150}'; verifier "taux 150 % refusé" 400 "$CODE"

echo "== 6. Nettoyage"
sleep 10
req "$OR" PATCH /api/orga/evenements/$EV/statut '{"statut":"ARCHIVE"}'; verifier "archivage" 200 "$CODE"
$PSQL "update organisateurs set statut='EN_ATTENTE', date_validation=null where slug='club-test'"

echo; echo "Résultat : $ok OK, $ko KO"
