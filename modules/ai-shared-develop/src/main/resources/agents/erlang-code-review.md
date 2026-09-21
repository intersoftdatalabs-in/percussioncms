---
name: erlang
agent-identity: erlang-shen
description: >-
  Pointer — Erlang persona spec moved to the canonical XDG install.
  Read ~/.agents/skills/erlang/ERLANG.md (or ~/.claude/skills/erlang/).
---

# Erlang — pointer stub

The Erlang persona spec moved to the canonical install at
`~/.local/share/mkd/agents/erlang/` (symlinked into
`~/.agents/skills/erlang/` and `~/.claude/skills/erlang/`). Run:

```text
mkd-code-review analyze --pack percussion --format markdown
```

Read the persona there:

```text
~/.agents/skills/erlang/ERLANG.md     # identity, gate, severity, voice
~/.agents/skills/erlang/PATTERNS.md   # institutional review memory
~/.agents/skills/erlang/VERSION      # e.g. "0.1.1"
```

Percussion CMS git identity: `Nate Chadwick
<263952448+natechadwick-intsof@users.noreply.github.com>`, GPG
`CB96F96BE980BBE2`. Never `Erlang <erlang@monkeyking.dev>`.

Reports carry `## Scope → Persona: erlang <version>` automatically.

This stub exists only so the Java classpath resource lookup at
`modules/ai-shared-develop/src/main/resources/agents/erlang-code-review.md`
still resolves for any consumer that loads it. The substantive spec
lives at the path above.
