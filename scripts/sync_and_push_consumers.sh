#!/usr/bin/env bash
set -euo pipefail

# Sync the shared Gradle version catalog from this build-logic repo into every
# consumer repo, then commit + push the change directly (no MR gate).
#
# Designed to run from a GitLab scheduled pipeline in magisk-build-logic.
# The shared catalog (shared/gradle/libs.versions.toml) is the single source of
# truth for every version key it defines; merge_catalogs.py lets each consumer
# keep its own repo-specific keys (versionCode, ndk, matrix-sdk-android, ...).
#
# Env:
#   CONSUMER_SYNC_TOKEN  GitLab token (project or personal) with push access to
#                        every consumer repo. Required in CI (missing token is a
#                        hard failure there); outside CI it falls back to DRY_RUN.
#   CONSUMER_BRANCH      Branch to sync/push on each consumer (default: beta).
#   GIT_AUTHOR / GIT_EMAIL  Commit identity.
#   DRY_RUN              if "1", merge + show diff but do not push.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MERGE_PY="${SCRIPT_DIR}/merge_catalogs.py"
SHARED_CATALOG="${SCRIPT_DIR}/../shared/gradle/libs.versions.toml"

CONSUMER_BRANCH="${CONSUMER_BRANCH:-beta}"
GIT_AUTHOR="${GIT_AUTHOR:-magisk317}"
GIT_EMAIL="${GIT_EMAIL:-code@mail.usdt.edu.kg}"
DRY_RUN="${DRY_RUN:-0}"

CONSUMERS=(
  "magisk3171/MiPushFramework"
  "magisk3171/XposedSmsCode"
  "magisk3171/xinyi-relay"
)

if [ -z "${CONSUMER_SYNC_TOKEN:-}" ]; then
  # In CI a missing token means the job would run green but push nothing (the
  # variable is protected, so it is invisible on unprotected refs). Fail closed
  # there; keep the local dry-run convenience outside CI.
  if [ "${CI:-}" = "true" ]; then
    echo "CONSUMER_SYNC_TOKEN is not set in CI; failing instead of silently dry-running" >&2
    exit 1
  fi
  echo "CONSUMER_SYNC_TOKEN is not set; enabling DRY_RUN" >&2
  DRY_RUN=1
fi

WORK_ROOT="$(mktemp -d)"
trap 'rm -rf "${WORK_ROOT}"' EXIT

for repo in "${CONSUMERS[@]}"; do
  name="$(basename "${repo}")"
  dst="${WORK_ROOT}/${name}"
  url="https://gitlab.com/${repo}.git"
  if [ "${DRY_RUN}" -ne 1 ]; then
    url="https://oauth2:${CONSUMER_SYNC_TOKEN}@gitlab.com/${repo}.git"
  fi
  echo ">>> ${repo} (branch ${CONSUMER_BRANCH})"
  git clone -b "${CONSUMER_BRANCH}" --depth 1 "${url}" "${dst}"

  python3 "${MERGE_PY}" "${SHARED_CATALOG}" "${dst}/gradle/libs.versions.toml"

  if git -C "${dst}" diff --quiet -- gradle/libs.versions.toml; then
    echo "    no change for ${repo}"
    continue
  fi

  git -C "${dst}" add gradle/libs.versions.toml
  git -C "${dst}" -c "user.name=${GIT_AUTHOR}" -c "user.email=${GIT_EMAIL}" \
    commit -m "chore(deps): sync shared gradle catalog from build-logic"

  if [ "${DRY_RUN}" -eq 1 ]; then
    echo "    DRY_RUN: would push the following to ${repo}@${CONSUMER_BRANCH}"
    git -C "${dst}" --no-pager diff --cached --stat
  else
    git -C "${dst}" push
    echo "    pushed ${repo}@${CONSUMER_BRANCH}"
  fi
done
