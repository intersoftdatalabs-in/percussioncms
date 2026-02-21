/**
 * minify.mjs
 *
 * Minifies all TinyMCE custom plugin files (plugin.js -> plugin.min.js) using esbuild.
 * Invoked by the Maven build via frontend-maven-plugin at the prepare-package phase.
 *
 * Source: src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/<name>/plugin.js
 * Output: target/classes/META-INF/resources/sys_resources/tinymce/plugins/<name>/plugin.min.js
 */

import { build } from "esbuild";
import { readdirSync, existsSync } from "node:fs";
import { join } from "node:path";

const srcBase =
  "src/main/resources/META-INF/resources/sys_resources/tinymce/plugins";
const outBase =
  "target/classes/META-INF/resources/sys_resources/tinymce/plugins";

const pluginDirs = readdirSync(srcBase, { withFileTypes: true })
  .filter((d) => d.isDirectory())
  .map((d) => d.name)
  .filter((name) => existsSync(join(srcBase, name, "plugin.js")));

if (pluginDirs.length === 0) {
  console.error("ERROR: No plugin.js files found under " + srcBase);
  process.exit(1);
}

console.log(
  `Minifying ${pluginDirs.length} TinyMCE plugin files with esbuild...`,
);

await Promise.all(
  pluginDirs.map(async (plugin) => {
    const inFile = join(srcBase, plugin, "plugin.js");
    const outFile = join(outBase, plugin, "plugin.min.js");

    await build({
      entryPoints: [inFile],
      outfile: outFile,
      bundle: false, // pure minification only — do not resolve imports
      minify: true,
      target: ["es5"], // ES5 output for maximum browser compatibility
      logLevel: "warning",
    });

    console.log(`  ✓ ${plugin}/plugin.min.js`);
  }),
);

console.log("Done.");
