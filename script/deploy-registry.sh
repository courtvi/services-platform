#!/usr/bin/env bash
# =============================================================================
# Déploie la plateforme sur un cluster k3s (local ou distant) à partir des images
# du registry. Le cluster est celui de KUBECONFIG ; aucune image n'est construite ici.
# Fonctionne sous Linux et sous Git Bash (Windows).
#
# Variables :
#   IMAGE_TAG     (obligatoire) tag déployé, poussé au préalable par build-push.sh
#   KUBECONFIG    accès au cluster (fichier kubeconfig de k3s)
#   GHCR_USER / GHCR_TOKEN   pour le secret de pull « ghcr-pull » (token avec read:packages)
#   REGISTRY      défaut : ghcr.io/courtvi
#   TARGET_HOST   machine qui expose les NodePorts (défaut : localhost)
#   PUBLIC_URL    adresse publique du site (ex. https://lorrconnect.com, via le tunnel Cloudflare).
#                 Vide : plateforme accessible seulement en local (localhost).
#   KEYCLOAK_PUBLIC_URL  adresse publique de Keycloak si elle a son propre sous-domaine
#                 (ex. https://keycloak.lorrconnect.com). Défaut : PUBLIC_URL.
#   P12_PASSWORD, MTLS_OUT_DIR, APP_USER, APP_PASSWORD, MAIL_USERNAME, MAIL_PASSWORD : voir deploy-ubuntu.sh
# =============================================================================
set -euo pipefail
cd "$(dirname "$0")"
ROOT="$(cd .. && pwd)"
# shellcheck source=lib/deploy-common.sh
source "$ROOT/script/lib/deploy-common.sh"

: "${IMAGE_TAG:?IMAGE_TAG obligatoire (le tag poussé par build-push.sh)}"
REGISTRY="${REGISTRY:-ghcr.io/courtvi}"

log "Prérequis"
need kubectl "https://kubernetes.io/docs/tasks/tools/"
need openssl "fourni par Git pour Windows, ou : sudo apt install -y openssl"
need keytool "fourni par le JDK 21"
need curl    "fourni par Git pour Windows, ou : sudo apt install -y curl"
python_cmd >/dev/null
kubectl get nodes >/dev/null 2>&1 || fail "cluster injoignable : vérifie KUBECONFIG (adresse du serveur, port 6443)"
ok "cluster joignable : $(kubectl get nodes -o jsonpath='{.items[*].metadata.name}')"
ensure_namespaces

log "Secret de pull du registry (ghcr-pull)"
: "${GHCR_USER:?GHCR_USER obligatoire}" "${GHCR_TOKEN:?GHCR_TOKEN obligatoire}"
for n in "$NS" default; do
  kubectl create secret docker-registry ghcr-pull -n "$n" \
    --docker-server="${REGISTRY%%/*}" --docker-username="$GHCR_USER" --docker-password="$GHCR_TOKEN" \
    --dry-run=client -o yaml | kubectl apply -f - >/dev/null
done
ok "ghcr-pull dans $NS et default"

create_app_secrets
setup_mtls

# Variante des manifestes : publique (adresse Internet) ou locale
if [ -n "${PUBLIC_URL:-}" ]; then
  PUBLIC_URL="${PUBLIC_URL%/}"
  [[ "$PUBLIC_URL" =~ ^https:// ]] || fail "PUBLIC_URL doit commencer par https:// (Keycloak l'exige hors localhost)"
  KEYCLOAK_PUBLIC_URL="${KEYCLOAK_PUBLIC_URL:-$PUBLIC_URL}"
  KEYCLOAK_PUBLIC_URL="${KEYCLOAK_PUBLIC_URL%/}"
  [[ "$KEYCLOAK_PUBLIC_URL" =~ ^https:// ]] || fail "KEYCLOAK_PUBLIC_URL doit commencer par https://"
  export KEYCLOAK_PUBLIC_URL
  KUSTOMIZE_DIR="$ROOT/k8s-overlays/public"
  ADMIN_URL="http://$TARGET_HOST:30090"
else
  KUSTOMIZE_DIR="$ROOT/k8s"
fi

log "Manifestes (images $REGISTRY/*:$IMAGE_TAG${PUBLIC_URL:+, adresse publique $PUBLIC_URL})"
# kustomize réécrit les images vers ghcr.io/courtvi/<image>:latest ; on y met le registry et le tag déployés,
# puis l'adresse publique à la place des marqueurs de la variante « public ».
# Un changement d'image ou de configuration redéploie les pods concernés.
prepare_postgres_strategy
kubectl kustomize "$(native_path "$KUSTOMIZE_DIR")" \
  | sed -E -e "s#image: ghcr\.io/courtvi/([a-z0-9-]+):latest#image: ${REGISTRY}/\1:${IMAGE_TAG}#" \
           -e "s#__PUBLIC_URL__#${PUBLIC_URL:-}#g" \
           -e "s#__KEYCLOAK_URL__#${KEYCLOAK_PUBLIC_URL:-}#g" -e "s#__ADMIN_URL__#${ADMIN_URL:-}#g" \
  | kubectl apply -f -

wait_platform
setup_db_monitoring
setup_keycloak
print_status
