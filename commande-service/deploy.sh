#!/bin/bash
cd /mnt/c/Users/bde_v/IdeaProjects/services-platform/commande-service
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
export NAMESPACE="lorrconnect"
SERVICE_NAME=commande-service

echo "📦 Maven build..."
mvn clean package -DskipTests

echo "🐳 Docker build..."
docker build -t commande-service:latest .

echo "🔄 Deployment"
kubectl rollout restart deployment/$SERVICE_NAME -n "$NAMESPACE"

echo "⏳ Attente du rollout"
if ! kubectl rollout status deployment/$SERVICE_NAME -n "$NAMESPACE" --timeout=120s; then
  echo "❌ Rollout échoué"
  kubectl get pods -n "$NAMESPACE" -l app=$SERVICE_NAME -o wide
  kubectl describe pods -n "$NAMESPACE" -l app=$SERVICE_NAME | tail -n 40
  kubectl logs -n "$NAMESPACE" -l app=$SERVICE_NAME --tail=50 --all-containers
  exit 1
fi


kubectl get pods -n "$NAMESPACE" -l app=$SERVICE_NAME -o wide
echo $SERVICE_NAME "démarré"
