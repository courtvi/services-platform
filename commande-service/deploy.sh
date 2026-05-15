#!/bin/bash

SERVICE=$1

if [ -z "$SERVICE" ]; then
  echo "❌ Usage: ./deploy.sh <service-name>"
  exit 1
fi

echo "🚀 Deploying $SERVICE..."

PROJECT_DIR="/mnt/c/Users/bde_v/IdeaProjects/services-platform/$SERVICE"

echo "📦 Step 1: Maven build"
cd "$PROJECT_DIR" || exit
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
  echo "❌ Maven build failed"
  exit 1
fi

echo "🐳 Step 2: Docker build"
docker build -t $SERVICE:latest .

if [ $? -ne 0 ]; then
  echo "❌ Docker build failed"
  exit 1
fi

echo "📦 Step 3: Load image into Kind"
kind load docker-image $SERVICE:latest

echo "🔁 Step 4: Restart Kubernetes deployment"
kubectl rollout restart deployment $SERVICE -n camping-haller

echo "⏳ Step 5: Waiting for rollout..."
kubectl rollout status deployment $SERVICE -n camping-haller

echo "📜 Step 6: Last logs"
kubectl logs deployment/$SERVICE -n camping-haller --tail=50

echo "✅ Deployment of $SERVICE completed!"
