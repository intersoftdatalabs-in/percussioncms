#!/usr/bin/env sh
# Generate umbrella-issue bodies for T010 (one per top-5 module owner).
# Reads triage.md and emits, for each module_owner, a markdown list of
# alert IDs grouped by disposition. The script is data-driven; per-module
# totals stay in sync with triage.md.

set -eu

cd "$(dirname "$0")/.."

triage="docs/ai-generated/tasks/gh-codeql-alerts/triage.md"
out_dir="tmp/gh-codeql-alerts/umbrellas"
mkdir -p "$out_dir"

modules="WebUI/
system/
projects/sitemanage/
modules/perc-packages/
modules/perc-common-ui-bundle/"

# Per-module report.
for mod in $modules; do
    out="$out_dir/$(echo "$mod" | tr '/' '_')"
    {
        echo "# Umbrella: code-scanning alerts for \`$mod\`"
        echo
        echo "Tracks the closure of every code-scanning (CodeQL) alert on"
        echo "\`development\` whose \`module_owner\` in"
        echo "[triage.md](docs/ai-generated/tasks/gh-codeql-alerts/triage.md)"
        echo "is \`$mod\` per spec \`004-zero-code-scanning-alerts\` (US2/US3/US4)."
        echo
        echo "Source of truth: \`docs/ai-generated/tasks/gh-codeql-alerts/triage.md\`"
        echo "(filter on column 7 = \`$mod\`)."
        echo
        echo "## Totals (seeded 2026-07-11)"
        echo
        echo "| Disposition | Count |"
        echo "|-------------|-------|"
        awk -F'|' -v m="$mod" '
            /^\| [0-9]+ \|/ {
                owner = $7
                gsub(/^ +| +$/, "", owner)
                gsub(/`/, "", owner)
                if (owner == m) {
                    d = $8
                    gsub(/`/, "", d)
                    gsub(/ *\(candidate\)/, "", d)
                    gsub(/^ +| +$/, "", d)
                    disp[d]++
                }
            }
            END {
                for (k in disp) printf "| `%s` | %d |\n", k, disp[k]
            }
        ' "$triage"
        echo
        echo "## Per-cluster PR plan"
        echo
        echo "| Cluster | Task | File(s) | Approx alert count |"
        echo "|---------|------|---------|--------------------|"
        case "$mod" in
            "WebUI/")
                echo "| knockout.js vendored dist | T021 | 5 files | 32+ |"
                echo "| twitter-bootstrap-3.0.0 | T022 | 9 files | 250+ |"
                echo "| jquery-migrate | T023 | 2 files | 12 |"
                echo "| shared-*.js + perc_utils.js | T024 | 7 files | 100+ |"
                echo "| PercDataTable widget | T025 | 3 files | 96+ |"
                echo "| third-party vendored JS | T026 | 3 files | mixed |"
                echo "| JS critical-severity fixes (xss/code-injection) | T038, T058, T060 | varies | TBD |"
                echo "| JS high/medium fixes | T044 etc. | varies | TBD |"
                ;;
            "system/")
                echo "| ApplicationFiles dojo/trinidad/jstree | T027 | 3+ files | 67 |"
                echo "| dojo vendor files in non-ApplicationFiles paths | T028 | varies | TBD |"
                echo "| Java critical: \`java/xxe\` (PSSerializerUtils) | T039 | 1 file | 2 |"
                echo "| Java critical: \`java/ldap-injection\` (PSJndiGroupProvider) | T040 | 1 file | 1 |"
                echo "| Java high: \`java/zipslip\` (PSArchiveFiles) | T041 | 1 file | 3 |"
                echo "| Java high: \`java/insecure-trustmanager\` (PSDeliveryClient) | T046 | 1 file | 2 |"
                echo "| Java medium: \`java/stack-trace-exposure\`, etc. | T055 etc. | varies | TBD |"
                ;;
            "projects/sitemanage/")
                echo "| test sample HTML (CM1094-SamplePage.html etc.) | T029 | 7 files | 28 |"
                echo "| Java high: \`java/sql-injection\` (PSPageDaoHelper) | T042 | 1 file | 7 |"
                echo "| Java high: \`java/path-injection\` (58 alerts) | T043 | 50+ files | 58 |"
                echo "| Java high: \`java/xss\` (PSSiteDataRestService) | T044 | 1 file | 35 |"
                echo "| Java medium: \`java/unsafe-hostname-verification\`, \`java/error-message-exposure\`, etc. | T053, T054 | varies | TBD |"
                ;;
            "modules/perc-packages/")
                echo "| Per-finding valid fixes | T042-T063 | TBD | 35 |"
                ;;
            "modules/perc-common-ui-bundle/")
                echo "| Per-finding valid fixes | T042-T063 | TBD | 14 |"
                ;;
        esac
        echo
        echo "## Definition of done (per cluster)"
        echo
        echo "1. PR merges to \`development\` with the standard closing checklist"
        echo "   per \`specs/004-zero-code-scanning-alerts/contracts/README.md\` C5."
        echo "2. \`scripts/verify-triage-inventory.sh\` still passes after"
        echo "   the \`linked_pr\` column is updated in \`triage.md\`."
        echo "3. \`scripts/verify-valid-fixes.sh\` reports 0 unlinked valid"
        echo "   rows for this module (T062 / SC-007)."
        echo "4. For US3 (valid): regression test fails on the pre-fix code and"
        echo "   passes on the post-fix code, with the pre-fix commit hash"
        echo "   recorded in the PR body (Constitution III, contracts/C5)."
        echo "5. For US2 (obsolete): \`scripts/verify-distribution-archive.sh\`"
        echo "   confirms the removed files are absent from the rebuilt"
        echo "   \`modules/perc-distribution-tree\` + \`modules/perc-packages\`"
        echo "   JARs and the assembled \`.ppkg\`."
        echo "6. Constitution IX: every review thread on the closing PR is"
        echo "   resolved via the GraphQL \`resolveReviewThread\` mutation"
        echo "   before the merge button is enabled (T078b)."
        echo
        echo "## Coordination"
        echo
        echo "- Spec: \`specs/004-zero-code-scanning-alerts/spec.md\`"
        echo "- Tasks: \`specs/004-zero-code-scanning-alerts/tasks.md\`"
        echo "- Triage: \`docs/ai-generated/tasks/gh-codeql-alerts/triage.md\`"
        echo "- Verify scripts: \`scripts/verify-{triage-inventory,valid-fixes,suppressions,distribution-archive,pr-review-resolution}.sh\`"
        echo "- Stale-cache filter: \`scripts/filter-stale-alerts.sh\` + \`docs/ai-generated/tasks/gh-codeql-alerts/alerts-stale-cache.md\`"
        echo
    } > "$out.md"
    echo "wrote $out.md"
done
