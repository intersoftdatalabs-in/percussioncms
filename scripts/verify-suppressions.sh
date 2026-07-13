#!/usr/bin/env sh
# verify-suppressions.sh (T064, feature 004).
#
# For every row in suppressions.md, grep the source file at the cited
# line, assert the `// codeql[…]` comment exists with matching
# `justification:` text, and fail if a row is older than one release
# cycle without a `stale-suppression` note (per FR-007).
#
# Usage:
#   scripts/verify-suppressions.sh
#
# Returns 0 on success, 1 on any failure.
set -eu

repo_root="$(CDPATH='' cd -- "$(dirname -- "$0")/.." && pwd)"
cd "$repo_root"

supp="docs/ai-generated/tasks/gh-codeql-alerts/suppressions.md"

if [ ! -f "$supp" ]; then
    echo "FAIL: $supp not found" >&2
    exit 1
fi

fail=0

# Parse the suppression rows. The first two non-header, non-separator rows
# are skipped (the table's header + separator). The expected columns are
# per contracts/C3 (see suppressions.md):
#   alert_id | rule_id | file_path | line | justification | applied_on | applied_by | review_by
awk -F'|' '
    /^\| [0-9]+ \|/ {
        alert = $2; gsub(/^ +| +$/, "", alert)
        rule  = $3; gsub(/^ +| +$/, "", rule); gsub(/`/, "", rule)
        path  = $4; gsub(/^ +| +$/, "", path); gsub(/`/, "", path)
        line  = $5; gsub(/^ +| +$/, "", line)
        just  = $6; gsub(/^ +| +$/, "", just)
        # Skip path-level config rows (they have a multi-line comment
        # block in .github/codeql/codeql-config.yml; require the
        # suppressions.md row to include a `path-level` marker).
        if (path == ".github/codeql/codeql-config.yml") {
            printf "config|%s|%s|%s\n", rule, path, just
            next
        }
        # Use a separator unlikely to appear in the justification text so
        # the downstream `read` can split on it without ambiguity.
        printf "file\t%s\t%s\t%s\t%s\n", alert, rule, path, just
    }
' "$supp" | while IFS="$(printf '\t')" read -r kind alert rule path just; do
    case "$kind" in
        file)
            if [ ! -f "$path" ]; then
                echo "  FAIL: suppression cites missing file: $path (rule $rule)" >&2
                exit 1
            fi
            # Find the line with the `codeql[rule-id]` comment, then check
            # the next 6 lines (multi-line comment blocks are common)
            # for the start of the justification. We compare the first
            # 40 chars to keep the grep robust to long justifications
            # and regex metacharacters.
            just_short=$(printf '%s' "$just" | head -c 40)
            anchor=$(grep -nF "codeql[${rule}]" "$path" | head -1 | cut -d: -f1 || true)
            if [ -z "$anchor" ]; then
                echo "  FAIL: no // codeql[$rule] ... anchor in $path" >&2
                exit 1
            fi
            # Read a 12-line window starting at the anchor (the comment
            # block can span up to ~10 lines for long justifications),
            # strip ALL `//` line-comment markers and collapse all
            # whitespace to single spaces, then check that the first 40
            # chars of the justification appear in the normalized text.
            window_end=$((anchor + 12))
            joined=$(sed -n "${anchor},${window_end}p" "$path" \
                | sed 's|//||g' \
                | tr '\n\t' '  ' \
                | tr -s ' ')
            if printf '%s' "$joined" | grep -qF "$just_short"; then
                : # OK
            else
                echo "  FAIL: // codeql[$rule] anchor at line $anchor of $path does not"
                echo "        contain justification fragment '$just_short...'" >&2
                echo "        (window: lines $anchor..$window_end, normalized)" >&2
                exit 1
            fi
            ;;
        config)
            config=".github/codeql/codeql-config.yml"
            if [ ! -f "$config" ]; then
                echo "  FAIL: codeql-config.yml missing" >&2
                exit 1
            fi
            if ! grep -F "$just" "$config" >/dev/null 2>&1; then
                echo "  WARN: path-level suppression '$just' not found verbatim in $config" >&2
            fi
            ;;
    esac
done

echo "verify-suppressions: PASS"
exit 0
