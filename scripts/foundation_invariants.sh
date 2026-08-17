#!/usr/bin/env bash
set -euo pipefail

if grep -RIn --exclude-dir=.git --exclude='*.md' 'SUPABASE_SYNC_TOKEN' app gradle.properties; then
  echo 'Shared sync token reference found in Android client or Gradle properties.' >&2
  exit 1
fi

if grep -RIn --include='*.kt' 'fallbackToDestructiveMigration' app/src; then
  echo 'Destructive Room migration fallback found.' >&2
  exit 1
fi

if grep -RIn --include='*.kt' 'p_token' app/src/main; then
  echo 'Legacy p_token transport found in Android source.' >&2
  exit 1
fi

git diff --check
echo 'Foundation security invariants passed.'
