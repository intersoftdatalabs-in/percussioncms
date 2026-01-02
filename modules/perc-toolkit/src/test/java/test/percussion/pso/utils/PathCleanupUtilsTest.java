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
import org.jmock.Mockery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PathCleanupUtilsTest {
  private static final Logger log = LogManager.getLogger(PathCleanupUtilsTest.class);

  Mockery context;

<<<<<<< HEAD
  @BeforeEach
  public void setUp() {}

  @Test
  void testIsNotLowerCase() {
=======
  @Before
  public void setUp() throws Exception {}

  @Test
  public final void testIsNotLowerCase() {
>>>>>>> development-8.1.x
    final String testString = "/A/B/c/d/e.jpg";
    String result = PathCleanupUtils.cleanupPathPart(testString, false, true);
    assertEquals("/A/B/c/d/e.jpg", result);
  }

  @Test
<<<<<<< HEAD
  void testIsLowerCase() {
=======
  public final void testIsLowerCase() {
>>>>>>> development-8.1.x
    final String testString = "/A/B/c/d/e.jpg";
    String result = PathCleanupUtils.cleanupPathPart(testString, true, true);
    assertEquals("/a/b/c/d/e.jpg", result);
  }

  @Test
<<<<<<< HEAD
  void testIsExtension() {
=======
  public final void testIsExtension() {
>>>>>>> development-8.1.x
    final String testString = "aaaa...bbb.jpg";
    String result = PathCleanupUtils.cleanupPathPart(testString, true, true);
    assertEquals("aaaa-bbb.jpg", result);
  }

  @Test
<<<<<<< HEAD
  void testIsNotExtension() {
=======
  public final void testIsNotExtension() {
>>>>>>> development-8.1.x
    final String testString = "aaaa...bbb.bbb";
    String result = PathCleanupUtils.cleanupPathPart(testString, true, false);
    assertEquals("aaaa-bbb-bbb", result);
  }

  @Test
<<<<<<< HEAD
  void testSpeciaChars() {
=======
  public final void testSpeciaChars() {
>>>>>>> development-8.1.x
    final String testString = "a/b/c\\d/Awefe.dd&&$32.jpg";
    String result = PathCleanupUtils.cleanupPathPart(testString, true, true);
    assertEquals("a/b/c-d/awefe-dd-and-and-32.jpg", result);
  }

  @Test
<<<<<<< HEAD
  void stripExtension() {
=======
  public final void stripExtension() {
>>>>>>> development-8.1.x
    final String testString = "a/b/c\\d/Awefe.dd&&$32.jpg";
    String result = PathCleanupUtils.cleanupPathPart(testString, true, true, true, "", "", "");
    assertEquals("a/b/c-d/awefe-dd-and-and-32", result);
  }

  @Test
<<<<<<< HEAD
  void addPrefixSuffixWithExtension() {
=======
  public final void addPrefixSuffixWithExtension() {
>>>>>>> development-8.1.x
    final String testString = "a/b/c\\d/Awefe.dd&&$32.jpg";
    String result =
        PathCleanupUtils.cleanupPathPart(testString, true, true, false, "prefix_", "_suffix", "");
    assertEquals("prefix_a/b/c-d/awefe-dd-and-and-32_suffix.jpg", result);
  }

  @Test
<<<<<<< HEAD
  void forceExtension() {
=======
  public final void forceExtension() {
>>>>>>> development-8.1.x
    final String testString = "filename.jpg";
    String result =
        PathCleanupUtils.cleanupPathPart(
            testString, true, true, false, "prefix_", "_suffix", "test");
    assertEquals("prefix_filename_suffix.test", result);
  }
}
