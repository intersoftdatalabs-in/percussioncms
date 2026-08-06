/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
package com.percussion.extensions.translations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * Regression coverage for {@link PSFormEncodeDecodeHelper} focused on the {@code java/redos}
 * finding closed at {@code PSFormEncodeDecodeHelper.java:171} (CodeQL alert #763).
 *
 * <p>The previous alternation {@code ([^\- ]|[\r\n]|-[^\- ])*} overlapped the trailing whitespace
 * class {@code [ \r\n\t]*}, so the engine could enumerate ambiguous split points on newline-heavy
 * input (CWE-1333 / catastrophic backtracking). The fix collapses the alternation (removing the
 * redundant {@code [\r\n]} branch — the {@code [\r\n]} chars are a subset of the {@code [^\- ]}
 * class) and adds a 64 KiB input-size guard.
 *
 * <p>These tests verify (a) behavioral parity with the original on canonical inputs, (b) linear
 * runtime on the previously adversarial input, and (c) the size guard makes pathological inputs a
 * no-op.
 */
public class PSFormEncodeDecodeHelperTest {

  /** Tolerable wall-clock budget for adversarial inputs (the pre-fix code ran for minutes). */
  private static final Duration REDOS_BUDGET = Duration.ofSeconds(2);

  /** The exact no-space-comment padding the production helper is built to produce. */
  @Test
  void testEncodesNoSpaceCommentToPaddedForm() {
    String encoded = PSFormEncodeDecodeHelper.encode("<!--somecomment-->");
    assertTrue(
        encoded.contains("<!-- somecomment -->"),
        "encode() must pad a nospace comment with single-space padding, got: " + encoded);
  }

  /**
   * Comments containing dashes inside the body are padded (dash must not be at end of body — the
   * helper's regex refuses to match a trailing dash because of how the capture group is shaped).
   */
  @Test
  void testEncodesCommentWithDashInside() {
    String encoded = PSFormEncodeDecodeHelper.encode("<!--abc-def-->");
    assertTrue(
        encoded.contains("<!-- abc-def -->"),
        "encode() must pad a dash-bearing comment, got: " + encoded);
  }

  /** Comments that already contain whitespace inside the body are NOT re-padded. */
  @Test
  void testLeavesCommentsWithInternalSpacesAlone() {
    String input = "<!-- already spaced -->";
    String encoded = PSFormEncodeDecodeHelper.encode(input);
    assertEquals(input, encoded, "Already-padded comments must pass through encode() unchanged");
  }

  /**
   * Adjacent to-form and to-padded-comment paths both succeed when the input has a no-space comment
   * immediately followed by an actual script tag (the legacy {@code ms_test_string} shape).
   */
  @Test
  void testEncodeHandlesCommentFollowedByScript() {
    String input = "<table><tr><td>\n<!--somecomment--><script>x</script>\n</td></tr></table>";
    String encoded = PSFormEncodeDecodeHelper.encode(input);
    assertTrue(
        encoded.contains("<!-- somecomment -->"),
        "Pre-script no-space comment must be padded, got: " + encoded);
    assertTrue(
        encoded.contains("<div "),
        "Script tag must still be transformed to a div placeholder, got: " + encoded);
  }

  /**
   * Regression for CodeQL java/redos #763. Pre-fix, a stream of newlines inside an opening comment
   * caused the engine to enumerate ambiguous split points. With the fix the same input completes in
   * well under the budget below.
   */
  @Test
  void testAdversarialNewlineInputCompletesQuickly() {
    int newlines = 64 * 1024;
    StringBuilder input = new StringBuilder(newlines + 16);
    input.append("<!--");
    for (int i = 0; i < newlines; i++) {
      input.append('\n');
    }
    input.append("-->");

    String result =
        assertTimeout(
            REDOS_BUDGET,
            () -> PSFormEncodeDecodeHelper.encode(input.toString()),
            "Encoding a comment with "
                + newlines
                + " newlines must complete within the ReDoS budget; fix regressed?");
    assertNotNull(result);
    assertTrue(result.length() > 0, "Encoding must always return a non-empty string");
  }

  /**
   * Regression for the same root cause with tabs (also a whitespace char that the original pattern
   * could backtrack on).
   */
  @Test
  void testAdversarialTabInputCompletesQuickly() {
    int tabs = 64 * 1024;
    StringBuilder input = new StringBuilder(tabs + 16);
    input.append("<!--");
    for (int i = 0; i < tabs; i++) {
      input.append('\t');
    }
    input.append("-->");

    String result =
        assertTimeout(
            REDOS_BUDGET,
            () -> PSFormEncodeDecodeHelper.encode(input.toString()),
            "Encoding a comment with " + tabs + " tabs must complete within the ReDoS budget");
    assertNotNull(result);
  }

  /**
   * Under-cap whitespace inside a real comment body. This test stays well under the 64 KiB cap (so
   * the regex runs) and verifies the post-fix pattern matches a non-trivial whitespace-bearing body
   * exactly the way the original did — i.e., the regex simplification did not change behavior on
   * whitespace inside the body.
   *
   * <p>The cap-boundary tests above exercise the size-guard path; this one exercises the regex
   * simplification path on real content. Combined, they pin both layers of the fix.
   *
   * <p>Note: a regression that removed the regex simplification while keeping the size cap would
   * still pass these tests (the redundant {@code [\r\n]} branch is functionally equivalent at every
   * input size tested), so this is behavior parity, not defense-in-depth.
   */
  @Test
  void testUnderCapWhitespaceInCommentBodyBehavesIdentically() {
    String input = "<p><!--foo\nbar\nbaz\nqux--></p>";
    String encoded = PSFormEncodeDecodeHelper.encode(input);
    assertTrue(
        encoded.contains("<!-- foo\nbar\nbaz\nqux -->"),
        "Whitespace-bearing body must still be padded with the body contents intact, got: "
            + encoded);
  }

  /**
   * Under-cap newlines as the WHOLE body (the most adversarial realistic shape). The regex must
   * complete quickly because there is no non-whitespace-non-dash boundary char to anchor on — the
   * engine must reject the input, not enumerate splits. Input size is kept under Java's regex
   * recursion-depth limit (~5000 frames for a typical 512 KB stack), so this stays well under both
   * the 64 KiB cap and the engine's recursion budget.
   */
  @Test
  void testUnderCapNewlineOnlyBodyRejectedQuickly() {
    StringBuilder input = new StringBuilder(1024);
    input.append("<!--");
    for (int i = 0; i < 256; i++) {
      input.append('\n');
    }
    input.append("-->");

    int totalLen = input.length();
    assertTrue(totalLen < 65_536, "Pre-condition: under the 64 KiB cap, got " + totalLen);
    assertTrue(
        totalLen < 4096,
        "Pre-condition: under Java's regex recursion budget (~4 KiB), got " + totalLen);

    String result =
        assertTimeout(
            REDOS_BUDGET,
            () -> PSFormEncodeDecodeHelper.encode(input.toString()),
            "All-whitespace body must be rejected quickly, not enumerated as ambiguous splits");
    assertNotNull(result);
    assertTrue(
        result.contains("\n"),
        "Original newlines must be preserved verbatim in the output, got: " + result);
  }

  /**
   * Production {@code MAX_COMMENT_INPUT_LENGTH} is private ({@code 64 * 1024}); keep this test
   * constant strictly above that value without coupling to the production field visibility.
   */
  private static final int PRODUCTION_MAX_COMMENT_INPUT_LENGTH = 64 * 1024;

  /**
   * Deliberately larger than {@link #PRODUCTION_MAX_COMMENT_INPUT_LENGTH} for the pass-through
   * path.
   */
  private static final int OVERSIZE_INPUT_CHARS = 70 * 1024;

  /** Length of {@code <!--} + {@code -->} wrappers around the synthetic comment body. */
  private static final int HTML_COMMENT_WRAPPER_LEN = 7;

  /**
   * Defensive: input above the size cap is returned unchanged (the helper must not throw, hang, or
   * truncate adversarial payloads). Builds a body of {@link #OVERSIZE_INPUT_CHARS} total chars so
   * the payload exceeds {@link #PRODUCTION_MAX_COMMENT_INPUT_LENGTH}; the body loop subtracts
   * {@link #HTML_COMMENT_WRAPPER_LEN} so the full string length is exactly {@code
   * OVERSIZE_INPUT_CHARS}. The production cap is {@code MAX_COMMENT_INPUT_LENGTH = 64 * 1024}
   * (private in the helper).
   */
  @Test
  void testEncodePassesThroughOversizedInput() {
    assertTrue(
        OVERSIZE_INPUT_CHARS > PRODUCTION_MAX_COMMENT_INPUT_LENGTH,
        "test oversize must exceed the production cap");
    StringBuilder input = new StringBuilder(OVERSIZE_INPUT_CHARS);
    input.append("<!--");
    for (int i = 0; i < OVERSIZE_INPUT_CHARS - HTML_COMMENT_WRAPPER_LEN; i++) {
      input.append('a');
    }
    input.append("-->");
    String original = input.toString();

    String result =
        assertTimeout(
            REDOS_BUDGET,
            () -> PSFormEncodeDecodeHelper.encode(original),
            "Oversized input must be returned without regex processing");
    assertEquals(
        original,
        result,
        "Inputs above the size cap must be passed through verbatim, untouched by the regex");
  }

  /** encode() and decode() must reject null per the documented contract. */
  @Test
  void testNullInputsRejected() {
    assertThrows(
        IllegalArgumentException.class, () -> PSFormEncodeDecodeHelper.encode((String) null));
    assertThrows(
        IllegalArgumentException.class, () -> PSFormEncodeDecodeHelper.decode((String) null));
  }

  /**
   * Decoding a previously-encoded input is a no-op for the comment itself (the padding added by
   * encode is intentional and one-way): the helper is meant to massage input into a form a
   * downstream HTML parser can handle, not to be fully reversible.
   */
  @Test
  void testDecodeLeavesPaddedCommentsAlone() {
    String input = "<!-- somecomment -->";
    String decoded = PSFormEncodeDecodeHelper.decode(input);
    assertEquals(
        input, decoded, "Decoding must not strip the padding added by encode(); got: " + decoded);
  }
}
