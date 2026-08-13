#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPOSITORY_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
DEPLOY_SCRIPT="$SCRIPT_DIR/deploy.sh"
COMPOSE_FILE="$REPOSITORY_ROOT/deploy/compose.dev.yml"
BASE_IMAGES_FILE="$REPOSITORY_ROOT/deploy/base-images.lock"
CADDY_FILE="$REPOSITORY_ROOT/deploy/Caddyfile"
TEST_ROOT="$(mktemp -d)"
FAKE_BIN="$TEST_ROOT/bin"
DOCKER_LOG="$TEST_ROOT/docker.log"
OLD_SHA="1111111111111111111111111111111111111111"
NEW_SHA="2222222222222222222222222222222222222222"
OLD_DIGEST="sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
NEW_DIGEST="sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
# 아래 값은 셸 표현식이 아니라 bcrypt 형식 검증용 리터럴이다.
# shellcheck disable=SC2016
OLD_ZIPKIN_BASIC_AUTH_HASH="$(printf '$2a$10$%053d' 1)"
# shellcheck disable=SC2016
NEW_ZIPKIN_BASIC_AUTH_HASH="$(printf '$2b$12$%053d' 2)"
# shellcheck disable=SC2016
ROTATED_ZIPKIN_BASIC_AUTH_HASH="$(printf '$2y$13$%053d' 3)"

cleanup() {
  rm -rf "$TEST_ROOT"
}
trap cleanup EXIT

mkdir -p "$FAKE_BIN"

create_manifest() {
  local target="$1"
  jq -n --arg sha "$NEW_SHA" --arg digest "$NEW_DIGEST" '
    {
      commit_sha: $sha,
      images: {
        "config-server": ("registry.example/config-server@" + $digest),
        "eureka-server": ("registry.example/eureka-server@" + $digest),
        "gateway": ("registry.example/gateway@" + $digest),
        "user-service": ("registry.example/user-service@" + $digest),
        "hub-service": ("registry.example/hub-service@" + $digest),
        "company-product-service": ("registry.example/company-product-service@" + $digest),
        "delivery-service": ("registry.example/delivery-service@" + $digest),
        "order-service": ("registry.example/order-service@" + $digest),
        "notification-service": ("registry.example/notification-service@" + $digest)
      }
    }
  ' > "$target"
}

write_current_env() {
  local target="$1"
  {
    printf 'CANDIDATE_SHA=%s\n' "$OLD_SHA"
    printf 'DEV_DOMAIN=dev.example.com\n'
    printf 'DB_PASSWORD=old-secret\n'
    printf 'JWT_ACCESS_SECRET=old-secret\n'
    printf 'JWT_REFRESH_SECRET=old-secret\n'
    printf 'DEV_PROVISION_KEY=old-secret\n'
    printf "ZIPKIN_BASIC_AUTH_HASH='%s'\n" "$OLD_ZIPKIN_BASIC_AUTH_HASH"
    printf 'POSTGRES_IMAGE=registry.example/postgres@%s\n' "$OLD_DIGEST"
    printf 'REDIS_IMAGE=registry.example/redis@%s\n' "$OLD_DIGEST"
    printf 'CADDY_IMAGE=registry.example/caddy@%s\n' "$OLD_DIGEST"
    printf 'ZIPKIN_IMAGE=registry.example/zipkin@%s\n' "$OLD_DIGEST"
    local service
    for service in CONFIG_SERVER EUREKA_SERVER GATEWAY USER_SERVICE HUB_SERVICE COMPANY_PRODUCT_SERVICE DELIVERY_SERVICE ORDER_SERVICE NOTIFICATION_SERVICE; do
      printf '%s_IMAGE=registry.example/%s@%s\n' "$service" "$service" "$OLD_DIGEST"
    done
  } > "$target"
}

write_current_env_for_candidate() {
  local target="$1"
  write_current_env "$target"
  sed -i.bak "s/^CANDIDATE_SHA=.*/CANDIDATE_SHA=$NEW_SHA/" "$target"
  rm "$target.bak"
}

write_fake_commands() {
  cat > "$FAKE_BIN/docker" <<'EOF'
#!/usr/bin/env bash
set -Eeuo pipefail

printf '%s\n' "$*" >> "$DOCKER_LOG"
if [[ "$1" == "compose" ]]; then
  shift
  env_file=""
  command=""
  while [[ "$#" -gt 0 ]]; do
    case "$1" in
      --project-directory|--env-file|-f)
        [[ "$1" == "--env-file" ]] && env_file="$2"
        shift 2
        ;;
      pull|up|ps|exec)
        command="$1"
        shift
        break
        ;;
      *) shift ;;
    esac
  done

  candidate="$(awk -F= '$1 == "CANDIDATE_SHA" {print $2; exit}' "$env_file")"
  printf 'candidate=%s command=%s args=%s\n' "$candidate" "$command" "$*" >> "$DOCKER_LOG"
  if [[ "$command" == "ps" ]]; then
    service="${!#}"
    printf '%s-%s-container\n' "$candidate" "$service"
  fi
  if [[ "$command" == "exec" && "$*" == *"p_hub_routes"* ]]; then
    printf '17|36\n'
  fi
  exit 0
fi

if [[ "$1" == "inspect" ]]; then
  container="${!#}"
  if [[ "$container" == "${FAIL_CANDIDATE_SHA:-unset}-${FAIL_HEALTH_SERVICE:-unset}-container" ]]; then
    printf 'unhealthy\n'
  else
    printf 'healthy\n'
  fi
  exit 0
fi
exit 1
EOF

  cat > "$FAKE_BIN/oci" <<'EOF'
#!/usr/bin/env bash
set -Eeuo pipefail
if [[ "$*" == *"--secret-id zipkin-auth"* ]]; then
  printf '%s' "$TEST_ZIPKIN_BASIC_AUTH_HASH" | base64
else
  printf 'dmFsdWU='
fi
EOF

  cat > "$FAKE_BIN/curl" <<'EOF'
#!/usr/bin/env bash
exit 0
EOF

  cat > "$FAKE_BIN/sleep" <<'EOF'
#!/usr/bin/env bash
exit 0
EOF

  chmod +x "$FAKE_BIN/docker" "$FAKE_BIN/oci" "$FAKE_BIN/curl" "$FAKE_BIN/sleep"
}

run_deploy() {
  local manifest="$1"
  local state_dir="$2"
  DEV_DOMAIN=dev.example.com \
  DEV_DB_PASSWORD_SECRET_OCID=db \
  DEV_JWT_ACCESS_SECRET_OCID=access \
  DEV_JWT_REFRESH_SECRET_OCID=refresh \
  DEV_PROVISION_KEY_SECRET_OCID=provision \
  DEV_ZIPKIN_BASIC_AUTH_HASH_SECRET_OCID=zipkin-auth \
  DEV_NAVER_MAPS_API_KEY_ID_SECRET_OCID=naver-key-id \
  DEV_NAVER_MAPS_API_KEY_SECRET_OCID=naver-key \
  TEST_ZIPKIN_BASIC_AUTH_HASH="${TEST_ZIPKIN_BASIC_AUTH_HASH:-$NEW_ZIPKIN_BASIC_AUTH_HASH}" \
  DOCKER_LOG="$DOCKER_LOG" \
  PATH="$FAKE_BIN:$PATH" \
    bash "$DEPLOY_SCRIPT" \
      --manifest "$manifest" \
      --compose "$COMPOSE_FILE" \
      --base-images "$BASE_IMAGES_FILE" \
      --state-dir "$state_dir"
}

assert_file_line() {
  local expected="$1"
  local file="$2"
  if ! grep -Fqx "$expected" "$file"; then
    echo "Expected line not found in $file: $expected" >&2
    exit 1
  fi
}

write_fake_commands
manifest="$TEST_ROOT/candidate.json"
create_manifest "$manifest"

failure_state="$TEST_ROOT/failure-state"
mkdir -p "$failure_state/runtime"
write_current_env "$failure_state/runtime/current.env"
cp "$failure_state/runtime/current.env" "$TEST_ROOT/original-current.env"
: > "$DOCKER_LOG"
export FAIL_CANDIDATE_SHA="$NEW_SHA"
export FAIL_HEALTH_SERVICE="hub-service"

if run_deploy "$manifest" "$failure_state" > "$TEST_ROOT/failure.out" 2>&1; then
  echo "Expected the unhealthy candidate deployment to fail." >&2
  exit 1
fi

cmp "$TEST_ROOT/original-current.env" "$failure_state/runtime/current.env"
cmp "$TEST_ROOT/original-current.env" "$failure_state/runtime/previous.env"
assert_file_line "candidate=$NEW_SHA command=up args=-d hub-service" "$DOCKER_LOG"
if grep -Fq "candidate=$NEW_SHA command=up args=-d company-product-service" "$DOCKER_LOG"; then
  echo "Deployment continued after the failed health check." >&2
  exit 1
fi
assert_file_line "candidate=$OLD_SHA command=pull args=" "$DOCKER_LOG"
assert_file_line "candidate=$OLD_SHA command=up args=-d caddy" "$DOCKER_LOG"
grep -Fq "Rollback completed successfully." "$TEST_ROOT/failure.out"

success_state="$TEST_ROOT/success-state"
mkdir -p "$success_state/runtime"
write_current_env "$success_state/runtime/current.env"
: > "$DOCKER_LOG"
unset FAIL_CANDIDATE_SHA FAIL_HEALTH_SERVICE
run_deploy "$manifest" "$success_state" > "$TEST_ROOT/success.out" 2>&1

assert_file_line "CANDIDATE_SHA=$NEW_SHA" "$success_state/runtime/current.env"
assert_file_line "CANDIDATE_SHA=$OLD_SHA" "$success_state/runtime/previous.env"
assert_file_line "HUB_ROUTE_DEFAULT_DATA_ENABLED=true" "$success_state/runtime/current.env"
assert_file_line "NAVER_MAPS_API_KEY_ID=value" "$success_state/runtime/current.env"
assert_file_line "NAVER_MAPS_API_KEY=value" "$success_state/runtime/current.env"
assert_file_line "ZIPKIN_BASIC_AUTH_HASH='$NEW_ZIPKIN_BASIC_AUTH_HASH'" "$success_state/runtime/current.env"
while IFS= read -r locked_image; do
  [[ -z "$locked_image" || "$locked_image" == \#* ]] && continue
  assert_file_line "$locked_image" "$success_state/runtime/current.env"
done < "$BASE_IMAGES_FILE"
grep -Fq "candidate=$NEW_SHA command=exec args=-T postgres sh -c" "$DOCKER_LOG"
grep -Fq "p_hub_routes" "$DOCKER_LOG"
grep -Fq "candidate=$NEW_SHA command=up args=-d postgres redis zipkin" "$DOCKER_LOG"

: > "$DOCKER_LOG"
run_deploy "$manifest" "$success_state" > "$TEST_ROOT/no-op.out" 2>&1
grep -Fq "No-op: candidate $NEW_SHA is already deployed." "$TEST_ROOT/no-op.out"
if [[ -s "$DOCKER_LOG" ]]; then
  echo "No-op deployment unexpectedly invoked Docker." >&2
  exit 1
fi

: > "$DOCKER_LOG"
TEST_ZIPKIN_BASIC_AUTH_HASH="$ROTATED_ZIPKIN_BASIC_AUTH_HASH" \
  run_deploy "$manifest" "$success_state" > "$TEST_ROOT/rotation.out" 2>&1
assert_file_line "ZIPKIN_BASIC_AUTH_HASH='$ROTATED_ZIPKIN_BASIC_AUTH_HASH'" "$success_state/runtime/current.env"
if [[ ! -s "$DOCKER_LOG" ]]; then
  echo "Rotated Zipkin Basic Auth Secret did not trigger deployment." >&2
  exit 1
fi

legacy_state="$TEST_ROOT/legacy-state"
mkdir -p "$legacy_state/runtime"
write_current_env_for_candidate "$legacy_state/runtime/current.env"
sed -i.bak '/^ZIPKIN_/d' "$legacy_state/runtime/current.env"
rm "$legacy_state/runtime/current.env.bak"
: > "$DOCKER_LOG"
run_deploy "$manifest" "$legacy_state" > "$TEST_ROOT/legacy.out" 2>&1
if [[ ! -s "$DOCKER_LOG" ]]; then
  echo "Legacy deployment without Hub default data configuration was not upgraded." >&2
  exit 1
fi
assert_file_line "HUB_ROUTE_DEFAULT_DATA_ENABLED=true" "$legacy_state/runtime/current.env"
assert_file_line "NAVER_MAPS_API_KEY_ID=value" "$legacy_state/runtime/current.env"
assert_file_line "NAVER_MAPS_API_KEY=value" "$legacy_state/runtime/current.env"
assert_file_line "ZIPKIN_BASIC_AUTH_HASH='$NEW_ZIPKIN_BASIC_AUTH_HASH'" "$legacy_state/runtime/current.env"

invalid_state="$TEST_ROOT/invalid-state"
mkdir -p "$invalid_state/runtime"
: > "$DOCKER_LOG"
if TEST_ZIPKIN_BASIC_AUTH_HASH=not-a-bcrypt-hash \
  run_deploy "$manifest" "$invalid_state" > "$TEST_ROOT/invalid.out" 2>&1; then
  echo "Expected an invalid Zipkin Basic Auth Secret to fail." >&2
  exit 1
fi
grep -Fq "Zipkin Basic Auth Secret must contain one bcrypt hash." "$TEST_ROOT/invalid.out"
if [[ -s "$DOCKER_LOG" ]]; then
  echo "Invalid Zipkin Basic Auth Secret unexpectedly invoked Docker." >&2
  exit 1
fi

compose_env="$TEST_ROOT/compose.env"
compose_config="$TEST_ROOT/compose.json"
{
  printf 'CANDIDATE_SHA=%s\n' "$NEW_SHA"
  printf 'DEV_DOMAIN=dev.example.com\n'
  printf 'DB_PASSWORD=test-secret\n'
  printf 'JWT_ACCESS_SECRET=test-secret\n'
  printf 'JWT_REFRESH_SECRET=test-secret\n'
  printf 'DEV_PROVISION_KEY=test-secret\n'
  printf "ZIPKIN_BASIC_AUTH_HASH='%s'\n" "$NEW_ZIPKIN_BASIC_AUTH_HASH"
  while IFS= read -r locked_image; do
    [[ -z "$locked_image" || "$locked_image" == \#* ]] && continue
    printf '%s\n' "$locked_image"
  done < "$BASE_IMAGES_FILE"
  for service in CONFIG_SERVER EUREKA_SERVER GATEWAY USER_SERVICE HUB_SERVICE COMPANY_PRODUCT_SERVICE DELIVERY_SERVICE ORDER_SERVICE NOTIFICATION_SERVICE; do
    printf '%s_IMAGE=registry.example/%s@%s\n' "$service" "$service" "$NEW_DIGEST"
  done
} > "$compose_env"

docker compose \
  --project-directory "$REPOSITORY_ROOT" \
  --env-file "$compose_env" \
  -f "$COMPOSE_FILE" \
  config --format json > "$compose_config"
compose_expected_hash="${NEW_ZIPKIN_BASIC_AUTH_HASH//\$/\$\$}"
jq -e --arg expected "$compose_expected_hash" \
  '.services.caddy.environment.ZIPKIN_BASIC_AUTH_HASH == $expected' \
  "$compose_config" >/dev/null
jq -e '(.services.zipkin.ports // []) | length == 0' "$compose_config" >/dev/null
# Caddy 환경변수 표기와 bcrypt 접두사는 grep이 그대로 찾아야 하는 리터럴이다.
# shellcheck disable=SC2016
grep -Fq 'dev {$ZIPKIN_BASIC_AUTH_HASH}' "$CADDY_FILE"
# shellcheck disable=SC2016
if grep -Eq 'dev[[:space:]]+\$2[aby]\$' "$CADDY_FILE"; then
  echo "Caddyfile contains a committed bcrypt hash." >&2
  exit 1
fi

echo "deploy.sh regression tests passed."
