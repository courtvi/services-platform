#!/bin/bash

set -e

NAMESPACE="camping-haller"

echo "🔧 STEP -1 - Chargement des variables OVH"
if [ ! -f ../k8s/.ovh-env ]; then
  echo "❌ ../k8s/.ovh-env introuvable. Lance d'abord : ./ovh-info.sh status (ou start)"
  exit 1
fi
source ../k8s/.ovh-env
echo "✅ IP OVH : $OVH_PUBLIC_IP ($OVH_NIP_HOST)"

echo "📝 STEP -0.5 - Génération des fichiers depuis les templates"
sed "s/__OVH_HOST__/$OVH_NIP_HOST/g" ../k8s/keycloak/deployment-ovh.yml.template > ../k8s/keycloak/deployment-ovh.yml
sed "s/__OVH_HOST__/$OVH_NIP_HOST/g" ../k8s/api-gateway/deployment-ovh.yml.template > ../k8s/api-gateway/deployment-ovh.yml
sed "s/__OVH_HOST__/$OVH_NIP_HOST/g" ../k8s/commande-service/deployment-ovh.yml.template > ../k8s/commande-service/deployment-ovh.yml
sed "s#__OVH_URL__#https://$OVH_NIP_HOST#g" ../frontend/src/app/environments/environment.ovh.ts.template > ../frontend/src/app/environments/environment.ovh.ts
echo "✅ Fichiers générés avec l'hôte $OVH_NIP_HOST"

echo "🚀 STEP 0 - Checking cluster"
kubectl cluster-info

kubectl get namespace $NAMESPACE &>/dev/null || kubectl create namespace $NAMESPACE

echo "📦 STEP 1 - Build Maven + Docker images"

# ⚠️ Vérifie ce chemin sur le serveur OVH, il vient de la config locale
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

echo "☸️ STEP 2 - Apply Kubernetes manifests (variantes OVH)"
kubectl apply -f ../k8s/keycloak/postgres.yml
kubectl apply -f ../k8s/keycloak/deployment-ovh.yml
kubectl apply -f ../k8s/keycloak/service.yml

kubectl apply -f ../k8s/eureka-server/deployment.yml
kubectl apply -f ../k8s/eureka-server/service.yml

kubectl apply -f ../k8s/api-gateway/deployment-ovh.yml
kubectl apply -f ../k8s/api-gateway/service.yml

kubectl apply -f ../k8s/commande-service/postgres.yml
kubectl apply -f ../k8s/commande-service/deployment-ovh.yml
kubectl apply -f ../k8s/commande-service/service.yml

kubectl apply -f ../k8s/front-end/deployment.yml
kubectl apply -f ../k8s/front-end/service.yml

echo "⏳ STEP 3 - Restart + wait"
kubectl rollout restart deployment/keycloak -n $NAMESPACE
kubectl rollout restart deployment/frontend -n $NAMESPACE
kubectl rollout restart deployment/api-gateway -n $NAMESPACE
kubectl rollout restart deployment/eureka -n $NAMESPACE
kubectl rollout restart deployment/commande-service -n $NAMESPACE

kubectl rollout status deployment/keycloak -n $NAMESPACE --timeout=180s
kubectl rollout status deployment/eureka -n $NAMESPACE --timeout=120s
kubectl rollout status deployment/commande-service -n $NAMESPACE --timeout=120s
kubectl rollout status deployment/api-gateway -n $NAMESPACE --timeout=120s
kubectl rollout status deployment/frontend -n $NAMESPACE --timeout=120s

echo "🔍 STEP 4 - Health checks"
kubectl get pods -n $NAMESPACE -o wide
kubectl get svc -n $NAMESPACE

echo "🌐 STEP 5 - URLs"
echo "Frontend:    https://$OVH_NIP_HOST"
echo "Keycloak:    https://$OVH_NIP_HOST/realms/camping-haller | /realms/chabeille"

echo "✅ DONE - OVH cluster ready"

# STEP 6 - Setup camping-haller Keycloak (idempotent, ne casse rien si déjà fait)
echo "🔐 STEP 6 - Setup camping-haller Keycloak"
for i in $(seq 1 24); do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:30090/realms/camping-haller)
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

CLIENT_ID=$(curl -s "http://localhost:30090/admin/realms/camping-haller/clients?clientId=haller" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)

curl -s -X POST "http://localhost:30090/admin/realms/camping-haller/clients/$CLIENT_ID/roles" \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"CLIENT"}' 2>/dev/null || true
curl -s -X POST "http://localhost:30090/admin/realms/camping-haller/clients/$CLIENT_ID/roles" \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"ADMIN"}' 2>/dev/null || true

curl -s -X POST "http://localhost:30090/admin/realms/camping-haller/users" \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"username":"vincent.courtois0@gmail.com","email":"vincent.courtois0@gmail.com","firstName":"vincent","lastName":"courtois","enabled":true,"emailVerified":true,"credentials":[{"type":"password","value":"password","temporary":false}]}' 2>/dev/null || true

USER_ID=$(curl -s "http://localhost:30090/admin/realms/camping-haller/users?username=vincent.courtois0%40gmail.com" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)

CLIENT_ROLE_ID=$(curl -s "http://localhost:30090/admin/realms/camping-haller/clients/$CLIENT_ID/roles/CLIENT" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
ADMIN_ROLE_ID=$(curl -s "http://localhost:30090/admin/realms/camping-haller/clients/$CLIENT_ID/roles/ADMIN" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)

curl -s -X POST "http://localhost:30090/admin/realms/camping-haller/users/$USER_ID/role-mappings/clients/$CLIENT_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d "[{\"id\":\"$CLIENT_ROLE_ID\",\"name\":\"CLIENT\"},{\"id\":\"$ADMIN_ROLE_ID\",\"name\":\"ADMIN\"}]" 2>/dev/null || true

echo "✅ camping-haller Keycloak configured!"

# STEP 7 - Setup chabeille realm (idempotent)
echo "🐝 STEP 7 - Setup chabeille realm"
REALM="chabeille"
CLIENT_NAME="chabeille"

curl -s -X POST "http://localhost:30090/admin/realms" \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d "{\"realm\":\"$REALM\",\"enabled\":true}" 2>/dev/null || true

curl -s -X POST "http://localhost:30090/admin/realms/$REALM/clients" \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d "{\"clientId\":\"$CLIENT_NAME\",\"publicClient\":true,\"redirectUris\":[\"*\"],\"webOrigins\":[\"*\"],\"standardFlowEnabled\":true,\"directAccessGrantsEnabled\":true}" 2>/dev/null || true

CLIENT_ID=$(curl -s "http://localhost:30090/admin/realms/$REALM/clients?clientId=$CLIENT_NAME" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)

curl -s -X POST "http://localhost:30090/admin/realms/$REALM/clients/$CLIENT_ID/roles" \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"CLIENT"}' 2>/dev/null || true
curl -s -X POST "http://localhost:30090/admin/realms/$REALM/clients/$CLIENT_ID/roles" \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"ADMIN"}' 2>/dev/null || true

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
