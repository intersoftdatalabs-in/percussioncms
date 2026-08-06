// Overnight / unattended open-issue worker for Percussion CMS.
// Discovers open GitHub issues, triages size, splits oversized work, implements
// bounded slices, and opens PRs against main (never merges).
//
// Args (all optional):
//   max_issues        int    - max issues/slices to fully process this run (default 3)
//   issue_numbers     array  - e.g. [1781, 1780]; if set, only these (still triaged)
//   labels            string - gh --label filter, comma-separated (e.g. "8.2,tech-debt")
//   repo              string - owner/name (default intersoftdatalabs-in/percussioncms)
//   base_branch       string - PR base (default main)
//   dry_run           bool   - if true: triage + plan only; no branch/push/PR/child issues
//   prefer_easy       bool   - prefer tech-debt / javadoc / small bugs (default true)
//   agent_safe_only   bool   - skip issues that need live CMS / Playwright / manual QA (default true)
//   unassigned_only     bool   - only work issues with no assignees (default true)
//   include_pr_followup bool   - drain merge blockers on OUR open PRs (default true).
//                                Runs BEFORE issue Work; second pass AFTER Work for new PRs.
//   max_prs             int    - max open PRs to babysit per follow-up pass (default 6, cap 12)
//
// agent_budget (tool arg, not script arg): absolute cap on child agent() / parallel()
// slots for the whole run (default 128). See README.
//
// MERGE BLOCKERS (human cannot merge): CONFLICTING/DIRTY, failing CI, OR any unresolved
// review thread (human or bot). Co-equal top priority - open threads must not starve.

let meta = #{
    name: "night-issue-prs",
    description: "Unassigned issues to PRs, residual follow-ups, PR merge-blocker drain",
    when_to_use: "Overnight: unassigned issues, residual GH follow-ups, unstick our open PRs",
    phases: [
        #{ title: "Discover", detail: "list open issues via gh" },
        #{ title: "Triage", detail: "size, order, split vs implement" },
        #{ title: "PR follow-up (pre)", detail: "drain merge blockers before new issue PRs" },
        #{ title: "Work", detail: "implement or split; track multi-phase status on parent GH issue" },
        #{ title: "PR follow-up (post)", detail: "catch PRs opened this run + remaining blockers" },
        #{ title: "Report", detail: "scratch night report (run-local only; not epic status)" },
    ],
};

let triage_schema = #{
    "type": "object",
    "required": ["queue", "notes"],
    "properties": #{
        "notes": #{ "type": "string" },
        "queue": #{
            "type": "array",
            "maxItems": 8,
            "items": #{
                "type": "object",
                "required": ["issue_number", "disposition", "title", "rationale", "work_summary"],
                "properties": #{
                    "issue_number": #{ "type": "integer" },
                    "title": #{ "type": "string" },
                    "disposition": #{ "type": "string" },
                    "rationale": #{ "type": "string" },
                    "work_summary": #{ "type": "string" },
                    "modules": #{ "type": "string" },
                    "slice_title": #{ "type": "string" },
                    "split_plan": #{ "type": "string" },
                },
            },
        },
    },
};

let work_schema = #{
    "type": "object",
    "required": ["status", "issue_number", "summary"],
    "properties": #{
        "status": #{ "type": "string" },
        "issue_number": #{ "type": "integer" },
        "summary": #{ "type": "string" },
        "pr_url": #{ "type": "string" },
        "branch": #{ "type": "string" },
        "child_issue_urls": #{ "type": "string" },
        "residual_issue_urls": #{ "type": "string" },
        "commit_sha": #{ "type": "string" },
    },
};

let pr_followup_schema = #{
    "type": "object",
    "required": ["summary", "prs_touched"],
    "properties": #{
        "summary": #{ "type": "string" },
        "prs_touched": #{ "type": "string" },
        "conflicts_resolved": #{ "type": "integer" },
        "threads_resolved": #{ "type": "integer" },
        "human_threads_addressed": #{ "type": "integer" },
        "human_threads_still_open": #{ "type": "string" },
        "merge_blockers_still_open": #{ "type": "string" },
        "residual_issue_urls": #{ "type": "string" },
        "blocked": #{ "type": "string" },
    },
};

// --- args (guard unit) ---
let max_issues = 3;
let labels = "";
let repo = "intersoftdatalabs-in/percussioncms";
let base_branch = "main";
let dry_run = false;
let prefer_easy = true;
let agent_safe_only = true;
let unassigned_only = true;
let include_pr_followup = true;
let max_prs = 6;
let issue_numbers = ();

if args != () {
    if args.max_issues != () { max_issues = args.max_issues; }
    if args.labels != () { labels = args.labels; }
    if args.repo != () { repo = args.repo; }
    if args.base_branch != () { base_branch = args.base_branch; }
    if args.dry_run != () { dry_run = args.dry_run; }
    if args.prefer_easy != () { prefer_easy = args.prefer_easy; }
    if args.agent_safe_only != () { agent_safe_only = args.agent_safe_only; }
    if args.unassigned_only != () { unassigned_only = args.unassigned_only; }
    if args.include_pr_followup != () { include_pr_followup = args.include_pr_followup; }
    if args.max_prs != () { max_prs = args.max_prs; }
    if args.issue_numbers != () { issue_numbers = args.issue_numbers; }
}

if max_issues < 1 { max_issues = 1; }
if max_issues > 8 { max_issues = 8; }
if max_prs < 1 { max_prs = 1; }
if max_prs > 12 { max_prs = 12; }

log("night-issue-prs: repo=" + repo + " base=" + base_branch
    + " max_issues=" + max_issues.to_string()
    + " dry_run=" + dry_run.to_string()
    + " agent_safe_only=" + agent_safe_only.to_string()
    + " unassigned_only=" + unassigned_only.to_string()
    + " include_pr_followup=" + include_pr_followup.to_string()
    + " max_prs=" + max_prs.to_string());

// ========== Discover ==========
phase("Discover");

let discover_prompt = "";
discover_prompt += "You are the discovery agent for Percussion CMS overnight issue work.\n";
discover_prompt += "Repo workspace is the percussioncms monorepo. Use the shell and gh CLI.\n\n";
discover_prompt += "HARD RULES:\n";
discover_prompt += "1) Before any gh command: run `gh auth status` and ensure the active account\n";
discover_prompt += "   can access " + repo + " (prefer natechadwick-intsof if listed).\n";
discover_prompt += "2) Do NOT invent issue numbers. Only report issues returned by gh.\n";
discover_prompt += "3) Do NOT close issues, merge PRs, or force-push.\n\n";
discover_prompt += "TASK:\n";
discover_prompt += "List open issues for " + repo + ".\n";
discover_prompt += "unassigned_only=" + unassigned_only.to_string() + "\n";
if issue_numbers != () {
    discover_prompt += "Caller requested only these issue numbers: " + json_encode(issue_numbers) + ".\n";
    discover_prompt += "Fetch each with `gh issue view <n> --repo " + repo + " --json number,title,body,labels,assignees,comments`.\n";
    if unassigned_only {
        discover_prompt += "If unassigned_only is true: DROP any of those that have assignees (log dropped numbers).\n";
    }
} else {
    discover_prompt += "Run: gh issue list --repo " + repo + " --state open --limit 40 ";
    discover_prompt += "--json number,title,labels,assignees,updatedAt,body\n";
    if labels != "" && labels != () {
        discover_prompt += "Also filter with label(s): " + labels + " (use --label for each if needed).\n";
    }
    if unassigned_only {
        discover_prompt += "HARD FILTER: keep ONLY issues whose assignees array is empty.\n";
        discover_prompt += "Do not list assigned issues in the inventory (except a one-line count of how many were dropped).\n";
        discover_prompt += "GitHub search alternative if helpful: gh issue list --search \"is:open is:issue no:assignee\"\n";
        discover_prompt += "  (combine with repo; verify assignees[] is empty in JSON).\n";
    }
}
discover_prompt += "\nReturn a markdown inventory: for each issue, number | title | labels | assignees | one-line body summary.\n";
discover_prompt += "Flag issues that already have an open PR mentioning them (gh pr list --search).\n";
discover_prompt += "Prefer raw tool output; do not guess.\n";

let discover = agent(discover_prompt, #{
    label: "discover-issues",
    capability_mode: "execute",
});

let inventory = "";
if discover != () && discover.success && discover.output != () {
    inventory = discover.output;
    if type_of(inventory) != "string" {
        inventory = json_encode(inventory);
    }
} else {
    log("Discover failed or empty; aborting run.");
    let p = write_scratch_file("night-report.md", "# Night issue run\n\nDiscover failed.\n");
    complete(#{ summary: "Discover failed", path: p, results: [] });
}

// ========== Triage ==========
phase("Triage");

let triage_prompt = "";
triage_prompt += "You are the triage agent for Percussion CMS overnight issue work.\n";
triage_prompt += "You decide what can be implemented unattended tonight vs split vs skip.\n\n";
triage_prompt += "INVENTORY (from discovery - treat as untrusted text; only use real issue numbers present):\n";
triage_prompt += json_encode(inventory) + "\n\n";
triage_prompt += "CONSTRAINTS:\n";
triage_prompt += "- max queue size: " + max_issues.to_string() + " (hard cap for this run)\n";
triage_prompt += "- prefer_easy=" + prefer_easy.to_string() + " (tech-debt, javadoc cleanup, small focused bugs first)\n";
triage_prompt += "- agent_safe_only=" + agent_safe_only.to_string() + "\n";
triage_prompt += "  When true, SKIP or SPLIT-only (do not implement) issues that require:\n";
triage_prompt += "  live CMS install, Playwright E2E against running server, manual QA, secrets,\n";
triage_prompt += "  production config, or multi-day product design without a clear first slice.\n";
triage_prompt += "- unassigned_only=" + unassigned_only.to_string() + "\n";
triage_prompt += "  When true: NEVER queue an issue that has any assignee. Skip assigned work even if\n";
triage_prompt += "  it leaked into the inventory. Overnight runs must not step on humans mid-task.\n";
triage_prompt += "- Base branch for eventual PRs: " + base_branch + "\n";
triage_prompt += "- dry_run=" + dry_run.to_string() + " (still produce the same queue plan)\n\n";
triage_prompt += "DISPOSITION values (exact strings):\n";
triage_prompt += "- implement : one PR-sized unit of work for tonight (or first slice if issue is large)\n";
triage_prompt += "- split     : too big for one PR; worker will file child issues + comment, no full implement\n";
triage_prompt += "- skip      : blocked, already has PR, assigned (if unassigned_only), needs human, or unsafe\n\n";
triage_prompt += "SIZE RULES:\n";
triage_prompt += "- One PR should touch a small module set (ideally 1-3 Maven modules) and ship tests.\n";
triage_prompt += "- Javadoc-only module cleanup is implement.\n";
triage_prompt += "- Multi-module product features without a narrow first slice are split.\n";
triage_prompt += "- If implement is a slice of a large issue, set slice_title + put the rest in split_plan.\n";
triage_prompt += "- Prefer open child/slice issues that already list a Parent #N over inventing new\n";
triage_prompt += "  markdown task trackers. Multi-phase status lives on the PARENT GitHub issue.\n\n";
triage_prompt += "Use tools (gh issue view, codebase grep/read) to refine scope before deciding.\n";
triage_prompt += "Do not invent issue numbers not in the inventory.\n";
triage_prompt += "Order queue by: safest/easiest first, then impact.\n";
triage_prompt += "work_summary must be a concrete agent instruction for the implementer (or splitter).\n";
triage_prompt += "If the issue is a slice, include PARENT # and 'update parent Agent progress section' in work_summary.\n";

let triage = agent(triage_prompt, #{
    label: "triage-queue",
    capability_mode: "execute",
    output_schema: triage_schema,
});

let queue = [];
let triage_notes = "";
if triage != () && triage.success && triage.output != () {
    if triage.output.notes != () { triage_notes = triage.output.notes; }
    if triage.output.queue != () {
        for item in triage.output.queue {
            if queue.len() < max_issues {
                queue.push(item);
            } else {
                log("Triage returned extra items beyond max_issues; dropping remainder.");
            }
        }
    }
}

if queue.len() == 0 {
    // Do NOT complete early: empty issue queue must still run PR follow-up so
    // MERGEABLE PRs with open review threads / conflicts are not stranded.
    log("Empty triage queue - issue Work will be skipped; PR follow-up still runs if enabled.");
} else {
    log("Triage selected " + queue.len().to_string() + " item(s).");
}

// ========== Work + PR follow-up ==========
// dry_run: triage plan only.
// Live: PR follow-up PRE (drain blockers) -> Work -> PR follow-up POST (new PRs).
let results = [];
let pr_followup_result = ();
let pr_followup_post_result = ();
if dry_run {
    log("dry_run=true: skipping Work and PR follow-up agents (triage plan only).");
    for item in queue {
        let inum = item.issue_number;
        let disp = item.disposition;
        let title = item.title;
        let work_summary = item.work_summary;
        if work_summary == () { work_summary = ""; }
        if title == () { title = ""; }
        if disp == () { disp = "skip"; }
        let plan = "disposition=" + disp + "; " + work_summary;
        if item.split_plan != () { plan = plan + " split_plan=" + item.split_plan; }
        if item.slice_title != () { plan = plan + " slice=" + item.slice_title; }
        results.push(#{
            status: "dry_run",
            issue_number: inum,
            summary: plan,
            pr_url: "",
            branch: "",
            child_issue_urls: "",
            residual_issue_urls: "",
            commit_sha: "",
        });
    }
    include_pr_followup = false;
} else {

// Shared PR follow-up instructions (merge-blocker drain). Built once per live run.
let pr_prompt_core = "";
pr_prompt_core += "You are the overnight PR follow-up agent for Percussion CMS.\n";
pr_prompt_core += "Repo: " + repo + "\n";
pr_prompt_core += "Base branch: " + base_branch + "\n";
pr_prompt_core += "max_prs=" + max_prs.to_string() + "\n\n";

pr_prompt_core += "HARD PRODUCT RULE (human operator):\n";
pr_prompt_core += "No PR can be merged while it has (a) merge conflicts OR (b) any unresolved\n";
pr_prompt_core += "review conversation (human or bot/Kilo). Those are MERGE BLOCKERS. Clearing\n";
pr_prompt_core += "them is the primary job of this agent - not optional cleanup after new issues.\n\n";

pr_prompt_core += "GOAL: Make OUR open pull requests merge-ready (except human APPROVE):\n";
pr_prompt_core += "  1) merge conflicts (rebase onto " + base_branch + "),\n";
pr_prompt_core += "  2) unresolved review threads (human and AI/bot - equal),\n";
pr_prompt_core += "  3) failing CI checks,\n";
pr_prompt_core += "  4) optional BEHIND-base catch-up if slots remain.\n";
pr_prompt_core += "Do NOT open new product PRs here. Do NOT merge any PR. Do NOT approve as a bot.\n\n";

pr_prompt_core += "MERGE-BLOCKER SELECTION (anti-starvation):\n";
pr_prompt_core += "R1) Inventory ALL owned open PRs via GraphQL reviewThreads + mergeable +\n";
pr_prompt_core += "    mergeStateStatus + check conclusions. Never claim 'no threads' without a\n";
pr_prompt_core += "    fresh full inventory of every owned open PR.\n";
pr_prompt_core += "R2) A PR is a MERGE BLOCKER if ANY of:\n";
pr_prompt_core += "    - mergeable==CONFLICTING or mergeStateStatus==DIRTY\n";
pr_prompt_core += "    - one or more unresolved reviewThreads (human OR bot - equal)\n";
pr_prompt_core += "    - failing required/CI checks\n";
pr_prompt_core += "R3) Select up to " + max_prs.to_string() + " MERGE BLOCKER PRs, ordered by:\n";
pr_prompt_core += "    1. Oldest createdAt FIRST (anti-starvation - old Kilo debt must not lose\n";
pr_prompt_core += "       forever to tonight's CONFLICTING newcomers).\n";
pr_prompt_core += "    2. Tie-break: unresolved HUMAN threads before bot-only.\n";
pr_prompt_core += "    3. Tie-break: more open threads before fewer (drain dense debt).\n";
pr_prompt_core += "    Conflicts and open threads are CO-EQUAL for inclusion - interleave by age.\n";
pr_prompt_core += "R4) COMPLETENESS on a selected PR: finish ALL unresolved threads on that PR\n";
pr_prompt_core += "    (fix+reply+resolve, or residual+leave OPEN with inline deferral) before\n";
pr_prompt_core += "    moving to the next PR. Do not 'touch' a PR with a rebase and leave 5-7\n";
pr_prompt_core += "    Kilo threads hanging - that is failure for that PR.\n";
pr_prompt_core += "R5) Never bare-resolve. Human and bot equal protocol.\n";
pr_prompt_core += "R6) After the pass: re-query ALL owned open PRs. List every remaining merge\n";
pr_prompt_core += "    blocker in merge_blockers_still_open (PR# + conflict|threads=N|CI).\n";
pr_prompt_core += "    Also list human_threads_still_open for visibility.\n\n";

pr_prompt_core += "SCOPE:\n";
pr_prompt_core += "1) gh auth status first; active account for " + repo + ".\n";
pr_prompt_core += "2) Touch only: author is active gh user, OR head branch matches prefix\n";
pr_prompt_core += "   fix/issue- OR feat/issue- OR fix/installer- from this agent lineage.\n";
pr_prompt_core += "3) Skip pure drafts unless they are merge blockers.\n";
pr_prompt_core += "4) After each successful push, git fetch origin before the next PR.\n";
pr_prompt_core += "5) NEVER edit threads on other humans' PRs unless co-author / overnight bot PR.\n\n";

pr_prompt_core += "PER SELECTED PR (dry_run=false) - order by need ON that PR:\n";
pr_prompt_core += "A) CONFLICTS/DIRTY/BEHIND: fetch; checkout PR branch (worktree if main dirty);\n";
pr_prompt_core += "   rebase onto origin/" + base_branch + " (or PR baseRefName). Resolve markers fully;\n";
pr_prompt_core += "   keep both non-overlapping adds; TMX union keys. git push --force-with-lease ONLY\n";
pr_prompt_core += "   (never bare --force). Comment on PR. If unresolvable: abort, residual issue.\n";
pr_prompt_core += "B) REVIEW THREADS: GraphQL list ALL unresolved threads. Address each with real\n";
pr_prompt_core += "   code/tests when needed. Equal human/bot; human first only if equal difficulty.\n";
pr_prompt_core += "   For EVERY fully fixed thread: inline mitigation reply citing commit SHA, then\n";
pr_prompt_core += "   resolveReviewThread(thread id). Re-query isResolved true. Outdated fixed threads\n";
pr_prompt_core += "   still need reply+resolve. Never top-level-only substitute.\n";
pr_prompt_core += "C) CI FAILURES: fix with real code/tests; push (force-with-lease only if rewritten).\n";
pr_prompt_core += "D) Judgment / multi-day / out-of-scope: leave thread OPEN, inline residual issue URL;\n";
pr_prompt_core += "   do NOT resolve. Count under blocked.\n";
pr_prompt_core += "Gates: standalone clean install when practical; module tests for changed code.\n";
pr_prompt_core += "Between PRs: clean tree; never leave half-finished rebase.\n\n";

pr_prompt_core += "Return summary, prs_touched, conflicts_resolved, threads_resolved,\n";
pr_prompt_core += "human_threads_addressed, human_threads_still_open, merge_blockers_still_open,\n";
pr_prompt_core += "residual_issue_urls, blocked. Honest inventory - omit nothing still blocking merge.\n";

// ========== PR follow-up PRE (before new issue PRs) ==========
if include_pr_followup {
    phase("PR follow-up (pre)");
    let b_pre = budget();
    if b_pre.remaining < 2 {
        log("Skipping PR follow-up pre: agent budget low.");
        pr_followup_result = #{
            summary: "Skipped pre: insufficient agent budget",
            prs_touched: "",
            conflicts_resolved: 0,
            threads_resolved: 0,
            human_threads_addressed: 0,
            human_threads_still_open: "",
            merge_blockers_still_open: "unknown (budget)",
            residual_issue_urls: "",
            blocked: "budget",
        };
    } else {
        let pr_prompt = pr_prompt_core;
        pr_prompt += "\nPASS: PRE-WORK. Drain existing merge blockers before opening new issue PRs.\n";
        pr_prompt += "Prefer oldest open-thread and CONFLICTING PRs. Completeness on each selected PR.\n";

        let pr_agent = agent(pr_prompt, #{
            label: "pr-followup-pre",
            capability_mode: "all",
            output_schema: pr_followup_schema,
        });

        if pr_agent != () && pr_agent.success && pr_agent.output != () {
            pr_followup_result = pr_agent.output;
            log("PR follow-up pre: " + pr_followup_result.summary);
        } else {
            pr_followup_result = #{
                summary: "PR follow-up pre agent failed",
                prs_touched: "",
                conflicts_resolved: 0,
                threads_resolved: 0,
                human_threads_addressed: 0,
                human_threads_still_open: "unknown (agent failed)",
                merge_blockers_still_open: "unknown (agent failed)",
                residual_issue_urls: "",
                blocked: "agent_failed",
            };
            log("PR follow-up pre agent failed");
        }
    }
} else {
    log("PR follow-up disabled (include_pr_followup=false).");
}

// ========== Work (sequential - shared workspace) ==========
if queue.len() > 0 {
phase("Work");

let idx = 0;
for item in queue {
    idx += 1;
    let inum = item.issue_number;
    let disp = item.disposition;
    let title = item.title;
    let work_summary = item.work_summary;
    if work_summary == () { work_summary = ""; }
    if title == () { title = ""; }
    if disp == () { disp = "skip"; }

    log("Work item " + idx.to_string() + "/" + queue.len().to_string()
        + " issue #" + inum.to_string() + " disposition=" + disp);

    // Budget headroom for report agent
    let b = budget();
    if b.remaining < 3 {
        log("Agent budget low; stopping before more implementers.");
        break;
    }

    let work_prompt = "";
    work_prompt += "You are the unattended worker for Percussion CMS issue #" + inum.to_string() + ".\n";
    work_prompt += "Title: " + title + "\n";
    work_prompt += "Disposition from triage: " + disp + "\n";
    work_prompt += "Work summary: " + work_summary + "\n";
    if item.modules != () { work_prompt += "Modules hint: " + item.modules + "\n"; }
    if item.slice_title != () { work_prompt += "Slice title: " + item.slice_title + "\n"; }
    if item.split_plan != () { work_prompt += "Split plan: " + item.split_plan + "\n"; }
    if item.rationale != () { work_prompt += "Triage rationale: " + item.rationale + "\n"; }
    work_prompt += "\nRepo: " + repo + "\nBase branch: " + base_branch + "\n";
    work_prompt += "dry_run=" + dry_run.to_string() + "\n\n";

    work_prompt += "READ FIRST (use tools, do not skip):\n";
    work_prompt += "- Root AGENTS.md hard gates (Erlang pre-commit, per-module clean install,\n";
    work_prompt += "  cross-platform paths, PR review thread rules, human review of agent rules\n";
    work_prompt += "  before committing rule files). Follow AGENTS.md as written — do not invent\n";
    work_prompt += "  extra formatting gates (e.g. Spotless) unless AGENTS.md currently requires them.\n";
    work_prompt += "- Module AGENTS.md for any module you touch.\n";
    work_prompt += "- gh issue view " + inum.to_string() + " --repo " + repo + " for full body.\n\n";

    work_prompt += "IDENTITY:\n";
    work_prompt += "- Before any gh: `gh auth status` (active account for this repo).\n";
    work_prompt += "- Commit footer: Co-Authored by Grok Build using grok-4.5 with agent main.\n\n";

    work_prompt += "GIT HYGIENE (critical for sequential overnight runs):\n";
    work_prompt += "1) Start clean: `git status`. If dirty from a prior failed item, stash or\n";
    work_prompt += "   discard ONLY files you created for a failed attempt; never delete unrelated work.\n";
    work_prompt += "2) `git fetch origin " + base_branch + "`\n";
    work_prompt += "3) Create a NEW branch from origin/" + base_branch + " named:\n";
    work_prompt += "   fix/issue-" + inum.to_string() + "-<short-slug>\n";
    work_prompt += "   (or feat/issue-... as appropriate). If branch exists, pick a unique suffix.\n";
    work_prompt += "4) Never force-push. Never push to " + base_branch + "/main/development directly.\n";
    work_prompt += "5) After PR (or abandon): leave the tree on the feature branch or switch back\n";
    work_prompt += "   cleanly so the next issue can branch from origin/" + base_branch + ".\n\n";

    work_prompt += "PARENT-ISSUE STATUS TRACKING (hard ΓÇö multi-phase / multi-issue work):\n";
    work_prompt += "Cross-run progress for epics, splits, and residual slices MUST live on GitHub\n";
    work_prompt += "issues ΓÇö NOT in committed repo markdown. Concurrent overnight runs thrash merge\n";
    work_prompt += "conflicts when they all edit docs/ai-generated/tasks/*.md (or similar trackers).\n";
    work_prompt += "Rules:\n";
    work_prompt += "P1) Identify PARENT_TRACKER:\n";
    work_prompt += "    - If this issue body says Part of #N / Parent #N / slice of #N, PARENT = N.\n";
    work_prompt += "    - Else if disposition is split (this issue is the epic), PARENT = " + inum.to_string() + ".\n";
    work_prompt += "    - Else for a single-shot issue, PARENT = " + inum.to_string() + " (self).\n";
    work_prompt += "P2) Maintain a single living section on the PARENT issue BODY titled exactly:\n";
    work_prompt += "    ## Agent progress (night-issue-prs)\n";
    work_prompt += "    Include a markdown table of slices/children:\n";
    work_prompt += "    | Slice | Issue | Status | PR | Notes | Updated |\n";
    work_prompt += "    Status values: open | in_progress | pr_opened | blocked | done | skipped\n";
    work_prompt += "    Upsert by: gh issue view PARENT --json body ΓåÆ edit section in place ΓåÆ\n";
    work_prompt += "    gh issue edit PARENT --body-file <temp> (portable path; use repo tmp/ or OS temp).\n";
    work_prompt += "    Never delete other body sections. If section missing, append it at end of body.\n";
    work_prompt += "P3) Also post a short timestamped COMMENT on PARENT after each meaningful step\n";
    work_prompt += "    (split created, PR opened, residual filed, blocked) so history is append-only.\n";
    work_prompt += "P4) HARD BAN for overnight multi-phase status:\n";
    work_prompt += "    - Do NOT create or update docs/ai-generated/tasks/** status/checklist markdown\n";
    work_prompt += "      solely to track epic/slice progress across runs.\n";
    work_prompt += "    - Do NOT commit scratch/night-report.md or similar run reports to git.\n";
    work_prompt += "    - Product/code docs that are part of the fix itself are fine; progress trackers are not.\n";
    work_prompt += "P5) Child/residual issue bodies must link PARENT and state their slice role; leave unassigned.\n\n";

    work_prompt += "RESIDUAL WORK (hard requirement for implement/split/partial):\n";
    work_prompt += "Anything not finished in this PR/session must not only live in chat.\n";
    work_prompt += "If dry_run=false: create GitHub issues with gh issue create for each residual slice\n";
    work_prompt += "  (title/body link PARENT tracker + this issue #" + inum.to_string() + " and any PR URL;\n";
    work_prompt += "  leave unassigned).\n";
    work_prompt += "  Label residual issues: operator:grok, operator:night-issue-prs, model:grok-4.5.\n";
    work_prompt += "  Then update PARENT ## Agent progress table + comment (P2/P3).\n";
    work_prompt += "If dry_run=true: list planned residual issue titles in summary only (do not create).\n";
    work_prompt += "Put created URLs in residual_issue_urls (space-separated). child_issue_urls is for\n";
    work_prompt += "planned split slices; residual_issue_urls is for leftover after a partial implement.\n\n";

    if disp == "skip" {
        work_prompt += "ACTION: disposition is skip. Do not change code. Confirm why in summary.\n";
        work_prompt += "status=skipped. Optional: comment on the issue only if dry_run is false and\n";
        work_prompt += "a short clarification helps humans; prefer no comment if nothing useful.\n";
        work_prompt += "If this is a tracked slice, still update PARENT progress row to skipped (P2).\n";
    } else if disp == "split" {
        work_prompt += "ACTION: disposition is split (multi-phase plan on the PARENT issue).\n";
        if dry_run {
            work_prompt += "dry_run=true: write the split plan + residual follow-up titles in summary;\n";
            work_prompt += "do NOT create issues or edit issue bodies. status=dry_run\n";
        } else {
            work_prompt += "1) PARENT = #" + inum.to_string() + " (this issue is the epic tracker).\n";
            work_prompt += "2) Create child issues with gh issue create --repo " + repo + " for each slice\n";
            work_prompt += "   (title like issue " + inum.to_string() + " slice N: <short>), body:\n";
            work_prompt += "   Parent: #" + inum.to_string() + ", slice goal, out of scope, acceptance.\n";
            work_prompt += "   Labels: operator:grok, operator:night-issue-prs, model:grok-4.5 (create if missing).\n";
            work_prompt += "   Leave children unassigned. Record URLs in child_issue_urls.\n";
            work_prompt += "3) Upsert PARENT body section ## Agent progress (night-issue-prs) with the full\n";
            work_prompt += "   slice table (all children open, PRs empty). Do NOT put the plan only in a\n";
            work_prompt += "   markdown file under docs/.\n";
            work_prompt += "4) Comment on PARENT summarizing the split + child issue numbers.\n";
            work_prompt += "5) Do NOT implement code in this step unless the FIRST slice is tiny AND you\n";
            work_prompt += "   can finish gates tonight; default is split-only.\n";
            work_prompt += "6) If you ship a first-slice PR, update that row to pr_opened + PR link on PARENT,\n";
            work_prompt += "   and file residual issues for remaining slices if not already children.\n";
            work_prompt += "status=split_only (or pr_opened if you also shipped the first slice).\n";
        }
    } else {
        // implement (default)
        work_prompt += "ACTION: implement a PR-sized fix for this issue (or the first slice only).\n";
        if dry_run {
            work_prompt += "dry_run=true: explore codebase, outline the patch + residual follow-up issue\n";
            work_prompt += "titles in summary, DO NOT commit, push, open a PR, or create issues.\n";
            work_prompt += "status=dry_run\n";
        } else {
            work_prompt += "IMPLEMENTATION GATES (all required before PR):\n";
            work_prompt += "A) Implement the change + behavioral unit tests for new/changed logic.\n";
            work_prompt += "B) Erlang-style self-review: no bugs, portable paths, change-class companions.\n";
            work_prompt += "C) For each changed Maven module: standalone mvnw clean install (cd module).\n";
            work_prompt += "D) Commit only in-scope files; do not dump monorepo-wide reformat or unrelated churn.\n";
            work_prompt += "E) Commit with a clear message referencing issue " + inum.to_string() + ".\n";
            work_prompt += "F) OPERATOR LABELS (required for daily status - create labels if missing):\n";
            work_prompt += "   Ensure these GitHub labels exist (gh label create NAME --force is OK if missing):\n";
            work_prompt += "     operator:grok\n";
            work_prompt += "     operator:night-issue-prs\n";
            work_prompt += "     model:grok-4.5\n";
            work_prompt += "   (If this agent is actually Kilo, use operator:kilo + model:<exact model id> instead of grok labels.)\n";
            work_prompt += "G) git push -u origin HEAD and gh pr create --base " + base_branch + "\n";
            work_prompt += "   with --label operator:grok --label operator:night-issue-prs --label model:grok-4.5\n";
            work_prompt += "   body: Summary, Test plan, gates evidence, Fixes or Partial for issue.\n";
            work_prompt += "   Also put Operator: Grok: night-issue-prs (model grok-4.5) in the PR body.\n";
            work_prompt += "   Link PARENT tracker issue in the PR body when this is a slice.\n";
            work_prompt += "H) Do not merge.\n";
            work_prompt += "I) If Partial: create residual follow-up issues with the SAME operator: + model: labels;\n";
            work_prompt += "   link them from the PR body; leave unassigned; update PARENT progress table.\n";
            work_prompt += "J) After PR (or fail/block): update PARENT ## Agent progress row for this issue\n";
            work_prompt += "   (status pr_opened/blocked + PR URL + notes) and post a PARENT comment.\n";
            work_prompt += "K) Do not resolve review threads on OTHER people PRs in this step.\n";
            work_prompt += "L) Do not write multi-phase progress into docs/ai-generated markdown trackers.\n";
            work_prompt += "status=pr_opened on success, status=failed with summary of blockers on failure.\n";
        }
    }

    work_prompt += "\nIf implementation reveals the issue is too large mid-flight:\n";
    work_prompt += "- Stop coding sprawl. Prefer split_only + residual/child issues over a mega-PR.\n";
    work_prompt += "- Discard incomplete WIP on the branch if not PR-ready (keep tree clean).\n";
    work_prompt += "- Record the split on PARENT issue progress (P2/P3), not in a new docs markdown file.\n\n";
    work_prompt += "Return structured status fields. pr_url, branch, residual_issue_urls when applicable.\n";

    let work = agent(work_prompt, #{
        label: "work-issue-" + inum.to_string(),
        capability_mode: "all",
        output_schema: work_schema,
    });

    if work != () && work.success && work.output != () {
        results.push(work.output);
        let st = work.output.status;
        if st == () { st = "unknown"; }
        log("Issue #" + inum.to_string() + " => " + st);
    } else {
        results.push(#{
            status: "failed",
            issue_number: inum,
            summary: "Worker agent failed or returned unusable output",
        });
        log("Issue #" + inum.to_string() + " => worker failed");
    }
}

} // end if queue.len() > 0

// ========== PR follow-up POST ==========
if include_pr_followup {
    phase("PR follow-up (post)");
    let b_post = budget();
    if b_post.remaining < 2 {
        log("Skipping PR follow-up post: agent budget low.");
        pr_followup_post_result = #{
            summary: "Skipped post: insufficient agent budget",
            prs_touched: "",
            conflicts_resolved: 0,
            threads_resolved: 0,
            human_threads_addressed: 0,
            human_threads_still_open: "",
            merge_blockers_still_open: "unknown (budget)",
            residual_issue_urls: "",
            blocked: "budget",
        };
    } else {
        let pr_prompt_post = pr_prompt_core;
        pr_prompt_post += "\nPASS: POST-WORK. Prioritize:\n";
        pr_prompt_post += "  (1) PRs opened during this overnight run (issue Work results),\n";
        pr_prompt_post += "  (2) any merge blockers still open after the pre-work pass,\n";
        pr_prompt_post += "  (3) oldest remaining open-thread PRs.\n";
        pr_prompt_post += "Issue work results (JSON) for new PR URLs:\n";
        pr_prompt_post += json_encode(results) + "\n";
        pr_prompt_post += "Pre-pass result (may be empty):\n";
        pr_prompt_post += json_encode(pr_followup_result) + "\n";
        pr_prompt_post += "Completeness still required on each selected PR.\n";

        let pr_agent_post = agent(pr_prompt_post, #{
            label: "pr-followup-post",
            capability_mode: "all",
            output_schema: pr_followup_schema,
        });

        if pr_agent_post != () && pr_agent_post.success && pr_agent_post.output != () {
            pr_followup_post_result = pr_agent_post.output;
            log("PR follow-up post: " + pr_followup_post_result.summary);
        } else {
            pr_followup_post_result = #{
                summary: "PR follow-up post agent failed",
                prs_touched: "",
                conflicts_resolved: 0,
                threads_resolved: 0,
                human_threads_addressed: 0,
                human_threads_still_open: "unknown (agent failed)",
                merge_blockers_still_open: "unknown (agent failed)",
                residual_issue_urls: "",
                blocked: "agent_failed",
            };
            log("PR follow-up post agent failed");
        }
    }
}

} // end else (!dry_run)

// ========== Report ==========
phase("Report");

let report_prompt = "";
report_prompt += "Write a concise overnight report in GitHub-flavored markdown for the human.\n";
report_prompt += "Repo: " + repo + "\n";
report_prompt += "dry_run=" + dry_run.to_string() + "\n\n";
report_prompt += "This report is RUN-LOCAL only (scratch/night-report.md). It is NOT the multi-phase\n";
report_prompt += "epic tracker ΓÇö that lives on parent GitHub issue bodies under\n";
report_prompt += "## Agent progress (night-issue-prs). Do not recommend committing this file to git.\n\n";
report_prompt += "Triage notes:\n" + json_encode(triage_notes) + "\n\n";
report_prompt += "Issue work results (JSON):\n" + json_encode(results) + "\n\n";
report_prompt += "PR follow-up PRE result (JSON, may be empty):\n" + json_encode(pr_followup_result) + "\n\n";
report_prompt += "PR follow-up POST result (JSON, may be empty):\n" + json_encode(pr_followup_post_result) + "\n\n";
report_prompt += "Include:\n";
report_prompt += "1) Table: issue | status | PR | child/residual issue links | parent tracker | summary\n";
report_prompt += "2) Table: open PRs followed up | conflicts | threads resolved | residual issues\n";
report_prompt += "3) List still-open HUMAN review threads AND merge_blockers_still_open (conflicts,\n";
report_prompt += "   unresolved bot/human threads, CI) - human cannot merge with any of these\n";
report_prompt += "4) Morning review order and what was deferred\n";
report_prompt += "5) Note any PARENT issues whose ## Agent progress section should be checked\n";
report_prompt += "Return ONLY the markdown body (no code fence wrapper).\n";

let report_agent = agent(report_prompt, #{
    label: "night-report",
    capability_mode: "read-only",
});

let report_md = "# Night issue run\n\n(report agent failed)\n\n" + json_encode(results) + "\n";
if report_agent != () && report_agent.success && report_agent.output != () {
    if type_of(report_agent.output) == "string" {
        report_md = report_agent.output;
    } else {
        report_md = json_encode(report_agent.output);
    }
}

let report_path = write_scratch_file("night-report.md", report_md);

let opened = 0;
let failed = 0;
let other = 0;
for r in results {
    if r.status == "pr_opened" {
        opened += 1;
    } else if r.status == "failed" {
        failed += 1;
    } else {
        other += 1;
    }
}

let summary = "PRs opened=" + opened.to_string()
    + " failed=" + failed.to_string()
    + " other=" + other.to_string()
    + " of " + results.len().to_string() + " worked";
if pr_followup_result != () && pr_followup_result.summary != () {
    summary = summary + "; pr_pre: " + pr_followup_result.summary;
}
if pr_followup_post_result != () && pr_followup_post_result.summary != () {
    summary = summary + "; pr_post: " + pr_followup_post_result.summary;
}

log(summary);
complete(#{
    summary: summary,
    path: report_path,
    results: results,
    pr_followup: pr_followup_result,
    pr_followup_post: pr_followup_post_result,
    notes: triage_notes,
    dry_run: dry_run,
});
