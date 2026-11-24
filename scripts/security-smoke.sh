#!/usr/bin/env bash
# Contrôle de sécurité de bout en bout (P9). Sort en code 1 si une ligne échoue.
set -u

GATEWAY=${GATEWAY:-http://localhost:8080}
KEYCLOAK=${KEYCLOAK:-http://localhost:8180}
REALM=${REALM:-petclinic}
CLIENT=${CLIENT:-petclinic-cli}

OK=0; FAIL=0; SKIP=0
pass() { printf 'OK   %s\n' "$1"; OK=$((OK+1)); }
fail() { printf 'FAIL %s (attendu %s, obtenu %s)\n' "$1" "$2" "$3"; FAIL=$((FAIL+1)); }
skip() { printf 'SKIP %s\n' "$1"; SKIP=$((SKIP+1)); }
expect() { [ "$2" = "$3" ] && pass "$1 -> $3" || fail "$1" "$2" "$3"; }
code() { curl -s -o /dev/null -w '%{http_code}' "$@"; }
tok() {
  curl -s -X POST "$KEYCLOAK/realms/$REALM/protocol/openid-connect/token" \
    -d grant_type=password -d client_id=$CLIENT -d username=$1 -d password=$2 \
    | sed -E 's/.*"access_token":"([^"]+)".*/\1/'
}

echo "== Jetons =="
TOKEN=$(tok alice alice-pass)
TOKEN_BOB=$(tok bob bob-pass)
TOKEN_CAROL=$(tok carol carol-pass)
echo "alice=${#TOKEN} bob=${#TOKEN_BOB} carol=${#TOKEN_CAROL}"
if [ "${#TOKEN}" -lt 100 ]; then
  echo "FAIL aucun jeton : Keycloak est-il démarré ?"
  exit 1
fi

echo
echo "== RBAC via le gateway =="
expect "GET owner sans jeton"     401 "$(code "$GATEWAY/api/customer/owners/1")"
expect "GET owner carol"          403 "$(code -H "Authorization: Bearer $TOKEN_CAROL" "$GATEWAY/api/customer/owners/1")"
expect "GET owner bob (viewer)"   200 "$(code -H "Authorization: Bearer $TOKEN_BOB" "$GATEWAY/api/customer/owners/1")"
expect "POST owner bob (viewer)"  403 "$(code -X POST -H "Authorization: Bearer $TOKEN_BOB" -H 'Content-Type: application/json' \
  -d '{"firstName":"Smoke","lastName":"Test","address":"1 rue","city":"Paris","telephone":"0102030405"}' \
  "$GATEWAY/api/customer/owners")"
expect "POST owner alice (staff)" 201 "$(code -X POST -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"firstName":"Smoke","lastName":"Test","address":"1 rue","city":"Paris","telephone":"0102030405"}' \
  "$GATEWAY/api/customer/owners")"

echo
echo "== Scénario dossier (P7) =="
expect "dossier alice"      200 "$(code -H "Authorization: Bearer $TOKEN" "$GATEWAY/api/gateway/owners/1/dossier")"
expect "dossier sans jeton" 401 "$(code "$GATEWAY/api/gateway/owners/1/dossier")"

echo
echo "== Durcissement (P8) =="
expect "actuator/env gateway" 404 "$(code -H "Authorization: Bearer $TOKEN" "$GATEWAY/actuator/env")"

echo
echo "== mTLS direct =="
check_mtls() {
  local PD="${PKI_DIR:-secrets}" SVC=localhost:8081
  if [ ! -f "$PD/rogue-client.crt" ]; then skip "mTLS direct : certificats absents (PKI_DIR)"; return; fi
  mtls_status() {  # $1 = nom du certificat ou "none", $2 = jeton (facultatif)
    local hdr="" r
    [ -n "${2:-}" ] && hdr="Authorization: Bearer $2\r\n"
    local certopt=()
    [ "$1" != none ] && certopt=(-cert "$PD/$1.crt" -key "$PD/$1.key")
    r=$(printf "GET /owners HTTP/1.1\r\nHost: localhost\r\n${hdr}Connection: close\r\n\r\n" \
      | openssl s_client -connect "$SVC" -CAfile "$PD/ca.pem" ${certopt[@]+"${certopt[@]}"} -quiet 2>/dev/null \
      | head -1 | grep -oE '[0-9]{3}' | head -1)
    echo "${r:-000}"
  }
  expect "service direct sans certificat"      000 "$(mtls_status none "$TOKEN")"
  expect "certificat gateway sans jeton"       401 "$(mtls_status api-gateway)"
  expect "certificat gateway + jeton alice"    200 "$(mtls_status api-gateway "$TOKEN")"
  expect "certificat intrus + jeton alice"     403 "$(mtls_status rogue-client "$TOKEN")"
}
check_mtls

echo
printf 'Résultat : %d OK, %d FAIL, %d SKIP\n' "$OK" "$FAIL" "$SKIP"
[ "$FAIL" -eq 0 ] || exit 1
[ "$SKIP" -eq 0 ] && echo "Contrôle complet." || echo "Contrôle PARTIEL : voir les lignes SKIP."
exit 0
