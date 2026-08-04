/**
 * ESLint flat config (ESLint 10+) for the WebUI module package root.
 *
 * Lints product React/TypeScript under src/main/ts. Maven's frontend-maven-plugin
 * uses src/main/frontend as workingDirectory — that tree has a twin config.
 *
 * Parser: @babel/eslint-parser with TypeScript + JSX parser plugins.
 * typescript-eslint still hard-rejects TypeScript 7.x (no public TS 7 compiler
 * API until ~7.1); Babel gives syntactic lint + react-hooks without changing
 * the product `typescript@^7` toolchain. Revisit typescript-eslint when it
 * supports TS ≥7.1 (typescript-eslint#10940).
 *
 * Baseline (issue #1593): rules that fire heavily under Babel-without-types
 * (unused imports, set-state-in-effect) are off/warn so `npm run lint` is green.
 * Tighten once typescript-eslint supports TS 7.
 *
 * Run: npm run lint  (from WebUI/)
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
      // Legacy jQuery / residual JS — not part of the modern TS product surface
      "src/main/webapp/**",
      "src/test/js/**",
    ],
  },
  js.configs.recommended,
  {
    files: ["src/main/ts/**/*.{ts,tsx}"],
    languageOptions: tsLanguageOptions,
    plugins: {
      "react-hooks": reactHooks,
    },
    rules: {
      // Hooks correctness (rules-of-hooks) is always on; newer stylistic
      // react-hooks rules are baseline-off until typescript-eslint + cleanups.
      "react-hooks/rules-of-hooks": "error",
      "react-hooks/exhaustive-deps": "warn",
      // Babel cannot distinguish type-only imports / React 17+ automatic JSX runtime
      // "unused" React imports — defer to typescript-eslint when TS 7 is supported.
      "no-unused-vars": "off",
      // Common data-fetch-on-mount pattern; react-hooks v7 flags as error.
      "react-hooks/set-state-in-effect": "off",
      "prefer-const": "warn",
      "no-empty": ["error", { allowEmptyCatch: true }],
      // TS enums / namespaces can look like redeclares on Babel TS ASTs
      "no-redeclare": "off",
      // TypeScript / bundler resolve modules; no-undef is noisy without type info
      "no-undef": "off",
      // Regex safety rules: keep as warn for intentional control-char sanitizers
      "no-control-regex": "warn",
      "no-useless-escape": "warn",
      "no-useless-assignment": "warn",
    },
  },
];
