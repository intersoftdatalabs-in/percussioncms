# Erlang review — #4614 PublishingShell incremental site publish

- Confirm gate before incremental API (cancel does not call REST).
- Job id extracted from SitePublishResponse; jobs region refreshes.
- Portable paths: no OS filesystem joins.
- Companions: Vitest, Playwright helper+spec, product-docs admin publishing.
- Layer split: no REST/sitemanage change (façade already present).
