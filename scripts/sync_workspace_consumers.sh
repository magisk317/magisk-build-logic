#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WORKSPACE_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"

if [ "$#" -gt 0 ]; then
  targets=("$@")
else
  targets=(
    "${WORKSPACE_DIR}/xinyi-relay"
    "${WORKSPACE_DIR}/XposedSmsCode"
    "${WORKSPACE_DIR}/push/MiPushFramework"
  )
fi

"${SCRIPT_DIR}/sync_consumer_gradle_assets.sh" "${targets[@]}"
