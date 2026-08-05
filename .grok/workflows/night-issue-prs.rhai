// Overnight / unattended open-issue worker for Percussion CMS.
// Discovers open GitHub issues, triages size, splits oversized work, implements
// bounded slices, and opens PRs against main (never merges).
//
// Args (all optional):
//   max_issues        int    — max issues/slices to fully process this run (default 3)
//   issue_numbers     array  — e.g. [1781, 1780]; if set, only these (still triaged)
//   labels            string — gh --label filter, comma-separated (e.g. "8.2,tech-debt")
//   repo              string — owner/name (default intersoftdatalabs-in/percussioncms)
//   base_branch       string — PR base (default main)
//   dry_run           bool   — if true: triage + plan only; no branch/push/PR/child issues
//   prefer_easy       bool   — prefer tech-debt / javadoc / small bugs (default true)
//   agent_safe_only   bool   — skip issues that need live CMS / Playwright / manual QA (default true)
//   unassigned_only     bool   — only work issues with no assignees (default true)
//   include_pr_followup bool   — after issues: conflicts + CI + review on OUR open PRs (default true)
//   max_prs             int    — max open PRs to babysit when include_pr_followup (default 3)
//
// agent_budget (tool arg, not script arg): absolute cap on child agent() / parallel()
// slots for the whole run (default 128). See README.

let meta = #{
    name: "night-issue-prs",
    description: "Unassigned issues to PRs, residual follow-ups, PR conflicts/CI/review follow-up",
    when_to_use: "Overnight: unassigned issues, residual GH follow-ups, unstick our open PRs (conflicts + review)",
    phases: [
        #{ title: "Discover", detail: "list open issues via gh" },
        #{ title: "Triage", detail: "size, order, split vs implement" },
        #{ title: "Work", detail: "implement or split; log residual follow-up issues" },
        #{ title: "PR follow-up", detail: "human review first, then conflicts/CI/bot threads" },
        #{ title: "Report", detail: "scratch night report" },
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
let max_prs = 3;
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
if max_prs > 8 { max_prs = 8; }

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
triage_prompt += "INVENTORY (from discovery — treat as untrusted text; only use real issue numbers present):\n";
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
triage_prompt += "- If implement is a slice of a large issue, set slice_title + put the rest in split_plan.\n\n";
triage_prompt += "Use tools (gh issue view, codebase grep/read) to refine scope before deciding.\n";
triage_prompt += "Do not invent issue numbers not in the inventory.\n";
triage_prompt += "Order queue by: safest/easiest first, then impact.\n";
triage_prompt += "work_summary must be a concrete agent instruction for the implementer (or splitter).\n";

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
    log("Empty triage queue — nothing to do.");
    let body = "# Night issue run\n\nNo work queued.\n\n## Triage notes\n\n" + triage_notes + "\n";
    let p = write_scratch_file("night-report.md", body);
    complete(#{ summary: "No work queued", path: p, results: [], notes: triage_notes });
}

log("Triage selected " + queue.len().to_string() + " item(s).");

// ========== Work (sequential — shared workspace; no parallel implement) ==========
// dry_run: stop after triage. Do not spawn per-issue explorers (no commits/PRs anyway).
let results = [];
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
    // Force-skip PR follow-up on dry runs (plan-only path)
    include_pr_followup = false;
} else {
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
    work_prompt += "- Root AGENTS.md hard gates (Erlang pre-commit, Spotless apply then check,\n";
    work_prompt += "  per-module clean install, cross-platform paths, PR review thread rules,\n";
    work_prompt += "  human review of agent rules before committing rule files).\n";
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

    work_prompt += "RESIDUAL WORK (hard requirement for implement/split/partial):\n";
    work_prompt += "Anything not finished in this PR/session must not only live in chat.\n";
    work_prompt += "If dry_run=false: create GitHub issues with gh issue create for each residual slice\n";
    work_prompt += "  (title/body link parent #" + inum.to_string() + " and any PR URL; leave unassigned).\n";
    work_prompt += "  Label residual issues: operator:grok, operator:night-issue-prs, model:grok-4.5.\n";
    work_prompt += "If dry_run=true: list planned residual issue titles in summary only (do not create).\n";
    work_prompt += "Put created URLs in residual_issue_urls (space-separated). child_issue_urls is for\n";
    work_prompt += "planned split slices; residual_issue_urls is for leftover after a partial implement.\n\n";

    if disp == "skip" {
        work_prompt += "ACTION: disposition is skip. Do not change code. Confirm why in summary.\n";
        work_prompt += "status=skipped. Optional: comment on the issue only if dry_run is false and\n";
        work_prompt += "a short clarification helps humans; prefer no comment if nothing useful.\n";
    } else if disp == "split" {
        work_prompt += "ACTION: disposition is split.\n";
        if dry_run {
            work_prompt += "dry_run=true: write the split plan + residual follow-up titles in summary;\n";
            work_prompt += "do NOT create issues. status=dry_run\n";
        } else {
            work_prompt += "1) Post a parent-issue comment with a numbered breakdown of PR-sized slices.\n";
            work_prompt += "2) Create child issues with gh issue create --repo " + repo + " for each slice\n";
            work_prompt += "   (title like issue " + inum.to_string() + " slice N), body linking parent.\n";
            work_prompt += "   Labels: operator:grok, operator:night-issue-prs, model:grok-4.5 (create if missing).\n";
            work_prompt += "   Leave children unassigned. Record URLs in child_issue_urls.\n";
            work_prompt += "3) Do NOT implement code in this step unless the FIRST slice is tiny AND you\n";
            work_prompt += "   can finish gates tonight; default is split-only.\n";
            work_prompt += "4) If you ship a first-slice PR, file residual issues for remaining slices too.\n";
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
            work_prompt += "C) Spotless from repo root: mvnw spotless:apply THEN spotless:check.\n";
            work_prompt += "   Commit only in-scope files; do not dump monorepo reformat.\n";
            work_prompt += "D) For each changed Maven module: standalone mvnw clean install (cd module).\n";
            work_prompt += "E) Commit with a clear message referencing issue " + inum.to_string() + ".\n";
            work_prompt += "F) OPERATOR LABELS (required for daily status — create labels if missing):\n";
            work_prompt += "   Ensure these GitHub labels exist (gh label create NAME --force is OK if missing):\n";
            work_prompt += "     operator:grok\n";
            work_prompt += "     operator:night-issue-prs\n";
            work_prompt += "     model:grok-4.5\n";
            work_prompt += "   (If this agent is actually Kilo, use operator:kilo + model:<exact model id> instead of grok labels.)\n";
            work_prompt += "G) git push -u origin HEAD and gh pr create --base " + base_branch + "\n";
            work_prompt += "   with --label operator:grok --label operator:night-issue-prs --label model:grok-4.5\n";
            work_prompt += "   body: Summary, Test plan, gates evidence, Fixes or Partial for issue.\n";
            work_prompt += "   Also put Operator: Grok: night-issue-prs (model grok-4.5) in the PR body.\n";
            work_prompt += "H) Do not merge.\n";
            work_prompt += "I) If Partial: create residual follow-up issues with the SAME operator: + model: labels;\n";
            work_prompt += "   link them from the PR body and parent issue comment. Leave unassigned.\n";
            work_prompt += "J) Do not resolve review threads on OTHER people PRs in this step.\n";
            work_prompt += "status=pr_opened on success, status=failed with summary of blockers on failure.\n";
        }
    }

    work_prompt += "\nIf implementation reveals the issue is too large mid-flight:\n";
    work_prompt += "- Stop coding sprawl. Prefer split_only + residual/child issues over a mega-PR.\n";
    work_prompt += "- Discard incomplete WIP on the branch if not PR-ready (keep tree clean).\n\n";
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
} // end else (!dry_run) Work loop

// ========== PR follow-up (our open PRs only) ==========
let pr_followup_result = ();
if include_pr_followup {
    phase("PR follow-up");
    let b2 = budget();
    if b2.remaining < 2 {
        log("Skipping PR follow-up: agent budget low.");
        pr_followup_result = #{
            summary: "Skipped: insufficient agent budget",
            prs_touched: "",
            conflicts_resolved: 0,
            threads_resolved: 0,
            human_threads_addressed: 0,
            human_threads_still_open: "",
            residual_issue_urls: "",
            blocked: "budget",
        };
    } else {
        let pr_prompt = "";
        pr_prompt += "You are the overnight PR follow-up agent for Percussion CMS.\n";
        pr_prompt += "Repo: " + repo + "\n";
        pr_prompt += "Base branch: " + base_branch + "\n";
        pr_prompt += "dry_run=" + dry_run.to_string() + "\n";
        pr_prompt += "max_prs=" + max_prs.to_string() + "\n\n";

        pr_prompt += "GOAL: Make OUR open pull requests merge-ready by (in order):\n";
        pr_prompt += "  0) **HUMAN review threads** (never starve or fake-resolve these),\n";
        pr_prompt += "  1) resolving merge conflicts (rebase onto " + base_branch + "),\n";
        pr_prompt += "  2) fixing CI failures,\n";
        pr_prompt += "  3) addressing bot/other review comments (reply + resolve threads).\n";
        pr_prompt += "Do NOT open new product PRs here (that is the Work phase).\n";
        pr_prompt += "Do NOT merge any PR.\n\n";

        pr_prompt += "HUMAN REVIEWS ARE SACRED (hard gate — never skip):\n";
        pr_prompt += "A thread is HUMAN if the first comment author is a real person account, NOT:\n";
        pr_prompt += "  kilo-code-bot, github-actions, dependabot, copilot, cursor, or *bot* logins.\n";
        pr_prompt += "Nate may comment as natechadwick or natechadwick-intsof — both are HUMAN when\n";
        pr_prompt += "the comment is a review (not the automated overnight bot posting mitigations).\n";
        pr_prompt += "Treat any non-bot login on an unresolved thread as HUMAN.\n";
        pr_prompt += "Rules:\n";
        pr_prompt += "  H1) ALWAYS inventory ALL owned open PRs for unresolved HUMAN threads BEFORE\n";
        pr_prompt += "      picking conflict/CI work. GraphQL reviewThreads on every candidate PR.\n";
        pr_prompt += "  H2) HUMAN-thread PRs **always** enter the work set (they do not compete with\n";
        pr_prompt += "      max_prs for exclusion). max_prs only caps *additional* conflict/CI/bot work.\n";
        pr_prompt += "  H3) Never resolve a HUMAN thread without either:\n";
        pr_prompt += "      (a) a real code/test fix + inline mitigation reply citing commit SHA, then resolve, OR\n";
        pr_prompt += "      (b) an inline reply explaining deferral + residual GitHub issue link, and LEAVE\n";
        pr_prompt += "          the thread OPEN (isResolved must stay false). Never bare-resolve.\n";
        pr_prompt += "  H4) Never claim 'no unresolved threads' without a fresh GraphQL re-query of\n";
        pr_prompt += "      ALL owned open PRs after work. List every still-open HUMAN thread in\n";
        pr_prompt += "      human_threads_still_open (PR# + one-line summary). Empty only if truly none.\n";
        pr_prompt += "  H5) Prefer HUMAN threads over bot suggestions when time is short. Bot threads\n";
        pr_prompt += "      may wait for a later tick; HUMAN threads must get action or documented deferral\n";
        pr_prompt += "      with residual issue every run they appear.\n\n";

        pr_prompt += "WHY CONFLICTS STILL MATTER:\n";
        pr_prompt += "Overnight Work often opens several PRs in the SAME area (i18n/TMX, gadgets).\n";
        pr_prompt += "They stack CONFLICTING against " + base_branch + ". After HUMAN threads are handled\n";
        pr_prompt += "(or deferred with residual), unstick oldest conflicted PRs next.\n\n";

        pr_prompt += "SCOPE (hard):\n";
        pr_prompt += "1) gh auth status first; use the active account for " + repo + ".\n";
        pr_prompt += "2) List open PRs you may touch:\n";
        pr_prompt += "   - author is the active gh user, OR\n";
        pr_prompt += "   - head branch starts with fix/issue- or feat/issue- or fix/installer- from this agent.\n";
        pr_prompt += "3) SELECTION (after HUMAN inventory):\n";
        pr_prompt += "   a) ALL PRs with unresolved HUMAN review threads (mandatory; no max_prs cap),\n";
        pr_prompt += "   b) then fill remaining slots up to " + max_prs.to_string() + " total non-human work:\n";
        pr_prompt += "      CONFLICTING/DIRTY → failing CI → unresolved bot threads → optional BEHIND.\n";
        pr_prompt += "4) PROCESSING ORDER: (1) HUMAN-thread PRs oldest first, (2) then other selected\n";
        pr_prompt += "   PRs oldest first. Same-area siblings still oldest→newest after humans.\n";
        pr_prompt += "   After each successful push, git fetch origin before the next PR.\n";
        pr_prompt += "5) Skip draft PRs unless CONFLICTING, checks fail, or they have HUMAN threads.\n";
        pr_prompt += "6) NEVER edit or resolve threads on PRs owned by other humans unless you are a\n";
        pr_prompt += "   listed co-author / the PR is clearly from this overnight bot session.\n\n";

        pr_prompt += "PER PR (when dry_run=false) — order is mandatory:\n\n";

        pr_prompt += "A0) HUMAN REVIEW THREADS (before conflicts/CI when this PR has any):\n";
        pr_prompt += "   GraphQL list ALL reviewThreads; filter isResolved=false; classify HUMAN vs bot.\n";
        pr_prompt += "   For each HUMAN thread: implement fix if clear and in-scope, else residual issue +\n";
        pr_prompt += "   inline deferral reply and leave OPEN. Count human_threads_addressed only when\n";
        pr_prompt += "   fixed+resolved. Still-open humans go in human_threads_still_open.\n\n";

        pr_prompt += "A) MERGE CONFLICTS / REBASE (after human threads on this PR, before bot CI polish):\n";
        pr_prompt += "   Query: gh pr view <n> --json mergeable,mergeStateStatus,baseRefName,headRefName,createdAt\n";
        pr_prompt += "   If mergeable is CONFLICTING or mergeStateStatus is DIRTY (or you chose BEHIND):\n";
        pr_prompt += "   1. git fetch origin\n";
        pr_prompt += "   2. Checkout the PR branch cleanly (gh pr checkout <n> or worktree if the main\n";
        pr_prompt += "      workspace is dirty / on another branch). Do not destroy unrelated WIP.\n";
        pr_prompt += "   3. git rebase origin/" + base_branch + "\n";
        pr_prompt += "      (use the PR baseRefName if it is not " + base_branch + ")\n";
        pr_prompt += "   4. On conflict markers:\n";
        pr_prompt += "      - Read each conflicted file in FULL (not only the marker region).\n";
        pr_prompt += "      - During rebase, HEAD (above =======) is the *base* branch; the bottom section\n";
        pr_prompt += "        is this PR's commit being replayed.\n";
        pr_prompt += "      - Non-overlapping adds (different keys/functions/imports): keep BOTH in\n";
        pr_prompt += "        logical order.\n";
        pr_prompt += "      - Same lines: combine intent; prefer keeping this PR's purposeful change\n";
        pr_prompt += "        while preserving newer base structure/API.\n";
        pr_prompt += "      - TMX / locale / message catalogs: union translation units / keys; never drop\n";
        pr_prompt += "        base keys that this PR did not intentionally remove; keep both locales'\n";
        pr_prompt += "        additions when they do not collide.\n";
        pr_prompt += "      - Remove ALL markers (<<<<<<<, =======, >>>>>>>). Re-read for syntax.\n";
        pr_prompt += "   5. git add <files> && git rebase --continue (repeat until done).\n";
        pr_prompt += "   6. If unresolvable without product judgment: git rebase --abort, leave PR alone,\n";
        pr_prompt += "      open a residual GitHub issue linking the PR + conflicted paths; do not force a\n";
        pr_prompt += "      bad merge. Count under blocked.\n";
        pr_prompt += "   7. After a clean rebase: Spotless apply then check for touched files if applicable;\n";
        pr_prompt += "      standalone clean install for changed Maven modules when practical.\n";
        pr_prompt += "   8. Push with **git push --force-with-lease** only (NEVER bare --force).\n";
        pr_prompt += "      Rebase rewrites the PR branch; force-with-lease is required and allowed here.\n";
        pr_prompt += "   9. gh pr comment: brief note that conflicts were resolved / rebased onto base,\n";
        pr_prompt += "      list main conflicted paths. Increment conflicts_resolved.\n";
        pr_prompt += "   10. Co-Authored footer on any new commits as usual.\n\n";

        pr_prompt += "B) CI FAILURES: after conflicts are clear (or none), fix failing checks with real\n";
        pr_prompt += "   code/tests. Push normally (or force-with-lease only if history was rewritten).\n\n";

        pr_prompt += "C) BOT / OTHER REVIEW COMMENTS: after humans + conflicts/CI on this PR, address\n";
        pr_prompt += "   remaining unresolved bot threads with real code/tests when needed.\n";
        pr_prompt += "   Gates: Spotless apply then check; standalone clean install when practical.\n\n";

        pr_prompt += "D) PR REVIEW THREAD RESOLUTION (HARD GATE — root AGENTS.md / REVIEW.md):\n";
        pr_prompt += "   For EVERY thread you **fully fix** (human or bot):\n";
        pr_prompt += "   1. GraphQL list reviewThreads (id, isResolved, first comment databaseId + author).\n";
        pr_prompt += "   2. Inline REPLY with mitigation citing commit SHA + what changed.\n";
        pr_prompt += "      Prefix: **Mitigation (commit `<sha>`):** ...\n";
        pr_prompt += "   3. resolveReviewThread GraphQL mutation using thread id (NOT databaseId).\n";
        pr_prompt += "   4. Re-query: that thread isResolved true.\n";
        pr_prompt += "   Outdated fixed threads still need reply + resolve. Never resolve without inline reply.\n";
        pr_prompt += "   Never top-level-only comment as a substitute for thread reply+resolve.\n";
        pr_prompt += "E) Judgment / multi-day / out-of-scope (esp. HUMAN): leave thread OPEN, inline reply\n";
        pr_prompt += "   with residual issue URL; do NOT resolve. Count under blocked / human_threads_still_open.\n\n";

        pr_prompt += "Between PRs: leave the tree clean; never leave a half-finished rebase. If rebase is\n";
        pr_prompt += "in progress and you must stop, abort it before switching branches.\n\n";

        pr_prompt += "When dry_run=true: inventory ALL owned open PRs with mergeable/mergeStateStatus +\n";
        pr_prompt += "unresolved HUMAN vs bot threads + planned fixes; do not push, rebase, reply, resolve,\n";
        pr_prompt += "or create issues. human_threads_still_open must still list every human thread found.\n\n";

        pr_prompt += "Return summary, prs_touched, conflicts_resolved, threads_resolved,\n";
        pr_prompt += "human_threads_addressed, human_threads_still_open (string list), residual_issue_urls,\n";
        pr_prompt += "and blocked notes. Never omit open human threads from human_threads_still_open.\n";

        let pr_agent = agent(pr_prompt, #{
            label: "pr-followup",
            capability_mode: "all",
            output_schema: pr_followup_schema,
        });

        if pr_agent != () && pr_agent.success && pr_agent.output != () {
            pr_followup_result = pr_agent.output;
            log("PR follow-up: " + pr_followup_result.summary);
        } else {
            pr_followup_result = #{
                summary: "PR follow-up agent failed",
                prs_touched: "",
                conflicts_resolved: 0,
                threads_resolved: 0,
                human_threads_addressed: 0,
                human_threads_still_open: "unknown (agent failed)",
                residual_issue_urls: "",
                blocked: "agent_failed",
            };
            log("PR follow-up agent failed");
        }
    }
} else {
    log("PR follow-up phase disabled (include_pr_followup=false).");
}

// ========== Report ==========
phase("Report");

let report_prompt = "";
report_prompt += "Write a concise overnight report in GitHub-flavored markdown for the human.\n";
report_prompt += "Repo: " + repo + "\n";
report_prompt += "dry_run=" + dry_run.to_string() + "\n\n";
report_prompt += "Triage notes:\n" + json_encode(triage_notes) + "\n\n";
report_prompt += "Issue work results (JSON):\n" + json_encode(results) + "\n\n";
report_prompt += "PR follow-up result (JSON, may be empty):\n" + json_encode(pr_followup_result) + "\n\n";
report_prompt += "Include:\n";
report_prompt += "1) Table: issue | status | PR | child/residual issue links | summary\n";
report_prompt += "2) Table: open PRs followed up | conflicts | threads | **human threads still open** | residual issues\n";
report_prompt += "3) Call out every unresolved HUMAN review thread prominently (PR# + summary)\n";
report_prompt += "4) Morning review order and what was deferred\n";
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
    summary = summary + "; pr_followup: " + pr_followup_result.summary;
}

log(summary);
complete(#{
    summary: summary,
    path: report_path,
    results: results,
    pr_followup: pr_followup_result,
    notes: triage_notes,
    dry_run: dry_run,
});
