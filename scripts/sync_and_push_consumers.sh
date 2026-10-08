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
# The Renovate bot identity doubles as the whitelist key: the consumer repos'
# telegram:ci auto-notify rules match CI_COMMIT_AUTHOR against
# Renovate317@users.noreply. Keep both fields on the bot: the id prefix is what
# lets GitLab attribute the commit to the Renovate317 account, while a bare
# "Renovate317@users.noreply" carries no .gitlab.com domain and pairs the bot
# address with a human name. Changing either field silently downgrades sync
# pushes to manual notify.
GIT_AUTHOR="${GIT_AUTHOR:-Renovate}"
GIT_EMAIL="${GIT_EMAIL:-40025304-Renovate317@users.noreply.gitlab.com}"
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

  # Renovate's own dependency commits are one line naming the dependency and
  # the version it moves to - "chore(deps): update dependency tink to v1.23.0"
  # - and nothing else, because the details belong to the MR description. Match
  # that shape: the consumer's beta pipeline relays the pushed APKs to
  # Telegram, and a subject that only says "sync shared gradle catalog" tells
  # whoever reads the notification nothing about what actually changed.
  #
  # Renovate reports a patch bump as fix(deps) and everything else as
  # chore(deps); a value that is not a version at all (a pin or a replacement)
  # is also a fix. Follow the same rule so a sync reads like the upgrades it
  # carries. One push can cover several catalog keys, so the remaining ones
  # follow in the body as their own Renovate-style lines.
  bump_commits="$(git -C "${dst}" diff -U0 -- gradle/libs.versions.toml | awk '
    BEGIN { SQ = sprintf("%c", 39) }
    function bare(v) { gsub(SQ, "", v); gsub(/"/, "", v); gsub(/^[ \t]+|[ \t]+$/, "", v); gsub(/^[vV]/, "", v); return v }
    # Semver-ish: a numeric core plus an optional pre-release/build suffix.
    # Kotlin ships "2.5.0-Beta1", and treating it as unparseable would report a
    # plain pre-release step as fix(deps) just because of the suffix.
    function numeric(v) { return bare(v) ~ /^[0-9]+(\.[0-9]+)*([-+.][A-Za-z0-9.+-]+)?$/ }
    function kind(o, n) {
      if (!numeric(o) || !numeric(n)) return "fix"
      split(bare(o), ko, "."); split(bare(n), kn, ".")
      if (ko[1] + 0 != kn[1] + 0) return "chore"
      if (ko[2] + 0 != kn[2] + 0) return "chore"
      return "fix"
    }
    # Only a plain version is worth printing. The [libraries] entries are
    # "name = { module = "...", version.ref = "..." }", and echoing that after
    # "to" would produce garbage instead of a version.
    function shown(v,   b) { b=bare(v); if (b ~ /^[0-9]/) return "v" b; return "" }
    /^\+\+\+|^---|^@@/ { next }
    /^-[a-zA-Z]/ { line=substr($0,2); key=line; sub(/[ \t]*=.*/, "", key); val=line; sub(/^[^=]*=[ \t]*/, "", val); old[key]=val; if (!(key in seen)) { seen[key]=1; order[++n]=key }; next }
    /^\+[a-zA-Z]/ { line=substr($0,2); key=line; sub(/[ \t]*=.*/, "", key); val=line; sub(/^[^=]*=[ \t]*/, "", val); new[key]=val; if (!(key in seen)) { seen[key]=1; order[++n]=key }; next }
    END {
      for (i=1;i<=n;i++) {
        k=order[i]
        if ((k in old) && (k in new)) {
          v=shown(new[k])
          if (v == "") printf "%s(deps): update dependency %s\n", kind(old[k], new[k]), k
          else printf "%s(deps): update dependency %s to %s\n", kind(old[k], new[k]), k, v
        } else if (k in new) {
          v=shown(new[k])
          if (v == "") printf "chore(deps): add dependency %s\n", k
          else printf "chore(deps): add dependency %s to %s\n", k, v
        } else if (k in old) {
          printf "chore(deps): remove dependency %s\n", k
        }
      }
    }
  ' || true)"

  subject="$(printf '%s\n' "${bump_commits}" | sed -n '1p')"
  body="$(printf '%s\n' "${bump_commits}" | sed -n '2,$p')"
  if [ -z "${subject}" ]; then
    # The parser recognised nothing. Keep the opaque subject rather than
    # skipping the push, so the catalog never falls behind unnoticed.
    subject="chore(deps): sync shared gradle catalog from build-logic"
    body=""
  fi

  git -C "${dst}" add gradle/libs.versions.toml
  if [ -n "${body}" ]; then
    git -C "${dst}" -c "user.name=${GIT_AUTHOR}" -c "user.email=${GIT_EMAIL}" \
      commit -m "${subject}" \
      -m "${body}"
  else
    git -C "${dst}" -c "user.name=${GIT_AUTHOR}" -c "user.email=${GIT_EMAIL}" \
      commit -m "${subject}"
  fi

  if [ "${DRY_RUN}" -eq 1 ]; then
    echo "    DRY_RUN: would push the following to ${repo}@${CONSUMER_BRANCH}"
    git -C "${dst}" --no-pager diff --cached --stat
  else
    git -C "${dst}" push
    echo "    pushed ${repo}@${CONSUMER_BRANCH}"
  fi
done
