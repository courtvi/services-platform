#!/usr/bin/env bash
# Test de fumée après déploiement. Usage : TARGET_HOST=localhost ./smoke-test.sh
set -uo pipefail
HOST="${TARGET_HOST:-localhost}"
FAILED=0
check() {  # check <libellé> <code attendu> <url>
  local code
  code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 20 "$3" || true)
  if [ "$code" = "$2" ]; then echo "OK  $1 ($code)"; else echo "KO  $1 : $code au lieu de $2"; FAILED=1; fi
}
# Catalogue : traverse gateway -> Eureka -> produit-service en mTLS
check "catalogue via la gateway"       200 "http://$HOST:31803/api/products"
check "catalogue de la boutique miel"  200 "http://$HOST:31803/api/products/boutique/miel"
# Sans JWT, la gateway doit refuser l'accès aux commandes
check "commandes sans JWT refusées"    401 "http://$HOST:31803/api/commandes"
check "front"                          200 "http://$HOST:30080/"
check "Keycloak (realm lorrconnect)"   200 "http://$HOST:30090/realms/lorrconnect"

# Adresse publique (tunnel Cloudflare) : mêmes chemins, sur le même domaine
if [ -n "${PUBLIC_URL:-}" ]; then
  P="${PUBLIC_URL%/}"
  check "en ligne : front"                       200 "$P/"
  check "en ligne : catalogue (API)"             200 "$P/api/products"
  check "en ligne : commandes sans JWT refusées" 401 "$P/api/commandes"
  # Keycloak (sous-domaine dédié ou même domaine) : on vérifie le JSON du realm, pas seulement le code HTTP
  K="${KEYCLOAK_PUBLIC_URL:-$P}"; K="${K%/}"
  if curl -s --max-time 20 "$K/realms/lorrconnect" | grep -q '"realm":"lorrconnect"'; then
    echo "OK  en ligne : Keycloak ($K)"
  else
    echo "KO  en ligne : $K/realms/lorrconnect ne mène pas à Keycloak (routes du tunnel Cloudflare ?)"; FAILED=1
  fi
  # La console d'administration de Keycloak ne doit pas être publique
  code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 20 "$K/admin/master/console/" || true)
  case "$code" in
    200) echo "KO  en ligne : console d'administration Keycloak accessible depuis Internet ($K/admin)"; FAILED=1 ;;
    *)   echo "OK  en ligne : console d'administration Keycloak non publiée ($code)" ;;
  esac
fi
exit $FAILED
