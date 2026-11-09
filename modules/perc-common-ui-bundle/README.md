# Percussion CMS Common UI Bundle

`com.percussion.cms:perc-common-ui-bundle` packages the minified
JavaScript bundles that the DTS delivery-tier widgets load as web
resources.

## What the module contains

This module is a packaging artifact only. It contains **no Java sources**;
the `src/main` and `src/test` directories hold JavaScript files that the
`esbuild` build (`build-scripts/bundle.mjs`) bundles and rewrites into
`target/classes/META-INF/resources/`. The Maven `jar` step then ships
those generated resources inside
`perc-common-ui-bundle-8.2.0-SNAPSHOT.jar`.

## CodeMirror bundle (issue #5784)

`build-scripts/bundle-codemirror.mjs` (npm script `bundle:codemirror`,
Maven `bundle-codemirror` execution) bundles the `codemirror` npm package
(pinned to CodeMirror 5 until #5701 lands the CodeMirror 6 replacement)
into `target/classes/META-INF/resources/sys_resources/codemirror/codemirror/`
(`codemirror.bundle.js` + `codemirror.bundle.css`). The classic template
editor (`editTemplate_js.jsp` mirrors) and the `sys_TextArea` code-editor
control (`sys_Templates.xsl`) load this TinyMCE-independent path, so they
survive the `perc-tinymce` packaging removal (#5734). Contract tests:
`src/test/js/codemirror-bundle.test.js` (pinned major line + entry
unwrapping); live coverage:
`modules/perc-qa-automation/frontend/tests/editor-codemirror-bundle.spec.js`.

The POM disables the standard `maven-compiler-plugin` execution and uses
`frontend-maven-plugin` (run via `npm`) plus `maven-jar-plugin` to
package the generated resources into the final JAR. The
`maven-resources-plugin` runs by default in the standard lifecycle but is
not explicitly configured here. See `pom.xml` for the wiring.

## Javadoc status

This module **does not generate Javadoc** because it has no Java sources.
The `maven-javadoc-plugin` reports `No Javadoc in project. Archive not
created.` during the build, which is the expected outcome.

The zero-warning Javadoc baseline for this module is recorded against
issue **#1942** and the broader cleanup sweep tracked by **#1909**.

## Build

Windows:

```bat
cd modules\perc-common-ui-bundle
..\..\mvnw.cmd clean install
```

Linux / macOS:

```bash
cd modules/perc-common-ui-bundle
../../mvnw clean install
```

Expected: `BUILD SUCCESS` with no Javadoc phase output (other than the
"Archive not created" notice) and zero source/plugin warnings.
