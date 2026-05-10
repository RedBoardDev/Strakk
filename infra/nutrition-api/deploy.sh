#!/usr/bin/env bash
set -euo pipefail

# Deploy nutrition-api to VPS
#
# Usage:
#   ./deploy.sh <alias>                   — uses SSH config alias
#   ./deploy.sh <host> <port> <user>      — explicit connection
#   ./deploy.sh vps --force               — redeploy + force re-import
#
# Examples:
#   ./deploy.sh vps
#   ./deploy.sh vps --force
#   ./deploy.sh 1.2.3.4 22 ubuntu

FORCE=0
ARGS=()
for arg in "$@"; do
  if [ "${arg}" = "--force" ]; then FORCE=1; else ARGS+=("${arg}"); fi
done

if [ ${#ARGS[@]} -eq 1 ]; then
  SSH_TARGET="${ARGS[0]}"
  BASE_SSH_OPTS=()
elif [ ${#ARGS[@]} -eq 3 ]; then
  SSH_TARGET="${ARGS[2]}@${ARGS[0]}"
  BASE_SSH_OPTS=(-p "${ARGS[1]}")
else
  echo "Usage: $0 <alias>  OR  $0 <host> <port> <user>  [--force]"
  exit 1
fi

REMOTE_DIR="~/nutrition-api"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONTROL_SOCKET="/tmp/deploy-nutrition-api-$$"

SSH_OPTS=("${BASE_SSH_OPTS[@]+"${BASE_SSH_OPTS[@]}"}" \
  -o ControlMaster=auto \
  -o "ControlPath=${CONTROL_SOCKET}" \
  -o ControlPersist=120)

cleanup() { ssh "${SSH_OPTS[@]}" -O exit "${SSH_TARGET}" 2>/dev/null || true; }
trap cleanup EXIT

echo "==> Deploying to ${SSH_TARGET}:${REMOTE_DIR}"

ssh "${SSH_OPTS[@]}" "${SSH_TARGET}" \
  "mkdir -p ${REMOTE_DIR}/src ${REMOTE_DIR}/config ${REMOTE_DIR}/import"

RSYNC_SSH="ssh $(printf '%q ' "${SSH_OPTS[@]}")"

rsync -az --delete -e "${RSYNC_SSH}" \
  --exclude='.env' --exclude='node_modules' --exclude='.git' \
  "${SCRIPT_DIR}/src/"    "${SSH_TARGET}:${REMOTE_DIR}/src/"

rsync -az -e "${RSYNC_SSH}" "${SCRIPT_DIR}/config/" "${SSH_TARGET}:${REMOTE_DIR}/config/"
rsync -az -e "${RSYNC_SSH}" "${SCRIPT_DIR}/import/" "${SSH_TARGET}:${REMOTE_DIR}/import/"

rsync -az -e "${RSYNC_SSH}" \
  "${SCRIPT_DIR}/docker-compose.yml" \
  "${SCRIPT_DIR}/Dockerfile" \
  "${SCRIPT_DIR}/deno.json" \
  "${SSH_TARGET}:${REMOTE_DIR}/"

if [ -f "${SCRIPT_DIR}/.env" ]; then
  rsync -az -e "${RSYNC_SSH}" "${SCRIPT_DIR}/.env" "${SSH_TARGET}:${REMOTE_DIR}/.env"
else
  echo "WARNING: no .env found locally — make sure ${REMOTE_DIR}/.env exists on the server"
fi

echo "==> Building and starting services"
ssh "${SSH_OPTS[@]}" "${SSH_TARGET}" "cd ${REMOTE_DIR} && docker compose up --build -d"

echo "==> Health check..."
sleep 8
HEALTH=$(ssh "${SSH_OPTS[@]}" "${SSH_TARGET}" "curl -sf http://localhost:8743/healthz")
echo "${HEALTH}"

ITEMS=$(echo "${HEALTH}" | grep -o '"items_count":[0-9]*' | grep -o '[0-9]*' || echo "0")

if [ "${ITEMS}" = "0" ] || [ "${FORCE}" = "1" ]; then
  if [ "${FORCE}" = "1" ] && [ "${ITEMS}" != "0" ]; then
    echo "==> --force: clearing Qdrant collection..."
    ssh "${SSH_OPTS[@]}" "${SSH_TARGET}" \
      "curl -sf -X DELETE http://localhost:6333/collections/food_catalog || true"
    sleep 2
  fi
  echo "==> Running food catalog import..."
  ssh "${SSH_OPTS[@]}" "${SSH_TARGET}" "cd ${REMOTE_DIR} && docker compose exec -T nutrition-api deno task import"
  echo "==> Import done."
else
  echo "==> Qdrant already has ${ITEMS} items — skipping import. Use --force to re-import."
fi

echo "==> Done."
