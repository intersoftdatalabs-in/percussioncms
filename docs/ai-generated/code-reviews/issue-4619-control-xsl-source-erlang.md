# Erlang review — #4619 Developer control XSL source

Independent of implementer. Scope: rest javadoc, ControlAdaptor system ControlMeta attach, UserControlIo, Playwright, product-docs.

## Findings

* No bug: system attach is read-only (`Files.readString` only). User writes still go through `findContainedUserFile` / `writeUserControlFile`.
* Paths: `Path` / `Files.readString`; no OS separators.
* Tests: ControlAdaptorWriteTest extract + system snippet; ControlsResourceTest GET system snippet; Playwright `developer-control-xsl-source.spec.js`.
* Product-docs: `product-docs/8.2/admin/developer-ce-controls.md`.
* C2: new `UserControlIo.findSystemControlFile` implemented by production + test IO only.

Hard gates: no remaining bugs, portable I/O, companions present.
