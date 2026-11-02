#!/usr/bin/env bash
set -euo pipefail

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
check_script="$root_dir/scripts/check_wrapper_checksums.sh"
tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT
mkdir -p "$tmp_dir/bin" "$tmp_dir/project/gradle/wrapper"

export OFFICIAL_SHA=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
export MIRROR_SHA=bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb
export CURL_LOG="$tmp_dir/curl.log"
cat > "$tmp_dir/bin/curl" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
url="${!#}"
printf '%s\n' "$url" >> "$CURL_LOG"
case "$url" in
  https://services.gradle.org/distributions/gradle-9.9-bin.zip.sha256)
    printf '%s\n' "$OFFICIAL_SHA"
    ;;
  https://mirror.example/gradle-9.9-bin.zip.sha256)
    printf '%s\n' "$MIRROR_SHA"
    ;;
  *)
    exit 22
    ;;
esac
EOF
chmod +x "$tmp_dir/bin/curl"

cat > "$tmp_dir/project/gradle/wrapper/gradle-wrapper.properties" <<EOF
distributionUrl=https\://mirror.example/gradle-9.9-bin.zip
distributionSha256Sum=$OFFICIAL_SHA
EOF
PATH="$tmp_dir/bin:$PATH" bash "$check_script" --root "$tmp_dir/project"
mapfile -t requested_urls < "$CURL_LOG"
[[ "${#requested_urls[@]}" -eq 1 ]]
[[ "${requested_urls[0]}" == "https://services.gradle.org/distributions/gradle-9.9-bin.zip.sha256" ]]

cat > "$tmp_dir/project/gradle/wrapper/gradle-wrapper.properties" <<EOF
distributionUrl=https\://mirror.example/gradle-9.9-bin.zip
distributionSha256Sum=$MIRROR_SHA
EOF
if PATH="$tmp_dir/bin:$PATH" bash "$check_script" --root "$tmp_dir/project" >/dev/null 2>&1; then
  echo "wrapper guard trusted the mirror sidecar instead of Gradle's checksum" >&2
  exit 1
fi

echo "wrapper checksum tests passed"
