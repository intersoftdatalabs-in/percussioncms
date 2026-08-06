import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

/** ESM-safe dirname (Vite native configLoader / package `"type": "module"`). */
const __dirname = dirname(fileURLToPath(import.meta.url));

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
    // Single CSS file with stable name — host JSPs / ensureModernStyles load it.
    // Without this, entry CSS modules (Login, BrandBar) are hashed and never linked
    // because thin JSPs only load the ES module entry (no Vite HTML transform).
    cssCodeSplit: false,
    rollupOptions: {
      input: resolve(__dirname, "../ts/index.ts"),
      output: {
        // Stable entry name so thin JSPs can load PercModernUI without a manifest
        entryFileNames: "assets/perc-modern-ui.js",
        chunkFileNames: "assets/[name]-[hash].js",
        assetFileNames: (assetInfo) => {
          const names = (assetInfo as { names?: string[] }).names;
          const name = names?.[0] ?? assetInfo.name ?? "";
          if (typeof name === "string" && name.endsWith(".css")) {
            return "assets/perc-modern-ui.css";
          }
          return "assets/[name]-[hash][extname]";
        },
      },
    },
  },
  resolve: {
    alias: {
      "@": resolve(__dirname, "../ts"),
      // Vendored package (file:../../../vendor/mkd-language). Keep in
      // lockstep with WebUI/vite.config.ts so Vitest from this
      // workingDirectory (Maven frontend-maven-plugin) resolves the same way
      // as runs from the WebUI root.
      "@mkd/language": resolve(__dirname, "../../../vendor/mkd-language"),
      // Pin country-flag-icons to the worktree's frontend copy. Recurring
      // stray `node_modules/country-flag-icons` installs outside this
      // workingDirectory (e.g. a leftover at the monorepo root) leak into
      // Vitest's resolver, and the package's `modules/react/3x2/index.js`
      // does `import React from "react"` which then fails because the stray
      // copy has no `react` next to it. Aliasing forces the resolution
      // back to the bundled copy under WebUI/src/main/frontend/node_modules.
      "country-flag-icons": resolve(
        __dirname,
        "node_modules/country-flag-icons",
      ),
    },
  },
  // Allow Vitest to load tests and sources outside frontend/ (WebUI/src/test/ts).
  server: {
    fs: {
      allow: [
        resolve(__dirname, ".."),
        resolve(__dirname, "../.."),
        resolve(__dirname, "../../.."),
      ],
    },
  },
  test: {
    globals: true,
    environment: "jsdom",
    setupFiles: [resolve(__dirname, "vitest.setup.ts")],
    include: [
      "src/test/ts/**/*.{test,spec}.{ts,tsx}",
      "src/test/js/**/*.{test,spec}.js",
      // Module-level modern tests (WebUI/src/test/ts) — Track B home, publishing, etc.
      // Relative paths so vitest's glob parser treats them as globs (not
      // escape sequences) on Windows where resolve(__dirname, ...) returns
      // backslash-separated absolute paths.
      "../../test/ts/**/*.{test,spec}.{ts,tsx}",
      // Legacy module-level JS tests
      "../../test/js/**/*.{test,spec}.js",
    ],
  },
});
