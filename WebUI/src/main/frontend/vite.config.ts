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
        entryFileNames: "assets/[name]-[hash].js",
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
    ],
  },
});
