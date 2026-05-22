#!/bin/bash

set -e

NAMESPACE="camping-haller"

echo "🚀 STEP 0 - Checking cluster"
kubectl cluster-info

kubectl get namespace $NAMESPACE &>/dev/null || kubectl create namespace $NAMESPACE

echo "📦 STEP 1 - Build Docker images (offline)"
docker build --pull=false -t frontend:latest ../frontend
docker build --pull=false -t api-gateway:latest ../api-gateway
docker build --pull=false -t eureka-server:latest ../eureka-server
docker build --pull=false -t commande-service:latest ../commande-service

echo "☸️ STEP 2 - Restart Kubernetes deployments"
kubectl rollout restart deployment/frontend -n $NAMESPACE
kubectl rollout restart deployment/api-gateway -n $NAMESPACE
kubectl rollout restart deployment/eureka -n $NAMESPACE
kubectl rollout restart deployment/commande-service -n $NAMESPACE

echo "⏳ STEP 3 - Waiting for pods"
kubectl rollout status deployment/keycloak -n $NAMESPACE --timeout=180s
kubectl rollout status deployment/eureka -n $NAMESPACE --timeout=60s
kubectl rollout status deployment/commande-service -n $NAMESPACE --timeout=60s
kubectl rollout status deployment/api-gateway -n $NAMESPACE --timeout=60s
kubectl rollout status deployment/frontend -n $NAMESPACE --timeout=60s


echo "⏳ STEP 4 - Waiting for pods"
kubectl rollout status deployment/keycloak -n $NAMESPACE --timeout=180s
kubectl rollout status deployment/eureka -n $NAMESPACE --timeout=60s
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
echo "Eureka:      http://localhost:8761"

echo "✅ DONE - Cluster ready"

echo "🚀 STEP 7 - Port-forwards"
# ✅ Tuez les anciens port-forwards avant de relancer
pkill -f "kubectl port-forward" 2>/dev/null || true
sleep 1

kubectl port-forward service/keycloak 30090:8080 -n $NAMESPACE &
kubectl port-forward service/eureka 8761:8761 -n $NAMESPACE &
kubectl port-forward service/commande-service 8082:8082 -n $NAMESPACE &
kubectl port-forward service/api-gateway 31803:8090 -n $NAMESPACE &
kubectl port-forward service/frontend 30080:80 -n $NAMESPACE &

sleep 3
echo "✅ Tous les port-forwards sont actifs !"
echo "➡️  Ouvrez http://localhost:30080 dans Chrome"

# ✅ STEP 8 - Recréez l'utilisateur Keycloak automatiquement
echo "🔐 STEP 8 - Setup Keycloak user"
# Attendez jusqu'à 120s que le realm soit disponible
for i in $(seq 1 24); do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    http://localhost:30090/realms/camping-haller)
  if [ "$STATUS" = "200" ]; then
    echo "✅ Realm camping-haller ready!"
    break
  fi
  echo "⏳ Waiting... ($i/24)"
  sleep 5
done

ADMIN_TOKEN=$(curl -s -X POST http://localhost:30090/realms/master/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=admin-cli&username=admin&password=admin" \
  | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)

ADMIN_TOKEN=$(curl -s -X POST http://localhost:30090/realms/master/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=admin-cli&username=admin&password=admin" \
  | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)

CLIENT_ID=$(curl -s "http://localhost:30090/admin/realms/camping-haller/clients?clientId=haller" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)

# Créez les rôles si absents
curl -s -X POST "http://localhost:30090/admin/realms/camping-haller/clients/$CLIENT_ID/roles" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"CLIENT"}' 2>/dev/null || true

curl -s -X POST "http://localhost:30090/admin/realms/camping-haller/clients/$CLIENT_ID/roles" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"ADMIN"}' 2>/dev/null || true

# Créez l'utilisateur
curl -s -X POST "http://localhost:30090/admin/realms/camping-haller/users" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"username":"vincent.courtois0@gmail.com","email":"vincent.courtois0@gmail.com","firstName":"vincent","lastName":"courtois","enabled":true,"emailVerified":true,"credentials":[{"type":"password","value":"password","temporary":false}]}' 2>/dev/null || true

USER_ID=$(curl -s "http://localhost:30090/admin/realms/camping-haller/users?username=vincent.courtois0%40gmail.com" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)

CLIENT_ROLE_ID=$(curl -s "http://localhost:30090/admin/realms/camping-haller/clients/$CLIENT_ID/roles/CLIENT" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)

ADMIN_ROLE_ID=$(curl -s "http://localhost:30090/admin/realms/camping-haller/clients/$CLIENT_ID/roles/ADMIN" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)

curl -s -X POST "http://localhost:30090/admin/realms/camping-haller/users/$USER_ID/role-mappings/clients/$CLIENT_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "[{\"id\":\"$CLIENT_ROLE_ID\",\"name\":\"CLIENT\"},{\"id\":\"$ADMIN_ROLE_ID\",\"name\":\"ADMIN\"}]" 2>/dev/null || true

echo "✅ Keycloak user configured!"
echo "➡️  Login: vincent.courtois0@gmail.com / password"