#!/usr/bin/env bash
# Assert that every gradle-wrapper.properties in this repository carries a
# distributionSha256Sum that equals the official checksum of its own
# distributionUrl.
#
# Why this guard lives here and not in a consumer: the staged template under
# shared/gradle/wrapper is the one Gradle wrapper in the fleet that no build
# ever executes. A consumer verifies its own committed copy the moment someone
# runs ./gradlew, so a wrong checksum there fails out loud. Here a mismatch is
# silent -- nothing reads the file until sync_consumer_gradle_assets.sh installs
# it into a consumer, which is how the 2026-10-31 Renovate bump (distributionUrl
# moved to 9.9.0-rc-1, checksum left at the 9.9.0-milestone-2 value) sat on main
# for a day and only surfaced while the submodule pins were being compared.
#
# The wrapper JAR publishes its own checksums (https://gradle.org/release-checksums/)
# but only inside an HTML table, so it is deliberately out of scope here.
#
# usage: bash scripts/check_wrapper_checksums.sh [--root DIR]
set -euo pipefail

root_dir="."
while [[ $# -gt 0 ]]; do
  case "$1" in
    --root) root_dir="${2:-}"; shift 2 ;;
    -h|--help) sed -n '1,20p' "$0"; exit 0 ;;
    *) echo "unknown argument: $1" >&2; exit 2 ;;
  esac
done
[[ -d "$root_dir" ]] || { echo "not a directory: $root_dir" >&2; exit 2; }
root_dir="$(cd "$root_dir" && pwd)"

# Read one key from a java properties file. Only the flat `key=value` shape the
# wrapper writes is supported, which is exactly what is being checked here.
property() {
  local file="$1" key="$2" line
  # BusyBox grep (the utility CI image) does not promise -m, so trim with head.
  line="$(grep -E "^[[:space:]]*${key}[[:space:]]*=" "$file" 2>/dev/null | head -n 1 || true)"
  [[ -n "$line" ]] || return 0
  line="${line#*=}"
  # A properties file escapes the URL colon with a backslash, and a CRLF
  # checkout leaves a trailing CR; neither is part of the URL Gradle downloads.
  printf '%s' "$line" | tr -d '\r' | sed -e 's/\\//g' -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//'
}

# Gradle publishes each distribution checksum on the canonical service. A
# mirror's adjacent sidecar is not an authority: trusting it would let a mirror
# replace both the archive and checksum while still passing this guard.
official_checksum() {
  local url="$1"
  curl -fsSL --retry 2 --max-time 30 \
    "https://services.gradle.org/distributions/$(basename "$url").sha256" \
    2>/dev/null | tr -d '[:space:]' | tr 'A-F' 'a-f'
}

checked=0
failures=0

while IFS= read -r file; do
  rel="${file#"$root_dir"/}"
  checked=$((checked + 1))

  url="$(property "$file" distributionUrl)"
  sha="$(property "$file" distributionSha256Sum)"

  if [[ -z "$url" ]]; then
    echo "FAIL $rel: no distributionUrl" >&2
    failures=$((failures + 1))
    continue
  fi
  if [[ -z "$sha" ]]; then
    echo "FAIL $rel: distributionUrl=$url has no distributionSha256Sum" >&2
    echo "     an unpinned wrapper downloads whatever the host serves that day" >&2
    failures=$((failures + 1))
    continue
  fi

  expected="$(official_checksum "$url" || true)"
  if [[ -z "$expected" ]]; then
    echo "FAIL $rel: cannot fetch the official checksum for $url" >&2
    echo "     missing .sha256 sidecar, or the job has no egress (proxy env?)" >&2
    failures=$((failures + 1))
    continue
  fi

  actual="$(printf '%s' "$sha" | tr -d '[:space:]' | tr 'A-F' 'a-f')"
  if [[ "$actual" != "$expected" ]]; then
    echo "FAIL $rel: distributionSha256Sum does not match $url" >&2
    echo "     in file : $actual" >&2
    echo "     official: $expected" >&2
    echo "     a Gradle version bump moves the URL and the checksum as one pair" >&2
    failures=$((failures + 1))
    continue
  fi

  echo "ok  $rel: $(basename "$url") @ ${actual:0:12}..."
done < <(find "$root_dir" -type f -name gradle-wrapper.properties \
  ! -path '*/.git/*' ! -path '*/build/*' ! -path '*/.magisk-ci-toolkit/*' \
  ! -path '*/node_modules/*' | sort)

if ((checked == 0)); then
  echo "no gradle-wrapper.properties found under $root_dir" >&2
  exit 1
fi
if ((failures > 0)); then
  echo "wrapper checksum guard failed: $failures of $checked file(s)" >&2
  exit 1
fi
echo "wrapper checksums verified: $checked file(s)"
