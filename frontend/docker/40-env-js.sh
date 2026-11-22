#!/bin/sh
# Exécuté par l'image nginx au démarrage (/docker-entrypoint.d) : écrit dans assets/env.js
# les adresses publiques lues par le front avant son lancement.
#   PUBLIC_URL           adresse du site (le front et /api)
#   KEYCLOAK_PUBLIC_URL  adresse de Keycloak, si elle diffère (ex. sous-domaine dédié)
set -e
TARGET=/usr/share/nginx/html/assets/env.js
URL="${PUBLIC_URL%/}"
KC="${KEYCLOAK_PUBLIC_URL%/}"
printf 'window.__env = { publicUrl: "%s", keycloakUrl: "%s" };\n' "$URL" "$KC" > "$TARGET"
echo "env.js : site=${URL:-localhost} keycloak=${KC:-${URL:-localhost}}"
