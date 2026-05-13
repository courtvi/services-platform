#!/bin/bash

set -e

NAMESPACE="camping-haller"
CLUSTER="kind"

echo "🚀 STEP 0 - Checking cluster"
kubectl cluster-info

# Crée le namespace s'il n'existe pas
kubectl get namespace $NAMESPACE &>/dev/null || kubectl create namespace $NAMESPACE

echo "📦 STEP 1 - Build Docker images"

echo "➡️ Building frontend"
docker build -t frontend:latest ../frontend

echo "➡️ Building api-gateway"
docker build -t api-gateway:latest ../api-gateway

echo "➡️ Building eureka-server"
docker build -t eureka-server:latest ../eureka-server

echo "➡️ Building commande-service"
docker build -t commande-service:latest ../commande-service

echo "📥 STEP 2 - Load images into Kind"

kind load docker-image frontend:latest --name $CLUSTER
kind load docker-image api-gateway:latest --name $CLUSTER
kind load docker-image eureka-server:latest --name $CLUSTER
kind load docker-image commande-service:latest --name $CLUSTER

echo "☸️ STEP 3 - Apply Kubernetes manifests"

# ✅ Keycloak en premier — les autres services en dépendent
kubectl apply -f ../k8s/keycloak/ -n $NAMESPACE
kubectl apply -f ../k8s/eureka-server/ -n $NAMESPACE
kubectl apply -f ../k8s/api-gateway/ -n $NAMESPACE
kubectl apply -f ../k8s/commande-service/ -n $NAMESPACE
kubectl apply -f ../k8s/front-end/ -n $NAMESPACE

echo "⏳ STEP 4 - Waiting for pods"

kubectl rollout status deployment/keycloak -n $NAMESPACE --timeout=120s
kubectl rollout status deployment/eureka-server -n $NAMESPACE --timeout=60s
kubectl rollout status deployment/commande-service -n $NAMESPACE --timeout=60s
kubectl rollout status deployment/api-gateway -n $NAMESPACE --timeout=60s
kubectl rollout status deployment/frontend -n $NAMESPACE --timeout=60s

echo "🔍 STEP 5 - Health checks"

kubectl get pods -n $NAMESPACE -o wide
kubectl get svc -n $NAMESPACE

echo "🌐 STEP 6 - URLs"
echo "Frontend:    http://localhost:30080"
echo "Gateway:     http://localhost:31803"
echo "Keycloak:    http://localhost:30090"
echo "Eureka:      http://localhost:31311"

echo "✅ DONE - Cluster ready"

echo "🚀 STEP 7 - port-forwards"
kubectl port-forward service/keycloak 30090:8080 -n camping-haller &
kubectl port-forward service/eureka-service 8761:8761 -n camping-haller &
kubectl port-forward service/commande-service 8082:8082 -n camping-haller &
kubectl port-forward service/api-gateway 31803:8090 -n camping-haller &
kubectl port-forward service/frontend 30080:80 -n camping-haller &

echo "✅ Tous les port-forwards sont actifs !"
