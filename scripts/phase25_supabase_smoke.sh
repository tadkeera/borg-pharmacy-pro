#!/bin/bash
set -euo pipefail

required=(SUPABASE_URL SUPABASE_ANON_KEY SUPABASE_TEST_OWNER_TOKEN SUPABASE_TEST_ADMIN_TOKEN SUPABASE_TEST_PHARMACIST_TOKEN SUPABASE_TEST_EMPLOYEE_TOKEN SUPABASE_TEST_REPRESENTATIVE_TOKEN SUPABASE_TEST_VIEWER_TOKEN SUPABASE_TEST_TENANT_A SUPABASE_TEST_TENANT_B)
for name in "${required[@]}"; do
  if [[ -z "${!name:-}" ]]; then
    echo "SKIP: missing $name; configure a disposable Supabase staging environment first" >&2
    exit 2
  fi
done

base="${SUPABASE_URL%/}"
api="$base/rest/v1"

request_count() {
  local token="$1" tenant="$2"
  curl --fail-with-body --silent --show-error \
    -H "apikey: $SUPABASE_ANON_KEY" \
    -H "Authorization: Bearer $token" \
    "$api/companies?select=id&tenant_id=eq.$tenant&limit=2" | python3 -c 'import json,sys; print(len(json.load(sys.stdin)))'
}

assert_own_tenant_visible() {
  local role="$1" token="$2"
  local own other
  own="$(request_count "$token" "$SUPABASE_TEST_TENANT_A")"
  other="$(request_count "$token" "$SUPABASE_TEST_TENANT_B")"
  if [[ "$other" != "0" ]]; then
    echo "FAIL: $role can read Tenant B" >&2
    exit 1
  fi
  echo "PASS: $role own=$own cross_tenant=$other"
}

assert_own_tenant_visible OWNER "$SUPABASE_TEST_OWNER_TOKEN"
assert_own_tenant_visible ADMIN "$SUPABASE_TEST_ADMIN_TOKEN"
assert_own_tenant_visible PHARMACIST "$SUPABASE_TEST_PHARMACIST_TOKEN"
assert_own_tenant_visible EMPLOYEE "$SUPABASE_TEST_EMPLOYEE_TOKEN"
assert_own_tenant_visible REPRESENTATIVE "$SUPABASE_TEST_REPRESENTATIVE_TOKEN"
assert_own_tenant_visible VIEWER "$SUPABASE_TEST_VIEWER_TOKEN"

echo "PASS: basic tenant visibility matrix"
echo "INFO: write permission and conflict scenarios require seeded UUID payloads and are covered by supabase/tests/phase25_sync_matrix.sql"
