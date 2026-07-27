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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.theme;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.percussion.share.service.IPSDataService.PSThemeNotFoundException;
import com.percussion.theme.service.impl.PSRegionCSSFileService;
import java.io.File;
import java.nio.file.Files;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the CWE-22 / CWE-23 path-injection alerts on {@link PSRegionCSSFileService}.
 * Covers GitHub code-scanning alerts #1714 and #1715 (java/path-injection, error severity,
 * projects/sitemanage/).
 *
 * <p>Pre-fix behavior: the public methods accepted any string as a file path and resolved it
 * against the JVM working directory. A payload like {@code ../../tmp/outside.css} would have
 * allowed read/write of files outside the themes root.
 *
 * <p>Post-fix behavior: the public methods validate the input via {@code
 * PSPathInjectionGuard.requireUnderBase} when the {@code themesRoot} is configured, rejecting any
 * path that does not resolve under the configured themes root. Tests MUST fail on the pre-fix code
 * (no validation, payload is accepted) and pass on the post-fix code (validation throws {@link
 * IllegalArgumentException}).
 */
public class PSRegionCSSFileServiceSecurityTest {

  private File themesRoot;
  private PSRegionCSSFileService service;

  @BeforeEach
  public void setUp() throws Exception {
    // Use a short prefix so the resulting temp dir name does not
    // exceed Windows' 260-char MAX_PATH limit when combined with the
    // JVM's user.home + Local\Temp prefix (which on this machine is
    // C:\Users\VIJAYA~1.BOD\... — already 50+ chars).
    themesRoot = Files.createTempDirectory("psc-").toFile();
    themesRoot.mkdirs();
    service = new PSRegionCSSFileService();
    service.setThemesRoot(themesRoot);
  }

  @AfterEach
  public void tearDown() throws Exception {
    deleteRecursively(themesRoot);
  }

  /** Closes alert #1714: read() with traversal payload rejected. */
  @Test
  public void read_rejectsTraversalAboveThemesRoot() {
    String payload = themesRoot.getAbsolutePath() + "/../../../etc/passwd";
    assertThrows(
        IllegalArgumentException.class,
        () -> service.read(payload),
        "read() must reject a path that escapes themesRoot");
  }

  /** Closes alert #1715: save() with traversal payload rejected. */
  @Test
  public void save_rejectsTraversalAboveThemesRoot() {
    String payload = themesRoot.getAbsolutePath() + "/../../escape.css";
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          try {
            service.save(new com.percussion.theme.data.PSRegionCSS("o", "r"), payload);
          } catch (PSThemeNotFoundException e) {
            throw new RuntimeException(e);
          }
        },
        "save() must reject a path that escapes themesRoot");
  }

  /** Same defense for delete(). */
  @Test
  public void delete_rejectsTraversalAboveThemesRoot() {
    String payload = themesRoot.getAbsolutePath() + "/../delete-target.css";
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          try {
            service.delete("o", "r", payload);
          } catch (PSThemeNotFoundException e) {
            throw new RuntimeException(e);
          }
        },
        "delete() must reject a path that escapes themesRoot");
  }

  /** write() with traversal payload rejected. */
  @Test
  public void write_rejectsTraversalAboveThemesRoot() {
    String payload = themesRoot.getAbsolutePath() + "/../write-target.css";
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          try {
            service.write(payload, new java.util.ArrayList<>());
          } catch (PSThemeNotFoundException e) {
            throw new RuntimeException(e);
          }
        },
        "write() must reject a path that escapes themesRoot");
  }

  /** mergeFile() with traversal target rejected. */
  @Test
  public void mergeFile_rejectsTraversalTargetPath() {
    String src = new File(themesRoot, "safe-src.css").getAbsolutePath();
    String target = themesRoot.getAbsolutePath() + "/../merge-target.css";
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          try {
            service.mergeFile(new com.percussion.pagemanagement.data.PSRegionTree(), src, target);
          } catch (PSThemeNotFoundException e) {
            throw new RuntimeException(e);
          }
        },
        "mergeFile() must reject a target path that escapes themesRoot");
  }

  /** copyFile() with traversal target rejected. */
  @Test
  public void copyFile_rejectsTraversalTargetPath() throws Exception {
    // Create the source file inside themesRoot so that copyFile()
    // reaches the target-path validation step (not the source
    // existence check, which would mask the traversal).
    File src = new File(themesRoot, "safe.css");
    Files.write(src.toPath(), "/* safe */".getBytes());
    String target = themesRoot.getAbsolutePath() + "/../copy-target.css";
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          try {
            service.copyFile(src.getAbsolutePath(), target);
          } catch (PSThemeNotFoundException e) {
            throw new RuntimeException(e);
          }
        },
        "copyFile() must reject a target path that escapes themesRoot");
  }

  /** Sanity check: a path inside themesRoot is accepted (does NOT throw). */
  @Test
  public void read_acceptsPathInsideThemesRoot() throws Exception {
    File insideFile = new File(themesRoot, "inside.css");
    Files.write(insideFile.toPath(), "/* safe */".getBytes());
    // Should NOT throw IAE - validates that the fix doesn't over-reject
    // legitimate use cases. The read() may throw PSThemeNotFoundException
    // if the CSS is malformed, but it must NOT throw IAE because the
    // path is under themesRoot.
    String absPath = insideFile.getAbsolutePath();
    try {
      service.read(absPath);
    } catch (PSThemeNotFoundException expected) {
      // Parse failure is fine - what matters is that validation
      // doesn't reject the path itself.
    } catch (IllegalArgumentException unexpected) {
      throw new AssertionError(
          "read() must accept a path inside themesRoot, but threw IAE: " + unexpected.getMessage(),
          unexpected);
    }
  }

  private static void deleteRecursively(File f) {
    if (f == null || !f.exists()) return;
    if (f.isDirectory()) {
      File[] children = f.listFiles();
      if (children != null) {
        for (File c : children) deleteRecursively(c);
      }
    }
    f.delete();
  }
}
