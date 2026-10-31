#!/usr/bin/env bash
# Génère une CA de test, un certificat serveur (product-service) et un certificat client (k6)
set -euo pipefail
cd "$(dirname "$0")"
DAYS=825

# 1. CA
openssl req -x509 -newkey rsa:4096 -sha256 -days $DAYS -nodes \
  -keyout ca.key -out ca.crt -subj "/CN=services-platform-test-ca"

# 2. Serveur
openssl req -newkey rsa:2048 -nodes -keyout server.key -out server.csr \
  -subj "/CN=product-service"
cat > server.ext <<EXT
basicConstraints=CA:FALSE
keyUsage=digitalSignature,keyEncipherment
extendedKeyUsage=serverAuth
subjectAltName=DNS:localhost,DNS:product-service,DNS:product-service.default.svc.cluster.local,IP:127.0.0.1
EXT
openssl x509 -req -in server.csr -CA ca.crt -CAkey ca.key -CAcreateserial \
  -out server.crt -days $DAYS -sha256 -extfile server.ext

# 3. Client k6
openssl req -newkey rsa:2048 -nodes -keyout client.key -out client.csr \
  -subj "/CN=k6-loadtest"
cat > client.ext <<EXT
basicConstraints=CA:FALSE
keyUsage=digitalSignature
extendedKeyUsage=clientAuth
EXT
openssl x509 -req -in client.csr -CA ca.crt -CAkey ca.key -CAcreateserial \
  -out client.crt -days $DAYS -sha256 -extfile client.ext

# 4. PKCS12
P12_PASSWORD="${P12_PASSWORD:-changeit}"
if [ ${#P12_PASSWORD} -lt 6 ]; then echo "P12_PASSWORD doit faire au moins 6 caractères" >&2; exit 1; fi

openssl pkcs12 -export -name product-service \
  -in server.crt -inkey server.key -certfile ca.crt \
  -out server.p12 -passout pass:"$P12_PASSWORD"

rm -f truststore.p12
keytool -importcert -noprompt -alias ca -file ca.crt \
  -keystore truststore.p12 -storetype PKCS12 -storepass "$P12_PASSWORD"

openssl pkcs12 -export -name k6-loadtest \
  -in client.crt -inkey client.key -certfile ca.crt \
  -out client.p12 -passout pass:"$P12_PASSWORD"

rm -f *.csr *.ext ca.srl
echo "OK : PEM (ca, server, client) + server.p12, truststore.p12, client.p12"
