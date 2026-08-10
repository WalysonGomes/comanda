#!/usr/bin/env bash
# Fallback pra VPS fraca: builda as imagens numa máquina forte (seu PC, não o VPS) e sobe pro
# GitHub Container Registry. O VPS só faz `pull`, nunca compila nada — evita OOM/disco cheio/rede
# lenta durante `mvn package` + `xcaddy build` numa e2-micro (1 vCPU, 1GB RAM).
#
# Pré-requisito: `docker login ghcr.io -u <usuario-github>` com um Personal Access Token
# (classic, escopo write:packages) antes de rodar este script.
#
# Uso:
#   GHCR_USER=walysongomes ./deploy/build-and-push.sh
#   GHCR_USER=walysongomes CADDYFILE=Caddyfile ./deploy/build-and-push.sh   # domínio real (DNS-01)

set -euo pipefail

GHCR_USER="${GHCR_USER:?defina GHCR_USER=<seu-usuario-github-em-minusculo>}"
CADDYFILE="${CADDYFILE:-Caddyfile.sslip}"
TAG="${TAG:-latest}"
APP_DOMAIN="${APP_DOMAIN:?defina APP_DOMAIN=<dominio-raiz-da-aplicacao>}"
VITE_ROOT_HOST_ALIASES="${VITE_ROOT_HOST_ALIASES:-}"

APP_IMAGE="ghcr.io/${GHCR_USER}/comanda-app:${TAG}"
CADDY_IMAGE="ghcr.io/${GHCR_USER}/comanda-caddy:${TAG}"

echo "==> Buildando ${APP_IMAGE}"
docker build \
  --build-arg "APP_DOMAIN=${APP_DOMAIN}" \
  --build-arg "VITE_ROOT_HOST_ALIASES=${VITE_ROOT_HOST_ALIASES}" \
  -t "${APP_IMAGE}" -f Dockerfile .

echo "==> Buildando ${CADDY_IMAGE} (CADDYFILE=${CADDYFILE})"
docker build -t "${CADDY_IMAGE}" --build-arg "CADDYFILE=${CADDYFILE}" ./deploy/caddy

echo "==> Enviando pro GHCR"
docker push "${APP_IMAGE}"
docker push "${CADDY_IMAGE}"

echo "==> Pronto. No VPS:"
echo "    docker login ghcr.io -u ${GHCR_USER}"
echo "    docker compose -f docker-compose.yml -f docker-compose.prebuilt.yml pull"
echo "    docker compose -f docker-compose.yml -f docker-compose.prebuilt.yml up -d"
