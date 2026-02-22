# Javadoc Comment Spec Summary (JDK 8, 21, 25)

These notes summarize the standard doclet syntax rules. Always match the JDK
version used by the project.

Sources:
- JDK 8 Javadoc tool: https://docs.oracle.com/javase/8/docs/technotes/tools/windows/javadoc.html
- JDK 21 Doc Comment Spec: https://docs.oracle.com/en/java/javase/21/docs/specs/javadoc/doc-comment-spec.html
- JDK 25 Doc Comment Spec: https://docs.oracle.com/en/java/javase/25/docs/specs/javadoc/doc-comment-spec.html

## JDK 8
- Traditional doc comments only: `/** ... */` immediately before declarations.
- Structure: summary sentence, main description, then block tags.
- Block tags start at line beginning (`@param`, `@return`, `@throws`, etc.).
- Inline tags use `{@tag ...}`; HTML is allowed in descriptions.
- Package docs: `package-info.java` (preferred) or legacy `package.html`.
- Extra package docs live in `doc-files` (HTML only).

## JDK 21
- Same core structure as JDK 8: summary + main description + block tags.
- Recognized on module, package, class/interface, constructor, method, enum member,
  and field declarations.
- Inline tags (`{@link ...}`, `{@code ...}`) and HTML are allowed in descriptions.
- Legacy `package.html` and `overview.html` are still accepted.
- No Markdown doc comments.

## JDK 25
- Two comment kinds: traditional `/** ... */` and Markdown `///` comments.
- Markdown comments can include Markdown plus inline and block tags.
- `doc-files/*.md` is supported (treated as Markdown comments); `.html` still works.
- Comment must appear before any annotations/modifiers; only one comment per
  declaration is used.

## Version Guardrails
- If the project JDK is less than 25, do NOT use Markdown `///` comments or
  `doc-files/*.md`.
- If the project JDK is 8, avoid module documentation and newer syntax.
