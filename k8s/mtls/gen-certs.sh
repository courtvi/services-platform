#!/usr/bin/env bash
# Génère un certificat par microservice, signé par une CA commune.
# Chaque certificat sert à la fois côté serveur (le service reçoit des appels en mTLS)
# et côté client (le service appelle un autre service) : extendedKeyUsage = serverAuth + clientAuth.
#
# Usage : P12_PASSWORD=... ./gen-certs.sh
# Sortie (dans out/) : ca.crt, <service>.crt/.key/.p12, truststore.p12
#
# La CA de produit-service/certs est réutilisée si elle existe : les certificats déjà
# émis avec elle (k6, etc.) restent valides.
set -euo pipefail
cd "$(dirname "$0")"
DAYS=825
: "${P12_PASSWORD:?exporte P12_PASSWORD (6 caractères minimum)}"
if [ ${#P12_PASSWORD} -lt 6 ]; then echo "P12_PASSWORD doit faire au moins 6 caractères" >&2; exit 1; fi

# Services et CN : le CN est le nom autorisé côté produit-service (profil mtls).
SERVICES=(product-service client-service commande-service devis-service api-gateway k6-loadtest)

mkdir -p out
EXISTING_CA_DIR="../../produit-service/certs"

# 1. CA
if [ -f out/ca.key ] && [ -f out/ca.crt ]; then
  echo ">>> CA existante réutilisée (out/)"
elif [ -f "$EXISTING_CA_DIR/ca.key" ] && [ -f "$EXISTING_CA_DIR/ca.crt" ]; then
  echo ">>> CA de produit-service réutilisée"
  cp "$EXISTING_CA_DIR/ca.key" "$EXISTING_CA_DIR/ca.crt" out/
else
  echo ">>> Nouvelle CA"
  openssl req -x509 -newkey rsa:4096 -sha256 -days $DAYS -nodes \
    -keyout out/ca.key -out out/ca.crt -subj "/CN=services-platform-ca"
fi

# 2. Un certificat par service
for SVC in "${SERVICES[@]}"; do
  echo ">>> $SVC"
  openssl req -newkey rsa:2048 -nodes -keyout "out/$SVC.key" -out "out/$SVC.csr" -subj "/CN=$SVC"
  cat > "out/$SVC.ext" <<EXT
basicConstraints=CA:FALSE
keyUsage=digitalSignature,keyEncipherment
extendedKeyUsage=serverAuth,clientAuth
subjectAltName=DNS:localhost,DNS:$SVC,DNS:$SVC.lorrconnect.svc.cluster.local,DNS:$SVC.default.svc.cluster.local,IP:127.0.0.1
EXT
  openssl x509 -req -in "out/$SVC.csr" -CA out/ca.crt -CAkey out/ca.key -CAcreateserial \
    -out "out/$SVC.crt" -days $DAYS -sha256 -extfile "out/$SVC.ext"
  # alias = nom du service (référencé par spring.ssl.bundle.*.key.alias)
  openssl pkcs12 -export -name "$SVC" -in "out/$SVC.crt" -inkey "out/$SVC.key" -certfile out/ca.crt \
    -out "out/$SVC.p12" -passout pass:"$P12_PASSWORD"
  rm -f "out/$SVC.csr" "out/$SVC.ext"
done

# 3. Truststore commun (la CA)
rm -f out/truststore.p12
keytool -importcert -noprompt -alias ca -file out/ca.crt \
  -keystore out/truststore.p12 -storetype PKCS12 -storepass "$P12_PASSWORD"
rm -f out/ca.srl

echo "OK : certificats dans $(pwd)/out — lance ensuite ./create-secrets.sh"
