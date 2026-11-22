#!/usr/bin/env bash
# =============================================================================
# Déploiement complet sur une Ubuntu locale (VM ou WSL) avec k3s et mTLS,
# SANS registry : les images sont construites ici et importées directement dans k3s.
# (Pour déployer à partir du registry ghcr.io : deploy-registry.sh)
#
# Usage (depuis le dossier script/) :  ./deploy-ubuntu.sh
#
# Variables facultatives :
#   SKIP_BUILD=1          ne reconstruit ni les jars ni les images (redéploiement rapide)
#   SKIP_MAVEN=1          reconstruit les images à partir des jars déjà présents
#   MTLS_OUT_DIR=...      dossier des certificats (défaut : k8s/mtls/out)
#   P12_PASSWORD=...      mot de passe des keystores mTLS (sinon généré puis conservé)
#   APP_USER=...          compte de connexion créé dans Keycloak (défaut : admin@lorrconnect.local)
#   APP_PASSWORD=...      son mot de passe (défaut : généré et affiché à la fin)
#   MAIL_USERNAME / MAIL_PASSWORD   identifiants SMTP pour les emails de commande (facultatif)
#
# Le script est relançable : secrets, certificats et compte Keycloak ne sont créés qu'une fois.
# =============================================================================
set -euo pipefail
cd "$(dirname "$0")"
ROOT="$(cd .. && pwd)"
# shellcheck source=lib/deploy-common.sh
source "$ROOT/script/lib/deploy-common.sh"

# -----------------------------------------------------------------------------
log "Prérequis"
# -----------------------------------------------------------------------------
need java    "sudo apt install -y openjdk-21-jdk"
need keytool "sudo apt install -y openjdk-21-jdk"
need mvn     "sudo apt install -y maven"
need docker  "https://docs.docker.com/engine/install/ubuntu/ puis : sudo usermod -aG docker \$USER (et reconnecte-toi)"
need openssl "sudo apt install -y openssl"
need curl    "sudo apt install -y curl"
python_cmd >/dev/null
JAVA_MAJOR=$(java -version 2>&1 | sed -nE 's/.*version "([0-9]+).*/\1/p' | head -1)
[ "${JAVA_MAJOR:-0}" -ge 21 ] || fail "Java 21 requis (trouvé : ${JAVA_MAJOR:-?}). sudo apt install -y openjdk-21-jdk"
docker info >/dev/null 2>&1 || fail "Docker ne répond pas (démarré ? utilisateur dans le groupe docker ?)"
ok "Java $JAVA_MAJOR, Maven, Docker, OpenSSL, Python"

# -----------------------------------------------------------------------------
log "Cluster k3s"
# -----------------------------------------------------------------------------
if ! command -v k3s >/dev/null 2>&1; then
  [ -z "${JENKINS_URL:-}" ] || fail "k3s absent : installe-le une fois à la main"
  echo "k3s absent : installation (sudo requis)..."
  curl -sfL https://get.k3s.io | sh -s - --write-kubeconfig-mode 644
fi
export KUBECONFIG=/etc/rancher/k3s/k3s.yaml
kubectl wait node --all --for=condition=Ready --timeout=180s >/dev/null
ensure_namespaces

create_app_secrets
setup_mtls

# -----------------------------------------------------------------------------
log "Build des jars et des images"
# -----------------------------------------------------------------------------
if [ "${SKIP_BUILD:-0}" = "1" ]; then
  echo "  SKIP_BUILD=1 : images existantes conservées"
else
  if [ "${SKIP_MAVEN:-0}" = "1" ]; then
    echo "  SKIP_MAVEN=1 : jars déjà construits (et testés) réutilisés"
  else
    (cd "$ROOT" && mvn -B -q clean package -DskipTests)
    ok "jars construits"
  fi
  image() {  # image <nom:tag> <dossier> : docker build puis import dans le containerd de k3s
    echo "  → $1"
    docker build -q -t "$1" "$2" >/dev/null
    docker save "$1" | sudo k3s ctr images import - >/dev/null
  }
  image eureka-server:latest    "$ROOT/eureka-server"
  image api-gateway:latest      "$ROOT/api-gateway"
  image product-service:mtls    "$ROOT/produit-service"
  image client-service:latest   "$ROOT/client-service"
  image commande-service:latest "$ROOT/commande-service"
  image devis-service:latest    "$ROOT/devis-service"
  image frontend:latest         "$ROOT/frontend"
  ok "7 images importées dans k3s"
fi

# -----------------------------------------------------------------------------
log "Manifestes Kubernetes (images locales)"
# -----------------------------------------------------------------------------
apply() { kubectl apply -f "$ROOT/k8s/$1" >/dev/null && echo "  ✓ $1"; }
prepare_postgres_strategy
apply keycloak/postgres.yml
apply keycloak/deployment.yml
apply keycloak/service.yml
apply eureka-server/deployment.yml
apply eureka-server/service.yml
apply commande-service/postgres.yml
apply product-service/deployment.yml        # namespace default
apply client-service/deployment.yml
apply commande-service/deployment.yml
apply commande-service/service.yml
apply devis-service/deployment.yml
apply api-gateway/deployment.yml
apply api-gateway/service.yml
apply front-end/deployment.yml
apply front-end/service.yml
apply observability/otel-lgtm.yml

# En local, une réplique suffit (les manifestes en prévoient 2 pour la gateway et produit)
kubectl scale deployment/api-gateway -n "$NS" --replicas=1 >/dev/null
kubectl scale deployment/product-service -n default --replicas=1 >/dev/null

# Les images gardent le même tag : on force le redémarrage pour prendre les nouvelles
for d in eureka client-service commande-service devis-service api-gateway frontend; do
  kubectl rollout restart deployment/"$d" -n "$NS" >/dev/null
done
kubectl rollout restart deployment/product-service -n default >/dev/null

wait_platform
setup_db_monitoring
setup_keycloak
print_status
