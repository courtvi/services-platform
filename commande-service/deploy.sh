#!/bin/bash
cd /mnt/c/Users/bde_v/IdeaProjects/services-platform/commande-service
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"

echo "📦 Maven build..."
mvn clean package -DskipTests

echo "🐳 Docker build..."
docker build -t commande-service:latest .

echo "📥 Kind load..."
kind load docker-image commande-service:latest --name kind

echo "🔁 Restart..."
kubectl rollout restart deployment/commande-service -n lorrconnect
kubectl rollout status deployment/commande-service -n lorrconnect
