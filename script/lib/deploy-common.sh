# shellcheck shell=bash
# =============================================================================
# Fonctions communes aux scripts de déploiement :
#   deploy-ubuntu.sh   (images construites et importées sur la machine k3s)
#   deploy-registry.sh (images tirées depuis le registry, cluster local ou distant)
#
# À sourcer après avoir défini ROOT (racine du dépôt). Fonctionne sous Linux
# et sous Git Bash (Windows), d'où native_path() pour les .exe Windows.
# =============================================================================

NS="${NS:-lorrconnect}"
TARGET_HOST="${TARGET_HOST:-localhost}"        # machine qui expose les NodePorts (30080, 30090, 31803)
MTLS_DIR="$ROOT/k8s/mtls"

log()  { echo -e "\n\033[1;34m▶ $*\033[0m"; }
ok()   { echo -e "\033[0;32m✔ $*\033[0m"; }
fail() { echo -e "\033[0;31m✘ $*\033[0m" >&2; exit 1; }
need() { command -v "$1" >/dev/null 2>&1 || fail "$1 introuvable. $2"; }

# --- Git Bash (Windows) ------------------------------------------------------
# - pas de conversion automatique des arguments en chemins (sinon openssl -subj "/CN=..." est cassé)
# - les chemins passés aux programmes Windows (kubectl.exe, docker.exe, java, python) sont
#   donnés sous la forme C:/... que comprennent aussi les outils de Git Bash
if [[ "$(uname -s)" =~ ^(MINGW|MSYS|CYGWIN) ]]; then
  export MSYS_NO_PATHCONV=1
  native_path() { cygpath -m "$1"; }
else
  native_path() { printf '%s' "$1"; }
fi

export MTLS_OUT_DIR
MTLS_OUT_DIR="$(native_path "${MTLS_OUT_DIR:-$MTLS_DIR/out}")"

python_cmd() {  # premier Python 3 réellement utilisable (sous Windows, « python » peut être un raccourci du Store)
  local p
  for p in python3 python; do
    if command -v "$p" >/dev/null 2>&1 && "$p" -c 'import sys; sys.exit(0 if sys.version_info >= (3, 8) else 1)' >/dev/null 2>&1; then
      echo "$p"; return
    fi
  done
  fail "Python 3 introuvable (nécessaire pour configurer Keycloak)"
}

ensure_namespaces() {
  local n
  for n in "$NS" default; do
    kubectl get namespace "$n" >/dev/null 2>&1 || kubectl create namespace "$n" >/dev/null
  done
  ok "namespaces $NS et default"
}

# --- Secrets applicatifs (créés une seule fois) -------------------------------
secret_if_absent() {  # secret_if_absent <nom> <arguments de kubectl create secret generic...>
  local name=$1; shift
  if kubectl get secret "$name" -n "$NS" >/dev/null 2>&1; then
    echo "  = $name (existe déjà)"
  else
    kubectl create secret generic "$name" -n "$NS" "$@" >/dev/null && echo "  + $name"
  fi
}

create_app_secrets() {
  log "Secrets applicatifs (créés une seule fois)"
  local kc_db_pwd
  # Keycloak : le même mot de passe sert à Postgres et à Keycloak
  kc_db_pwd=$(kubectl get secret postgres-keycloak-secret -n "$NS" -o jsonpath='{.data.POSTGRES_PASSWORD}' 2>/dev/null | base64 -d 2>/dev/null || true)
  [ -n "$kc_db_pwd" ] || kc_db_pwd=$(openssl rand -hex 16)
  secret_if_absent postgres-keycloak-secret \
    --from-literal=POSTGRES_USER=keycloak --from-literal=POSTGRES_PASSWORD="$kc_db_pwd" --from-literal=POSTGRES_DB=keycloak
  secret_if_absent keycloak-db-secret --from-literal=KC_DB_PASSWORD="$kc_db_pwd"
  secret_if_absent postgres-commande-secret \
    --from-literal=POSTGRES_USER=commande --from-literal=POSTGRES_PASSWORD="$(openssl rand -hex 16)" --from-literal=POSTGRES_DB=commandedb
  secret_if_absent mail-secret \
    --from-literal=MAIL_USERNAME="${MAIL_USERNAME:-}" --from-literal=MAIL_PASSWORD="${MAIL_PASSWORD:-}"
}

# --- Certificats et secrets mTLS ----------------------------------------------
setup_mtls() {
  log "Certificats et secrets mTLS ($MTLS_OUT_DIR)"
  local pwd_file="$MTLS_OUT_DIR/.p12-password" cluster_ca=""
  # CA actuellement installée dans le cluster (clé ca.crt ajoutée par create-secrets.sh)
  cluster_ca=$(kubectl get secret client-service-mtls -n "$NS" -o jsonpath='{.data.ca\.crt}' 2>/dev/null | base64 -d 2>/dev/null || true)

  if [ ! -f "$MTLS_OUT_DIR/truststore.p12" ] && kubectl get secret client-service-mtls -n "$NS" >/dev/null 2>&1; then
    # Le cluster a déjà ses certificats, générés depuis une autre machine (Jenkins, ou un autre poste) :
    # on les garde. En générer de nouveaux ici casserait la confiance entre les services.
    echo "  = certificats déjà présents dans le cluster, conservés (pas de copie locale dans $MTLS_OUT_DIR)"
    return
  fi
  if [ -n "$cluster_ca" ] && [ -f "$MTLS_OUT_DIR/ca.crt" ] && [ "$cluster_ca" != "$(cat "$MTLS_OUT_DIR/ca.crt")" ]; then
    fail "La CA de $MTLS_OUT_DIR n'est pas celle du cluster : ces certificats viennent d'une autre machine. Pointe MTLS_OUT_DIR vers le bon dossier (par ex. celui de Jenkins), ou supprime ce dossier local pour garder ceux du cluster."
  fi

  if [ -z "${P12_PASSWORD:-}" ]; then
    if [ -f "$pwd_file" ]; then P12_PASSWORD=$(cat "$pwd_file"); else P12_PASSWORD=$(openssl rand -hex 12); fi
  fi
  export P12_PASSWORD
  if [ -f "$pwd_file" ] && [ "$(cat "$pwd_file")" != "$P12_PASSWORD" ]; then
    fail "P12_PASSWORD ne correspond pas aux certificats existants ($MTLS_OUT_DIR). Remets l'ancien mot de passe, ou supprime ce dossier pour tout régénérer."
  fi
  if [ -f "$MTLS_OUT_DIR/truststore.p12" ]; then
    echo "  = certificats existants réutilisés"
  else
    bash "$MTLS_DIR/gen-certs.sh"
  fi
  mkdir -p "$MTLS_OUT_DIR"
  ( umask 077; echo "$P12_PASSWORD" > "$pwd_file" )
  bash "$MTLS_DIR/create-secrets.sh" >/dev/null
  ok "secrets mTLS à jour (5 services)"
}

# --- Attente du démarrage -----------------------------------------------------
wait_for() {  # wait_for <deployment> <namespace> <timeout>
  if kubectl rollout status deployment/"$1" -n "$2" --timeout="$3" >/dev/null 2>&1; then
    ok "$1"
  else
    echo -e "\033[0;31m✘ $1 ne démarre pas — derniers logs :\033[0m"
    kubectl logs -n "$2" -l app="$1" --tail=30 --all-containers 2>/dev/null || true
    exit 1
  fi
}

wait_platform() {
  log "Attente du démarrage"
  wait_for postgres-keycloak "$NS" 180s
  wait_for keycloak          "$NS" 300s
  wait_for eureka            "$NS" 180s
  wait_for postgres-commande "$NS" 180s
  wait_for product-service   default 240s
  wait_for client-service    "$NS" 240s
  wait_for commande-service  "$NS" 240s
  wait_for devis-service     "$NS" 240s
  wait_for api-gateway       "$NS" 240s
  wait_for frontend          "$NS" 120s
}

# --- Keycloak -------------------------------------------------------------------
setup_keycloak() {
  log "Keycloak (realm, clients du front, rôles, compte de connexion)"
  local cred_file="$MTLS_OUT_DIR/.app-credentials"
  APP_USER="${APP_USER:-admin@lorrconnect.local}"
  # Le mot de passe du compte n'est imposé que s'il est connu (fourni, ou conservé par un déploiement
  # précédent depuis cette machine). Un mot de passe tiré au hasard ne sert qu'à créer le compte :
  # il n'écrase pas celui d'un compte existant (cas d'un déploiement depuis une autre machine).
  KC_RESET_PASSWORD=1
  if [ -z "${APP_PASSWORD:-}" ]; then
    if [ -f "$cred_file" ] && grep -q "^$APP_USER " "$cred_file"; then
      APP_PASSWORD=$(grep "^$APP_USER " "$cred_file" | cut -d' ' -f2)
    else
      APP_PASSWORD=$(openssl rand -hex 8)
      KC_RESET_PASSWORD=0
    fi
  fi
  ( umask 077; echo "$APP_USER $APP_PASSWORD" > "$cred_file" )
  APP_USER="$APP_USER" APP_PASSWORD="$APP_PASSWORD" KC_RESET_PASSWORD="$KC_RESET_PASSWORD" KC_URL="http://$TARGET_HOST:30090" PUBLIC_URL="${PUBLIC_URL:-}" \
    "$(python_cmd)" "$(native_path "$ROOT/script/keycloak-setup.py")" \
                    "$(native_path "$ROOT/k8s/keycloak/realm/realm-export.json")"
}

print_status() {
  log "État final"
  kubectl get pods -n "$NS" -o wide
  kubectl get pods -n default -l app=product-service -o wide
  echo
  ok "Plateforme déployée"
  [ -z "${PUBLIC_URL:-}" ] || echo "  En ligne  : $PUBLIC_URL  (Keycloak : ${KEYCLOAK_PUBLIC_URL:-$PUBLIC_URL})"
  echo "  Front     : http://$TARGET_HOST:30080"
  echo "  Keycloak  : http://$TARGET_HOST:30090  (console admin : admin / admin)"
  echo "  Gateway   : http://$TARGET_HOST:31803"
  if [ -n "${JENKINS_URL:-}" ]; then
    # pas de mot de passe dans la console Jenkins
    echo "  Connexion : $APP_USER (mot de passe dans $MTLS_OUT_DIR/.app-credentials)"
  else
    echo "  Connexion : $APP_USER / $APP_PASSWORD   (conservé dans $MTLS_OUT_DIR/.app-credentials)"
  fi
}
