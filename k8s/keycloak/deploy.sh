#!/bin/bash
set -e

NAMESPACE="lorrconnect"
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo ">>> Vérifications préalables..."

if ! kubectl get namespace $NAMESPACE > /dev/null 2>&1; then
  echo "❌ Namespace '$NAMESPACE' introuvable"
  exit 1
fi

if ! kubectl get secret postgres-secret -n $NAMESPACE > /dev/null 2>&1; then
  echo "❌ Secret 'postgres-secret' introuvable"
  exit 1
fi

if ! kubectl get pvc postgres-pvc -n $NAMESPACE > /dev/null 2>&1; then
  echo "❌ PVC 'postgres-pvc' introuvable"
  exit 1
fi

echo "✅ Vérifications OK"

echo ">>> Déploiement PostgreSQL..."
kubectl apply -f "$DIR/postgres.yml"

echo ">>> Déploiement Keycloak..."
kubectl apply -f "$DIR/deployment.yml"
kubectl apply -f "$DIR/service.yml"

echo "✅ PostgreSQL et Keycloak déployés"
