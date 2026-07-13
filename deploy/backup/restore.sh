#!/usr/bin/env bash
set -euo pipefail

# deploy-vps 7.4: restore a dump into a throwaway database to verify it comes back consistent —
# never restores over the production database.
# Uso: restore.sh <dump.sql.gz> [nome-do-banco-descartavel]

DUMP_FILE="${1:?uso: restore.sh <dump.sql.gz> [db_destino]}"
TARGET_DB="${2:-comanda_restore_test}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_DIR="${COMPOSE_DIR:-$SCRIPT_DIR/../..}"

cd "$COMPOSE_DIR"
set -a
[ -f .env ] && source .env
set +a

docker compose exec -T db psql -U "${POSTGRES_USER}" -d postgres -c "DROP DATABASE IF EXISTS ${TARGET_DB};"
docker compose exec -T db psql -U "${POSTGRES_USER}" -d postgres -c "CREATE DATABASE ${TARGET_DB};"
gunzip -c "$DUMP_FILE" | docker compose exec -T db psql -U "${POSTGRES_USER}" -d "${TARGET_DB}"

echo "Restaurado em '${TARGET_DB}'. Validar manualmente (ex.: contar linhas em tabelas-chave) e depois:"
echo "  docker compose exec db psql -U ${POSTGRES_USER} -d postgres -c 'DROP DATABASE ${TARGET_DB};'"
