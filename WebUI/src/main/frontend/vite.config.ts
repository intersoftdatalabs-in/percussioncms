import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { resolve } from "path";

export default defineConfig({
  plugins: [
    react(),
    {
      name: "node-modules-resolver",
      async resolveId(id, importer, options) {
        if (!id.startsWith(".") && !id.startsWith("/") && !id.startsWith("@/")) {
          const frontendImporter = resolve(__dirname, "dummy.ts");
          const resolved = await this.resolve(id, frontendImporter, {
            skipSelf: true,
            ...options,
          });
          return resolved;
        }
        return null;
      },
    },
  ],
  root: ".",
  base: "/cm/modern/",
  build: {
    outDir: "../../../target/generated-webui/cm/modern",
    emptyOutDir: true,
    sourcemap: true,
    rollupOptions: {
      input: resolve(__dirname, "../ts/index.ts"),
      output: {
        // Stable entry name so thin JSPs can load PercModernUI without a manifest
        entryFileNames: "assets/perc-modern-ui.js",
        chunkFileNames: "assets/[name]-[hash].js",
        assetFileNames: "assets/[name]-[hash][extname]",
      },
    },
  },
  resolve: {
    alias: {
      "@": resolve(__dirname, "../ts"),
    },
  },
  test: {
    globals: true,
    environment: "jsdom",
    include: [
      "src/test/ts/**/*.{test,spec}.{ts,tsx}",
      "src/test/js/**/*.{test,spec}.js",
      // Legacy per-source-file test suites under the module-level
      // src/test/js directory. These exercise the legacy jQuery/Knockout
      // UI sources (e.g. plugins/PercListEditorWidget.js) and were
      // previously not picked up by vitest because its root is the
      // frontend folder. Adding the absolute path here ensures both
      // legacy and modern suites run under `npm test`.
      "../../src/test/js/**/*.{test,spec}.js",
    ],
  },
});
