#!/usr/bin/env bash
# Crée (ou remplace) les secrets k8s mTLS à partir de $MTLS_OUT_DIR (défaut : out/, généré par gen-certs.sh).
# Usage : P12_PASSWORD=... ./create-secrets.sh
set -euo pipefail
cd "$(dirname "$0")"
: "${P12_PASSWORD:?exporte P12_PASSWORD (le même que pour gen-certs.sh)}"
OUT="${MTLS_OUT_DIR:-out}"

secret() {  # secret <nom> <namespace> <clé-du-p12-dans-le-secret> <fichier-p12>
  kubectl create secret generic "$1" -n "$2" \
    --from-file="$3=$OUT/$4" \
    --from-file=truststore.p12="$OUT/truststore.p12" \
    --from-file=ca.crt="$OUT/ca.crt" \
    --from-literal=P12_PASSWORD="$P12_PASSWORD" \
    --dry-run=client -o yaml | kubectl apply -f -
}

# product-service est déployé dans le namespace default ; son deployment attend la clé server.p12.
secret product-service-mtls  default     server.p12            product-service.p12
secret api-gateway-mtls      lorrconnect api-gateway.p12       api-gateway.p12
secret client-service-mtls   lorrconnect client-service.p12    client-service.p12
secret commande-service-mtls lorrconnect commande-service.p12  commande-service.p12
secret devis-service-mtls    lorrconnect devis-service.p12     devis-service.p12

echo "OK : secrets mTLS créés. Redémarre les déploiements pour qu'ils les prennent en compte :"
echo "  kubectl rollout restart deployment -n lorrconnect && kubectl rollout restart deployment/product-service -n default"
