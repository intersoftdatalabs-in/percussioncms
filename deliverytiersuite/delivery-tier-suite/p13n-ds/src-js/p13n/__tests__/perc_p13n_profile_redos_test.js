/*
 * Copyright 1999-2025 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Regression test for js/redos alert #1040 in perc_p13n_profile.js.
 *
 * <p>The pre-fix regex `<script(.|\s)*?\/script>` exhibited exponential
 * backtracking when given input of the form `<script` followed by many
 * whitespace characters without a closing `</script>`. The post-fix
 * sanitization applies two patterns in sequence:
 * <ol>
 *   <li><code>&lt;script(?:(?!&lt;\/script&gt;)[\s\S])*&lt;\/script&gt;</code>
 *       — tempered greedy token that matches a well-formed script block
 *       without exponential backtracking.</li>
 *   <li><code>&lt;script\b[^&gt;]*&gt;</code> (case-insensitive) — strips
 *       any remaining unclosed/malformed <code>&lt;script&gt;</code> tag
 *       (CodeQL js/incomplete-multi-character-sanitization follow-up).
 *       Without this pass, attacker-supplied unclosed <code>&lt;script&gt;</code>
 *       would survive sanitization and execute on DOM insertion.</li>
 * </ol>
 *
 * <p>Run with:
 *   node deliverytiersuite/delivery-tier-suite/p13n-ds/src-js/p13n/__tests__/perc_p13n_profile_redos_test.js
 *
 * <p>This test only exercises the POST-FIX regex (which is what ships
 * in the repo) because the PRE-FIX regex with adversarial input of
 * 50K+ spaces runs in exponential time and would hang the test
 * runner. The PR body documents the empirical timing comparison; the
 * CI verifier is the CodeQL re-scan on the merged commit.
 */

"use strict";

const POST_FIX_PAIR = [
  /<script(?:(?!<\/script>)[\s\S])*<\/script>/gi,
  /<script\b[^>]*>?/gi,
];
const TIME_BUDGET_MS = 1000;

/**
 * Apply the post-fix sanitization (both patterns).
 */
function sanitize(text) {
  let out = text;
  for (const re of POST_FIX_PAIR) {
    out = out.replace(re, "");
  }
  return out;
}

/**
 * Asserts that the post-fix regex completes within a generous time
 * bound on adversarial input that would have caused the pre-fix regex
 * to backtrack exponentially.
 */
function assertFastOnAdversarialInput() {
  // 100000 spaces — the post-fix regex must complete in linear time
  // (single pass, no backtracking) regardless of how many spaces
  // appear between `<script` and `</script>`.
  const adversarial = "<script" + " ".repeat(100000);
  const start = Date.now();
  const result = sanitize(adversarial);
  const elapsed = Date.now() - start;

  console.log(`Post-fix sanitize on 100K-space adversarial input: ${elapsed}ms`);

  if (elapsed > TIME_BUDGET_MS) {
    throw new Error(
      `Post-fix sanitize exceeded ${TIME_BUDGET_MS}ms budget (elapsed=${elapsed}ms) — likely exponential backtracking`
    );
  }
  // Input has no closing </script>; pass 1 leaves it untouched but pass 2
  // (unclosed-tag pattern) should still strip the opening tag.
  if (result.includes("<script")) {
    throw new Error(
      `Post-fix sanitize should strip unclosed <script> tag. Got: '${result.slice(0, 80)}...'`
    );
  }
}

/**
 * Verify the regex correctly strips a normal script tag.
 */
function assertStripsNormalScriptTag() {
  const input = 'hello <script type="text/javascript">alert(1);</script> world';
  const expected = "hello  world";
  const actual = sanitize(input);
  if (actual !== expected) {
    throw new Error(
      `Post-fix sanitize did not strip normal script tag. Expected: '${expected}', Got: '${actual}'`
    );
  }
}

/**
 * Verify the regex correctly handles multi-line script tags (the
 * `[\s\S]` character class is intentional — `.` in JS does not match
 * `\n` unless the DOTALL flag is set).
 */
function assertStripsMultiLineScriptTag() {
  const input = 'before\n<script type="text/javascript">\nalert(1);\n</script>\nafter';
  const actual = sanitize(input);
  if (actual !== "before\n\nafter") {
    throw new Error(
      `Post-fix sanitize did not strip multi-line script tag correctly. Got: '${actual}'`
    );
  }
}

/**
 * Verify the regex strips multiple script tags in one input.
 */
function assertStripsMultipleScriptTags() {
  const input = '<script>a</script> middle <script>b</script> end';
  const actual = sanitize(input);
  if (actual !== " middle  end") {
    throw new Error(
      `Post-fix sanitize did not strip multiple script tags. Got: '${actual}'`
    );
  }
}

/**
 * Verify the regex leaves benign `<scripture>`-like text alone (the `\b`
 * word-boundary in pass 2 prevents stripping because there is no
 * word/non-word transition between `<script` and the trailing `u`).
 */
function assertLeavesBenignScriptLikeTextAlone() {
  const input = "<scripture>is a word that should not be modified";
  const actual = sanitize(input);
  if (actual !== input) {
    throw new Error(
      `Post-fix sanitize should not modify '<scripture>' word. Got: '${actual}'`
    );
  }
}

/**
 * Verify that unclosed `<script>` tags are also stripped (closes
 * CodeQL js/incomplete-multi-character-sanitization alert on the
 * post-fix code path).
 */
function assertStripsUnclosedScriptTag() {
  const input = 'safe text <script src=//evil.example/x.js and never closed';
  const actual = sanitize(input);
  if (actual.includes("<script")) {
    throw new Error(
      `Post-fix sanitize should strip unclosed <script> tag. Got: '${actual}'`
    );
  }
}

/**
 * Verify that a `<SCRIPT>` (uppercase) tag is also stripped — the
 * regexes use the `i` flag.
 */
function assertStripsUpperCaseScriptTag() {
  const input = 'safe <SCRIPT>alert(1)</SCRIPT> end';
  const actual = sanitize(input);
  if (actual !== "safe  end") {
    throw new Error(
      `Post-fix sanitize should strip uppercase <SCRIPT> tag. Got: '${actual}'`
    );
  }
}

function runAllTests() {
  const tests = [
    ["Fast on adversarial input (100K spaces)", assertFastOnAdversarialInput],
    ["Strips normal script tag", assertStripsNormalScriptTag],
    ["Strips multi-line script tag", assertStripsMultiLineScriptTag],
    ["Strips multiple script tags", assertStripsMultipleScriptTags],
    ["Leaves benign <scripture> word alone", assertLeavesBenignScriptLikeTextAlone],
    ["Strips unclosed <script> tag", assertStripsUnclosedScriptTag],
    ["Strips uppercase <SCRIPT> tag", assertStripsUpperCaseScriptTag],
  ];

  let passed = 0;
  let failed = 0;
  for (const [name, fn] of tests) {
    try {
      fn();
      console.log(`PASS  ${name}`);
      passed++;
    } catch (e) {
      console.error(`FAIL  ${name}: ${e.message}`);
      failed++;
    }
  }
  console.log(`\nResult: ${passed} passed, ${failed} failed`);
  process.exit(failed === 0 ? 0 : 1);
}

runAllTests();