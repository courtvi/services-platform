#!/bin/bash

set -e

NAMESPACE="lorrconnect"
CLUSTER="kind"

echo "🚀 STEP 0 - Checking cluster"
kubectl cluster-info

kubectl get namespace $NAMESPACE &>/dev/null || kubectl create namespace $NAMESPACE

echo "📦 STEP 1 - Build Maven + Docker images"

export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"

echo "➡️ Maven build api-gateway"
cd ../api-gateway && mvn clean package -DskipTests && cd ../script

echo "➡️ Maven build eureka-server"
cd ../eureka-server && mvn clean package -DskipTests && cd ../script

echo "➡️ Maven build commande-service"
cd ../commande-service && mvn clean package -DskipTests && cd ../script

echo "➡️ Docker builds"
cd ../frontend && rm -rf dist && cd ../script
docker build --no-cache -t frontend:latest ../frontend
docker build --no-cache -t api-gateway:latest ../api-gateway
docker build --no-cache -t eureka-server:latest ../eureka-server
docker build --no-cache -t commande-service:latest ../commande-service

echo "📥 STEP 2 - Load images into Kind"
kind load docker-image frontend:latest --name $CLUSTER
kind load docker-image api-gateway:latest --name $CLUSTER
kind load docker-image eureka-server:latest --name $CLUSTER
kind load docker-image commande-service:latest --name $CLUSTER

echo "☸️ STEP 3 - Apply Kubernetes manifests"
kubectl apply -f ../k8s/keycloak/ -n $NAMESPACE
kubectl apply -f ../k8s/eureka-server/ -n $NAMESPACE
kubectl apply -f ../k8s/api-gateway/ -n $NAMESPACE
kubectl apply -f ../k8s/commande-service/ -n $NAMESPACE
kubectl apply -f ../k8s/front-end/ -n $NAMESPACE

echo "⏳ STEP 4 - Waiting for pods"
kubectl rollout restart deployment/frontend -n $NAMESPACE
kubectl rollout restart deployment/api-gateway -n $NAMESPACE
kubectl rollout restart deployment/eureka -n $NAMESPACE
kubectl rollout restart deployment/commande-service -n $NAMESPACE

kubectl rollout status deployment/frontend -n $NAMESPACE --timeout=120s
kubectl rollout status deployment/api-gateway -n $NAMESPACE --timeout=120s
kubectl rollout status deployment/eureka -n $NAMESPACE --timeout=120s
kubectl rollout status deployment/commande-service -n $NAMESPACE --timeout=120s

echo "🔍 STEP 5 - Health checks"
kubectl get pods -n $NAMESPACE -o wide
kubectl get svc -n $NAMESPACE

echo "🌐 STEP 6 - URLs"
echo "Frontend:    http://localhost:30080"
echo "Gateway:     http://localhost:31803"
echo "Keycloak:    http://localhost:30090"
echo "Eureka:      http://localhost:8761"

echo "✅ DONE - Cluster ready"

# ✅ STEP 7 - Port-forwards avec auto-restart
echo "🚀 STEP 7 - Port-forwards"
pkill -f "kubectl port-forward" 2>/dev/null || true
sleep 2

nohup kubectl port-forward --address 0.0.0.0 service/keycloak 30090:8080 -n $NAMESPACE > /tmp/pf-keycloak.log 2>&1 &
nohup kubectl port-forward --address 0.0.0.0 service/eureka 8761:8761 -n $NAMESPACE > /tmp/pf-eureka.log 2>&1 &
nohup kubectl port-forward --address 0.0.0.0 service/commande-service 8082:8082 -n $NAMESPACE > /tmp/pf-commande.log 2>&1 &
nohup kubectl port-forward --address 0.0.0.0 service/api-gateway 31803:8090 -n $NAMESPACE > /tmp/pf-gateway.log 2>&1 &
nohup kubectl port-forward --address 0.0.0.0 service/frontend 30080:80 -n $NAMESPACE > /tmp/pf-frontend.log 2>&1 &

sleep 3
echo "✅ Port-forwards actifs !"
echo "Frontend:    http://localhost:30080"
echo "Gateway:     http://localhost:31803"
echo "Keycloak:    http://localhost:30090"

sleep 3
echo "✅ Port-forwards avec auto-restart actifs !"


# ✅ STEP 8  Setup camping haller Keycloak automatiquement
echo "🔐 STEP 8 - Setup camping haller Keycloak user"
# Attendez jusqu'à 120s que le realm soit disponible
for i in $(seq 1 24); do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    http://localhost:30090/realms/lorrconnect)
  if [ "$STATUS" = "200" ]; then
    echo "✅ Realm lorrconnect ready!"
    break
  fi
  echo "⏳ Waiting... ($i/24)"
  sleep 5
done

ADMIN_TOKEN=$(curl -s -X POST http://localhost:30090/realms/master/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=admin-cli&username=admin&password=admin" \
  | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)

CLIENT_ID=$(curl -s "http://localhost:30090/admin/realms/lorrconnect/clients?clientId=haller" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)

# Créez les rôles si absents
curl -s -X POST "http://localhost:30090/admin/realms/lorrconnect/clients/$CLIENT_ID/roles" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"CLIENT"}' 2>/dev/null || true

curl -s -X POST "http://localhost:30090/admin/realms/lorrconnect/clients/$CLIENT_ID/roles" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"ADMIN"}' 2>/dev/null || true

# Créez l'utilisateur
curl -s -X POST "http://localhost:30090/admin/realms/lorrconnect/users" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"username":"vincent.courtois0@gmail.com","email":"vincent.courtois0@gmail.com","firstName":"vincent","lastName":"courtois","enabled":true,"emailVerified":true,"credentials":[{"type":"password","value":"password","temporary":false}]}' 2>/dev/null || true

USER_ID=$(curl -s "http://localhost:30090/admin/realms/lorrconnect/users?username=vincent.courtois0%40gmail.com" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)

CLIENT_ROLE_ID=$(curl -s "http://localhost:30090/admin/realms/lorrconnect/clients/$CLIENT_ID/roles/CLIENT" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)

ADMIN_ROLE_ID=$(curl -s "http://localhost:30090/admin/realms/lorrconnect/clients/$CLIENT_ID/roles/ADMIN" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)

curl -s -X POST "http://localhost:30090/admin/realms/lorrconnect/users/$USER_ID/role-mappings/clients/$CLIENT_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "[{\"id\":\"$CLIENT_ROLE_ID\",\"name\":\"CLIENT\"},{\"id\":\"$ADMIN_ROLE_ID\",\"name\":\"ADMIN\"}]" 2>/dev/null || true

echo "✅ Keycloak user configured!"
echo "➡️  Login: vincent.courtois0@gmail.com / password"

# ✅ STEP 9 - Setup Keycloak realm chabeille (vente de miel à Saulnes)
echo "🐝 STEP 9 - Setup Keycloak realm chabeille"

REALM="chabeille"
CLIENT_NAME="chabeille"

ADMIN_TOKEN=$(curl -s -X POST http://localhost:30090/realms/master/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=admin-cli&username=admin&password=admin" \
  | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)

# 1. Créer le realm (s'il n'existe pas déjà)
curl -s -X POST "http://localhost:30090/admin/realms" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"realm\":\"$REALM\",\"enabled\":true}" 2>/dev/null || true

# 2. Créer le client
curl -s -X POST "http://localhost:30090/admin/realms/$REALM/clients" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"clientId\":\"$CLIENT_NAME\",\"publicClient\":true,\"redirectUris\":[\"*\"],\"webOrigins\":[\"*\"],\"standardFlowEnabled\":true,\"directAccessGrantsEnabled\":true}" 2>/dev/null || true

CLIENT_ID=$(curl -s "http://localhost:30090/admin/realms/$REALM/clients?clientId=$CLIENT_NAME" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)

# 3. Rôles
curl -s -X POST "http://localhost:30090/admin/realms/$REALM/clients/$CLIENT_ID/roles" \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"CLIENT"}' 2>/dev/null || true
curl -s -X POST "http://localhost:30090/admin/realms/$REALM/clients/$CLIENT_ID/roles" \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"ADMIN"}' 2>/dev/null || true

# 4. Premier utilisateur admin
curl -s -X POST "http://localhost:30090/admin/realms/$REALM/users" \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"username":"vincent.courtois0@gmail.com","email":"vincent.courtois0@gmail.com","firstName":"vincent","lastName":"courtois","enabled":true,"emailVerified":true,"credentials":[{"type":"password","value":"password","temporary":false}]}' 2>/dev/null || true

USER_ID=$(curl -s "http://localhost:30090/admin/realms/$REALM/users?username=vincent.courtois0%40gmail.com" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)

CLIENT_ROLE_ID=$(curl -s "http://localhost:30090/admin/realms/$REALM/clients/$CLIENT_ID/roles/CLIENT" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
ADMIN_ROLE_ID=$(curl -s "http://localhost:30090/admin/realms/$REALM/clients/$CLIENT_ID/roles/ADMIN" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)

curl -s -X POST "http://localhost:30090/admin/realms/$REALM/users/$USER_ID/role-mappings/clients/$CLIENT_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d "[{\"id\":\"$CLIENT_ROLE_ID\",\"name\":\"CLIENT\"},{\"id\":\"$ADMIN_ROLE_ID\",\"name\":\"ADMIN\"}]" 2>/dev/null || true

# 5. Second utilisateur admin
curl -s -X POST "http://localhost:30090/admin/realms/$REALM/users" \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"username":"chocobonette@hotmail.fr","email":"chocobonette@hotmail.fr","enabled":true,"emailVerified":true,"credentials":[{"type":"password","value":"password","temporary":false}]}' 2>/dev/null || true

USER_ID_2=$(curl -s "http://localhost:30090/admin/realms/$REALM/users?username=chocobonette%40hotmail.fr" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)

curl -s -X POST "http://localhost:30090/admin/realms/$REALM/users/$USER_ID_2/role-mappings/clients/$CLIENT_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d "[{\"id\":\"$CLIENT_ROLE_ID\",\"name\":\"CLIENT\"},{\"id\":\"$ADMIN_ROLE_ID\",\"name\":\"ADMIN\"}]" 2>/dev/null || true

echo "✅ Realm $REALM configuré !"
echo "➡️  Login 1: vincent.courtois0@gmail.com / password"
echo "➡️  Login 2: chocobonette@hotmail.fr / password"