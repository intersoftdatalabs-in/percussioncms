# admin audit

Area: `WebUI/src/main/ts/admin/**` (shell + tools, 7 `.tsx`/`.ts`).
Cross-references the screen inventory in
`docs/ai-generated/tasks/webui-i18n-string-extraction/plan.md`
and the area row at plan.md:110 / plan.md:435 (PR-F).

## Scope

Per-file hit counts taken from
`tmp/webui-i18n-by-area/candidates-admin.tsv` (15 rows total).

|                          File                          |   Hits |                   Kinds                    |
|--------------------------------------------------------|-------:|--------------------------------------------|
| `WebUI/src/main/ts/admin/TaskNotifications.tsx`        |      1 | 1 text (JSX)                               |
| `WebUI/src/main/ts/admin/TasksSection.tsx`             |     10 | 9 text (JSX) + 1 attribute (`placeholder`) |
| `WebUI/src/main/ts/admin/tools/ConsistencyChecker.tsx` |      4 | 4 text (JSX)                               |
| **Total**                                              | **15** | 14 text + 1 attribute                      |

> **Out of scope for this audit doc (but worth flagging):**
> `WebUI/src/main/ts/admin/messages.ts` already exports a local
> `ADMIN_MSG` catalog (35 keys) that wires most of the
> already-localized chrome in `TasksSection.tsx` and `TaskNotifications.tsx`
> (e.g. `TAB_NOTIFICATIONS`, `CREATE_TASK`, `EDIT_TASK`, `TASK_NAME`,
> `CRON_EXPRESSION`, `TASK_TYPE`, `RUN_NOW`, `LOADING`,
> `CONFIRM_DELETE_TASK`, `NAME_REQUIRED`, `CRON_REQUIRED`, `TYPE_REQUIRED`,
> `TEMPLATE_NAME`, `SUBJECT`, `BODY`, `SAVE`, `CANCEL`, `STATUS`,
> `NOTIFY_WHEN`, `EMAIL_ADDRESSES`, `ENABLED`, `SERVER`, `LOG_TIME`,
> `MESSAGE`, `SERVER_NAME`, `PURGE_LOGS`, `CONFIRM_PURGE_LOGS`,
> `NOTIFICATION_TEMPLATE`, `TASK_TYPE`-related, `ERROR_GENERIC`, …).
> Every one of those keys points to a **bare** `perc.ui.admin@…` tuid
> that **does not exist** in `CmsUi.tmx` (verified — 0 matches, see
> Reusable keys (TMX) below). Phase 2 must seed these under the new
> `perc.ui.admin.shell.task@…` sub-prefix proposed in the plan (and
> Phase 1 should rewrite `admin/messages.ts` to use the sub-prefix
> instead of the bare `perc.ui.admin@…` form). This audit doc only
> enumerates the **hardcoded** strings not yet wrapped; the local
> `ADMIN_MSG` re-keying is a Phase 1 follow-up.

## Reusable keys (MSG)

`WebUI/src/main/ts/i18n/message.ts` (the global `MSG` catalog) —
**verified: no `ADMIN_*` constants exist.** The global catalog has
nested groups for `HOME`, `PUBLISH`, `NAV`, `DASHBOARD`, `WIDGET_CONFIG`,
`WELCOME`, `ACTIVITY`, `GADGET`, and `GADGET_DESC`, but **no
`MSG.ADMIN` or `MSG.ADMIN_SHELL` block.** Admin currently lives only
in the local `admin/messages.ts` module (see Scope above), which
exported strings are not exposed via the global `MSG` re-export.

Therefore **no global `MSG` constant can be reused** for the 15
candidates. All 15 are net-new and require both a new `MSG.ADMIN.*`
group in `message.ts` and matching `<tu>` entries in `CmsUi.tmx`.

## Reusable keys (TMX)

Pre-flight grep against
`modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx` with
`Select-String -Pattern 'tuid="perc\.ui\.admin\.'` returned **5 matches**:

|                    tuid                    | English (en-us `<seg>`) |
|--------------------------------------------|-------------------------|
| `perc.ui.admin.title@Administration`       | Administration          |
| `perc.ui.admin.packed@Delete Template`     | Delete Template         |
| `perc.ui.admin.packed@Template`            | Template                |
| `perc.ui.admin.workflow@Categories Locked` | Categories Locked       |
| `perc.ui.admin.workflow@Categories`        | Categories              |

None of these match the English text of the 15 candidates, so **none
are reusable**. A follow-up `Select-String -Pattern 'tuid="perc\.ui\.admin@'`
(bare prefix, the form used by the local `ADMIN_MSG`) returned
**0 matches** — confirming the local catalog's tuids are placeholders
that need Phase 2 seeding under the new sub-prefixes.

## New keys

Per plan §"Phase 1" prefix proposal:
- `perc.ui.admin.shell.task.<field>@…` for `TasksSection.tsx` and
`TaskNotifications.tsx`.
- `perc.ui.admin.tools.consistencychecker.<field>@…` for
`ConsistencyChecker.tsx`.

The `<option value="…">…</option>` text in `TasksSection.tsx:300-306`
is the **human label** of a Java class FQCN; the value attribute is
machine-readable and **must NOT be localized** (hard rule). Only the
`<option>` text content (e.g. `Purge Scheduled Task Log`) is the
localizable label, and it is the *English* that becomes the `<seg>`
in TMX. The trailing colon is preserved where the source emits one
(`Subject:`, `Cron:`, `Class:`, `Status:`).

|                file:line                 |                         english                         |                                         proposed tuid                                          |       proposed MSG constant (or `inline message(...)`)        |                                                                                                                             notes                                                                                                                             |
|------------------------------------------|---------------------------------------------------------|------------------------------------------------------------------------------------------------|---------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `admin/TasksSection.tsx:195`             | `Cron:`                                                 | `perc.ui.admin.shell.task@Cron:`                                                               | `MSG.ADMIN.TASKS.LABEL_CRON`                                  | Reuse same key for any future Tasks card "Cron" label.                                                                                                                                                                                                        |
| `admin/TasksSection.tsx:198`             | `Class:`                                                | `perc.ui.admin.shell.task@Class:`                                                              | `MSG.ADMIN.TASKS.LABEL_CLASS`                                 | Pair label. Truncated FQCN renders next to it.                                                                                                                                                                                                                |
| `admin/TasksSection.tsx:300`             | `Purge Scheduled Task Log`                              | `perc.ui.admin.shell.task.option@Purge Scheduled Task Log`                                     | `MSG.ADMIN.TASKS.OPTION_PURGE_TASK_LOG`                       | `<option value="com.percussion.services.schedule.impl.PSPurgeScheduledTaskLog">`. **Do NOT localize value** (Java FQCN).                                                                                                                                      |
| `admin/TasksSection.tsx:301`             | `Run Edition`                                           | `perc.ui.admin.shell.task.option@Run Edition`                                                  | `MSG.ADMIN.TASKS.OPTION_RUN_EDITION`                          | `<option value="com.percussion.services.schedule.impl.PSRunEdition">`. Value unchanged.                                                                                                                                                                       |
| `admin/TasksSection.tsx:302`             | `Purge Revisions`                                       | `perc.ui.admin.shell.task.option@Purge Revisions`                                              | `MSG.ADMIN.TASKS.OPTION_PURGE_REVISIONS`                      | `<option value="com.percussion.services.schedule.impl.PSPurgeRevisions">`. Value unchanged.                                                                                                                                                                   |
| `admin/TasksSection.tsx:303`             | `Run Command`                                           | `perc.ui.admin.shell.task.option@Run Command`                                                  | `MSG.ADMIN.TASKS.OPTION_RUN_COMMAND`                          | `<option value="com.percussion.services.schedule.impl.PSRunCommand">`. Value unchanged.                                                                                                                                                                       |
| `admin/TasksSection.tsx:304`             | `Purge Publishing Log`                                  | `perc.ui.admin.shell.task.option@Purge Publishing Log`                                         | `MSG.ADMIN.TASKS.OPTION_PURGE_PUBLISHING_LOG`                 | `<option value="com.percussion.services.schedule.impl.PSPurgePublishingLog">`. Value unchanged.                                                                                                                                                               |
| `admin/TasksSection.tsx:305`             | `Purge Expired Log`                                     | `perc.ui.admin.shell.task.option@Purge Expired Log`                                            | `MSG.ADMIN.TASKS.OPTION_PURGE_EXPIRED_LOG`                    | `<option value="com.percussion.services.schedule.impl.PSPurgeExpiredLog">`. Value unchanged.                                                                                                                                                                  |
| `admin/TasksSection.tsx:306`             | `Custom...`                                             | `perc.ui.admin.shell.task.option@Custom...`                                                    | `MSG.ADMIN.TASKS.OPTION_CUSTOM`                               | `<option value="custom">`. Value is the literal sentinel string `"custom"`, **not** localized.                                                                                                                                                                |
| `admin/TasksSection.tsx:311`             | `Enter fully-qualified class name`                      | `perc.ui.admin.shell.task@Enter fully-qualified class name`                                    | `MSG.ADMIN.TASKS.PLACEHOLDER_CLASS_NAME`                      | `placeholder` attribute on the "Custom" extension-name input.                                                                                                                                                                                                 |
| `admin/TasksSection.tsx:219`             | `Edit`                                                  | `perc.ui.admin.shell.task@Edit`                                                                | `MSG.ADMIN.TASKS.BUTTON_EDIT`                                 | Action button on the task card. (Not in TSV but already uses raw literal — adjacent to the localized `RUN_NOW` button on line 210; wire for consistency. Listed here so Phase 3 catches it.)                                                                  |
| `admin/TasksSection.tsx:228`             | `Delete`                                                | `perc.ui.admin.shell.task@Delete`                                                              | `MSG.ADMIN.TASKS.BUTTON_DELETE`                               | Action button on the task card. (Not in TSV; same rationale as Edit.)                                                                                                                                                                                         |
| `admin/TaskNotifications.tsx:130`        | `Subject:`                                              | `perc.ui.admin.shell.task@Subject:`                                                            | `MSG.ADMIN.TASKS.LABEL_SUBJECT`                               | **Reuse the same tuid** if a future TasksSection notification preview also shows `Subject:`. Local `ADMIN_MSG.SUBJECT` already has the bare form `perc.ui.admin@Subject` (no colon) — prefer the colon form here because that is what the rendered DOM emits. |
| `admin/tools/ConsistencyChecker.tsx:114` | `System Consistency Checker`                            | `perc.ui.admin.tools.consistencychecker@System Consistency Checker`                            | `MSG.ADMIN.TOOLS.CONSISTENCY_CHECKER.TITLE`                   | Tool `<h2>`.                                                                                                                                                                                                                                                  |
| `admin/tools/ConsistencyChecker.tsx:127` | `Run Consistency Check`                                 | `perc.ui.admin.tools.consistencychecker@Run Consistency Check`                                 | `MSG.ADMIN.TOOLS.CONSISTENCY_CHECKER.BUTTON_RUN`              | Primary action button label when not loading. (Not in TSV — adjacent to the localized `LOADING` branch; wire for consistency.)                                                                                                                                |
| `admin/tools/ConsistencyChecker.tsx:140` | `Status:`                                               | `perc.ui.admin.tools.consistencychecker@Status:`                                               | `MSG.ADMIN.TOOLS.CONSISTENCY_CHECKER.LABEL_STATUS`            | Pair label. Note local `ADMIN_MSG.STATUS` exists with bare `perc.ui.admin@Status` (no colon) — the rendered DOM has the colon, so the canonical form is the colon-bearing one.                                                                                |
| `admin/tools/ConsistencyChecker.tsx:156` | `Reported Issues`                                       | `perc.ui.admin.tools.consistencychecker@Reported Issues`                                       | `MSG.ADMIN.TOOLS.CONSISTENCY_CHECKER.SECTION_REPORTED_ISSUES` | Section heading.                                                                                                                                                                                                                                              |
| `admin/tools/ConsistencyChecker.tsx:159` | `No consistency issues found. System is fully aligned.` | `perc.ui.admin.tools.consistencychecker@No consistency issues found. System is fully aligned.` | `MSG.ADMIN.TOOLS.CONSISTENCY_CHECKER.EMPTY_ISSUES`            | Empty-state message. Note sentence ends in a period.                                                                                                                                                                                                          |

### Constants block (proposed Phase 1 shape)

```ts
// In WebUI/src/main/ts/i18n/message.ts, audited in audit-admin.md
ADMIN: {
  TASKS: {
    LABEL_CRON: "perc.ui.admin.shell.task@Cron:",
    LABEL_CLASS: "perc.ui.admin.shell.task@Class:",
    LABEL_SUBJECT: "perc.ui.admin.shell.task@Subject:",
    PLACEHOLDER_CLASS_NAME:
      "perc.ui.admin.shell.task@Enter fully-qualified class name",
    BUTTON_EDIT: "perc.ui.admin.shell.task@Edit",
    BUTTON_DELETE: "perc.ui.admin.shell.task@Delete",
    OPTION_PURGE_TASK_LOG:
      "perc.ui.admin.shell.task.option@Purge Scheduled Task Log",
    OPTION_RUN_EDITION: "perc.ui.admin.shell.task.option@Run Edition",
    OPTION_PURGE_REVISIONS:
      "perc.ui.admin.shell.task.option@Purge Revisions",
    OPTION_RUN_COMMAND: "perc.ui.admin.shell.task.option@Run Command",
    OPTION_PURGE_PUBLISHING_LOG:
      "perc.ui.admin.shell.task.option@Purge Publishing Log",
    OPTION_PURGE_EXPIRED_LOG:
      "perc.ui.admin.shell.task.option@Purge Expired Log",
    OPTION_CUSTOM: "perc.ui.admin.shell.task.option@Custom...",
  },
  TOOLS: {
    CONSISTENCY_CHECKER: {
      TITLE:
        "perc.ui.admin.tools.consistencychecker@System Consistency Checker",
      BUTTON_RUN:
        "perc.ui.admin.tools.consistencychecker@Run Consistency Check",
      LABEL_STATUS: "perc.ui.admin.tools.consistencychecker@Status:",
      SECTION_REPORTED_ISSUES:
        "perc.ui.admin.tools.consistencychecker@Reported Issues",
      EMPTY_ISSUES:
        "perc.ui.admin.tools.consistencychecker@No consistency issues found. System is fully aligned.",
    },
  },
} as const,
```

## False positives

- `admin/messages.ts:17` — the `export const ADMIN_MSG` declaration and
  its object body (all 35 keys) are TS literals, not user-visible
  English. They are listed here only as the *reuse target* for
  Phase 1: the local catalog's bare `perc.ui.admin@…` tuids need to
  be rewritten to the new sub-prefixes when `MSG.ADMIN.*` lands.
- `admin/TasksSection.tsx:289-291` — the JS array of FQCN strings
  (`"com.percussion.services.schedule.impl.PSPurgeScheduledTaskLog"`,
  …) and the `"custom"` sentinel are **machine identifiers** and
  must remain literal. False positive.
- `admin/TasksSection.tsx:308` — same FQCN list used in the conditional
  `.includes(...)` check. False positive.
- `admin/TaskNotifications.tsx:152` — `placeholder="…"` (etc.) not in
  the TSV sweep; verify during Phase 3 against the same regex pass if
  the notification form grows.
- `data-testid="task-card-${task.id}"`,
  `data-testid="run-task-${task.id}"`, `data-testid="edit-task-${task.id}"`,
  `data-testid="delete-task-${task.id}"`,
  `data-testid="task-dialog"`, `data-testid="task-name-input"`,
  `data-testid="task-cron-input"`, `data-testid="task-type-select"`,
  `data-testid="perc-consistency-checker"`,
  `data-testid="start-check-btn"`, `data-testid="job-status-badge"`,
  `data-testid="template-card-${template.id}"`,
  `data-testid="edit-template-form"` — selectors for tests, **not**
  localized (per plan §"Phase 0" item 1: "`data-testid` is **not**
  localized"). All false positives.
- `admin/tools/ConsistencyChecker.tsx:152` — `{jobStatus.status}` is a
  server-supplied enum (`"COMPLETE"`, `"PENDING"`, …). Not localizable
  text. False positive.
- All `style={{ … }}` and `className="perc-button-…"` literals — CSS
  / theme plumbing, not user-visible copy. False positive.

## Cross-references

- Plan: `docs/ai-generated/tasks/webui-i18n-string-extraction/plan.md`
  (PR-F slot, area row at plan.md:435; prefix proposal at
  plan.md:151).
- Candidate list: `tmp/webui-i18n-by-area/candidates-admin.tsv` (15
  rows; this audit's "New keys" table preserves all 15 and adds 3
  adjacent literals — `Edit`, `Delete`, `Run Consistency Check` — that
  the regex sweep missed but are obviously hardcoded English next to
  localized siblings).
- Pre-flight grep target file:
  `modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx` (5 existing
  `perc.ui.admin.*` tuids, none reusable; 0 `perc.ui.admin@` bare-form
  matches).
- Playwright (HARD GATE) target for PR-F:
  `modules/perc-qa-automation/frontend/tests/admin.spec.js` (per plan
  §"Phase 3" PR-F).

