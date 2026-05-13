Write-Host "Demarrage des port-forwards..." -ForegroundColor Green

# ✅ Keycloak — port 30090 (cohérent avec K8s NodePort)
Start-Process powershell -ArgumentList "-NoExit", "-Command", "kubectl port-forward service/keycloak 30090:8080 -n camping-haller"
Start-Sleep -Seconds 2

# ✅ Eureka
Start-Process powershell -ArgumentList "-NoExit", "-Command", "kubectl port-forward service/eureka-server 8761:8761 -n camping-haller"
Start-Sleep -Seconds 2

# ✅ commande-service
Start-Process powershell -ArgumentList "-NoExit", "-Command", "kubectl port-forward service/commande-service 8082:8082 -n camping-haller"
Start-Sleep -Seconds 2

# ✅ API Gateway — port 31803
Start-Process powershell -ArgumentList "-NoExit", "-Command", "kubectl port-forward service/api-gateway 31803:8090 -n camping-haller"
Start-Sleep -Seconds 2

# ✅ Frontend — port 30080
Start-Process powershell -ArgumentList "-NoExit", "-Command", "kubectl port-forward service/frontend 30080:80 -n camping-haller"
Start-Sleep -Seconds 2

Write-Host "Tous les port-forwards sont actifs !" -ForegroundColor Green
Write-Host "Frontend    : http://localhost:30080" -ForegroundColor Cyan
Write-Host "Keycloak    : http://localhost:30090" -ForegroundColor Cyan
Write-Host "Eureka      : http://localhost:8761"  -ForegroundColor Cyan
Write-Host "API Gateway : http://localhost:31803" -ForegroundColor Cyan