#!/usr/bin/env bash
# =============================================================================
# Déploiement depuis le poste de développement (configurations Run d'IntelliJ) :
# mêmes étapes que le pipeline Jenkins, avec les identifiants de script/.deploy.env.
#
# Usage :
#   ./deploy-from-ide.sh            build Maven → images vers ghcr.io → déploiement k3s → test de fumée
#   ./deploy-from-ide.sh redeploy   redéploie sans rien reconstruire (IMAGE_TAG de .deploy.env, sinon latest)
#
# Configuration : copier script/.deploy.env.example en script/.deploy.env (ignoré par Git).
# RUN_TESTS=1 dans .deploy.env pour lancer les tests avant le déploiement (désactivés par défaut).
# =============================================================================
set -euo pipefail
cd "$(dirname "$0")"
ROOT="$(cd .. && pwd)"
ENV_FILE="$ROOT/script/.deploy.env"

# Identifiants locaux (fins de ligne Windows retirées, au cas où le fichier a été édité sous Windows)
if [ ! -f "$ENV_FILE" ]; then
  echo "✘ $ENV_FILE introuvable : copie script/.deploy.env.example en script/.deploy.env et complète-le." >&2
  exit 1
fi
set -a
# shellcheck disable=SC1090
source <(tr -d '\r' < "$ENV_FILE")
set +a

# shellcheck source=lib/deploy-common.sh
source "$ROOT/script/lib/deploy-common.sh"

# Champs obligatoires de .deploy.env
missing=()
for v in GHCR_USER GHCR_TOKEN KUBECONFIG; do [ -n "${!v:-}" ] || missing+=("$v"); done
[ ${#missing[@]} -eq 0 ] || fail "à compléter dans script/.deploy.env : ${missing[*]}"
[ -f "$KUBECONFIG" ] || fail "kubeconfig introuvable : $KUBECONFIG
  → copie /etc/rancher/k3s/k3s.yaml de l'Ubuntu à cet emplacement (ou corrige KUBECONFIG dans script/.deploy.env)"

MODE="${1:-full}"
need git     "Git pour Windows"
need kubectl "https://kubernetes.io/docs/tasks/tools/"

case "$MODE" in
  full)
    need mvn    "Maven dans le PATH"
    need docker "Installe Docker Desktop (winget install -e --id Docker.DockerDesktop), démarre-le, puis redémarre IntelliJ"
    # Tag = commit court ; si des modifications ne sont pas commitées, tag distinct pour ne pas
    # écraser l'image d'un commit propre (ce code-là n'existe que sur ce poste).
    TAG=$(git -C "$(native_path "$ROOT")" rev-parse --short HEAD)
    if [ -n "$(git -C "$(native_path "$ROOT")" status --porcelain)" ]; then
      TAG="$TAG-dev$(date +%Y%m%d%H%M%S)"
      echo "⚠ modifications non commitées : images taguées $TAG"
    fi
    export IMAGE_TAG="$TAG"

    log "Build Maven"
    # Le script mvn de Maven convertit lui-même ses chemins pour java.exe sous Git Bash :
    # il ne doit pas hériter de MSYS_NO_PATHCONV (posé pour openssl dans deploy-common.sh).
    if [ "${RUN_TESTS:-0}" = "1" ]; then
      (unset MSYS_NO_PATHCONV; cd "$ROOT" && mvn -B -q clean verify)
    else
      (unset MSYS_NO_PATHCONV; cd "$ROOT" && mvn -B -q clean package -DskipTests)
    fi
    ok "jars construits"

    bash "$ROOT/script/build-push.sh"
    ;;
  redeploy)
    export IMAGE_TAG="${IMAGE_TAG:-latest}"
    echo "Redéploiement du tag $IMAGE_TAG (aucun build)"
    ;;
  *)
    fail "mode inconnu : $MODE (attendu : full ou redeploy)"
    ;;
esac

bash "$ROOT/script/deploy-registry.sh"

log "Test de fumée"
bash "$ROOT/script/smoke-test.sh"
