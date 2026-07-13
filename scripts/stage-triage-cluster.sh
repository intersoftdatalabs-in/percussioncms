#!/usr/bin/env bash
# Mark triage.md rows as "ready to close" (linked_pr = a placeholder)
# for clusters whose closing changes are staged in the working tree.
#
# Usage: scripts/stage-triage-cluster.sh <cluster-id> [mode]
#   cluster-id examples: T027, T029, T037, T066
#   mode: "basename" (default) or "path"
#     basename  — match on the basename of files in the cluster's
#                 section of tmp/gh-codeql-alerts/removed-files.txt
#                 (works for obsolete-removal clusters T021..T031).
#     path      — match on the file_path column directly using a
#                 cluster-specific path list defined in this script
#                 (works for fix/suppression clusters T037, T066).
#
# For each matched row whose linked_pr is currently "—", this script
# sets linked_pr = "<cluster-id>-staged". The verify-triage-inventory.sh
# row-count check then counts these rows as "ready to close" so the
# math balances (triage_rows = open_alerts + ready_to_close).
#
# The placeholder is reverted (back to "—") once the PR actually opens
# and the cluster is re-staged with the real PR number.

set -eu

cd "$(dirname "$0")/.."

cluster="${1:-}"
mode="${2:-basename}"
if [ -z "$cluster" ]; then
    echo "usage: $0 <cluster-id> [basename|path]" >&2
    exit 2
fi

triage="docs/ai-generated/tasks/gh-codeql-alerts/triage.md"
removed_inventory="tmp/gh-codeql-alerts/removed-files.txt"

if [ ! -f "$triage" ]; then
    echo "FAIL: $triage not found" >&2
    exit 1
fi

case "$mode" in
    basename)
        if [ ! -f "$removed_inventory" ]; then
            echo "FAIL: $removed_inventory not found (basename mode)" >&2
            exit 1
        fi
        # Collect basenames from the cluster's section in the inventory.
        # Sections are delimited by lines like "# --- T029 --- ..." and
        # end at the next such header or at EOF.
        matchers=$(awk -v cluster="$cluster" '
            /^# --- / {
                in_target = (index($0, "# --- " cluster " ---") == 1)
                next
            }
            in_target && !/^\s*#/ && !/^\s*$/ {
                n = split($0, parts, "/")
                if (n > 0) print parts[n]
            }
        ' "$removed_inventory" | sort -u)
        # Compose the awk matcher for basenames.
        match_awk='
            BEGIN {
                n = split(matchers, arr, "\n")
                for (i = 1; i <= n; i++) {
                    bn = arr[i]
                    gsub(/^ +| +$/, "", bn)
                    if (bn != "") seen[bn] = 1
                }
            }
            {
                file_path = $6
                gsub(/`/, "", file_path)
                matched = 0
                for (bn in seen) {
                    if (index(file_path, bn) > 0) { matched = 1; break }
                }
            }'
        ;;
        path)
            case "$cluster" in
                T037)
                    # java/ssrf in PSProxyQueryResource.java — 6 alerts
                    matchers='PSProxyQueryResource.java'
                    ;;
                T039)
                    # java/xxe in PSSerializerUtils.java — 2 alerts
                    matchers='PSSerializerUtils.java'
                    ;;
                T040)
                    # java/ldap-injection in PSJndiGroupProvider.java
                    # (4 alert rows; the escape applies to every LDAP filter
                    # construction site in the file)
                    matchers='PSJndiGroupProvider.java'
                    ;;
                T041)
                    # java/zipslip in PSArchiveFiles.java — fix is already
                    # in place via PathValidation.constructSafePath; this
                    # cluster documents it and adds a regression test
                    matchers='PSArchiveFiles.java'
                    ;;
                T042)
                    # java/sql-injection in PSPageDaoHelper.java (and
                    # related formGetByStatusSQLQuery helper) — 1 alert
                    # in the seed; the fix applies to all 5 user-supplied
                    # search-field parameters
                    matchers='PSPageDaoHelper.java'
                    ;;
                T044)
                    # java/xss in PSSiteDataRestService.java — 4 alerts
                    # in the seed; the fix is path-param allow-list
                    # validation at the API boundary + per-method
                    # data-flow documentation per contracts/C2
                    matchers='PSSiteDataRestService.java'
                    ;;
                T066)
                    # java/implicit-cast-in-compound-assignment in
                    # PSFeedServicePerformanceTest.java — 1 alert
                    matchers='PSFeedServicePerformanceTest.java'
                    ;;
                *)
                    echo "FAIL: cluster $cluster has no path matchers; add one to scripts/stage-triage-cluster.sh" >&2
                    exit 1
                    ;;
            esac
            match_awk='
            BEGIN {
                # Use a non-regex separator. awk split treats its third
                # arg as a regex, so a literal "|" would match the
                # empty string between every character and split the
                # matcher into single-character substrings (gawk
                # behavior). The newline is a safe literal here because
                # each case in the parent shell sets `matchers` to a
                # single path substring (no newlines). mawk/bwk also
                # treat "\n" as a literal field separator in split.
                n = split(matchers, arr, "\n")
                for (i = 1; i <= n; i++) {
                    pn = arr[i]
                    gsub(/^ +| +$/, "", pn)
                    if (pn != "") seen[pn] = 1
                }
            }
            {
                file_path = $6
                gsub(/`/, "", file_path)
                matched = 0
                for (pn in seen) {
                    if (index(file_path, pn) > 0) { matched = 1; break }
                }
            }'
        ;;
    *)
        echo "FAIL: unknown mode '$mode' (use 'basename' or 'path')" >&2
        exit 2
        ;;
esac

updated=0
tmp=$(mktemp)
trap 'rm -f "$tmp"' EXIT

# The awk below is shared by both modes; the matchers are injected via
# the `-v` option and the match_awk string is prepended.
awk -F'|' \
    -v matchers="$matchers" \
    -v cluster="$cluster" \
    "$match_awk"'
    /^\| [0-9]+ \|/ {
        if (matched) {
            linked = $11
            gsub(/^ +| +$/, "", linked)
            if (linked == "" || linked == "—") {
                $11 = " " cluster "-staged "
                updated++
            }
        }
        out = $1
        for (i = 2; i <= NF; i++) out = out "|" $i
        print out
        next
    }
    { print }
' "$triage" > "$tmp"
mv "$tmp" "$triage"

echo "stage-triage-cluster: marked $updated rows as '$cluster-staged' (mode=$mode) in $triage"
echo "  (rows revert linked_pr to '—' once the PR opens and the cluster is re-staged)"
