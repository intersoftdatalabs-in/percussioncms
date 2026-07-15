#!/usr/bin/env bash
# scripts/release-audit/lib/report.sh
# User Story 3 — summary report generator.

run_report_phase() {
  local output_dir="$1"
  local inventory="${output_dir}/inventory.json"
  local verdicts="${output_dir}/verdicts.json"
  local excluded="${output_dir}/dependabot-excluded.json"
  local report="${output_dir}/v8.1.7-to-8.2-migration-report.md"
  local config="${output_dir}/_audit_config.json"

  log_info "generating v8.1.7-to-8.2-migration-report.md"

  local total_inv total_excl verdict_dist p0_count backlog_path sec_heur_count empty_modules_count
  total_inv="$(jq 'length' "${inventory}")"
  total_excl="$(jq 'length' "${excluded}")"
  verdict_dist="$(jq -r '[.[].verdict] | group_by(.) | map({k:.[0], v:length}) | .[] | "\(.k)=\(.v)"' "${verdicts}" | tr '\n' ' ' | sed 's/ $//')"
  p0_count="$(jq '[.[] | select(.verdict == "needs-migration" and .securityFlag == true)] | length' "${verdicts}")"
  sec_heur_count="$(jq '[.[] | select(.securityFlag == true)] | length' "${verdicts}")"
  empty_modules_count="$(jq '[.[] | select(.modulePaths | length == 0)] | length' "${inventory}")"
  backlog_path="migration-backlog.md"

  local from_tag to_tag target_branch run_ts
  from_tag="$(jq -r '.fromTag' "${config}")"
  to_tag="$(jq -r '.toTag' "${config}")"
  target_branch="$(jq -r '.targetBranch' "${config}")"
  run_ts="$(jq -r '.runTimestamp' "${config}")"

  # Top-10 needs-migration by priority (P0 first), then by merge date.
  # Join verdicts with inventory to bring in modulePaths/securityFlag/mergedAt.
  local top10
  top10="$(jq -rs '
    .[0] as $inv
    | .[1] as $verd
    | [
        $verd[] as $v
        | ($inv | map(select(.number == $v.prNumber)) | .[0]) as $pr
        | select($v.verdict == "needs-migration")
        | {
            number: $v.prNumber,
            title: ($pr.title // ""),
            mergedAt: ($pr.mergedAt // ""),
            modulePaths: ($pr.modulePaths // []),
            securityFlag: ($v.securityFlag // false),
            priority: (
              if $v.securityFlag then "P0"
              elif (($pr.modulePaths // []) | any(. == "rest" or (. | startswith("projects/sitemanage")) or (. | startswith("deliverytiersuite/delivery-tier-suite")))) then "P1"
              elif (($pr.modulePaths // []) | any(. | startswith("WebUI"))) then "P2"
              else "P3"
              end
            )
          }
      ]
    | sort_by(if .priority == "P0" then 0 elif .priority == "P1" then 1 elif .priority == "P2" then 2 else 3 end, .mergedAt)
    | .[0:10]
    | to_entries
    | .[] | "\(.key + 1). [#\(.value.number)](https://github.com/intersoftdatalabs-in/percussioncms/pull/\(.value.number)) — \(.value.title[0:80]) _(\(.value.priority))_"
  ' "${inventory}" "${verdicts}")"

  {
    printf '# v8.1.7 → %s Migration Report\n\n' "${target_branch}"
    printf '**Tag range**: `%s..%s`  \n' "${from_tag}" "${to_tag}"
    printf '**Target branch**: `%s`  \n' "${target_branch}"
    printf '**Run timestamp**: %s\n\n' "${run_ts}"

    printf '## TL;DR\n\n'
    printf -- '- **Inventory**: %s non-dependabot PRs (after excluding %s dependabot PRs)\n' "${total_inv}" "${total_excl}"
    printf -- '- **Verdict distribution**: %s\n' "${verdict_dist}"
    printf -- '- **P0 (security) backlog items**: %s\n' "${p0_count}"
    printf -- '- **Actionable backlog**: see [`%s`](%s)\n\n' "${backlog_path}" "${backlog_path}"

    printf '## Verdict Distribution\n\n'
    printf '| Verdict | Count |\n|--------|-------|\n'
    jq -r '[.[].verdict] | group_by(.) | map({k:.[0], v:length}) | .[] | "| \(.k) | \(.v) |"' "${verdicts}"
    printf '\n'

    printf '## Top 10 Backlog Items (by priority)\n\n'
    printf '%s\n\n' "${top10}"

    printf '## Exclusions\n\n'
    printf 'Excluded %s dependabot PRs (dependency updates, not in scope per FR-002).\n\n' "${total_excl}"

    printf '## Open Questions / Data Gaps\n\n'
    printf -- '- %s PRs flagged `securityFlag == true` via filename heuristic; per-component dependency version comparison (FR-006a) is a follow-up — current verdicts treat them as `needs-migration` if dev is missing the patched version.\n' "${sec_heur_count}"
    printf -- '- %s PRs without files-changed data have empty `modulePaths`; their priority defaults to P3.\n' "${empty_modules_count}"
    printf -- '- Verdict heuristic uses commit-message tokens; manual review recommended for ambiguous cases (verdict != `already-present` AND verdict != `conflicts-with-newer-design`).\n\n'

    printf '## Next Steps\n\n'
    printf -- '1. Review this report and [`%s`](%s).\n' "${backlog_path}" "${backlog_path}"
    printf -- '2. For each P0 item: assign a porter, open a porting PR per spec US4.\n'
    printf -- '3. Per Constitution Principle IX, when review comments arrive on porting PRs, reply inline AND resolve each thread (see root `AGENTS.md`).\n'
    printf -- '4. Re-run this audit after each v8.x release is tagged.\n'
  } > "${report}"

  log_info "report written: ${report}"
}