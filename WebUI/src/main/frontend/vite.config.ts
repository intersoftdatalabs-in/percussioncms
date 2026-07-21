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
    include: [
      "src/test/ts/**/*.{test,spec}.{ts,tsx}",
      "src/test/js/**/*.{test,spec}.js",
      // Module-level modern tests (WebUI/src/test/ts) — Track B home, publishing, etc.
      resolve(__dirname, "../../test/ts/**/*.{test,spec}.{ts,tsx}"),
      // Legacy module-level JS tests
      resolve(__dirname, "../../test/js/**/*.{test,spec}.js"),
    ],
  },
});
