# How to Write Doc Comments for the Javadoc Tool (Summary)

Source: https://www.oracle.com/technical-resources/articles/java/javadoc-tool.html

## Purpose and Audience
- Primary goal is API specification: define contract, boundary conditions, and
  corner cases for implementors and test writers.

## Comment Structure
- First sentence is the summary sentence.
- Main description follows; block tags (`@param`, `@return`, `@throws`, etc.) last.
- Use complete sentences in third person, present tense.

## Tag and Style Conventions
- `@param` for every parameter; describe meaning and constraints.
- `@return` for non-void methods; describe returned value and special cases.
- `@throws` for each exception with clear conditions.
- `@deprecated` with replacement guidance.
- Use `{@link ...}` and `{@code ...}` for references and code.
- Use `<p>` to start new paragraphs; keep HTML minimal.

## Special Cases
- Document default constructors explicitly when needed.
- Package docs: prefer `package-info.java`; legacy `package.html` is supported.
- Images and supplemental docs should go in `doc-files`.
