import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { resolve } from "path";

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
    include: [
      "src/test/ts/**/*.{test,spec}.{ts,tsx}",
      "src/test/js/**/*.{test,spec}.js",
    ],
  },
});
