# Rhino JavaScript extensions (non-assembly) — short note

| Field | Value |
|-------|--------|
| **Status** | Informational — Phase 5 optional note (#2834) |
| **Parent** | [#2632](https://github.com/intersoftdatalabs-in/percussioncms/issues/2632) · epic [#2626](https://github.com/intersoftdatalabs-in/percussioncms/issues/2626) |
| **Audience** | Implementers who see Rhino on the classpath and wonder if templates should use JS |
| **Related** | [ADR-001](./adr/001-jexl-bindings-stay.md), [README.md](./README.md) “Why Rhino is on the classpath”, [xsl-migration-cookbook.md](./xsl-migration-cookbook.md), [plan.md](./plan.md) §2.5 / §3.5 |

## One-sentence rule

**Rhino powers legacy JavaScript *extensions* / UDFs — it is not a template assembler and not a replacement for JEXL bindings or for XSL migration.**

## Surfaces (do not conflate)

| Surface | Mechanism | Assembly role? |
|---------|-----------|----------------|
| Template **bindings** | Commons JEXL 3 (`PSScript`, `PSTemplateBinding`) | **Yes** — assembly variables; **stay JEXL** (ADR-001) |
| Text **assemblers** | Velocity / HTML-first / Markdown / `legacyAssembler` / specialized | **Yes** — render body |
| **JavaScript extensions / UDFs** | Rhino + `PSJavaScriptExtensionHandler` (`handler="JavaScript"`) | **No** — classic extension exits, Workbench “JavaScript” category |
| WebUI / gadgets host | Browser JS | **No** — client only |

Code anchors:

- `system/src/main/java/com/percussion/extension/PSJavaScriptExtensionHandler.java` — handler name `"JavaScript"`
- Root / system POMs: `org.mozilla:rhino` dependency (classpath presence is **not** evidence that template bindings are JS)

## 8.2 posture

| Do | Do not |
|----|--------|
| Leave existing JavaScript extension exits alone if they still work | Use Rhino/JS as a **new assembler** language |
| Prefer **JEXL** + `@IPSJexlMethod` / `$rx.*` tools for new assembly-side logic | Plan XSL → Rhino “because both are old scripting” |
| Treat Rhino extension **health** (engine version, security, rewrites) as a **separate** workstream | Block template/assembler normalization on a Rhino rewrite |

Plan text (unchanged intent): optional later revisit of Rhino JS extension handler health is **independent of assembly**; this track does **not** expand or replace Rhino for bindings.

## When you are migrating off XSL

Use the [XSL migration cookbook](./xsl-migration-cookbook.md):

1. Target **HTML-first**, **Markdown**, or **Velocity** (+ JEXL bindings).
2. Do **not** invent a “JavaScript assembler” on Rhino for product templates.
3. If an old XSL path called a **JavaScript UDF exit**, that exit can remain as an extension while the **template body** moves to a modern assembler — migrate the UDF later only if the extension itself is unhealthy.

## Out of scope for this note

- Removing Rhino from the product classpath
- Porting JavaScript extensions to GraalJS or pure Java
- Changing the Design SPA assembler picker to include JavaScript
- Any runtime code change

## See also

- [xsl-migration-cookbook.md](./xsl-migration-cookbook.md) — XSL / `legacyAssembler` support statement + migration steps
- [implementer-guide.md](./implementer-guide.md) — Phase 5 Assemblers & Templates guide (when present)
- [adr/001-jexl-bindings-stay.md](./adr/001-jexl-bindings-stay.md) — bindings stay JEXL
