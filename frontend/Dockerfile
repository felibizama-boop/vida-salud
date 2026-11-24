# =============================================================================
#  VidaSalud - Frontend Angular servido con Nginx
#  EP5 / DSY1107 - Desarrollo Cloud Native I
# -----------------------------------------------------------------------------
#  Build multi-etapa:
#     1) node:22  -> compila la aplicación Angular (configuración production).
#     2) nginx    -> sirve los archivos estáticos en el puerto 80.
#
#  Uso (mediante el compose de la capa de aplicación):
#     docker compose -f infra/apps/compose.yml up -d --build frontend
# =============================================================================

# ---- Etapa 1: compilación Angular ----
FROM node:22-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build -- --configuration production

# ---- Etapa 2: servidor Nginx ----
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
