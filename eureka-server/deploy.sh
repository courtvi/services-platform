#!/bin/bash

set -e

SERVICE_NAME=$1
NAMESPACE="lorrconnect"
IMAGE_NAME="$SERVICE_NAME:latest"

if [ -z "$SERVICE_NAME" ]; then
  echo "❌ Usage: ./deploy.sh <service-name>"
  exit 1
fi

echo "🚀 Deploying $SERVICE_NAME..."

# 1. Maven build
echo "📦 Step 1: Maven build"
mvn clean package -DskipTests

# 2. Docker build
echo "🐳 Step 2: Docker build"
docker build -t $IMAGE_NAME .

# 3. Load image into Kind
echo "📦 Step 3: Load image into Kind"
kind load docker-image $IMAGE_NAME

# 4. Apply Kubernetes manifests
echo "☸️ Step 4: Apply Kubernetes manifests"

kubectl apply -f k8s/ -n $NAMESPACE

# 5. Restart deployment if exists
echo "🔁 Step 5: Restart deployment"

if kubectl get deployment $SERVICE_NAME -n $NAMESPACE >/dev/null 2>&1; then
  kubectl rollout restart deployment $SERVICE_NAME -n $NAMESPACE
else
  echo "⚠️ Deployment $SERVICE_NAME not found"
fi

# 6. Wait rollout
echo "⏳ Step 6: Waiting for rollout..."
kubectl rollout status deployment/$SERVICE_NAME -n $NAMESPACE || true

# 7. Show logs
echo "📜 Step 7: Logs"
POD=$(kubectl get pods -n $NAMESPACE -l app=$SERVICE_NAME -o jsonpath="{.items[0].metadata.name}" 2>/dev/null || true)

if [ ! -z "$POD" ]; then
  kubectl logs $POD -n $NAMESPACE --tail=50
else
  echo "⚠️ No pod found"
fi

echo "✅ Deployment finished for $SERVICE_NAME"
