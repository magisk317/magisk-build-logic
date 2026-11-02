#!/usr/bin/env bash
set -euo pipefail

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
merge_script="$root_dir/scripts/merge_catalogs.py"
tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT

cat > "$tmp_dir/shared.toml" <<'EOF'
[versions]
minSdk = "38"
javaBytecode = "27"
kotlin = "2.5.0"
sharedOnly = "1"
EOF
cat > "$tmp_dir/private.toml" <<'EOF'
[versions]
minSdk = "37"
javaBytecode = "26"
kotlin = "2.4.0"
localOnly = "9"

[libraries]
keep = { module = "example:keep", version = "1" }
EOF
python3 "$merge_script" "$tmp_dir/shared.toml" "$tmp_dir/private.toml"
grep -Fxq 'minSdk = "37"' "$tmp_dir/private.toml"
grep -Fxq 'javaBytecode = "26"' "$tmp_dir/private.toml"
grep -Fxq 'kotlin = "2.5.0"' "$tmp_dir/private.toml"
grep -Fxq 'sharedOnly = "1"' "$tmp_dir/private.toml"
grep -Fxq 'localOnly = "9"' "$tmp_dir/private.toml"
grep -Fxq 'keep = { module = "example:keep", version = "1" }' "$tmp_dir/private.toml"

cat > "$tmp_dir/shared-default.toml" <<'EOF'
[versions]
minSdk = "28"
EOF
cat > "$tmp_dir/missing.toml" <<'EOF'
[versions]
kotlin = "2.5.0"

[libraries]
EOF
python3 "$merge_script" "$tmp_dir/shared-default.toml" "$tmp_dir/missing.toml"
grep -Fxq 'minSdk = "28"' "$tmp_dir/missing.toml"

cat > "$tmp_dir/legacy.toml" <<'EOF'
[versions]
minSdk = "26"
EOF
python3 "$merge_script" "$tmp_dir/shared-default.toml" "$tmp_dir/legacy.toml"
grep -Fxq 'minSdk = "26"' "$tmp_dir/legacy.toml"

echo "catalog merge tests passed"
