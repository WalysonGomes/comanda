#!/usr/bin/env bash
set -euo pipefail

# deploy-vps 7.1-7.3: daily Postgres dump + off-site copy + rotation. Cron runs this via
# comanda-backup.cron; COMPOSE_DIR must point at the directory holding docker-compose.yml + .env.
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_DIR="${COMPOSE_DIR:-$SCRIPT_DIR/../..}"
BACKUP_DIR="${BACKUP_DIR:-/var/comanda/backups}"

cd "$COMPOSE_DIR"
set -a
[ -f .env ] && source .env
set +a

RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"

mkdir -p "$BACKUP_DIR"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
DUMP_FILE="$BACKUP_DIR/comanda-$STAMP.sql.gz"

docker compose exec -T db pg_dump -U "${POSTGRES_USER}" "${POSTGRES_DB}" | gzip > "$DUMP_FILE"
echo "Dump gerado: $DUMP_FILE ($(du -h "$DUMP_FILE" | cut -f1))"

if [ -n "${BACKUP_RCLONE_REMOTE:-}" ]; then
	rclone copy "$DUMP_FILE" "${BACKUP_RCLONE_REMOTE}"
	echo "Cópia off-site enviada para ${BACKUP_RCLONE_REMOTE}"
else
	echo "AVISO: BACKUP_RCLONE_REMOTE não definido — dump ficou só local em $BACKUP_DIR, sem cópia off-site." >&2
fi

# Rotação: mantém apenas os últimos $RETENTION_DAYS dias de dumps locais.
find "$BACKUP_DIR" -name 'comanda-*.sql.gz' -mtime "+${RETENTION_DAYS}" -delete
