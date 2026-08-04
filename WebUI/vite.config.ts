import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

/** ESM-safe dirname (Vite native configLoader / package `"type": "module"`). */
const __dirname = dirname(fileURLToPath(import.meta.url));

export default defineConfig({
  plugins: [react()],
  root: ".",
  base: "/cm/modern/",
  build: {
    outDir: "war/modern",
    emptyOutDir: true,
    sourcemap: true,
    rollupOptions: {
      input: resolve(__dirname, "src/main/ts/index.ts"),
      output: {
        entryFileNames: "assets/perc-modern-ui.js",
        chunkFileNames: "assets/[name]-[hash].js",
        assetFileNames: "assets/[name]-[hash][extname]",
      },
    },
  },
  resolve: {
    alias: {
      "@": resolve(__dirname, "src/main/ts"),
      // Vendored package is linked under src/main/frontend/node_modules (Maven
      // frontend workingDirectory). Alias so `npm test` from WebUI root also works.
      "@mkd/language": resolve(__dirname, "vendor/mkd-language"),
    },
  },
  test: {
    globals: true,
    environment: "jsdom",
    // Same setup as src/main/frontend/vite.config.ts so `npm test` from
    // WebUI root also gets the jsdom location navigation mock, canvas
    // stubs, and jest-dom matchers.
    setupFiles: [resolve(__dirname, "src/main/frontend/vitest.setup.ts")],
    include: [
      "src/test/ts/**/*.{test,spec}.{ts,tsx}",
      "src/test/js/**/*.{test,spec}.js",
    ],
  },
});
