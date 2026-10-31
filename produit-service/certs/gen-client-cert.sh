#!/usr/bin/env bash
# Émet un certificat CLIENT signé par la CA existante
# Usage : P12_PASSWORD=... ./gen-client-cert.sh <CN>
set -euo pipefail
cd "$(dirname "$0")"
CN="${1:?usage: $0 <CN>}"
: "${P12_PASSWORD:?exporte P12_PASSWORD}"

openssl req -newkey rsa:2048 -nodes -keyout "$CN.key" -out "$CN.csr" -subj "/CN=$CN"
cat > "$CN.ext" <<EXT
basicConstraints=CA:FALSE
keyUsage=digitalSignature
extendedKeyUsage=clientAuth
EXT
openssl x509 -req -in "$CN.csr" -CA ca.crt -CAkey ca.key -CAcreateserial \
  -out "$CN.crt" -days 825 -sha256 -extfile "$CN.ext"
openssl pkcs12 -export -name "$CN" -in "$CN.crt" -inkey "$CN.key" -certfile ca.crt \
  -out "$CN.p12" -passout pass:"$P12_PASSWORD"
rm -f "$CN.csr" "$CN.ext" ca.srl
echo "OK : $CN.crt, $CN.key, $CN.p12"
