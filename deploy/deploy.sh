#!/usr/bin/env bash
set -euo pipefail

# deploy-vps 9.3/9.4: build a new image and switch the app service to it, keeping the image that
# was running as `comanda-api:previous` so rollback.sh has something to go back to.
cd "$(dirname "$0")/.."

RUNNING_ID="$(docker compose images -q app 2>/dev/null || true)"
if [ -n "$RUNNING_ID" ]; then
	docker tag "$RUNNING_ID" comanda-api:previous
	echo "Imagem em produção marcada como comanda-api:previous (rollback disponível)."
fi

docker compose build app
docker compose up -d app
echo "Deploy concluído. Para reverter: ./deploy/rollback.sh"
