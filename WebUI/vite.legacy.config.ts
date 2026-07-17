import { defineConfig } from "vite";

/**
 * Legacy bundle configuration for pre-existing jQuery/global-scope code.
 * This builds the 8 page bundles referenced by JSP files in production mode.
 *
 * Each bundle entry file uses side-effect imports (not modules) to concatenate
 * all dependencies in the required global scope, similar to the old minify-maven-plugin.
 */
export default defineConfig({
  root: ".",
  build: {
    outDir: "war",
    emptyOutDir: false, // Don't wipe the war directory (modern build already populated it)
    sourcemap: false,
    lib: {
      entry: {
        "jslibMin/perc_dashboard.packed.min": "src/main/bundles/perc_dashboard.bundle.js",
        "jslibMin/perc_architecture.packed.min":
          "src/main/bundles/perc_architecture.bundle.js",
        "jslibMin/perc_webmgt.packed.min": "src/main/bundles/perc_webmgt.bundle.js",
        "jslibMin/perc_publish.packed.min": "src/main/bundles/perc_publish.bundle.js",
        "jslibMin/perc_users.packed.min": "src/main/bundles/perc_users.bundle.js",
        "jslibMin/perc_editTemplate.packed.min":
          "src/main/bundles/perc_editTemplate.bundle.js",
        "jslibMin/perc_admin.packed.min": "src/main/bundles/perc_admin.bundle.js",
        "jslibMin/perc_common_ui": "src/main/bundles/perc_common_ui.bundle.js",
      },
      formats: ["umd"],
      name: "PercBundle",
    },
    rollupOptions: {
      output: {
        entryFileNames: "[name].js",
        chunkFileNames: "[name]-[hash].js",
        assetFileNames: "[name][extname]",
      },
      // Legacy code is NOT modular; disable tree-shaking to preserve all imports
      treeshake: false,
    },
    minify: false, // Don't minify — we're just concatenating
  },
});
