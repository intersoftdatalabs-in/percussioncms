== GitHub/Git Messages (Commit, PR, Review, Comment, etc)

\* In order to clearly differentiate from direct developer messages and those co-authored by agents or sub-agents, you \*MUST\* use a footer that looks like the following for agent / sub-agent assisted work:

```

> Co-Authored by <coding tool> <coding tool version> using <model name with version> with agent <agent name>.

```

\* In addition, every agent-authored PR (and residual issue) \*MUST\* carry GitHub
  labels for daily status reporting: `operator:kilo` + `model:<session model id>`.
  See `.kilo/rules/operator-pr-labels.md` (same scheme as Grok `night-issue-prs`).

