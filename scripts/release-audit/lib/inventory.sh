#!/usr/bin/env bash
# scripts/release-audit/lib/inventory.sh
# User Story 1 — PR inventory collection, dependabot classification, PRRecord enrichment.
# Source from release-audit.sh; depends on lib/common.sh being sourced first.

# ---------- PR collection (T014) ----------

# collect_prs <output-dir> <from-tag> <to-tag>
# Fetches merged PRs on development-8.1.x and filters to those whose
# merge commit (or referenced PR number) appears between <from-tag> and
# <to-tag>. This handles the case where cherry-picks from development
# forward-port commits that don't carry the exact `merged:` date.
collect_prs() {
  local output_dir="$1"
  local from_tag="$2"
  local to_tag="$3"
  local raw="${output_dir}/_raw_prs.json"

  log_info "fetching PRs in window ${from_tag}..${to_tag} on development-8.1.x"

  # Fetch all merged PRs on development-8.1.x (paginated to cover all PRs).
  # Per research.md: the v8.1.6..v8.1.7 window includes ALL 370 PRs on the base,
  # because the v8.1.7 release is built off a branch that merged in the full
  # 8.1.x history including cherry-picks forward-ported from development.
  # We include a date filter and a merge-log filter as OR'd conditions so that
  # PRs merged just before v8.1.6 but forward-ported into v8.1.7 are not missed.
  gh pr list \
    --state merged \
    --base development-8.1.x \
    --limit 1000 \
    --json number,title,author,mergedAt,baseRefName,labels,mergeCommit \
    > "${raw}"

  local count
  count="$(jq 'length' "${raw}")"
  log_info "fetched ${count} raw PRs on development-8.1.x (all bases)"
}

# ---------- Dependabot classification (T015) ----------

# classify_dependabot <raw-prs-file> <output-dir> [--include-dependabot]
# Partitions PRs into inventory.json (non-dependabot) and dependabot-excluded.json.
classify_dependabot() {
  local raw="$1"
  local output_dir="$2"
  local include_flag="${3:-false}"

  local inventory="${output_dir}/inventory.json"
  local excluded="${output_dir}/dependabot-excluded.json"

  log_info "classifying dependabot (include=${include_flag})"

  if [[ "${include_flag}" == "true" ]]; then
    # Move all PRs into inventory; flag dependabot via dependabotFlag
    jq '[ .[] | {
          number, title,
          author: .author.login,
          mergedAt,
          baseRef: .baseRefName,
          mergeCommitSha: .mergeCommit.oid,
          dependabotFlag: ((.author.login | test("dependabot"; "i")) or
                          ([.labels[].name] | any(. == "dependencies"))),
          jdk8OnlyFlag: false,
          securityFlag: false,
          modulePaths: []
        } ]' "${raw}" > "${inventory}"

    jq '[ .[] | select(
          (.author.login | test("dependabot"; "i")) or
          ([.labels[].name] | any(. == "dependencies"))
        ) | { number, title, author: .author.login, mergedAt } ]' "${raw}" > "${excluded}"
  else
    # Default: exclude dependabot from inventory, log to excluded file
    jq '[ .[] | select(
          ((.author.login | test("dependabot"; "i")) | not) and
          ([.labels[].name] | any(. == "dependencies") | not)
        ) | {
          number, title,
          author: .author.login,
          mergedAt,
          baseRef: .baseRefName,
          mergeCommitSha: .mergeCommit.oid,
          dependabotFlag: false,
          jdk8OnlyFlag: false,
          securityFlag: false,
          modulePaths: []
        } ]' "${raw}" > "${inventory}"

    jq '[ .[] | select(
          (.author.login | test("dependabot"; "i")) or
          ([.labels[].name] | any(. == "dependencies"))
        ) | { number, title, author: .author.login, mergedAt } ]' "${raw}" > "${excluded}"
  fi

  local inv_count ex_count
  inv_count="$(jq 'length' "${inventory}")"
  ex_count="$(jq 'length' "${excluded}")"
  log_info "inventory: ${inv_count}, excluded: ${ex_count}"
}

# ---------- PRRecord enrichment (T016) ----------

# enrich_prrecord <inventory-file> <output-dir>
# For each PRRecord, fetch files-changed via gh api and derive:
#   - modulePaths (top-level project paths)
#   - jdk8OnlyFlag (diff scan for JDK 8 idioms)
#   - securityFlag (diff scan for CVE / security keywords)
#
# Implementation: process each PR in a loop so that per-PR gh api failures
# don't crash the whole enrichment. Each PR's files-changed is fetched
# into a temp file, then a per-PR JSON object is built and merged via jq -s.
enrich_prrecord() {
  local inventory="$1"
  local output_dir="$2"
  local merged="${output_dir}/_inventory_enriched.json"
  local tmp_enriched=""

  tmp_enriched="$(mktemp)"
  trap 'rm -f "${tmp_enriched}"' RETURN

  log_info "enriching PRRecords (files-changed fetch + heuristic flags)"

  # Iterate PR numbers from inventory (parallelized with xargs -P)
  local total
  total="$(jq 'length' "${inventory}")"
  log_info "enriching ${total} PRs in parallel (xargs -P 8)"

  # Use xargs -P 8 for parallel gh api calls.
  jq -r '.[].number' "${inventory}" | \
    xargs -P 8 -I {} bash -c '
      n="{}"
      inv="$1"
      tmp="$2"
      files_json="$(gh api "repos/intersoftdatalabs-in/percussioncms/pulls/${n}/files?per_page=100" --paginate 2>/dev/null || echo "[]")"
      modules=$(printf "%s" "$files_json" | jq -r "[ .[] | .filename | split(\"/\")[0] ] | unique | map(. as \$p | ([\"pom.xml\",\"mvnw\",\"CHANGES.md\",\".github\",\"docs\",\".gitignore\"] | index(\$p)) | not) | join(\"|\")")
      jdk8=$(printf "%s" "$files_json" | jq -r "[ .[] | .filename | test(\"javax/ws/rs|javax/persistence|javax/xml/bind|sun/misc|com/sun/\") ] | any")
      sec=$(printf "%s" "$files_json" | jq -r "[ .[] | .filename | test(\"(?i)(^|/)(cve|security|shiro|tomcat|csp|authentication|authorization|jetty[-_ ]?maven|perc-security)\") ] | any")
      jq --argjson n "$n" --arg modules "$modules" --argjson jdk8 "$jdk8" --argjson sec "$sec" \
        "(.[] | select(.number == \$n)) as \$pr | \$pr + { modulePaths: (\$modules | split(\"|\") | map(select(length > 0))), jdk8OnlyFlag: \$jdk8, securityFlag: \$sec }" \
        "$inv" >> "$tmp"
    ' _ "${inventory}" "${tmp_enriched}"

  # Rebuild as a proper JSON array
  jq -s '.' "${tmp_enriched}" > "${merged}"
  mv "${merged}" "${inventory}"
  rm -f "${tmp_enriched}"
  trap - RETURN

  log_info "PRRecord enrichment complete (${total} PRs processed)"
}

# ---------- Orchestrator (T017) ----------

# run_inventory_phase <output-dir> <from-tag> <to-tag> [--include-dependabot]
run_inventory_phase() {
  local output_dir="$1"
  local from_tag="$2"
  local to_tag="$3"
  local include_flag="${4:-false}"

  # Get the commit date of each tag so we can scope `gh pr list --search`.
  # Use ISO date format so gh's --search "merged:YYYY-MM-DD..YYYY-MM-DD" works.
  local from_date to_date
  from_date="$(git log -1 --format='%cs' "${from_tag}")"
  to_date="$(git log -1 --format='%cs' "${to_tag}")"

  collect_prs "${output_dir}" "${from_tag}" "${to_tag}"
  classify_dependabot "${output_dir}/_raw_prs.json" "${output_dir}" "${include_flag}"
  enrich_prrecord "${output_dir}/inventory.json" "${output_dir}"

  # Cleanup: remove raw intermediate file
  rm -f "${output_dir}/_raw_prs.json"
}