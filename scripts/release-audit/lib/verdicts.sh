#!/usr/bin/env bash
# scripts/release-audit/lib/verdicts.sh
# User Story 2 — per-PR verdict classification.
# Source from release-audit.sh; depends on lib/common.sh being sourced first.

# ---------- Path resolution (T020) ----------

# resolve_dev_path <v817-path>
# Handles known file migrations between development-8.1.x and development:
#   system/Packages/    → modules/perc-packages/src/main/resources/Packages/
# Extend this function as new migrations are discovered.
resolve_dev_path() {
  local p="$1"
  case "${p}" in
    system/Packages/*)
      printf '%s\n' "modules/perc-packages/src/main/resources/Packages/${p#system/Packages/}"
      ;;
    *)
      printf '%s\n' "${p}"
      ;;
  esac
}

# ---------- Verdict enum ----------
readonly VERDICT_ALREADY_PRESENT="already-present"
readonly VERDICT_NEEDS_MIGRATION="needs-migration"
readonly VERDICT_NOT_APPLICABLE="not-applicable"
readonly VERDICT_SUPERSEDED="superseded"
readonly VERDICT_CONFLICTS="conflicts-with-newer-design"

# Path to this script's directory (used by parallel subprocesses)
: "${SCRIPT_DIR:=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)}"
export SCRIPT_DIR

# classify_pr <pr-json-string> <output-dir>
# Echoes a single PRVerdict JSON object to stdout.
# Side effect: writes the same verdict to <output-dir>/_evidence/<n>.json
#
# Performance:
#   - Uses $DEV_PATHS_FILE (cached dev tree paths) for existence checks.
#   - Each call does at most 2 git operations (git show for diff, git log for token).
#   - Total budget: ~141 PRs * ~200ms = ~30s.
classify_pr() {
  local pr="$1"
  local output_dir="$2"

  local n merge_commit_sha dependabot_flag jdk8 sec
  n="$(jq -r '.number' <<<"${pr}")"
  merge_commit_sha="$(jq -r '.mergeCommitSha // ""' <<<"${pr}")"
  dependabot_flag="$(jq -r '.dependabotFlag // false' <<<"${pr}")"
  jdk8="$(jq -r '.jdk8OnlyFlag // false' <<<"${pr}")"
  sec="$(jq -r '.securityFlag // false' <<<"${pr}")"

  local evidence_dir="${output_dir}/_evidence"
  mkdir -p "${evidence_dir}"

  local verdict="${VERDICT_NEEDS_MIGRATION}"
  local evidence_commit=""
  local evidence_file=""
  local evidence_note="not found at path on development"

  # Defensive defaults
  if [[ "${dependabot_flag}" == "true" ]]; then
    verdict="${VERDICT_NOT_APPLICABLE}"
    evidence_note="excluded as dependabot in inventory phase"
    emit_verdict "${n}" "${verdict}" "${evidence_commit}" "${evidence_file}" "${evidence_note}" "${jdk8}" "${sec}" "${evidence_dir}"
    return 0
  fi

  if [[ "${jdk8}" == "true" ]]; then
    verdict="${VERDICT_NOT_APPLICABLE}"
    evidence_note="JDK-8-only idiom detected in PR; superseded by JDK 21 / Jakarta EE 10 on development"
    emit_verdict "${n}" "${verdict}" "${evidence_commit}" "${evidence_file}" "${evidence_note}" "${jdk8}" "${sec}" "${evidence_dir}"
    return 0
  fi

  # Get the diff files for the v8.1.7 merge commit (timeout-protected)
  local diff_files
  diff_files="$(timeout 10 git show --name-only --pretty=format: "${merge_commit_sha}" 2>/dev/null | grep -v '^$' || true)"

  if [[ -z "${diff_files}" ]]; then
    verdict="${VERDICT_NOT_APPLICABLE}"
    evidence_note="merge commit ${merge_commit_sha} not resolvable in local clone; cannot classify"
    emit_verdict "${n}" "${verdict}" "${evidence_commit}" "${evidence_file}" "${evidence_note}" "${jdk8}" "${sec}" "${evidence_dir}"
    return 0
  fi

  # Existence check using cached DEV_PATHS_FILE (fast grep -F, no git show per file)
  local any_exists="false"
  local first_existing=""
  while IFS= read -r f; do
    [[ -z "${f}" ]] && continue
    local dp
    dp="$(resolve_dev_path "${f}")"
    if [[ -n "${DEV_PATHS_FILE:-}" ]] && timeout 2 grep -Fqx -- "${dp}" "${DEV_PATHS_FILE}" 2>/dev/null; then
      any_exists="true"
      [[ -z "${first_existing}" ]] && first_existing="${dp}"
      break
    fi
  done <<<"${diff_files}"

  if [[ "${any_exists}" == "false" ]]; then
    verdict="${VERDICT_CONFLICTS}"
    evidence_note="all diff target paths absent on development; likely deleted by a refactor (first_diff=${diff_files%%$'\n'*})"
    emit_verdict "${n}" "${verdict}" "${evidence_commit}" "${evidence_file}" "${evidence_note}" "${jdk8}" "${sec}" "${evidence_dir}"
    return 0
  fi

  # Heuristic: extract a stable token from the commit subject and check if dev's
  # first-existing path contains it.
  local token
  token="$(timeout 5 git log -1 --format='%s' "${merge_commit_sha}" 2>/dev/null | tr '[:upper:]' '[:lower:]' | grep -oE '[a-z][a-z_-]{7,}' | sort -u | head -3 | tr '\n' '|' | sed 's/|$//')"

  if [[ -n "${token}" && -n "${first_existing}" ]]; then
    local hit
    hit="$(timeout 10 git show "development:${first_existing}" 2>/dev/null | timeout 5 grep -ciE "${token}" || true)"
    if [[ "${hit}" -gt 0 ]]; then
      verdict="${VERDICT_ALREADY_PRESENT}"
      evidence_commit="$(timeout 5 git log development --oneline -- "${first_existing}" 2>/dev/null | head -1 | awk '{print $1}')"
      evidence_file="${first_existing}"
      evidence_note="string token '${token}' found in development:${first_existing}"
    fi
  fi

  if [[ "${verdict}" == "${VERDICT_NEEDS_MIGRATION}" ]]; then
    evidence_note="not found at path on development (first_existing=${first_existing})"
  fi

  emit_verdict "${n}" "${verdict}" "${evidence_commit}" "${evidence_file}" "${evidence_note}" "${jdk8}" "${sec}" "${evidence_dir}"
}

# emit_verdict <n> <verdict> <commit> <file> <note> <jdk8> <sec> <evidence_dir>
# Writes the verdict as both:
#   - <evidence_dir>/<n>.json
#   - a single JSON object on stdout (for piping into verdicts.json)
emit_verdict() {
  local n="$1"; local verdict="$2"; local commit="$3"; local file="$4"
  local note="$5"; local jdk8="$6"; local sec="$7"; local evidence_dir="$8"

  local payload
  payload="$(jq -n \
    --argjson n "${n}" \
    --arg verdict "${verdict}" \
    --arg commit "${commit}" \
    --arg file "${file}" \
    --arg note "${note}" \
    --argjson jdk8 "${jdk8}" \
    --argjson sec "${sec}" \
    '{prNumber:$n, verdict:$verdict, evidenceCommit:$commit, evidenceFilePath:$file, evidenceNote:$note, jdk8Only:$jdk8, securityFlag:$sec}')"

  printf '%s\n' "${payload}" > "${evidence_dir}/${n}.json"
  printf '%s\n' "${payload}"
}

# ---------- Orchestrator (T024) ----------

# run_verdicts_phase <output-dir> <target-branch>
run_verdicts_phase() {
  local output_dir="$1"
  local target_branch="$2"
  local inventory="${output_dir}/inventory.json"
  local evidence_dir="${output_dir}/_evidence"
  local verdicts="${output_dir}/verdicts.json"

  log_info "classifying verdicts against ${target_branch}"

  if ! git rev-parse --verify "${target_branch}" >/dev/null 2>&1; then
    log_warn "target branch ${target_branch} not local; relying on remote-tracking"
  fi

  mkdir -p "${evidence_dir}"

  # Cache the full set of dev tree paths to a file (avoiding ARG_MAX for export).
  # classify_pr reads from this file via grep -F for O(1) existence checks.
  log_info "caching ${target_branch} tree paths for existence checks"
  DEV_PATHS_FILE="${output_dir}/_dev_paths.txt"
  git ls-tree -r --name-only "${target_branch}" 2>/dev/null > "${DEV_PATHS_FILE}"
  export DEV_PATHS_FILE
  log_info "cached $(wc -l < "${DEV_PATHS_FILE}") dev paths"

  # Sequential classify_pr loop with timeout-guarded git operations.
  # With DEV_PATHS_FILE cached, each PR takes ~200ms; 141 PRs ≈ 30s.
  local collected
  collected="$(mktemp)"
  local n
  n=0
  while IFS= read -r pr; do
    [[ -z "${pr}" ]] && continue
    classify_pr "${pr}" "${output_dir}" >> "${collected}" 2>/dev/null || true
    n=$((n + 1))
  done < <(jq -c '.[]' "${inventory}")

  jq -s '.' "${collected}" > "${verdicts}"
  rm -f "${collected}" "${DEV_PATHS_FILE}"
  unset DEV_PATHS_FILE

  log_info "verdicts written: $(jq 'length' "${verdicts}") entries from ${n} PRs processed"
}