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

Output defaults to `tmp/product-docs-site/`. The build fails if internal `id:` or relative Markdown
links cannot be resolved.

## Working model (8.2)

Add or update Markdown here when features land so documentation stays in lockstep with the product.
Keep frontmatter `id` values stable when paths rename.
