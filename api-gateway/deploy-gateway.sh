#!/bin/bash

set -e

NAMESPACE="camping-haller"
CLUSTER="kind"

echo "🔨 Compile api-gateway"
mvn clean package -DskipTests


echo "📦 Build api-gateway"
docker build --no-cache -t api-gateway:latest ../api-gateway

echo "📥 Load into Kind"
kind load docker-image api-gateway:latest --name $CLUSTER

echo "🔄 Restart deployment"
kubectl rollout restart deployment/api-gateway -n $NAMESPACE
kubectl rollout status deployment/api-gateway -n $NAMESPACE --timeout=60s

echo "🔁 Port-forward"
pkill -f "kubectl port-forward service/api-gateway" 2>/dev/null || true
sleep 1
kubectl port-forward service/api-gateway 31803:8090 -n $NAMESPACE &

echo "✅ api-gateway déployé !"
echo "➡️  Gateway: http://localhost:31803"