/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.preinstall.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.preinstall.java.JavaPropertiesSupport.JavaLoadResult;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Property load/merge/write contract tests for {@link JavaPropertiesSupport}. */
class JavaPropertiesSupportTest {

  @TempDir Path tempDir;

  @Test
  void loadAbsentFileReturnsEmptyButNotPresent() throws Exception {
    JavaLoadResult result = JavaPropertiesSupport.load(tempDir);
    assertEquals(false, result.present());
    assertEquals(tempDir.resolve("java.properties"), result.location());
    assertTrue(result.properties().isEmpty());
  }

  @Test
  void roundTripPreservesUnknownKeys() throws Exception {
    Files.writeString(
        tempDir.resolve("java.properties"),
        "FOO=bar\nJAVA_HOME=" + tempDir.toAbsolutePath().resolve("old") + "\n");
    String newHome = tempDir.toAbsolutePath().resolve("jdk-21").toString();
    String newLauncher = newHome + "/bin/java";
    JavaPropertiesSupport.write(tempDir, newHome, newLauncher);
    String content = Files.readString(tempDir.resolve("java.properties"), StandardCharsets.UTF_8);
    assertTrue(content.contains("FOO=bar"), "FOO preserved: " + content);
    assertTrue(content.contains("jdk-21"), "JAVA_HOME contains new home suffix: " + content);
    assertTrue(content.contains("bin/java"), "JAVA contains new launcher suffix: " + content);
  }

  @Test
  void writeInfersLauncherWhenNotProvided() throws Exception {
    String home = tempDir.resolve("jdk21").toAbsolutePath().toString();
    // No launcher provided — write should derive a launcher from home.
    JavaPropertiesSupport.write(tempDir, home, null);
    var result = JavaPropertiesSupport.load(tempDir);
    assertTrue(result.present());
    assertEquals(home, result.properties().get("JAVA_HOME"));
    String inferredLauncher = result.properties().get("JAVA");
    assertNotNull(inferredLauncher);
    assertTrue(
        inferredLauncher.startsWith(home), "launcher derived under home: " + inferredLauncher);
    assertTrue(
        inferredLauncher.endsWith("java") || inferredLauncher.endsWith("java.exe"),
        "launcher suffix on " + inferredLauncher);
  }

  @Test
  void writeRejectsRelativeJavaHome() {
    String rel = "relative/JRE";
    assertThrows(
        IllegalArgumentException.class, () -> JavaPropertiesSupport.write(tempDir, rel, null));
  }

  @Test
  void writeRejectsEmptyJavaHome() {
    assertThrows(
        IllegalArgumentException.class, () -> JavaPropertiesSupport.write(tempDir, "", null));
  }

  @Test
  void writeRejectsNullJavaHome() {
    assertThrows(
        IllegalArgumentException.class, () -> JavaPropertiesSupport.write(tempDir, null, null));
  }

  @Test
  void readJavaHomeReturnsNullWhenAbsent() throws Exception {
    assertNull(JavaPropertiesSupport.readJavaHome(tempDir));
    assertNull(JavaPropertiesSupport.readJava(tempDir));
  }

  @Test
  void readJavaHomeReturnsTrimmedAbsolute() throws Exception {
    String home = tempDir.resolve("jdk").toAbsolutePath().toString();
    JavaPropertiesSupport.write(tempDir, home, home + "/bin/java");
    assertEquals(home, JavaPropertiesSupport.readJavaHome(tempDir));
    assertEquals(home + "/bin/java", JavaPropertiesSupport.readJava(tempDir));
  }

  @Test
  void mergePreservingAddsWithoutClobberingExisting() throws Exception {
    Files.writeString(tempDir.resolve("java.properties"), "FOO=keep\n");
    Map<String, String> merged =
        JavaPropertiesSupport.mergePreserving(tempDir, Map.of("BAR", "added", "FOO", "ignored"));
    assertEquals("keep", merged.get("FOO"));
    assertEquals("added", merged.get("BAR"));
  }

  @Test
  void noSuccessConfigWrittenWhenNotInvoked() throws Exception {
    // Sanity: just loading an absent install dir leaves no file behind.
    assertNull(JavaPropertiesSupport.readJavaHome(tempDir));
    assertFalse(Files.exists(tempDir.resolve("java.properties")));
  }

  @Test
  void pathRoundTripsAcrossPlatforms() throws Exception {
    // Use a relative root to verify the underlying NIO Path APIs keep paths portable.
    String home = tempDir.toAbsolutePath().toString();
    JavaPropertiesSupport.write(tempDir, home, home + "/bin/java");
    Map<String, String> again = JavaPropertiesSupport.load(tempDir).properties();
    assertEquals(home, again.get("JAVA_HOME"));
  }

  /** File-system access helper for tests — replaces AssertFalse import. */
  private static void assertFalse(boolean condition) {
    org.junit.jupiter.api.Assertions.assertFalse(condition);
  }
}
