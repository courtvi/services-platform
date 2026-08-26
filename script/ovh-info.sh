#!/bin/bash
# Usage: ./ovh-info.sh [status|start|stop]
#
# Prérequis (une seule fois, interactif) :
#   curl -fsSL https://raw.githubusercontent.com/ovh/ovhcloud-cli/main/install.sh | sh
#   ovhcloud login
#
# Ce script :
#   - retrouve le projet cloud et l'instance lorrconnect
#   - démarre/arrête l'instance si demandé
#   - récupère IP publique/privée + statut
#   - écrit ces infos dans k8s/.ovh-env (source-able par deploy-ovh.sh)

set -e

ACTION="${1:-status}"

if ! command -v ovhcloud &> /dev/null; then
  echo "❌ 'ovhcloud' introuvable. Installe-le avec :"
  echo "   curl -fsSL https://raw.githubusercontent.com/ovh/ovhcloud-cli/main/install.sh | sh"
  exit 1
fi

if ! command -v jq &> /dev/null; then
  echo "❌ 'jq' introuvable. Installe-le avec : sudo apt install jq"
  exit 1
fi

echo "🔑 STEP 0 - Vérification du projet cloud"
PROJECT_ID=$(ovhcloud cloud project list -o json | jq -r '.[0].project_id // .[0].id // empty')

if [ -z "$PROJECT_ID" ]; then
  echo "❌ Impossible de récupérer le project ID."
  echo "   As-tu bien lancé 'ovhcloud login' au préalable ?"
  exit 1
fi
echo "✅ Project ID: $PROJECT_ID"

echo "🔍 STEP 1 - Recherche de l'instance"
# ID connu de l'instance lorrconnect (nommée "b2-7-gra11" côté OVH, d'où le besoin de le fixer en dur)
INSTANCE_ID="97855841-0854-436a-8050-478023ea2c90"

# Vérifie qu'elle existe toujours sous cet ID dans ce projet
if ! ovhcloud cloud instance get --cloud-project "$PROJECT_ID" "$INSTANCE_ID" -o json &>/dev/null; then
  echo "❌ Instance $INSTANCE_ID introuvable dans le projet $PROJECT_ID. Liste complète :"
  ovhcloud cloud instance list --cloud-project "$PROJECT_ID"
  exit 1
fi
echo "✅ Instance ID: $INSTANCE_ID"

case "$ACTION" in
  start)
    echo "▶️  STEP 2 - Démarrage de l'instance..."
    ovhcloud cloud instance start --cloud-project "$PROJECT_ID" "$INSTANCE_ID"
    echo "⏳ Attente du statut ACTIVE..."
    for i in $(seq 1 24); do
      STATUS=$(ovhcloud cloud instance get --cloud-project "$PROJECT_ID" "$INSTANCE_ID" -o json | jq -r '.status')
      echo "   [$i/24] Statut: $STATUS"
      [ "$STATUS" == "ACTIVE" ] && break
      sleep 5
    done
    ;;
  stop)
    echo "⏹️  STEP 2 - Arrêt de l'instance..."
    ovhcloud cloud instance stop --cloud-project "$PROJECT_ID" "$INSTANCE_ID"
    echo "✅ Commande d'arrêt envoyée."
    exit 0
    ;;
  status)
    echo "ℹ️  STEP 2 - Lecture du statut (aucune action)"
    ;;
  *)
    echo "❌ Action inconnue: $ACTION (attendu: status|start|stop)"
    exit 1
    ;;
esac

echo "📡 STEP 3 - Récupération des infos de l'instance"
DETAILS=$(ovhcloud cloud instance get --cloud-project "$PROJECT_ID" "$INSTANCE_ID" -o json)

STATUS=$(echo "$DETAILS" | jq -r '.status')
PUBLIC_IP=$(echo "$DETAILS" | jq -r '[.ipAddresses[]? | select(.type=="public" and .version==4)][0].ip // empty')
PRIVATE_IP=$(echo "$DETAILS" | jq -r '[.ipAddresses[]? | select(.type=="private" and .version==4)][0].ip // empty')
FLAVOR=$(echo "$DETAILS" | jq -r '.flavor.name // .flavorId // "?"')

echo ""
echo "✅ Statut       : $STATUS"
echo "✅ IP publique  : ${PUBLIC_IP:-<aucune - instance probablement arrêtée>}"
echo "✅ IP privée    : ${PRIVATE_IP:-?}"
echo "✅ Flavor       : $FLAVOR"

if [ -z "$PUBLIC_IP" ]; then
  echo ""
  echo "⚠️  Pas d'IP publique récupérée (instance arrêtée ou champ JSON différent)."
  echo "    Sortie brute pour diagnostic :"
  echo "$DETAILS" | jq '.'
  exit 1
fi

mkdir -p ../k8s
cat > ../k8s/.ovh-env <<EOF
export OVH_PROJECT_ID="$PROJECT_ID"
export OVH_INSTANCE_ID="$INSTANCE_ID"
export OVH_PUBLIC_IP="$PUBLIC_IP"
export OVH_PRIVATE_IP="$PRIVATE_IP"
export OVH_NIP_HOST="${PUBLIC_IP}.nip.io"
EOF

echo ""
echo "📝 Variables écrites dans k8s/.ovh-env"
echo "   Charge-les avec : source ../k8s/.ovh-env"
