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
 * regex `<script(?:(?!<\/script>)[\s\S])*<\/script>` uses a tempered
 * greedy pattern that matches each character only if it is NOT the
 * start of the terminator, removing the ambiguity that caused the
 * exponential blow-up.
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

const POST_FIX_REGEX = /<script(?:(?!<\/script>)[\s\S])*<\/script>/g;
const TIME_BUDGET_MS = 1000;

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
  const result = adversarial.replace(POST_FIX_REGEX, "");
  const elapsed = Date.now() - start;

  console.log(`Post-fix regex on 100K-space adversarial input: ${elapsed}ms`);

  if (elapsed > TIME_BUDGET_MS) {
    throw new Error(
      `Post-fix regex exceeded ${TIME_BUDGET_MS}ms budget (elapsed=${elapsed}ms) — likely exponential backtracking`
    );
  }
  // Input has no closing </script> so the regex should NOT match.
  if (result !== adversarial) {
    throw new Error(`Post-fix regex should not match adversarial input`);
  }
}

/**
 * Verify the regex correctly strips a normal script tag.
 */
function assertStripsNormalScriptTag() {
  const input = 'hello <script type="text/javascript">alert(1);</script> world';
  const expected = "hello  world";
  const actual = input.replace(POST_FIX_REGEX, "");
  if (actual !== expected) {
    throw new Error(
      `Post-fix regex did not strip normal script tag. Expected: '${expected}', Got: '${actual}'`
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
  const actual = input.replace(POST_FIX_REGEX, "");
  if (actual !== "before\n\nafter") {
    throw new Error(
      `Post-fix regex did not strip multi-line script tag correctly. Got: '${actual}'`
    );
  }
}

/**
 * Verify the regex strips multiple script tags in one input.
 */
function assertStripsMultipleScriptTags() {
  const input = '<script>a</script> middle <script>b</script> end';
  const actual = input.replace(POST_FIX_REGEX, "");
  if (actual !== " middle  end") {
    throw new Error(
      `Post-fix regex did not strip multiple script tags. Got: '${actual}'`
    );
  }
}

/**
 * Verify the regex leaves benign `<script>`-like text alone.
 */
function assertLeavesPlainTextAlone() {
  const input = "no scripts here, just plain text";
  const actual = input.replace(POST_FIX_REGEX, "");
  if (actual !== input) {
    throw new Error(`Post-fix regex should not modify plain text. Got: '${actual}'`);
  }
}

function runAllTests() {
  const tests = [
    ["Fast on adversarial input (100K spaces)", assertFastOnAdversarialInput],
    ["Strips normal script tag", assertStripsNormalScriptTag],
    ["Strips multi-line script tag", assertStripsMultiLineScriptTag],
    ["Strips multiple script tags", assertStripsMultipleScriptTags],
    ["Leaves plain text alone", assertLeavesPlainTextAlone],
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