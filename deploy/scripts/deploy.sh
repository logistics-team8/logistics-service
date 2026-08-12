#!/usr/bin/env bash
set -Eeuo pipefail

usage() {
  echo "Usage: $0 --manifest <candidate.json> --compose <compose.dev.yml> --base-images <base-images.lock> --state-dir <directory>" >&2
  exit 64
}

MANIFEST=""
COMPOSE_FILE=""
BASE_IMAGES_FILE=""
STATE_DIR=""
while [[ "$#" -gt 0 ]]; do
  case "$1" in
    --manifest) MANIFEST="$2"; shift 2 ;;
    --compose) COMPOSE_FILE="$2"; shift 2 ;;
    --base-images) BASE_IMAGES_FILE="$2"; shift 2 ;;
    --state-dir) STATE_DIR="$2"; shift 2 ;;
    *) usage ;;
  esac
done
[[ -n "$MANIFEST" && -n "$COMPOSE_FILE" && -n "$BASE_IMAGES_FILE" && -n "$STATE_DIR" ]] || usage
[[ -f "$MANIFEST" && -f "$COMPOSE_FILE" && -f "$BASE_IMAGES_FILE" ]] || usage

: "${DEV_DOMAIN:?DEV_DOMAIN is required}"
: "${DEV_DB_PASSWORD_SECRET_OCID:?DEV_DB_PASSWORD_SECRET_OCID is required}"
: "${DEV_JWT_ACCESS_SECRET_OCID:?DEV_JWT_ACCESS_SECRET_OCID is required}"
: "${DEV_JWT_REFRESH_SECRET_OCID:?DEV_JWT_REFRESH_SECRET_OCID is required}"
: "${DEV_PROVISION_KEY_SECRET_OCID:?DEV_PROVISION_KEY_SECRET_OCID is required}"
: "${DEV_NAVER_MAPS_API_KEY_ID_SECRET_OCID:?DEV_NAVER_MAPS_API_KEY_ID_SECRET_OCID is required}"
: "${DEV_NAVER_MAPS_API_KEY_SECRET_OCID:?DEV_NAVER_MAPS_API_KEY_SECRET_OCID is required}"

umask 077
mkdir -p "$STATE_DIR/releases" "$STATE_DIR/runtime"

candidate_sha="$(jq -er '.commit_sha' "$MANIFEST")"
if [[ ! "$candidate_sha" =~ ^[0-9a-f]{40}$ ]]; then
  echo "Manifest commit_sha must be a full lowercase Git SHA." >&2
  exit 65
fi

read_locked_image() {
  local key="$1"
  local count value
  count="$(grep -c "^${key}=" "$BASE_IMAGES_FILE" || true)"
  if [[ "$count" != "1" ]]; then
    echo "${BASE_IMAGES_FILE} must define ${key} exactly once." >&2
    return 1
  fi

  value="$(grep "^${key}=" "$BASE_IMAGES_FILE" | cut -d= -f2-)"
  if [[ ! "$value" =~ ^[^[:space:]]+@sha256:[0-9a-f]{64}$ ]]; then
    echo "${key} must use an immutable sha256 image digest." >&2
    return 1
  fi
  printf '%s' "$value"
}

LOCKED_POSTGRES_IMAGE="$(read_locked_image POSTGRES_IMAGE)"
LOCKED_REDIS_IMAGE="$(read_locked_image REDIS_IMAGE)"
LOCKED_CADDY_IMAGE="$(read_locked_image CADDY_IMAGE)"

current_env="$STATE_DIR/runtime/current.env"
previous_env="$STATE_DIR/runtime/previous.env"
release_env="$STATE_DIR/releases/${candidate_sha}.env"

env_value() {
  local key="$1"
  local file="$2"
  awk -F= -v key="$key" '$1 == key {sub(/^[^=]*=/, ""); print; exit}' "$file"
}

if [[ -f "$current_env" ]] \
  && [[ "$(env_value CANDIDATE_SHA "$current_env")" == "$candidate_sha" ]] \
  && [[ "$(env_value POSTGRES_IMAGE "$current_env")" == "$LOCKED_POSTGRES_IMAGE" ]] \
  && [[ "$(env_value REDIS_IMAGE "$current_env")" == "$LOCKED_REDIS_IMAGE" ]] \
  && [[ "$(env_value CADDY_IMAGE "$current_env")" == "$LOCKED_CADDY_IMAGE" ]] \
  && [[ "$(env_value HUB_ROUTE_DEFAULT_DATA_ENABLED "$current_env")" == "true" ]] \
  && [[ -n "$(env_value NAVER_MAPS_API_KEY_ID "$current_env")" ]] \
  && [[ -n "$(env_value NAVER_MAPS_API_KEY "$current_env")" ]]; then
  echo "No-op: candidate $candidate_sha is already deployed."
  exit 0
fi

secret_value() {
  local secret_ocid="$1"
  OCI_CLI_AUTH=instance_principal \
    oci secrets secret-bundle get --secret-id "$secret_ocid" \
      --query 'data."secret-bundle-content".content' --raw-output \
    | base64 --decode
}

write_release_env() {
  local target="$1"
  local db_password jwt_access_secret jwt_refresh_secret provision_key
  local naver_maps_api_key_id naver_maps_api_key image_lines

  db_password="$(secret_value "$DEV_DB_PASSWORD_SECRET_OCID")"
  jwt_access_secret="$(secret_value "$DEV_JWT_ACCESS_SECRET_OCID")"
  jwt_refresh_secret="$(secret_value "$DEV_JWT_REFRESH_SECRET_OCID")"
  provision_key="$(secret_value "$DEV_PROVISION_KEY_SECRET_OCID")"
  naver_maps_api_key_id="$(secret_value "$DEV_NAVER_MAPS_API_KEY_ID_SECRET_OCID")"
  naver_maps_api_key="$(secret_value "$DEV_NAVER_MAPS_API_KEY_SECRET_OCID")"
  image_lines="$(jq -er '
    .images | to_entries[] |
    (.key | ascii_upcase | gsub("-"; "_") + "_IMAGE") + "\t" + .value
  ' "$MANIFEST")"

  {
    printf 'CANDIDATE_SHA=%s\n' "$candidate_sha"
    printf 'DEV_DOMAIN=%s\n' "$DEV_DOMAIN"
    printf 'DB_PASSWORD=%s\n' "$db_password"
    printf 'JWT_ACCESS_SECRET=%s\n' "$jwt_access_secret"
    printf 'JWT_REFRESH_SECRET=%s\n' "$jwt_refresh_secret"
    printf 'DEV_PROVISION_KEY=%s\n' "$provision_key"
    printf 'HUB_ROUTE_DEFAULT_DATA_ENABLED=true\n'
    printf 'NAVER_MAPS_API_KEY_ID=%s\n' "$naver_maps_api_key_id"
    printf 'NAVER_MAPS_API_KEY=%s\n' "$naver_maps_api_key"
    printf 'POSTGRES_IMAGE=%s\n' "$LOCKED_POSTGRES_IMAGE"
    printf 'REDIS_IMAGE=%s\n' "$LOCKED_REDIS_IMAGE"
    printf 'CADDY_IMAGE=%s\n' "$LOCKED_CADDY_IMAGE"
    while IFS=$'\t' read -r environment_key image; do
      printf '%s=%s\n' "$environment_key" "$image"
    done <<< "$image_lines"
  } > "$target"
}

required_images=(
  CONFIG_SERVER_IMAGE EUREKA_SERVER_IMAGE GATEWAY_IMAGE USER_SERVICE_IMAGE
  HUB_SERVICE_IMAGE COMPANY_PRODUCT_SERVICE_IMAGE DELIVERY_SERVICE_IMAGE
  ORDER_SERVICE_IMAGE NOTIFICATION_SERVICE_IMAGE
)
for key in "${required_images[@]}"; do
  jq -e --arg key "${key%_IMAGE}" '
    .images[$key | ascii_downcase | gsub("_"; "-")]
    | strings
    | test("@sha256:[0-9a-f]{64}$")
  ' "$MANIFEST" >/dev/null
done

write_release_env "$release_env"

ACTIVE_ENV_FILE="$release_env"
compose() {
  docker compose --project-directory "$(dirname "$COMPOSE_FILE")/.." --env-file "$ACTIVE_ENV_FILE" -f "$COMPOSE_FILE" "$@"
}

initialize_database_schemas() {
  # PostgreSQL variables expand inside the container.
  # shellcheck disable=SC2016
  compose exec -T postgres sh -c \
    'psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" --file /docker-entrypoint-initdb.d/01-create-schemas.sql'
}

verify_hub_default_data() {
  if [[ "$(env_value HUB_ROUTE_DEFAULT_DATA_ENABLED "$ACTIVE_ENV_FILE")" != "true" ]]; then
    return 0
  fi

  local counts hub_count route_count
  counts="$(compose exec -T postgres psql \
    --username logistics \
    --dbname logistics \
    --tuples-only \
    --no-align \
    --field-separator '|' \
    --set ON_ERROR_STOP=1 \
    --command "
      SELECT
        (SELECT count(*) FROM hubs.p_hubs
          WHERE deleted_at IS NULL
            AND created_by = '00000000-0000-0000-0000-000000000001'),
        (SELECT count(*) FROM hubs.p_hub_routes
          WHERE deleted_at IS NULL
            AND created_by = '00000000-0000-0000-0000-000000000001');
    ")"
  IFS='|' read -r hub_count route_count <<< "$counts"
  if [[ "$hub_count" != "17" || "$route_count" != "36" ]]; then
    echo "Hub default data verification failed: hubs=${hub_count:-unknown}, routes=${route_count:-unknown}" >&2
    return 1
  fi
}

wait_healthy() {
  local service="$1"
  while (( SECONDS < DEPLOY_DEADLINE )); do
    local container health
    container="$(compose ps -q "$service")"
    health="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container" 2>/dev/null || true)"
    if [[ "$health" == "healthy" || "$health" == "running" ]]; then
      return 0
    fi
    if [[ "$health" == "unhealthy" || "$health" == "exited" || "$health" == "dead" ]]; then
      echo "$service is $health" >&2
      return 1
    fi
    sleep 5
  done
  echo "Timed out waiting for $service" >&2
  return 1
}

wait_https() {
  while (( SECONDS < DEPLOY_DEADLINE )); do
    if curl --fail --silent --resolve "${DEV_DOMAIN}:443:127.0.0.1" "https://${DEV_DOMAIN}/actuator/health" >/dev/null 2>&1; then
      return 0
    fi
    sleep 5
  done
  echo "Timed out waiting for HTTPS health endpoint" >&2
  return 1
}

deploy_environment() {
  compose pull || return 1
  compose up -d postgres redis || return 1
  wait_healthy postgres || return 1
  initialize_database_schemas || return 1
  wait_healthy redis || return 1
  compose up -d config-server eureka-server || return 1
  wait_healthy config-server || return 1
  wait_healthy eureka-server || return 1

  local service
  for service in user-service hub-service company-product-service delivery-service order-service notification-service; do
    compose up -d "$service" || return 1
    wait_healthy "$service" || return 1
    if [[ "$service" == "hub-service" ]]; then
      verify_hub_default_data || return 1
    fi
  done
  compose up -d gateway || return 1
  wait_healthy gateway || return 1
  compose up -d caddy || return 1
  wait_https || return 1
}

ROLLBACK_ENV=""
rollback() {
  if [[ -z "$ROLLBACK_ENV" || ! -f "$ROLLBACK_ENV" ]]; then
    echo "Deployment failed and no previous healthy release is available." >&2
    return 1
  fi

  echo "Deployment failed; rolling back to the immediately previous image digests." >&2
  ACTIVE_ENV_FILE="$ROLLBACK_ENV"
  DEPLOY_DEADLINE=$((SECONDS + 600))
  deploy_environment
}

handle_deployment_failure() {
  local exit_code="$1"
  local rollback_exit=0
  trap - ERR INT TERM
  set +e

  rollback
  rollback_exit=$?
  if [[ "$rollback_exit" -ne 0 ]]; then
    echo "Automatic rollback did not complete successfully." >&2
  else
    echo "Rollback completed successfully." >&2
  fi
  exit "$exit_code"
}

if [[ -f "$current_env" ]]; then
  cp "$current_env" "$previous_env"
  ROLLBACK_ENV="$previous_env"
fi

DEPLOY_DEADLINE=$((SECONDS + 600))
trap 'handle_deployment_failure $?' ERR
trap 'handle_deployment_failure 130' INT
trap 'handle_deployment_failure 143' TERM
deploy_environment
trap - ERR INT TERM

cp "$release_env" "$current_env"
echo "Deployment completed for candidate $candidate_sha."
