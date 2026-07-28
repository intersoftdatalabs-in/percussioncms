# Gaps from Home → Design / templates / item types

Living backlog of issues discovered while making Home functional.  
**Owner track:** [design-templates-item-types](./README.md) (not Home PR scope unless a thin handoff).

| ID | Date | Gap | How Home hit it | Suggested Design phase | Status |
|----|------|-----|-----------------|------------------------|--------|
| G-01 | 2026-07-28 | No SPA Design / template editor | TopNav Design is legacy exit; blog templates cannot be built in SPA | D1, D4 | open |
| G-02 | 2026-07-28 | Blog list template must include **Blog List** widget (`percBlogIndexPage`) | Blogs gadget / blog section create needs index template | D2 | partially mitigated in #1577 (filter only) |
| G-03 | 2026-07-28 | Blog post template must include **Blog Post** widget (`percBlogPost`) | Same; post template dropdown | D2 | partially mitigated in #1577 (filter only) |
| G-04 | 2026-07-28 | Demo (and bare sites) ship with page template(s) but **no blog-eligible templates** | Create blog disabled until Design work / seeds | D2 (seed or library) | open |
| G-05 | 2026-07-28 | Server clones templates on blog create (`createBlogTemplate`) but **sources must already be valid** | copyTemplates does not invent Blog List/Post widgets | D2 | documented |
| G-06 | 2026-07-28 | Home Create Blog **Post** requires existing **blog section** | Only classic Blogs gadget / section create produces blogs | Gadgets + D1/D2 | Gadgets create-section in #1577 |
| G-07 | 2026-07-28 | Classic WebUI vocabulary ≈ Pages + Assets + Page templates | SPA must not assume that triad for all create/open flows | D3 | open |
| G-08 | 2026-07-28 | Non-page / Rhythmyx items and non-page templates not modeled in Home Create | Create wizard only Page / Asset / Blog post | D3 | open |
| G-09 | 2026-07-28 | Template load API needed for widget eligibility | Full template GET used to scan `definitionId`s | D0/D1 | SPA helper landed with #1577 |
| G-10 | 2026-07-28 | Full template layout edit = large surface (regions, widgets, themes) | Risk of blocking Home on full Design rewrite | D4 last | open / deferred |
| G-11 | 2026-07-28 | Gadgets host fixed; most gadgets still wrong/missing APIs | Separate Gadgets wave; Design only if gadget needs templates | Gadgets wave | open |
| G-12 | 2026-07-28 | Public `rest` preferred for new APIs; design/template may still be internal pagemanagement | Creatable widgets pattern to copy | D0 | open |

## How to append

When Home (or another surface) hits a template/item-type wall, add a row:

```text
| G-nn | YYYY-MM-DD | short gap | how discovered | D0–D4 | open |
```

Keep descriptions factual; link PR/issue when fixed.

## Non-goals for this file

- FTS HTML body extract / Hibernate connection null ([issue #1561](https://github.com/intersoftdatalabs-in/percussioncms/issues/1561)) — engineering residual, not Design SPA
- Publish editions/contexts — Publish wave
- Full Active Assembly page editor — separate mega-track
