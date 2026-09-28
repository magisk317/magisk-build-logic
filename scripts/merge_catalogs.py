#!/usr/bin/env python3
"""Merge shared catalog into consumer's libs.versions.toml.
Consumer entries override shared entries with the same alias.
Usage: merge_catalogs.py <shared.toml> <consumer.toml>
"""
import re
import sys
from pathlib import Path

def parse_section_keys(text, section):
    """Return {key: full_line} for a section."""
    entries = {}
    inside = False
    for line in text.splitlines():
        if re.match(rf'^\[{section}\]', line):
            inside = True
            continue
        if re.match(r'^\[', line):
            inside = False
            continue
        if inside:
            m = re.match(r'^([a-zA-Z0-9_.-]+)\s*=', line)
            if m:
                entries[m.group(1)] = line
    return entries

# Keys in [versions] that are owned by the shared catalog: the shared value
# always wins so a single bump here reaches every consumer on next sync.
SHARED_MANAGED_VERSION_KEYS = set()  # retained for compatibility; versions merge now favors shared
# Keys where the CONSUMER value wins even though shared defines one.
# build-logic needs these present in its own catalog (build.gradle.kts /
# MagiskCatalog.kt read javaBytecode), but some consumers cannot move to
# the shared value (xinyi crashes on bytecode 27), so the consumer pin wins.
CONSUMER_OVERRIDE_VERSION_KEYS = {"javaBytecode"}

shared_text = Path(sys.argv[1]).read_text()
consumer_text = Path(sys.argv[2]).read_text() if Path(sys.argv[2]).exists() else ""

out = []
for section in ('versions', 'libraries', 'plugins'):
    shared = parse_section_keys(shared_text, section)
    consumer = parse_section_keys(consumer_text, section)
    # Consumer overrides shared, except shared-managed SDK/language keys.
    if section == "versions":
        # Shared catalog is the single source of truth for every version key it
        # defines; consumer-only keys are preserved. A single bump in
        # build-logic now reaches every consumer on the next sync.
        merged = {**consumer, **shared}
        for _k in CONSUMER_OVERRIDE_VERSION_KEYS:
            if _k in consumer:
                merged[_k] = consumer[_k]
    else:
        merged = {**shared, **consumer}

    out.append(f'[{section}]')
    # Shared entries in order (resolves to merged value → deduped)
    consumed = set()
    for key in shared:
        if key in merged:
            out.append(merged[key])
            consumed.add(key)
    # Consumer-only entries
    for key in consumer:
        if key not in consumed:
            out.append(consumer[key])
    out.append('')

Path(sys.argv[2]).write_text('\n'.join(out) + '\n')
total = sum(len(parse_section_keys(shared_text, s)) for s in ('versions','libraries','plugins'))
local = sum(len(parse_section_keys(consumer_text, s)) for s in ('versions','libraries','plugins'))
print(f"Merged: {total} shared + {local} local")
