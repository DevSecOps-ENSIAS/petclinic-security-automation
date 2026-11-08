#!/usr/bin/env bash
# Génère une CA, un certificat serveur par service et un certificat client pour le gateway.
# Usage : PKI_PASS='mot-de-passe-fort' ./scripts/pki/generate.sh [dossier-sortie]
set -euo pipefail
: "${PKI_PASS:?définir PKI_PASS (ex. export PKI_PASS='...')}"
OUT="${1:-secrets}"
mkdir -p "$OUT"
cd "$OUT"

# Évite la conversion de chemins de Git Bash pour -subj
export MSYS_NO_PATHCONV=1

echo "== 1. Autorité de certification =="
openssl genrsa -out ca.key 2048
openssl req -x509 -new -key ca.key -days 365 -sha256 -out ca.pem \
  -subj "/C=MA/ST=Maroc/L=Rabat/O=ensias/OU=info/CN=PetClinic-CA"

issue() {  # issue <nom> <serverAuth|clientAuth> <SAN>
  local name="$1" eku="$2" san="$3"
  echo "== Certificat $name ($eku) =="
  openssl genrsa -out "$name.key" 2048
  openssl req -new -key "$name.key" -out "$name.csr" \
    -subj "/C=MA/ST=Maroc/L=Casa/O=ensias/OU=petclinic/CN=$name"
  printf "subjectAltName=%s\nextendedKeyUsage=%s\nbasicConstraints=CA:FALSE\nkeyUsage=digitalSignature,keyEncipherment\n" \
    "$san" "$eku" > "$name.ext"
  openssl x509 -req -in "$name.csr" -CA ca.pem -CAkey ca.key -CAcreateserial \
    -days 365 -sha256 -extfile "$name.ext" -out "$name.crt"
  openssl pkcs12 -export -in "$name.crt" -inkey "$name.key" -certfile ca.pem \
    -name "$name" -out "$name.p12" -passout "pass:$PKI_PASS"
}

for svc in customers vets visits; do
  issue "$svc" serverAuth "DNS:localhost,DNS:$svc-service,IP:127.0.0.1"
done
issue api-gateway clientAuth "DNS:localhost,DNS:api-gateway"

echo "== Truststore Java =="
rm -f truststore.p12
keytool -importcert -noprompt -alias petclinic-ca -file ca.pem \
  -keystore truststore.p12 -storetype PKCS12 -storepass "$PKI_PASS"

echo "== Vérifications =="
for f in customers vets visits api-gateway; do openssl verify -CAfile ca.pem "$f.crt"; done
echo "Terminé. Fichiers dans $(pwd)"