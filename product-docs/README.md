# product-docs

Git-backed **product documentation** for Percussion CMS (Virtual Site source tree).

This directory is intentionally separate from repository `docs/` (engineering / AI / policy notes).

## Layout

```text
product-docs/
  _config.yaml       # site title, versions, nav, theme
  _theme/            # HTML-first layout templates
  assets/            # static CSS/images
  8.2/               # versioned Markdown content
    getting-started/
    admin/
    developer/
    reference/
```

Every page uses YAML frontmatter with a stable `id` (see
`docs/ai-generated/tasks/virtual-sites-git-docs/contracts/frontmatter.md` and the product
page `8.2/reference/frontmatter.md`).

## Build

From the repo root (after `system` is built/installed once so the classpath is available):

```bat
scripts\build-cms-docs.bat
```

```bash
scripts/build-cms-docs.sh
```

Output defaults to `tmp/product-docs-site/` (versioned pages under `8.2/`, e.g. `8.2/index.html`).
The build fails if internal `id:` or relative Markdown links cannot be resolved.

### CI smoke (path-filtered)

GitHub Actions workflow **`product-docs-build`** (`.github/workflows/product-docs-build.yml`)
runs on PRs/pushes that touch:

- `product-docs/**`
- `scripts/build-cms-docs*` / `scripts/ci-smoke-product-docs*`
- `system/**/virtualsite/**` (Virtual Site build sources/tests)
- the workflow file itself

**CI runner (ubuntu):** installs `system` + upstream reactor SNAPSHOTs (`./mvnw -pl system -am -DskipTests install`), then runs:

```bash
scripts/ci-smoke-product-docs.sh
```

That wrapper builds the site and **fails** if the build exits non-zero or no `index.html` is emitted (expects `tmp/product-docs-site/8.2/index.html`). Broken Markdown, frontmatter, or `_config.yaml` that break the Virtual Site build will red the job.

**Local Windows parity** (same assertions; not used by the ubuntu CI job):

```bat
scripts\ci-smoke-product-docs.bat
```

**Local Unix parity:**

```bash
scripts/ci-smoke-product-docs.sh
```

Operators: prefer the smoke wrappers before opening a docs-only PR so CI does not become the first signal of a broken tree.

## Working model (8.2)

Add or update Markdown here when features land so documentation stays in lockstep with the product.
Keep frontmatter `id` values stable when paths rename.
