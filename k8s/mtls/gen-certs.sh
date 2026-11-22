#!/usr/bin/env bash
# Génère un certificat par microservice, signé par une CA commune.
# Chaque certificat sert à la fois côté serveur (le service reçoit des appels en mTLS)
# et côté client (le service appelle un autre service) : extendedKeyUsage = serverAuth + clientAuth.
#
# Usage : P12_PASSWORD=... ./gen-certs.sh
# Sortie (dans $MTLS_OUT_DIR, par défaut $OUT/) : ca.crt, <service>.crt/.key/.p12, truststore.p12
# MTLS_OUT_DIR ne doit pas contenir d'espace (Jenkins : $JENKINS_HOME/mtls).
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

OUT="${MTLS_OUT_DIR:-out}"
mkdir -p "$OUT"
EXISTING_CA_DIR="../../produit-service/certs"

# 1. CA
if [ -f $OUT/ca.key ] && [ -f $OUT/ca.crt ]; then
  echo ">>> CA existante réutilisée ($OUT)"
elif [ -f "$EXISTING_CA_DIR/ca.key" ] && [ -f "$EXISTING_CA_DIR/ca.crt" ]; then
  echo ">>> CA de produit-service réutilisée"
  cp "$EXISTING_CA_DIR/ca.key" "$EXISTING_CA_DIR/ca.crt" $OUT/
else
  echo ">>> Nouvelle CA"
  openssl req -x509 -newkey rsa:4096 -sha256 -days $DAYS -nodes \
    -keyout $OUT/ca.key -out $OUT/ca.crt -subj "/CN=services-platform-ca"
fi

# 2. Un certificat par service
for SVC in "${SERVICES[@]}"; do
  echo ">>> $SVC"
  openssl req -newkey rsa:2048 -nodes -keyout "$OUT/$SVC.key" -out "$OUT/$SVC.csr" -subj "/CN=$SVC"
  cat > "$OUT/$SVC.ext" <<EXT
basicConstraints=CA:FALSE
keyUsage=digitalSignature,keyEncipherment
extendedKeyUsage=serverAuth,clientAuth
subjectAltName=DNS:localhost,DNS:$SVC,DNS:$SVC.lorrconnect.svc.cluster.local,DNS:$SVC.default.svc.cluster.local,IP:127.0.0.1
EXT
  openssl x509 -req -in "$OUT/$SVC.csr" -CA $OUT/ca.crt -CAkey $OUT/ca.key -CAcreateserial \
    -out "$OUT/$SVC.crt" -days $DAYS -sha256 -extfile "$OUT/$SVC.ext"
  # alias = nom du service (référencé par spring.ssl.bundle.*.key.alias)
  openssl pkcs12 -export -name "$SVC" -in "$OUT/$SVC.crt" -inkey "$OUT/$SVC.key" -certfile $OUT/ca.crt \
    -out "$OUT/$SVC.p12" -passout pass:"$P12_PASSWORD"
  rm -f "$OUT/$SVC.csr" "$OUT/$SVC.ext"
done

# 3. Truststore commun (la CA)
rm -f $OUT/truststore.p12
keytool -importcert -noprompt -alias ca -file $OUT/ca.crt \
  -keystore $OUT/truststore.p12 -storetype PKCS12 -storepass "$P12_PASSWORD"
rm -f $OUT/ca.srl

echo "OK : certificats dans $OUT — lance ensuite ./create-secrets.sh"
