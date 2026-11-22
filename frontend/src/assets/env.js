// Configuration d'exécution du front. En production, ce fichier est réécrit au démarrage
// du conteneur nginx à partir de la variable PUBLIC_URL (docker/40-env-js.sh).
// Sans PUBLIC_URL : le front utilise les ports localhost (voir environment.ts).
window.__env = window.__env || {};
