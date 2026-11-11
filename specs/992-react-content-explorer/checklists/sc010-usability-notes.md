# SC-010 Prefer-CE Usability Study — Facilitator Pack & Score Sheet (T084)

**Feature**: [992 React Content Explorer](../spec.md) · **Criterion**: SC-010 · **Task**: T084
**Status**: ⚠️ STUDY NOT RUN — this file is a facilitator pack and blank score
sheet prepared so the UAT owner can run the study. It contains **no results**.
No rater has been recruited, no session has been held, and no score below may
be treated as recorded feedback. When the study is complete, the UAT owner
replaces this status line with run date, build under test, and rater roster,
fills in the per-rater tables, computes the tally, and checks off T084 in
`../tasks.md`.

**Source of truth for the rubric**:
[spec.md](../spec.md) § SC-010 and [quickstart.md](../quickstart.md) §
"Usability survey (SC-010) — intentionally manual".
If this pack and the spec disagree, the spec wins.

## Pass rule (from SC-010)

- At least **5** internal users familiar with **both** UIs (modern explorer and
  miller-column Finder).
- Each rater scores the **five** tasks below on a 1–5 Likert scale
  (1 = very unclear / hard, 5 = very clear / easy), once for the **modern
  explorer** and once for the **miller-column Finder**.
- **Pass**: a majority of raters (**≥ 3 of 5**) score the modern explorer
  **higher** than Finder on the **sum across the five tasks**.
- Measured at or before Finder primary-nav hard cut; record the build in the
  run header when the study is executed.

## Facilitator script (per rater, ~20 minutes)

1. Confirm the rater has used both the modern explorer and the miller-column
   Finder for daily folder work. If not, do not count them toward the 5.
2. For each of the five tasks, have the rater perform it in **both** UIs
   (alternate starting UI across raters to cancel ordering bias), then record
   one 1–5 clarity score per UI per task.
3. Collect one free-text comment per task (optional but recommended) and one
   overall preference statement.
4. Do not coach, do not explain UI internals, and do not reveal other raters'
   scores during a session.

## The five tasks (from quickstart.md)

| # | Task | What to observe |
|---|------|-----------------|
| 1 | Find a folder by name | Navigation / search clarity |
| 2 | Open a specific item from a known parent | Tree + list orientation |
| 3 | Create a new folder and rename it | Folder ops discoverability |
| 4 | Move an item between folders | Drag / cut-paste clarity |
| 5 | Recover from a permission-denied state | Error clarity and recovery path |

## Score sheets (blank — fill in during the study)

Copy one block per rater. `M` = modern explorer, `F` = miller-column Finder.

### Rater 1 — name: ______________ date: __________ build: ______________

| Task | M (1–5) | F (1–5) | Comment |
|------|---------|---------|---------|
| 1. Find folder by name | | | |
| 2. Open item from known parent | | | |
| 3. Create + rename folder | | | |
| 4. Move item between folders | | | |
| 5. Recover from permission-denied | | | |
| **SUM** | | | |

Overall preference (rater's own words): ______________

### Rater 2 — name: ______________ date: __________ build: ______________

| Task | M (1–5) | F (1–5) | Comment |
|------|---------|---------|---------|
| 1. Find folder by name | | | |
| 2. Open item from known parent | | | |
| 3. Create + rename folder | | | |
| 4. Move item between folders | | | |
| 5. Recover from permission-denied | | | |
| **SUM** | | | |

Overall preference (rater's own words): ______________

### Rater 3 — name: ______________ date: __________ build: ______________

| Task | M (1–5) | F (1–5) | Comment |
|------|---------|---------|---------|
| 1. Find folder by name | | | |
| 2. Open item from known parent | | | |
| 3. Create + rename folder | | | |
| 4. Move item between folders | | | |
| 5. Recover from permission-denied | | | |
| **SUM** | | | |

Overall preference (rater's own words): ______________

### Rater 4 — name: ______________ date: __________ build: ______________

| Task | M (1–5) | F (1–5) | Comment |
|------|---------|---------|---------|
| 1. Find folder by name | | | |
| 2. Open item from known parent | | | |
| 3. Create + rename folder | | | |
| 4. Move item between folders | | | |
| 5. Recover from permission-denied | | | |
| **SUM** | | | |

Overall preference (rater's own words): ______________

### Rater 5 — name: ______________ date: __________ build: ______________

| Task | M (1–5) | F (1–5) | Comment |
|------|---------|---------|---------|
| 1. Find folder by name | | | |
| 2. Open item from known parent | | | |
| 3. Create + rename folder | | | |
| 4. Move item between folders | | | |
| 5. Recover from permission-denied | | | |
| **SUM** | | | |

Overall preference (rater's own words): ______________

(Add further rater blocks if more than 5 participate; the pass rule needs a
majority of all raters, minimum 5 raters total.)

## Tally (fill in after all sessions)

| Rater | Sum M | Sum F | M > F? |
|-------|-------|-------|--------|
| 1 | | | |
| 2 | | | |
| 3 | | | |
| 4 | | | |
| 5 | | | |

- Raters with M > F: ____ / ____
- **Verdict**: [ ] PASS (majority M > F) · [ ] FAIL · date: __________
- UAT owner sign-off: ______________

## Close-out

- On PASS/FAIL recorded above: check off T084 in `../tasks.md` and update
  issue [#6093](https://github.com/intersoftdatalabs-in/percussioncms/issues/6093)
  (record waiver with rationale instead if the study is scoped down).
- A FAIL does not pend code changes by itself — file follow-up UX issues for
  the lowest-scoring tasks and link them from this file.
