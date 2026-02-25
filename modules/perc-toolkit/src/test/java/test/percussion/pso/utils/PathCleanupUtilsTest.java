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
package test.percussion.pso.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.percussion.pso.utils.PathCleanupUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PathCleanupUtilsTest {
  private static final Logger log = LogManager.getLogger(PathCleanupUtilsTest.class);


  @BeforeEach
  public void setUp() {}

  @Test
  void testIsNotLowerCase() {
    final String testString = "/A/B/c/d/e.jpg";
    String result = PathCleanupUtils.cleanupPathPart(testString, false, true);
    assertEquals("/A/B/c/d/e.jpg", result);
  }

  @Test
  void testIsLowerCase() {
    final String testString = "/A/B/c/d/e.jpg";
    String result = PathCleanupUtils.cleanupPathPart(testString, true, true);
    assertEquals("/a/b/c/d/e.jpg", result);
  }

  @Test
  void testIsExtension() {
    final String testString = "aaaa...bbb.jpg";
    String result = PathCleanupUtils.cleanupPathPart(testString, true, true);
    assertEquals("aaaa-bbb.jpg", result);
  }

  @Test
  void testIsNotExtension() {
    final String testString = "aaaa...bbb.bbb";
    String result = PathCleanupUtils.cleanupPathPart(testString, true, false);
    assertEquals("aaaa-bbb-bbb", result);
  }

  @Test
  void testSpeciaChars() {
    final String testString = "a/b/c\\d/Awefe.dd&&$32.jpg";
    String result = PathCleanupUtils.cleanupPathPart(testString, true, true);
    assertEquals("a/b/c-d/awefe-dd-and-and-32.jpg", result);
  }

  @Test
  void stripExtension() {
    final String testString = "a/b/c\\d/Awefe.dd&&$32.jpg";
    String result = PathCleanupUtils.cleanupPathPart(testString, true, true, true, "", "", "");
    assertEquals("a/b/c-d/awefe-dd-and-and-32", result);
  }

  @Test
  void addPrefixSuffixWithExtension() {
    final String testString = "a/b/c\\d/Awefe.dd&&$32.jpg";
    String result =
        PathCleanupUtils.cleanupPathPart(testString, true, true, false, "prefix_", "_suffix", "");
    assertEquals("prefix_a/b/c-d/awefe-dd-and-and-32_suffix.jpg", result);
  }

  @Test
  void forceExtension() {
    final String testString = "filename.jpg";
    String result =
        PathCleanupUtils.cleanupPathPart(
            testString, true, true, false, "prefix_", "_suffix", "test");
    assertEquals("prefix_filename_suffix.test", result);
  }
}
