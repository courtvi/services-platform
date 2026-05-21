#!/bin/bash

set -e

cd "/mnt/c/Users/bde_v/IdeaProjects/services-platform/frontend"

echo "🔨 Build Angular"
ng build --configuration=production

echo "📦 Build Docker"
docker build -t frontend:latest .

echo "📥 Load into Kind"
kind load docker-image frontend:latest --name kind

echo "🔄 Restart deployment"
kubectl rollout restart deployment/frontend -n camping-haller
kubectl rollout status deployment/frontend -n camping-haller --timeout=60s

echo "🔁 Port-forward"
pkill -f "kubectl port-forward service/frontend" 2>/dev/null || true
sleep 1
kubectl port-forward service/frontend 30080:80 -n camping-haller &

echo "✅ Frontend déployé !"
echo "➡️  http://localhost:30080"
