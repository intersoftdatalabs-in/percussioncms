# Playwright failure artifacts — night-issue attach conventions

|    Field     |                                                                                                                      Value                                                                                                                       |
|--------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Status**   | Direction of record for overnight / night-issue agents                                                                                                                                                                                           |
| **Audience** | Agents and humans attaching QA-mode Playwright failures to PRs/issues                                                                                                                                                                            |
| **Parent**   | [#1928](https://github.com/intersoftdatalabs-in/percussioncms/issues/1928) slice C ([#2066](https://github.com/intersoftdatalabs-in/percussioncms/issues/2066)); epic [#1827](https://github.com/intersoftdatalabs-in/percussioncms/issues/1827) |
| **Related**  | Slice A env ([#2064](https://github.com/intersoftdatalabs-in/percussioncms/issues/2064)); slice B golden smoke ([#2065](https://github.com/intersoftdatalabs-in/percussioncms/issues/2065)); CI upload pipeline is **#1930** (out of scope here) |
| **Module**   | [`modules/perc-qa-automation`](../../modules/perc-qa-automation/)                                                                                                                                                                                |

This runbook answers three questions when Playwright fails during night-issue / unattended work:

1. **Where** do screenshots, traces, HTML reports, and error context land?
2. **How** do agents collect those paths on Windows, Linux, and macOS?
3. **How** should failures be attached to a PR or issue comment **without** inventing CI infra?

It does **not** implement golden smoke (#2065), env-only `TEST_CMS_URL` wiring (#2064), or a full CI artifact pipeline (#1930).

---

## 1. Always run Playwright from `frontend/`

Playwright resolves `playwright.config.js` and `testDir: ./tests` relative to the config file. **CWD must be:**

```text
modules/perc-qa-automation/frontend
```

Repo-relative (forward slashes for docs; use OS path APIs in code/scripts):

|             Role             |                             Path under repo root                             |
|------------------------------|------------------------------------------------------------------------------|
| Config                       | `modules/perc-qa-automation/frontend/playwright.config.js`                   |
| Specs                        | `modules/perc-qa-automation/frontend/tests/`                                 |
| Default failure output       | `modules/perc-qa-automation/frontend/test-results/`                          |
| HTML report (when enabled)   | `modules/perc-qa-automation/frontend/playwright-report/`                     |
| Manual screenshot convention | `modules/perc-qa-automation/frontend/tests/screenshots/` (see module AGENTS) |

These output dirs are **gitignored** (see `modules/perc-qa-automation/.gitignore`). **Never commit** `test-results/`, `playwright-report/`, or screenshot dumps.

---

## 2. What to collect on failure

Collect from the **frontend cwd** after a failed `npx playwright test` / `npm test` run.

### 2.1 Primary — `test-results/` (default `outputDir`)

Playwright’s default `outputDir` is `test-results` next to the config (no `outputDir` override in `playwright.config.js` today).

Typical contents per failed test (names vary by project/title hash):

|          Artifact           |                  When present                   |                        Why attach                         |
|-----------------------------|-------------------------------------------------|-----------------------------------------------------------|
| `error-context.md`          | Often on failure                                | Compact, text-friendly; paste into PR/issue comments      |
| `trace.zip`                 | When trace was retained (CLI or config)         | Best offline debug; open with `npx playwright show-trace` |
| `test-failed-*.png` / video | When screenshot/video on failure is enabled     | Visual proof of UI state                                  |
| Per-test subfolders         | Always under `test-results/` for failed retries | Scope the zip to the failing folder(s)                    |

**Minimum attach for night-issue:** the failing test’s `error-context.md` (if present) plus the terminal excerpt (spec path, assertion, last request URL).

### 2.2 Optional — `playwright-report/`

Present only if the HTML reporter ran, for example:

```bash
# From modules/perc-qa-automation/frontend
npx playwright test --reporter=line,html
```

Useful for humans browsing a full run; can be large. Prefer zipping **only** when the run was small (golden smoke) and size limits allow (see §4).

### 2.3 Optional — manual screenshots

Module guidance may write PNGs under `tests/screenshots/` on `testInfo.status === 'failed'`. Include those PNGs when present.

### 2.4 Companion — CMS container / matrix logs (QA mode)

When the run targeted H2 Docker QA (`perc-devctl.py qa-up`), also capture:

|              Source               |                                                       How                                                       |
|-----------------------------------|-----------------------------------------------------------------------------------------------------------------|
| `qa-up` / `qa-health` RESULT line | Copy from agent stdout (`RESULT:OK|FAIL STEP:… LOG:…`)                                                          |
| Container logs                    | `docker logs perc-matrix-cms-h2` (name from [workbench-rest-and-qa-modes.md](./workbench-rest-and-qa-modes.md)) |
| Matrix JSON (if used)             | `docker/logs/matrix-results-*.json` under repo root when using matrix harness                                   |

Do not dump multi-GB docker volumes. Prefer the last ~200–500 log lines plus the RESULT line.

---

## 3. Recommended richer artifacts for unattended smoke

Current `playwright.config.js` does **not** force screenshot/trace on failure. For night-issue golden smoke, prefer **CLI flags** (no config change required) so a failure produces attachable files:

```bash
# Unix / Git Bash — from modules/perc-qa-automation/frontend
npx playwright test tests/login.spec.js \
  --trace=retain-on-failure \
  --screenshot=only-on-failure \
  --reporter=line,html
```

```bat
REM Windows cmd — from modules\perc-qa-automation\frontend
npx playwright test tests\login.spec.js --trace=retain-on-failure --screenshot=only-on-failure --reporter=line,html
```

Then collect `test-results/` (and `playwright-report/` if generated). Env for QA mode remains `TEST_CMS_URL` + admin creds (see #2064 / workbench QA section) — not part of this slice.

---

## 4. Cross-platform path notes

|                   Rule                   |                                                                              Detail                                                                               |
|------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Join paths with NIO / `path` modules** | Java: `Path.of` / `resolve`. Node: `path.join` / `path.resolve`. Python: `pathlib.Path`. Do not build filesystem paths with hardcoded `"/"` or `"\\"` in scripts. |
| **Docs and git paths use `/`**           | Repo-relative documentation paths always use forward slashes.                                                                                                     |
| **CWD is mandatory**                     | Commands below assume you already `cd` into `modules/perc-qa-automation/frontend` (or the Windows equivalent).                                                    |
| **Case sensitivity**                     | Windows/macOS volumes may be case-insensitive; use the canonical names above.                                                                                     |
| **Line endings**                         | When pasting `error-context.md`, normalize or paste as fenced text; do not assert raw `\n` vs `\r\n` in comments.                                                 |

### Size limits (practical attach budget)

|                    Channel                    |                                                Soft limit guidance                                                 |
|-----------------------------------------------|--------------------------------------------------------------------------------------------------------------------|
| PR/issue **comment body**                     | Keep under ~50–100 KB of text; paste summaries + short `error-context.md`, not multi-MB base64                     |
| **Gist** (`gh gist create`)                   | Prefer under ~10 MB total; skip huge HTML reports / full video                                                     |
| **Zip of selected failures**                  | Prefer under ~25 MB; if larger, do **not** force-upload — document host paths and attach only the small text files |
| Full `playwright-report/` or multi-trace zips | Often too large; attach **one** failing `trace.zip` or omit and keep local                                         |

Full CI artifact upload (Actions `upload-artifact`, retention policies) is **#1930** — do not invent that pipeline here.

---

## 5. Attach convention for night-issue PRs / issues

**Goal:** every failed unattended Playwright run leaves a **reproducible trail** on the PR or parent issue without committing binaries to git.

### Prefer (in order)

1. **Inline summary comment** (always) — required even when no files are uploaded.
2. **Paste or attach small text** — `error-context.md`, stack excerpt, RESULT line.
3. **Gist for a small zip or few files** — when screenshots/traces exist and stay under size limits.
4. **Host-path inventory** — when artifacts are too large; state absolute paths on the agent machine so a human can pull them.

Do **not**:

- Commit `test-results/` or `playwright-report/` to the feature branch.
- Open a release or invent Actions workflows for this slice.
- Upload secrets (`.env`, generated passwords, tokens). Redact admin passwords from logs.

### 5.1 Comment template (copy into PR or issue)

Use this shape (fill in placeholders; do not commit secrets):

~~~markdown
## Playwright failure artifacts

| Field | Value |
|-------|-------|
| Mode | QA (H2 Docker) / Dev |
| Spec | `tests/<file>.spec.js` — `<test title>` |
| `TEST_CMS_URL` | `http://127.0.0.1:<port>` (no secrets) |
| CWD | `modules/perc-qa-automation/frontend` |
| Exit | non-zero |

### Summary
<one-paragraph: what failed, assertion or timeout>

### Local paths (portable relative)
- `modules/perc-qa-automation/frontend/test-results/<failing-folder>/`
- `modules/perc-qa-automation/frontend/playwright-report/` (if present)

### Attached
- [ ] Inline `error-context.md` excerpt below
- [ ] Gist: <url> (if created)
- [ ] Too large to upload — paths only

### error-context.md (excerpt)
    <paste first ~80 lines or the assertion block as indented text or a fenced block>

### Container (QA mode only)
    RESULT:…
    docker logs perc-matrix-cms-h2 (last lines):
    …
~~~

### 5.2 `gh` patterns (peer CLI; no new repo script)

Use existing `gh` + shell zip tools. There is **no** dedicated `scripts/` helper for this (none existed as a peer; full CI is #1930).

**Post the summary comment:**

```bash
# PR number from gh pr view / create output
gh pr comment <PR_NUMBER> --body-file path/to/comment.md
# or issue:
gh issue comment <ISSUE_NUMBER> --body-file path/to/comment.md
```

**Zip only the failing test folder** (prefer over whole tree):

```bash
# From modules/perc-qa-automation/frontend — Unix / Git Bash
# Replace FAIL_DIR with the subdirectory under test-results/ for the failure
mkdir -p ../../../tmp
zip -r ../../../tmp/playwright-fail.zip "test-results/${FAIL_DIR}" \
  $(test -f "test-results/${FAIL_DIR}/error-context.md" && echo "test-results/${FAIL_DIR}/error-context.md")
```

```powershell
# From modules\perc-qa-automation\frontend — Windows PowerShell
# Prefer Compress-Archive; paths via Join-Path
$failDir = "test-results\<FAIL_DIR>"
$out = Join-Path (Resolve-Path ..\..\..\tmp) "playwright-fail.zip"
if (-not (Test-Path ..\..\..\tmp)) { New-Item -ItemType Directory -Path ..\..\..\tmp | Out-Null }
Compress-Archive -Path $failDir -DestinationPath $out -Force
```

**Upload small bundle via gist** (public by default unless `--secret`; prefer `--secret` for internal failures that may show host paths):

```bash
# From repo root after creating tmp/playwright-fail.zip under soft size limit
gh gist create --secret tmp/playwright-fail.zip \
  -d "Playwright failure artifacts for PR #<PR> / issue #2066"
# Paste the gist URL into the PR/issue comment template
```

If `gh gist create` fails (auth, size), fall back to the **paths-only** row in the template and keep artifacts on the agent host until a human retrieves them.

**Inspect a trace locally (do not attach unless small):**

```bash
cd modules/perc-qa-automation/frontend
npx playwright show-trace test-results/<failing-folder>/trace.zip
```

### 5.3 Success path

If Playwright is green, no artifact attach is required. Optionally note in the PR body:

```text
Playwright: green (spec …). No failure artifacts.
```

---

## 6. Checklist for night-issue agents

On **Playwright failure** before closing the session:

- [ ] CWD was `modules/perc-qa-automation/frontend`
- [ ] Listed relative paths under `test-results/` (and report/screenshots if any)
- [ ] Posted PR/issue comment using §5.1 template
- [ ] Pasted `error-context.md` or equivalent text excerpt
- [ ] If zip/gist used: size under soft limits; **secret** gist when host paths appear
- [ ] Redacted passwords/tokens from logs
- [ ] Did **not** `git add` ignored output dirs
- [ ] Cross-linked parent tracker (**#1928**) and related slices as needed
- [ ] For QA mode: captured RESULT line + short `docker logs` excerpt

---

## 7. Out of scope / follow-ups

|                  Item                   |                                      Owner                                      |
|-----------------------------------------|---------------------------------------------------------------------------------|
| `TEST_CMS_URL` env without host install | #2064 (slice A)                                                                 |
| Golden unattended smoke green proof     | #2065 (slice B)                                                                 |
| CI Actions artifact upload / retention  | #1930 / #1827 slice 4                                                           |
| Changing Playwright defaults in config  | Optional product PR; not required for this convention                           |
| New heavy `scripts/` automation         | Only if a peer one-liner already exists — none for attach; use `gh` + zip above |

---

## 8. See also

- [workbench-rest-and-qa-modes.md](./workbench-rest-and-qa-modes.md) — dev vs QA modes, `qa-up` / `TEST_CMS_URL`
- [modules/perc-qa-automation/README.md](../../modules/perc-qa-automation/README.md) — how to run Playwright
- [modules/perc-qa-automation/frontend/playwright.config.js](../../modules/perc-qa-automation/frontend/playwright.config.js) — current defaults

