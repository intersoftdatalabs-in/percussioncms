---
name: erlang-review
description: >-
  Pointer — Erlang skill moved to the canonical XDG install.
  Read ~/.agents/skills/erlang/ERLANG.md, then run mkd-code-review.
---

# Erlang Review — pointer stub

The Erlang review skill moved to the canonical install at
`~/.local/share/mkd/agents/erlang/` (symlinked into
`~/.agents/skills/erlang/` and `~/.claude/skills/erlang/`). Run:

```text
mkd-code-review analyze --pack percussion --format markdown --gate advisory \
    --git-base origin/main \
    --models /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml

Paste the full markdown report into the PR body under **Pre-push local code review**
and write `docs/ai-generated/code-reviews/pr-<N>-erlang.md`.
```

Read the persona there:

```text
~/.agents/skills/erlang/ERLANG.md     # identity, gate, severity, voice
~/.agents/skills/erlang/PATTERNS.md   # institutional review memory
~/.agents/skills/erlang/VERSION      # e.g. "0.1.1"
```

Percussion CMS commits from Erlang / erlang-fix use
`Nate Chadwick <263952448+natechadwick-intsof@users.noreply.github.com>`
and GPG `CB96F96BE980BBE2` (`AGENTS.local.md`). Never
`Erlang <erlang@monkeyking.dev>`.

Kilo `/erlang-review` (or `.kilocode/workflows/erlang-review.md`) is the
preferred invocation in Kilo Code. The OpenCode `.opencode/agent/erlang-review.md`
and the Grok `.grok/workflows/night-issue-prs.rhai::erlang_gate_text()` both
collapse to the same one-paragraph pointer.

This stub exists only so the Java classpath resource lookup at
`modules/ai-shared-develop/src/main/resources/skills/erlang-review/SKILL.md`
still resolves for any consumer that loads it. The substantive skill
lives at the path above.
