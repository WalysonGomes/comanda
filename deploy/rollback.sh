#!/usr/bin/env bash
set -euo pipefail

# deploy-vps 9.4: reimplanta o artefato anterior (design.md "Rollback"). deploy.sh sempre marca a
# imagem em produção como comanda-api:previous antes de trocar — este script devolve essa imagem.
cd "$(dirname "$0")/.."

if ! docker image inspect comanda-api:previous >/dev/null 2>&1; then
	echo "Nenhuma imagem 'comanda-api:previous' encontrada — nada para reverter." >&2
	exit 1
fi

TARGET_IMAGE="$(docker compose config --images app)"
docker tag comanda-api:previous "$TARGET_IMAGE"
docker compose up -d --no-build app
echo "Revertido para a imagem anterior ($TARGET_IMAGE)."
