#!/usr/bin/env bash
# =============================================================================
# Construit les 7 images de la plateforme et les pousse vers le registry.
# Les jars doivent déjà être construits (mvn verify / package).
# Fonctionne sous Linux et sous Git Bash (Windows, avec Docker Desktop).
#
# Variables :
#   IMAGE_TAG   (obligatoire) tag des images, en pratique le commit Git court
#   REGISTRY    défaut : ghcr.io/courtvi
#   GHCR_USER / GHCR_TOKEN   identifiants du registry (token GitHub avec write:packages)
# Chaque image est poussée avec IMAGE_TAG et avec « latest ».
# =============================================================================
set -euo pipefail
cd "$(dirname "$0")"
ROOT="$(cd .. && pwd)"
# shellcheck source=lib/deploy-common.sh
source "$ROOT/script/lib/deploy-common.sh"

: "${IMAGE_TAG:?IMAGE_TAG obligatoire (ex. le commit court : git rev-parse --short HEAD)}"
REGISTRY="${REGISTRY:-ghcr.io/courtvi}"
REPO_URL="${REPO_URL:-https://github.com/courtvi/services-platform}"
need docker "Installe Docker Desktop (Windows) ou Docker Engine (Linux)"

if [ -n "${GHCR_TOKEN:-}" ]; then
  printf '%s' "$GHCR_TOKEN" | docker login "${REGISTRY%%/*}" -u "${GHCR_USER:?GHCR_USER obligatoire avec GHCR_TOKEN}" --password-stdin >/dev/null
  ok "connecté à ${REGISTRY%%/*}"
fi

# image -> dossier du Dockerfile
IMAGES=(
  "eureka-server:eureka-server"
  "api-gateway:api-gateway"
  "product-service:produit-service"
  "client-service:client-service"
  "commande-service:commande-service"
  "devis-service:devis-service"
  "frontend:frontend"
)

log "Build et push vers $REGISTRY (tag $IMAGE_TAG)"
for entry in "${IMAGES[@]}"; do
  name="${entry%%:*}"; dir="${entry#*:}"
  ref="$REGISTRY/$name"
  echo "  → $ref:$IMAGE_TAG"
  docker build -q \
    --label "org.opencontainers.image.source=$REPO_URL" \
    --label "org.opencontainers.image.revision=$IMAGE_TAG" \
    -t "$ref:$IMAGE_TAG" -t "$ref:latest" \
    "$(native_path "$ROOT/$dir")" >/dev/null
  docker push -q "$ref:$IMAGE_TAG" >/dev/null
  docker push -q "$ref:latest" >/dev/null
done
ok "7 images poussées"
