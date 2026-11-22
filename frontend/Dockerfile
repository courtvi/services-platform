FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build -- --configuration=production

FROM nginx:alpine
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
# Configuration d'exécution : PUBLIC_URL → assets/env.js au démarrage du conteneur
COPY --chmod=755 docker/40-env-js.sh /docker-entrypoint.d/40-env-js.sh
# nginx tourne en root dans cette image : il peut réécrire env.js
EXPOSE 80
