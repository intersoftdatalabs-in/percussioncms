## Summary

<!-- What changed and why (1–3 short paragraphs). Link issues with Fixes #N or Partial #N. -->

## Test plan

<!-- Numbered steps a reviewer or QA can run. Note env (local install, QA H2 docker, etc.). -->

1.
2.

## Checklist

- [ ] **Product documentation** — updated or created pages under `product-docs/` (usually `product-docs/8.2/`) for any operator-, admin-, integrator-, or end-user-facing change  
  - **or N/A** (not product-facing): <!-- pure refactor / test-only / CI-only / agent-rules-only — state which -->
- [ ] **Unit / module tests** — behavioral tests for new or changed non-trivial logic; changed modules green under standalone `mvnw clean install`  
  - **or N/A** (docs/rules-only)
- [ ] **WebUI + Playwright** — screen/chrome/user-visible WebUI work includes Playwright under `modules/perc-qa-automation/`  
  - **or N/A** (no WebUI product screen change)
- [ ] **Build gates** — each changed Maven module: standalone clean install (no skipTests); reverse-deps when `final`/API shape changes  
  - **or N/A** (docs/rules-only)
- [ ] **Cross-platform** — path/file I/O changes are portable (Windows / Linux / macOS)  
  - **or N/A** (no path/file I/O)

### Product-docs pointers

- Gate: root `AGENTS.md` → **Product documentation (HARD GATE)**
- Tree: `product-docs/README.md` (layout, frontmatter `id` stability, build smoke)
