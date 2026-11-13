#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""QA fixture published-site publisher (spec 2402-qa-dts-publish, slice 3).

Slice 1 seeded ``published-site-smoke.spec.js`` with a ``PUBLISHED_SITE_URL``
skip contract; slice 2 brought up the in-cell DTS Tomcat cell
(``qa-up --with-dts``) but left ``PUBLISHED_SITE_URL`` /
``PUBLISHED_CATEGORY_URL`` pointed at the Tomcat origin root placeholder.
This module closes the gap: it stages a fixture published site — the real
publish-time ``web_resources`` tree (FancyTree bytes from #6052, copied
verbatim from the CMS install, the same source the publisher copies from)
plus two generated fixture pages — and merges it into the in-cell DTS
Tomcat ROOT webapp so the static surface specs go green instead of
skipping:

* ``published-site-smoke.spec.js`` (seeded in slice 1)
* ``published-category-tree-fancytree.spec.js`` (#6189 checkbox 1)

Stdlib only. All joins use :class:`pathlib.Path` (spec 994 FR-001
cross-platform rule — no hardcoded ``/`` separators for host/container
filesystem paths). Container-side paths are POSIX by construction
(Linux matrix cells) and are built from the cells' install roots, not
from string concatenation with host separators.

Source of truth for ``web_resources``: ``<cms-install>/web_resources`` —
the assembled publish tree (``installDistributionFiles.xml`` copies the
npm-backed FancyTree bytes there at install time). Copying it verbatim
guarantees byte fidelity with what a real edition publish emits.
"""

from __future__ import annotations

import shutil
from pathlib import Path
from typing import List, Sequence

# Tomcat ROOT webapp name: the fixture merges into the ROOT context so
# the published site IS the cell origin (site-root semantics, exactly what
# a real publish emits: ``web_resources/`` + pages at the docroot). The
# smoke spec asserts assets at ``<origin>/web_resources/...``, so a
# ``/published`` sub-context alone can never go green.
FIXTURE_CONTEXT = "ROOT"

# Fixture pages (site-root-relative).
FIXTURE_HOME_PAGE = "index.html"
FIXTURE_CATEGORY_PAGE = "category.html"

# web_resources assets the Playwright surface specs assert HTTP 200 for
# (smoke: first two + css; category-tree: all three + loading.gif).
# The fixture must ship every script/CSS the fixture pages load plus
# every asset the specs GET directly — otherwise the specs record a
# web_resources failure.
REQUIRED_PUBLISHED_ASSETS: Sequence[str] = (
    "web_resources/cm/jslib/jquery.js",
    "web_resources/cm/jslib/jquery-migrate.js",
    "web_resources/cm/jslib/jquery-ui.js",
    "web_resources/cm/jslib/jquery.fancytree.js",
    "web_resources/cm/css/fancytree/skin/ui.fancytree.css",
    "web_resources/cm/css/fancytree/skin-win8/loading.gif",
)

# Dead DynaTree copies the specs assert HTTP 404 for (#6052 retirement).
# The fixture must NOT ship these — a real publish no longer emits them.
REMOVED_PUBLISHED_ASSETS: Sequence[str] = (
    "web_resources/cm/jslib/jquery.dynatree.js",
    "web_resources/cm/css/dynatree/skin/ui.dynatree.css",
)

# Publish-tree dir inside a CMS install root (source of web_resources).
CMS_WEB_RESOURCES_DIR = "web_resources"

# CATALINA_BASE is <dts-install>/Deployment/Server; webapps holds the
# DTS service wars plus this fixture's ``published`` context.
DTS_TOMCAT_WEBAPPS_REL = "Deployment/Server/webapps"

# RESULT contract tokens (agent-parseable, cf. perc-devctl _run_logged).
STEP_QA_PUBLISH_FIXTURE = "qa-publish-fixture"

# Fixture category entries: a nested pair so the expand spec finds a
# collapsed parent with a working expander (mirrors the delivery-side
# collapsible markup in PercCategoryListView.js).
FIXTURE_CATEGORIES = (
    ("Company News", ("Press Releases", "Events")),
    ("Products", ("Widgets", "Gadgets")),
)


def fixture_context_dir(container_webapps: str) -> str:
    """Container-side deploy dir: the Tomcat ROOT webapp (POSIX cell).

    The deploy only writes fixture-owned names (``index.html``,
    ``category.html``, ``web_resources/``); the merge overwrites ROOT's
    stock ``index.html`` so ``/`` serves the fixture home page. The DTS
    probe (``/`` expecting an HTTP ready code) still passes, and
    ``qa-down`` removes the whole cell, so nothing persists.
    """
    return f"{container_webapps.rstrip('/')}/{FIXTURE_CONTEXT}"


def site_base_for(dts_base: str) -> str:
    """Published fixture origin: the DTS root (site-root semantics)."""
    return dts_base.rstrip("/")


def category_url_for(dts_base: str) -> str:
    """Fixture category page URL (repoints the banner placeholder)."""
    return f"{site_base_for(dts_base)}/{FIXTURE_CATEGORY_PAGE}"


def expected_asset_urls(site_base: str) -> List[str]:
    """Absolute URLs the specs require HTTP 200 for (verification)."""
    base = site_base.rstrip("/")
    return [f"{base}/{asset}" for asset in REQUIRED_PUBLISHED_ASSETS]


def removed_asset_urls(site_base: str) -> List[str]:
    """Absolute URLs the specs require HTTP 404 for (DynaTree gone)."""
    base = site_base.rstrip("/")
    return [f"{base}/{asset}" for asset in REMOVED_PUBLISHED_ASSETS]


def fixture_home_html() -> str:
    """Static fixture home page (relative asset URLs — host-agnostic)."""
    return """<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>QA fixture published home (issue #6237 slice 3)</title>
<link rel="stylesheet" href="web_resources/cm/css/fancytree/skin/ui.fancytree.css">
<script src="web_resources/cm/jslib/jquery.js"></script>
<script src="web_resources/cm/jslib/jquery-migrate.js"></script>
<script src="web_resources/cm/jslib/jquery-ui.js"></script>
<script src="web_resources/cm/jslib/jquery.fancytree.js"></script>
</head>
<body>
<h1>QA fixture published site</h1>
<p>Static home page for the in-cell DTS fixture (spec 2402-qa-dts-publish).</p>
<p><a href="category.html">Category list (collapsible FancyTree)</a></p>
</body>
</html>
"""


def fixture_category_html() -> str:
    """Fixture category page: collapsible list init via ``.fancytree(``.

    Mirrors the delivery-side pattern in ``PercCategoryListView.js``: a
    ``.perc-category-list-collapsible`` div filled with nested ``ul/li``
    markup, then ``.fancytree({minExpandLevel: 2})`` so the top-level
    parents start collapsed and the expand spec can open one.
    """
    items = []
    for parent, children in FIXTURE_CATEGORIES:
        kids = "".join(f"<li>{child}</li>" for child in children)
        items.append(f"<li>{parent}<ul>{kids}</ul></li>")
    tree = "".join(items)
    return f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>QA fixture categories (issue #6237 slice 3)</title>
<link rel="stylesheet" href="web_resources/cm/css/fancytree/skin/ui.fancytree.css">
<script src="web_resources/cm/jslib/jquery.js"></script>
<script src="web_resources/cm/jslib/jquery-migrate.js"></script>
<script src="web_resources/cm/jslib/jquery-ui.js"></script>
<script src="web_resources/cm/jslib/jquery.fancytree.js"></script>
</head>
<body>
<h1>Categories</h1>
<div class="perc-category-list"><div class="perc-category-list-collapsible"><ul>{tree}</ul></div></div>
<script>
window.addEventListener("DOMContentLoaded", function () {{
  window.jQuery(".perc-category-list-collapsible").fancytree({{
    // NOTE: delivery (PercCategoryListView.js) uses minExpandLevel 2, which
    // force-expands level 1 with fixed (non-collapsible) expanders. The
    // fixture uses 1 so the top-level parents start collapsed with working
    // expanders, letting the expand surface spec exercise expand (#6189).
    minExpandLevel: 1
  }});
}});
</script>
</body>
</html>
"""


def build_fixture_tree(staging_dir: Path, cms_install: Path) -> List[str]:
    """Stage the fixture site: copy publish-time web_resources + write pages.

    ``cms_install`` is the CMS install root; the publish tree is read from
    ``<cms-install>/web_resources`` (the same source the publisher copies
    from). Returns the context-relative paths written (pages + required
    assets present after copy), for logging and unit assertions. Raises
    ``FileNotFoundError`` when the CMS publish tree is missing a required
    asset — a loud failure beats a fixture that 404s under Playwright.
    """
    staging = Path(staging_dir)
    staging.mkdir(parents=True, exist_ok=True)
    src = Path(cms_install) / CMS_WEB_RESOURCES_DIR
    if not src.is_dir():
        raise FileNotFoundError(
            f"CMS publish tree missing: {src} "
            "(expected <cms-install>/web_resources)"
        )
    missing = [
        asset
        for asset in REQUIRED_PUBLISHED_ASSETS
        if not (
            src / Path(*asset.split("/")).relative_to(CMS_WEB_RESOURCES_DIR)
        ).is_file()
    ]
    if missing:
        raise FileNotFoundError(
            f"CMS publish tree missing required assets: {missing}"
        )
    dest_root = staging / CMS_WEB_RESOURCES_DIR
    if dest_root.exists():
        shutil.rmtree(dest_root)
    shutil.copytree(src, dest_root)
    written = list(REQUIRED_PUBLISHED_ASSETS)
    for name, html in (
        (FIXTURE_HOME_PAGE, fixture_home_html()),
        (FIXTURE_CATEGORY_PAGE, fixture_category_html()),
    ):
        (staging / name).write_text(html, encoding="utf-8")
        written.append(name)
    # The retirement half: fail the staging step if a dead DynaTree copy
    # leaked back into the publish tree (#6052 must stay gone).
    leaked = [
        asset
        for asset in REMOVED_PUBLISHED_ASSETS
        if (staging / Path(*asset.split("/"))).exists()
    ]
    if leaked:
        raise FileExistsError(
            f"retired DynaTree copies present in publish tree: {leaked}"
        )
    return written


def _category_page_problems(category_file: Path) -> List[str]:
    """Markup-contract checks for the fixture category page (pure)."""
    problems = []
    if not category_file.is_file():
        return [f"missing fixture page: {FIXTURE_CATEGORY_PAGE}"]
    text = category_file.read_text(encoding="utf-8")
    if "perc-category-list-collapsible" not in text:
        problems.append("category page lacks .perc-category-list-collapsible")
    # Widget-matrix marker contract (published-widget-matrix.js
    # percCategoryList entry): the list must sit inside the delivery
    # `.perc-category-list` container (trailing quote excludes the
    # `-collapsible`/`-expanded` variants).
    if 'perc-category-list"' not in text:
        problems.append("category page lacks .perc-category-list")
    if ".fancytree(" not in text:
        problems.append("category page never initializes .fancytree(")
    return problems


def verify_fixture_tree(staging_dir: Path) -> List[str]:
    """Assert a staged tree serves the spec contract (pages + 200s, no 404s).

    Returns problem strings (empty when the tree is publishable). Used by
    ``--dry-run`` and unit tests; the live HTTP verify happens in
    ``perc-devctl qa-publish-fixture`` after ``docker cp``.
    """
    staging = Path(staging_dir)
    problems = []
    for page in (FIXTURE_HOME_PAGE,):
        if not (staging / page).is_file():
            problems.append(f"missing fixture page: {page}")
    for asset in REQUIRED_PUBLISHED_ASSETS:
        if not (staging / Path(*asset.split("/"))).is_file():
            problems.append(f"missing required asset (spec 404): {asset}")
    for asset in REMOVED_PUBLISHED_ASSETS:
        if (staging / Path(*asset.split("/"))).exists():
            problems.append(f"retired asset resurrected: {asset}")
    problems.extend(_category_page_problems(staging / FIXTURE_CATEGORY_PAGE))
    return problems


def docker_cp_into_container_argv(
    host_src: Path, container: str, container_dest: str
) -> List[str]:
    """``docker cp <host-src> <container>:<dest>`` argv (pure)."""
    return ["docker", "cp", str(host_src), f"{container}:{container_dest}"]


def docker_copy_from_container_argv(
    container: str, container_src: str, host_dest: Path
) -> List[str]:
    """``docker cp <container>:<src> <host-dest>`` argv (pure)."""
    return ["docker", "cp", f"{container}:{container_src}", str(host_dest)]


def print_published_contract(site_url: str, category_url: str) -> None:
    """Emit the spec env-table contract after a successful publish."""
    print(f"PUBLISHED_SITE_URL={site_url}")
    print(f"PUBLISHED_CATEGORY_URL={category_url}")
