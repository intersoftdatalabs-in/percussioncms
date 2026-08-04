/**
 * ESLint flat config (ESLint 10+) for the Maven frontend workingDirectory.
 *
 * Product TS sources live at ../ts (WebUI/src/main/ts). This package is the
 * canonical npm root used by frontend-maven-plugin (see WebUI/pom.xml).
 *
 * Parser / baseline: same as WebUI/eslint.config.mjs (see that file).
 *
 * Run: npm run lint  (from WebUI/src/main/frontend/)
 */
import babelParser from "@babel/eslint-parser";
import js from "@eslint/js";
import reactHooks from "eslint-plugin-react-hooks";

/** Shared language options for TS/TSX product sources. */
const tsLanguageOptions = {
  ecmaVersion: 2022,
  sourceType: "module",
  parser: babelParser,
  parserOptions: {
    requireConfigFile: false,
    babelOptions: {
      babelrc: false,
      configFile: false,
      // @babel/eslint-parser v8 reads parser plugins from parserOpts, not presets
      parserOpts: {
        plugins: [["typescript", { isTSX: true, allExtensions: true }], "jsx"],
      },
    },
  },
};

export default [
  {
    ignores: [
      "**/node_modules/**",
      "**/target/**",
      "**/war/**",
      "**/vendor/**",
      "**/dist/**",
      // Local test/helpers under frontend only (not product TS)
      "src/**",
    ],
  },
  js.configs.recommended,
  {
    // Paths relative to this config file's package (frontend/)
    files: ["../ts/**/*.{ts,tsx}"],
    languageOptions: tsLanguageOptions,
    plugins: {
      "react-hooks": reactHooks,
    },
    rules: {
      "react-hooks/rules-of-hooks": "error",
      "react-hooks/exhaustive-deps": "warn",
      // Babel cannot distinguish type-only imports / React 17+ automatic JSX runtime
      "no-unused-vars": "off",
      "react-hooks/set-state-in-effect": "off",
      "prefer-const": "warn",
      "no-empty": ["error", { allowEmptyCatch: true }],
      "no-redeclare": "off",
      "no-undef": "off",
      "no-control-regex": "warn",
      "no-useless-escape": "warn",
      "no-useless-assignment": "warn",
    },
  },
];
