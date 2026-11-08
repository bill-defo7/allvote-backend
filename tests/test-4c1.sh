#!/usr/bin/env bash
# =====================================================================
#  Tests de l'étape 4c-1 (espace organisateur). Lancer : bash tests/test-4c1.sh
#  Utilise le compte de test "club.test@gmail.com" créé à l'étape 4b.
# =====================================================================
API=${API:-http://localhost:8081}
ORGA_EMAIL=club.test@gmail.com
ORGA_PASS=ClubTest2026
MEMBRE_EMAIL="membre.$(date +%s)@gmail.com"

ok=0; ko=0
verifier() {   # verifier "libellé" code_attendu code_obtenu [corps]
  if [ "$2" = "$3" ]; then echo "  OK  $1 ($3)"; ok=$((ok+1));
  else echo "  KO  $1 : attendu $2, obtenu $3  $4"; ko=$((ko+1)); fi
}
champ() { python3 -c "import sys,json; d=json.load(sys.stdin); print($1)"; }
appel() {      # appel METHODE URL [JSON] -> imprime "code|corps"
  local m=$1 u=$2 d=$3 tmp; tmp=$(mktemp)
  if [ -n "$d" ]; then
    code=$(curl -s -o "$tmp" -w '%{http_code}' -X "$m" "$API$u" -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d "$d")
  else
    code=$(curl -s -o "$tmp" -w '%{http_code}' -X "$m" "$API$u" -H "Authorization: Bearer $TOKEN")
  fi
  CODE=$code; CORPS=$(cat "$tmp"); rm -f "$tmp"
}
connexion() {
  TOKEN=$(curl -s -X POST "$API/api/auth/login" -H 'Content-Type: application/json' \
    -d "{\"email\":\"$1\",\"password\":\"$2\"}" | champ "d.get('token','')")
}

echo "== 1. Connexion du responsable Club Test (organisation EN_ATTENTE)"
connexion "$ORGA_EMAIL" "$ORGA_PASS"
[ -n "$TOKEN" ] && verifier "jeton reçu" 1 1 || { verifier "jeton reçu" 1 0; exit 1; }

echo "== 2. Profil"
appel GET /api/orga/profil;                       verifier "voir le profil" 200 "$CODE" "$CORPS"
echo "     statut=$(echo "$CORPS" | champ "d['statut']")  commission=$(echo "$CORPS" | champ "d['tauxCommission']") %"
appel PUT /api/orga/profil '{"nom":"Club Test","email":"club.test@gmail.com","ville":"Ebolowa","description":"Club de test AllVote","numeroReversement":"+237 681 50 72 45","operateurReversement":"MTN"}'
verifier "modifier le profil" 200 "$CODE" "$CORPS"
echo "     numéro de reversement normalisé : $(echo "$CORPS" | champ "d['numeroReversement']")"

echo "== 3. Événement"
appel POST /api/orga/evenements '{"titre":"Gala Club Test 2026","lieu":"Ebolowa","description":"Événement de test","dateDebut":"2026-12-20T18:00:00Z","dateFin":"2026-12-20T23:00:00Z"}'
verifier "créer un événement" 201 "$CODE" "$CORPS"
EV=$(echo "$CORPS" | champ "d['idEvenement']"); echo "     idEvenement=$EV slug=$(echo "$CORPS" | champ "d['slug']") statut=$(echo "$CORPS" | champ "d['statut']")"
appel POST /api/orga/evenements '{"titre":"X"}';  verifier "titre trop court refusé" 400 "$CODE"
appel GET /api/orga/evenements;                   verifier "lister mes événements" 200 "$CODE"

echo "== 4. Concours + catégories + candidats"
appel POST /api/orga/evenements/$EV/concours '{"titre":"Miss & Master Club","prixVoix":100,"scoresPublics":true,"dateOuverture":"2026-11-01T00:00:00Z","dateFermeture":"2026-12-20T20:00:00Z","brouillon":false}'
verifier "créer un concours" 201 "$CODE" "$CORPS"
CO=$(echo "$CORPS" | champ "d['idConcours']"); echo "     idConcours=$CO statut=$(echo "$CORPS" | champ "d['statut']")"
appel POST /api/orga/evenements/$EV/concours '{"titre":"Mauvaises dates","prixVoix":100,"dateOuverture":"2026-12-20T00:00:00Z","dateFermeture":"2026-12-01T00:00:00Z"}'
verifier "dates inversées refusées" 400 "$CODE"
appel POST /api/orga/concours/$CO/categories '{"nom":"Miss"}';    verifier "catégorie Miss" 201 "$CODE"
K1=$(echo "$CORPS" | champ "d['categories'][-1]['idCategorie']")
appel POST /api/orga/concours/$CO/categories '{"nom":"Master"}';  verifier "catégorie Master" 201 "$CODE"
K2=$(echo "$CORPS" | champ "d['categories'][-1]['idCategorie']")
appel PUT /api/orga/concours/$CO/categories/ordre "{\"ids\":[$K2,$K1]}"; verifier "réordonner" 200 "$CODE"
echo "     ordre : $(echo "$CORPS" | champ "[k['nom'] for k in d['categories']]")"
appel POST /api/orga/categories/$K1/candidats '{"nom":"AMINA TEST","sousTitre":"Club","description":"Candidate de test"}'
verifier "candidat 1" 201 "$CODE"; X1=$(echo "$CORPS" | champ "d['idCandidat']")
appel POST /api/orga/categories/$K1/candidats '{"nom":"BELLA TEST","numero":1}'; verifier "numéro déjà pris refusé" 409 "$CODE"
appel POST /api/orga/categories/$K1/candidats '{"nom":"BELLA TEST"}'; verifier "candidat 2 (numéro auto)" 201 "$CODE"
echo "     numéro attribué : $(echo "$CORPS" | champ "d['numero']")"
appel PATCH /api/orga/candidats/$X1/actif '{"actif":false}'; verifier "désactiver un candidat" 200 "$CODE"
appel PATCH /api/orga/concours/$CO/statut '{"statutForce":"SUSPENDU"}'; verifier "suspendre le concours" 200 "$CODE"
appel PATCH /api/orga/concours/$CO/statut '{"statutForce":null}';       verifier "retour en automatique" 200 "$CODE"

echo "== 5. Publication (organisation pas encore validée)"
appel PATCH /api/orga/evenements/$EV/statut '{"statut":"PUBLIE"}'; verifier "publication refusée" 403 "$CODE" "$CORPS"

echo "== 6. Cloisonnement : les données d'un autre organisateur sont invisibles"
appel GET /api/orga/concours/1;          verifier "concours du département GI" 404 "$CODE"
appel PUT /api/orga/candidats/1 '{"nom":"PIRATE"}'; verifier "modifier un candidat GI" 404 "$CODE"
appel GET /api/admin/organisateurs;      verifier "espace super-admin interdit" 403 "$CODE"

echo "== 7. Upload d'image"
python3 - <<'PY'
import struct, zlib
def png(w, h):
    raw = b''.join(b'\x00' + bytes([200, 160, 40]) * w for _ in range(h))
    chunk = lambda t, d: struct.pack('>I', len(d)) + t + d + struct.pack('>I', zlib.crc32(t + d) & 0xffffffff)
    return b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 2, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(raw)) + chunk(b'IEND', b'')
open('/tmp/allvote-test.png', 'wb').write(png(64, 64))
open('/tmp/allvote-faux.png', 'w').write('<script>alert(1)</script>')
PY
R=$(curl -s -w '|%{http_code}' -X POST "$API/api/orga/uploads" -H "Authorization: Bearer $TOKEN" -F "fichier=@/tmp/allvote-test.png;type=image/png")
verifier "vraie image" 200 "${R##*|}" "$R"; URL=$(echo "${R%|*}" | champ "d['url']"); echo "     $URL"
verifier "image accessible publiquement" 200 "$(curl -s -o /dev/null -w '%{http_code}' "$API$URL")"
R=$(curl -s -w '|%{http_code}' -X POST "$API/api/orga/uploads" -H "Authorization: Bearer $TOKEN" -F "fichier=@/tmp/allvote-faux.png;type=image/png")
verifier "faux PNG refusé" 400 "${R##*|}"
appel PUT /api/orga/candidats/$X1 "{\"nom\":\"AMINA TEST\",\"photoUrl\":\"$URL\"}"; verifier "photo du candidat" 200 "$CODE"
appel PUT /api/orga/candidats/$X1 '{"nom":"AMINA TEST","photoUrl":"https://site-pirate.com/x.jpg"}'; verifier "lien externe refusé" 400 "$CODE"

echo "== 8. Membres"
appel POST /api/orga/membres "{\"nom\":\"Membre\",\"prenom\":\"Test\",\"email\":\"$MEMBRE_EMAIL\",\"motDePasseProvisoire\":\"Membre2026x\"}"
verifier "ajouter un membre" 201 "$CODE" "$CORPS"; M=$(echo "$CORPS" | champ "d['idUtilisateur']")
appel GET /api/orga/membres; verifier "lister les membres" 200 "$CODE"
echo "     $(echo "$CORPS" | champ "[(m['email'], m['roles']) for m in d]")"
TOKEN_RESP=$TOKEN
connexion "$MEMBRE_EMAIL" "Membre2026x"
appel GET /api/orga/evenements;  verifier "membre : voit les événements" 200 "$CODE"
appel GET /api/orga/membres;     verifier "membre : pas la gestion des membres" 403 "$CODE"
appel PATCH /api/orga/evenements/$EV/statut '{"statut":"PUBLIE"}'; verifier "membre : ne peut pas publier" 403 "$CODE"
TOKEN=$TOKEN_RESP
appel PATCH /api/orga/membres/$M/actif '{"actif":false}'; verifier "désactiver le membre" 200 "$CODE"
connexion "$MEMBRE_EMAIL" "Membre2026x"
[ -z "$TOKEN" ] && verifier "membre désactivé : connexion refusée" 1 1 || verifier "membre désactivé : connexion refusée" 1 0
TOKEN=$TOKEN_RESP
appel DELETE /api/orga/membres/$M; verifier "supprimer le membre" 204 "$CODE"

echo "== 9. Suppression"
appel DELETE /api/orga/evenements/$EV; verifier "supprimer l'événement de test" 204 "$CODE"

echo; echo "Résultat : $ok OK, $ko KO"
rm -f /tmp/allvote-test.png /tmp/allvote-faux.png
