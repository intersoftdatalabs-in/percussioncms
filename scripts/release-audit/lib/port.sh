# scripts/release-audit/lib/port.sh
# User Story 4 — per-item porting workflow.
#
# The audit does NOT perform porting (see spec Clarifications Q2); this file
# provides the workflow + helpers a porter uses to take a single
# `needs-migration` item and produce a development-branch PR.
#
# Per Constitution Principle III (Test Discipline) and IX (PR Review Comment
# Resolution), porting PRs MUST include regression tests and follow the
# inline-reply + resolveReviewThread procedure (see root AGENTS.md).

# cherry_pick_pr <pr-number> <target-branch>
# Creates a feature branch from <target-branch>, cherry-picks the v8.1.7
# merge commit (resolved via gh api), returns the branch name on success
# or the conflict list on partial failure.
cherry_pick_pr() {
  local pr_number="$1"
  local target_branch="${2:-development}"

  # Get the merge commit SHA for the v8.1.7 PR
  local merge_sha
  merge_sha="$(gh pr view "${pr_number}" --repo intersoftdatalabs-in/percussioncms \
               --json mergeCommit --jq '.mergeCommit.oid' 2>/dev/null || echo "")"

  if [[ -z "${merge_sha}" ]]; then
    log_error "could not resolve merge commit for PR #${pr_number}"
    return 2
  fi

  local feature_branch="005-migrate-${pr_number}"
  log_info "creating branch ${feature_branch} from ${target_branch}"
  git switch -c "${feature_branch}" "${target_branch}" || return 2

  log_info "cherry-picking ${merge_sha} from PR #${pr_number}"
  git cherry-pick -x "${merge_sha}" || {
    log_warn "cherry-pick had conflicts; resolve and run git cherry-pick --continue"
    return 2
  }

  printf '%s\n' "${feature_branch}"
}

# flag_jdk8_idioms <diff-file>
# Scans the supplied diff for JDK 8-only idioms and emits a warning file
# listing each match's file:line for the porter to translate manually.
flag_jdk8_idioms() {
  local diff="$1"
  local warnings="$2"

  if [[ ! -f "${diff}" ]]; then
    log_error "diff file not found: ${diff}"
    return 2
  fi

  log_info "scanning diff for JDK 8 idioms (javax.ws.rs, javax.persistence, javax.xml.bind, sun.misc, com.sun.)"
  : > "${warnings}"

  # Per-line scan with file:line numbers
  local current_file=""
  local lineno=0
  while IFS= read -r line; do
    if [[ "${line}" =~ ^\+\+\+\ b/ ]]; then
      current_file="${line#+++ b/}"
      lineno=0
    elif [[ "${line}" =~ ^@@ ]]; then
      lineno="$(echo "${line}" | sed -nE 's/^@@ .*\+([0-9]+).*/\1/p')"
    elif [[ "${line}" =~ ^\+[^+] ]]; then
      if [[ "${line}" =~ javax\.ws\.rs|javax\.persistence|javax\.xml\.bind|sun\.misc|com\.sun\. ]]; then
        printf '%s:%s: %s\n' "${current_file}" "${lineno}" "${line}" >> "${warnings}"
      fi
      lineno=$((lineno + 1))
    fi
  done < "${diff}"

  local count
  count="$(wc -l < "${warnings}")"
  if [[ "${count}" -gt 0 ]]; then
    log_warn "JDK 8 idioms detected (${count}); see ${warnings} — translate to jakarta.* / java.* equivalents"
  else
    log_info "no JDK 8 idioms detected"
    rm -f "${warnings}"
  fi
}

# verify_tests <module> <test-class>
# Invokes ./mvn-env.sh -pl <module> -am test -Dtest=<test-class>
# and asserts exit 0.
verify_tests() {
  local module="$1"
  local test_class="$2"

  log_info "running tests: ./mvn-env.sh -pl ${module} -am test -Dtest=${test_class}"
  ./mvn-env.sh -pl "${module}" -am test -Dtest="${test_class}"
}

# spotless_check <module>
# Runs Spotless on a single module. Returns non-zero if formatting needs fix.
spotless_check() {
  local module="$1"
  log_info "running spotless:check on ${module}"
  ./mvn-env.sh -pl "${module}" -am spotless:check
}