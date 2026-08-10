# Operator guide: Virtual Site filesystem/Git source, build, and link report

| Field | Value |
|-------|--------|
| **Audience** | Operators, release engineers, docs authors |
| **Epic** | [#2678](https://github.com/intersoftdatalabs-in/percussioncms/issues/2678) |
| **Slice** | [#2705](https://github.com/intersoftdatalabs-in/percussioncms/issues/2705) (Phase 2 residual 3) |
| **Related contracts** | [site-config.md](./contracts/site-config.md), [site-properties.md](./contracts/site-properties.md), [frontmatter.md](./contracts/frontmatter.md) |

This note is **operator-facing**: how to point a Virtual Site at a Git/filesystem tree, run the offline docs build, and interpret `link-report.txt`. It does not replace product Site Manager UI (out of scope for Phase 1/2 residuals).

---

## 1. Filesystem / Git source tree

Phase 1 Virtual Sites use the **git-filesystem** adapter: content is discovered from a local directory tree (typically a Git checkout). Git remains system of record; the CMS does **not** ingest Markdown into the content repository.

### Default product tree

| Item | Path |
|------|------|
| Site root | `product-docs/` (repo root) |
| Config | `product-docs/_config.yaml` |
| Theme | `product-docs/_theme/` |
| Static assets | `product-docs/assets/` |
| Versioned pages | `product-docs/8.2/**/*.md` |

### Layout rules

1. Site root must contain `_config.yaml` (or the file named by `virtual.configFile`).
2. Each `versions[].path` is a directory under the site root (e.g. `8.2`).
3. Every page is Markdown with YAML frontmatter and a stable `id` (see [frontmatter.md](./contracts/frontmatter.md)).
4. Theme layout HTML lives under `_theme/` (default `page.html`).

### Pointing a CMS Site object at the tree (property contract)

When a Percussion **Site** is configured as virtual (no new `RXSITES` columns in Phase 1), set site properties:

| Property | Required | Example | Meaning |
|----------|----------|---------|---------|
| `virtual.sourceKind` | Yes | `git-filesystem` | Non-blank and not `repository` ⇒ Virtual |
| `virtual.rootPath` | Yes | absolute path to `product-docs` (or install-relative) | Filesystem root |
| `virtual.configFile` | No | `_config.yaml` | Config file name under root |
| `virtual.siteKey` | No | `product-docs` | Participant registry key; default = site name |

Helper class: `com.percussion.services.virtualsite.PSVirtualSiteHelper`.

Empty / missing `virtual.sourceKind` (or value `repository`) means a traditional repository-backed Site.

### Cross-platform paths

- Prefer absolute paths on Windows (`C:\…`) and Unix (`/opt/…`).
- Build and link-check code uses `java.nio.file.Path` for filesystem I/O and forward-slash logical hrefs for published paths.
- Do not hardcode OS separators when scripting; use the repo wrappers below.

---

## 2. Running the offline build

The build does **not** require a running CMS or Spring. It compiles the `system` module classpath and invokes:

`com.percussion.services.virtualsite.PSVirtualSiteBuildMain`

### Scripts (preferred)

From the **repository root**:

**Windows**

```bat
scripts\build-cms-docs.bat
scripts\build-cms-docs.bat C:\path\to\product-docs C:\path\to\out
```

**Linux / macOS**

```bash
scripts/build-cms-docs.sh
scripts/build-cms-docs.sh /path/to/product-docs /path/to/out
```

| Argument | Default |
|----------|---------|
| `siteRoot` | `<repo>/product-docs` |
| `outputRoot` | `<repo>/tmp/product-docs-site` |
| site key (fixed in scripts) | `product-docs` |

### Prerequisites

- **JDK 21** via `JAVA_HOME`
- Repo Maven wrapper (`mvnw` / `mvnw.cmd`)
- First run may download dependencies; subsequent runs are incremental

### CLI equivalent

```text
PSVirtualSiteBuildMain <siteRoot> <outputRoot> [siteKey]
```

Exit codes:

| Code | Meaning |
|------|---------|
| `0` | Build succeeded and **no** link problems |
| `1` | Build wrote HTML but **link problems** were found (stderr lists them) |
| `2` | Usage error (missing args) |

### Build pipeline (what happens)

1. Load `_config.yaml` → `VirtualSiteConfig`
2. Discover `*.md` under each version path (`PSGitFilesystemVirtualSiteSource`)
3. Parse frontmatter; assemble Markdown → HTML
4. Render layout + nav + version switcher
5. Write static HTML under `outputRoot`
6. Copy `assets/` and theme assets
7. Upsert virtual participants (JSONL under `outputRoot/_meta/` when using the CLI)
8. Run link check; write **`link-report.txt`** at `outputRoot/link-report.txt`

---

## 3. Interpreting `link-report.txt`

Always present after a successful page write pass.

### Clean build

```text
OK: no link problems
```

CLI exits `0`.

### Problems found

One problem per line, for example:

```text
8.2/index.md: missing path target 'nope.md' → 8.2/nope.html
8.2/admin/index.md: missing id target 'legacy-page' (version 8.2, site product-docs)
```

| Pattern | Meaning |
|---------|---------|
| `missing path target '…' → …` | Relative Markdown/HTML link does not resolve to a published page in that version |
| `missing id target '…'` | `id:stable-id` link has no matching frontmatter `id` in that version |

### What is checked

- Markdown links `[text](target)`
- Relative `.md` / `.html` paths (resolved against the source page directory; `.md` mapped to `.html`)
- Stable id links `id:some-id`

### What is skipped

- `http://`, `https://`, `mailto:`, protocol-relative `//…`
- Fragment-only anchors (`#section`)

### Operator response

1. Open the source `.md` path from the report line.
2. Fix the href or add the missing page / frontmatter `id`.
3. Rebuild; confirm `link-report.txt` returns to `OK`.
4. Offline review: open HTML under `outputRoot` in a browser (file:// or any static file server). There is **no** production CDN packaging in Phase 1.

---

## 4. Offline HTML artifact (optional)

| Item | Location |
|------|----------|
| Default HTML output | `tmp/product-docs-site/` (gitignored via repo `tmp/`) |
| Link report | `tmp/product-docs-site/link-report.txt` |
| Participant meta (CLI) | `tmp/product-docs-site/_meta/participants-product-docs.jsonl` |

To keep a review snapshot, copy `outputRoot` to a durable location outside `tmp/`. Do not commit generated HTML into `product-docs/` (source is Markdown only).

---

## 5. Code anchors

| Area | Path |
|------|------|
| Package | `system/services/src/com/percussion/services/virtualsite/` |
| CLI | `PSVirtualSiteBuildMain` |
| Build service | `PSVirtualSiteBuildService` |
| Link check | `VirtualLinkChecker` |
| Site properties | `PSVirtualSiteHelper` |
| Scripts | `scripts/build-cms-docs.bat`, `scripts/build-cms-docs.sh` |
| CI smoke (Phase 2 residual 2) | Path-filtered workflow lands via #2704 / PR #2707 (`.github/workflows/product-docs-build.yml` + `scripts/ci-smoke-product-docs.*`); until merge, local build is `scripts/build-cms-docs.bat` / `.sh` |

---

## 6. Unit test coverage (residual)

Public helpers with dedicated residual tests (issue #2705):

- `VirtualLinkChecker` — path/id problems, externals, path normalization
- `VirtualSiteConfigLoader` — load/parse validation
- `PSVirtualSiteHelper` — source kind, config file, site key, root path
- `PSVirtualSiteBuildService` — sample tree build + link report OK/problem files

Run from `system/`:

```bat
..\mvnw.cmd -Dtest=VirtualLinkCheckerTest,VirtualSiteConfigLoaderTest,PSVirtualSiteHelperTest,PSVirtualSiteBuildServiceTest,VirtualFrontmatterParserTest,VirtualMarkdownLinkRewriterTest,VirtualNavBuilderTest test
```
