#!/bin/bash
set -e

NAMESPACE="lorrconnect"
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo ">>> Vérifications préalables..."

if ! kubectl get namespace $NAMESPACE > /dev/null 2>&1; then
  echo "❌ Namespace '$NAMESPACE' introuvable"
  exit 1
fi

if ! kubectl get secret postgres-commande-secret -n $NAMESPACE > /dev/null 2>&1; then
  echo "❌ Secret 'postgres-commande-secret' introuvable"
  exit 1
fi

if ! kubectl get secret mail-secret -n $NAMESPACE > /dev/null 2>&1; then
  echo "❌ Secret 'mail-secret' introuvable"
  exit 1
fi

if ! kubectl get pvc postgres-commande-pvc -n $NAMESPACE > /dev/null 2>&1; then
  echo "❌ PVC 'postgres-commande-pvc' introuvable"
  exit 1
fi

echo "✅ Vérifications OK"

echo ">>> Déploiement PostgreSQL commande..."
kubectl apply -f "$DIR/postgres.yml"

echo ">>> Déploiement commande-service..."
kubectl apply -f "$DIR/deployment.yml"

echo "✅ PostgreSQL et commande-service déployés"