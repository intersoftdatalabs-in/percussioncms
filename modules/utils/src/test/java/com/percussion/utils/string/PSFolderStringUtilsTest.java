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

package com.percussion.utils.string;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.security.SecureStringUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class PSFolderStringUtilsTest {

  @TempDir public Path temporaryFolder;

  @Test
  public void testFolderStringUtils() throws IOException {

    File parentA = temporaryFolder.resolve("parentA").toFile();
    File parentB = temporaryFolder.resolve("parentB").toFile();
    File childA = temporaryFolder.resolve("parentA").resolve("childA").toFile();

    assertFalse(SecureStringUtils.isChildOfFilePath(parentA.toPath(), parentB.toPath()));
    assertTrue(SecureStringUtils.isChildOfFilePath(parentA.toPath(), childA.toPath()));
    assertFalse(SecureStringUtils.isChildOfFilePath(parentB.toPath(), childA.toPath()));

    assertTrue(SecureStringUtils.isSameFileAs(parentA.toPath(), parentA.toPath()));
    assertFalse(SecureStringUtils.isSameFileAs(parentA.toPath(), childA.toPath()));
    assertFalse(SecureStringUtils.isSameFileAs(parentA.toPath(), parentB.toPath()));
  }

  /**
   * Regression test for the {@code java/regex-injection} alerts CodeQL raised at
   * PSFolderStringUtils.java:73. The pre-fix code escaped non-alphanumeric characters to
   * Unicode hex sequences but {@code CodeQL}'s static analysis couldn't statically prove
   * the escaping covered every meta-character. The post-fix code splits the path on '%' (the
   * documented wildcard) and applies {@link Pattern#quote} to each literal segment, joining
   * with {@code .*} so the wildcard feature still works.
   *
   * <p>Adversarial inputs containing regex meta-characters ({@code .}, {@code $}, {@code (}, etc.)
   * must be treated as literal characters in the produced pattern, not as regex syntax.
   */
  @Test
  public void testGetFolderPatternsTreatsMetaCharactersAsLiterals() {
    // Each of these would match unexpected paths if interpreted as regex.
    String[] adversarialInputs = {
      "a.b.c", // '.' must be literal, not "any char"
      "site(name)", // '(' and ')' must be literal
      "site$name", // '$' must be literal
      "site^name", // '^' must be literal
      "site|name", // '|' must be literal
      "site[abc]name", // character class brackets must be literal
      "site+name", // '+' must be literal
    };

    for (String adversarial : adversarialInputs) {
      Pattern[] patterns = PSFolderStringUtils.getFolderPatterns(adversarial);
      assertEquals(1, patterns.length, "expected exactly one pattern for input: " + adversarial);
      String compiled = patterns[0].pattern();
      // The pattern source must contain a \Q..\E wrapper (Pattern.quote marker) so that
      // regex-injection-aware scanners (CodeQL) recognize the input as quoted.
      assertTrue(
          compiled.contains("\\Q"),
          "Pattern.quote was not applied for input: '"
              + adversarial
              + "' (compiled regex: "
              + compiled
              + ")");
      assertTrue(
          compiled.endsWith("\\E/"),
          "Pattern must end with '\\E/' so paths match the trailing slash; compiled: "
              + compiled);

      // Behavioural check: each pattern must match its own literal input plus trailing slash.
      Pattern p = patterns[0];
      String literalInput = adversarial + "/";
      assertTrue(
          p.matcher(literalInput).matches(),
          "literal match should succeed for: '" + adversarial + "' against '" + literalInput + "'");

      // And each regex meta-character in the input must NOT match arbitrary other characters.
      // For example "a.b.c/" must NOT match "aXbXc/" (the '.' would otherwise be a wildcard).
      // Replace every non-alphanumeric, non-/, non-% char with 'X' to produce a sentinel that
      // shares the structure but should NOT match if the meta-chars are properly quoted.
      String sentinel = adversarial.replaceAll("[^A-Za-z0-9/%]", "X") + "/";
      assertFalse(
          p.matcher(sentinel).matches(),
          "regex meta characters must not match arbitrary chars for input: '"
              + adversarial
              + "' (sentinel: '"
              + sentinel
              + "', compiled: "
              + compiled
              + ")");
    }
  }

  /**
   * Verifies that the documented {@code %} wildcard still works after the regex-injection fix.
   * Splitting on '%' and joining with {@code .*} preserves the original semantics.
   */
  @Test
  public void testGetFolderPatternsPreservesPercentWildcard() {
    Pattern[] patterns = PSFolderStringUtils.getFolderPatterns("foo%bar");
    assertEquals(1, patterns.length);
    String compiled = patterns[0].pattern();
    assertTrue(
        compiled.contains(".*"),
        "percent wildcard should still compile to .* in: " + compiled);
    // foo%bar/ should match foo + anything + bar + /
    assertTrue(patterns[0].matcher("fooanythingbar/").matches());
    assertTrue(patterns[0].matcher("foobar/").matches()); // % matches empty
    assertFalse(patterns[0].matcher("foo/ba/").matches()); // too short
  }

  /**
   * Verifies that multiple folder patterns separated by ';' are correctly compiled.
   */
  @Test
  public void testGetFolderPatternsMultiplePaths() {
    Pattern[] patterns = PSFolderStringUtils.getFolderPatterns("path1;path2;path3");
    assertEquals(3, patterns.length);
  }

  /**
   * Verifies that null/blank input returns an empty array (never null).
   */
  @Test
  public void testGetFolderPatternsBlankInput() {
    assertEquals(0, PSFolderStringUtils.getFolderPatterns(null).length);
    assertEquals(0, PSFolderStringUtils.getFolderPatterns("").length);
    assertEquals(0, PSFolderStringUtils.getFolderPatterns("   ").length);
  }
}
