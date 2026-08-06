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
package com.percussion.distribution.install;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for the portable ({@code .sh} → Java port) entry point of the build-time JDBC driver
 * verification gate. Exercises exit-code semantics, glob matching, JAR validation, and entry-level
 * CSV parsing without invoking any shell.
 */
class VerifyJdbcDriversTest {

  @Test
  @DisplayName("Empty jdbc dir → exit 2 (missing-or-empty)")
  void emptyJdbcDirExitsTwo(@TempDir Path workdir) throws Exception {
    Path jdbcDir = workdir.resolve("jdbc");
    Files.createDirectories(jdbcDir);
    Path fakeArtifact = createFakeArtifactWithJdbc(workdir, jdbcDir);

    int code =
        VerifyJdbcDrivers.run(
            new String[] {
              "--artifact", fakeArtifact.toString(), "--workdir", workdir.resolve("out").toString()
            });

    assertEquals(2, code);
  }

  @Test
  @DisplayName("No jdbc dir under dist → exit 2 (missing-or-empty)")
  void missingJdbcDirExitsTwo(@TempDir Path workdir) throws Exception {
    Path fakeArtifact =
        createArtifact(
            workdir,
            (IOWriter<ZipOutputStream>)
                out -> {
                  try {
                    out.putNextEntry(new ZipEntry("readme.txt"));
                    out.write("hello".getBytes());
                    out.closeEntry();
                  } catch (IOException e) {
                    throw new RuntimeException(e);
                  }
                });

    int code =
        VerifyJdbcDrivers.run(
            new String[] {
              "--artifact", fakeArtifact.toString(), "--workdir", workdir.resolve("out").toString()
            });
    assertEquals(2, code);
  }

  @Test
  @DisplayName("Zero-byte jar → exit 3")
  void zeroByteJarExitsThree(@TempDir Path workdir) throws Exception {
    Path jdbcDir = workdir.resolve("jdbc");
    Files.createDirectories(jdbcDir);
    Files.write(jdbcDir.resolve("empty.jar"), new byte[0]);
    Path fakeArtifact = createFakeArtifactWithJdbc(workdir, jdbcDir);

    int code =
        VerifyJdbcDrivers.run(
            new String[] {
              "--artifact", fakeArtifact.toString(), "--workdir", workdir.resolve("out").toString()
            });
    assertEquals(3, code);
  }

  @Test
  @DisplayName("Invalid (non-ZIP) jar → exit 4")
  void invalidJarExitsFour(@TempDir Path workdir) throws Exception {
    Path jdbcDir = workdir.resolve("jdbc");
    Files.createDirectories(jdbcDir);
    Files.write(jdbcDir.resolve("bad.jar"), "this is not a zip".getBytes());
    Path fakeArtifact = createFakeArtifactWithJdbc(workdir, jdbcDir);

    int code =
        VerifyJdbcDrivers.run(
            new String[] {
              "--artifact", fakeArtifact.toString(), "--workdir", workdir.resolve("out").toString()
            });
    assertEquals(4, code);
  }

  @Test
  @DisplayName("All good → exit 0")
  void happyPathExitsZero(@TempDir Path workdir) throws Exception {
    Path jdbcDir = workdir.resolve("jdbc");
    Files.createDirectories(jdbcDir);
    writeMinimalValidJar(jdbcDir.resolve("good.jar"));
    Path fakeArtifact = createFakeArtifactWithJdbc(workdir, jdbcDir);

    int code =
        VerifyJdbcDrivers.run(
            new String[] {
              "--artifact", fakeArtifact.toString(), "--workdir", workdir.resolve("out").toString()
            });
    assertEquals(0, code);
  }

  @Test
  @DisplayName("Missing artifact → exit 1 (invocation)")
  void missingArtifactExitsOne(@TempDir Path workdir) throws Exception {
    int code =
        VerifyJdbcDrivers.run(
            new String[] {"--artifact", workdir.resolve("does-not-exist.jar").toString()});
    assertEquals(1, code);
  }

  @Test
  @DisplayName("--expected-driver-glob matches and exits 6 when no JAR matches")
  void expectedGlobMatch(@TempDir Path workdir) throws Exception {
    Path jdbcDir = workdir.resolve("jdbc");
    Files.createDirectories(jdbcDir);
    writeMinimalValidJar(jdbcDir.resolve("derby-10.17.1.0.jar"));
    writeMinimalValidJar(jdbcDir.resolve("mariadb-java-client-3.5.7.jar"));
    Path fakeArtifact = createFakeArtifactWithJdbc(workdir, jdbcDir);

    int codeMatch =
        VerifyJdbcDrivers.run(
            new String[] {
              "--artifact",
              fakeArtifact.toString(),
              "--workdir",
              workdir.resolve("out1").toString(),
              "--expected-driver-glob",
              "derby-*.jar"
            });
    assertEquals(0, codeMatch, "derby glob matches and should succeed");

    int codeMiss =
        VerifyJdbcDrivers.run(
            new String[] {
              "--artifact",
              fakeArtifact.toString(),
              "--workdir",
              workdir.resolve("out2").toString(),
              "--expected-driver-glob",
              "missing-*.jar"
            });
    assertEquals(6, codeMiss);
  }

  @Test
  @DisplayName("--expected-driver-set (exact) → exit 6 when name absent")
  void expectedSetMissing(@TempDir Path workdir) throws Exception {
    Path jdbcDir = workdir.resolve("jdbc");
    Files.createDirectories(jdbcDir);
    writeMinimalValidJar(jdbcDir.resolve("foo.jar"));
    Path fakeArtifact = createFakeArtifactWithJdbc(workdir, jdbcDir);

    int code =
        VerifyJdbcDrivers.run(
            new String[] {
              "--artifact", fakeArtifact.toString(),
              "--workdir", workdir.resolve("out").toString(),
              "--expected-driver-set", "missing.jar"
            });
    assertEquals(6, code);
  }

  @Test
  @DisplayName("Glob matcher handles '*' and '?' correctly")
  void globMatcher() {
    assertTrue(
        VerifyJdbcDrivers.matchesGlob(Set.of("mariadb-java-client-3.5.7.jar"), "mariadb-*.jar"));
    assertTrue(VerifyJdbcDrivers.matchesGlob(Set.of("mariadb-1.2.3.jar"), "mariadb-?.?.?.jar"));
    assertFalse(VerifyJdbcDrivers.matchesGlob(Set.of("derby.jar"), "mariadb-*.jar"));
    // Literal dot, plus, and paren characters in the pattern are matched verbatim — they
    // do NOT act as regex wildcards (POSIX shell-glob semantics, which the .sh script uses).
    assertTrue(VerifyJdbcDrivers.matchesGlob(Set.of("a+b.jar"), "a+b.jar"));
    assertTrue(VerifyJdbcDrivers.matchesGlob(Set.of("(a).jar"), "(a).jar"));
    // A literal dot must NOT match any character; only the literal "." is a match.
    assertTrue(VerifyJdbcDrivers.matchesGlob(Set.of("a.b.jar"), "a.b.jar"));
    assertFalse(VerifyJdbcDrivers.matchesGlob(Set.of("axbXjar"), "a.b.jar"));
    // Backslash is treated as a literal char in shell glob; the Java port matches that behavior.
    assertTrue(VerifyJdbcDrivers.matchesGlob(Set.of("a\\.b\\.jar"), "a\\.b\\.jar"));
    assertFalse(VerifyJdbcDrivers.matchesGlob(Set.of("a.b.jar"), "a\\.b\\.jar"));
  }

  @Test
  @DisplayName("splitCsv mirrors the .sh IFS=',' loop and trims entries")
  void splitCsvTrimsAndDropsEmpty() {
    assertEquals(List.of("a", "b", "c"), VerifyJdbcDrivers.splitCsv("a,b,c"));
    assertEquals(List.of("a", "b"), VerifyJdbcDrivers.splitCsv(" a , b ,"));
    assertTrue(VerifyJdbcDrivers.splitCsv("").isEmpty());
    assertTrue(VerifyJdbcDrivers.splitCsv(null).isEmpty());
  }

  @Test
  @DisplayName("isValidJar rejects non-zip payloads")
  void isValidJarRejects(@TempDir Path workdir) throws Exception {
    Path good = workdir.resolve("good.jar");
    writeMinimalValidJar(good);
    Path bad = workdir.resolve("bad.jar");
    Files.write(bad, "not a zip".getBytes());
    assertTrue(VerifyJdbcDrivers.isValidJar(good));
    assertFalse(VerifyJdbcDrivers.isValidJar(bad));
  }

  // --- helpers ---

  private static Path createFakeArtifactWithJdbc(Path root, Path jdbcDir) throws IOException {
    Path artifact = root.resolve("artifact.zip");
    try (OutputStream fos = Files.newOutputStream(artifact);
        ZipOutputStream zf = new ZipOutputStream(fos)) {
      zf.putNextEntry(new ZipEntry("distribution/jetty/base/lib/jdbc/"));
      zf.closeEntry();
      for (Path p : Files.list(jdbcDir).toArray(Path[]::new)) {
        zf.putNextEntry(new ZipEntry("distribution/jetty/base/lib/jdbc/" + p.getFileName()));
        Files.copy(p, zf);
        zf.closeEntry();
      }
    }
    return artifact;
  }

  private static Path createArtifact(Path root, IOWriter<ZipOutputStream> body) throws IOException {
    Path artifact = root.resolve("artifact.zip");
    try (OutputStream fos = Files.newOutputStream(artifact);
        ZipOutputStream zf = new ZipOutputStream(fos)) {
      body.write(zf);
    }
    return artifact;
  }

  /** Builds a real JAR with a single empty {@code META-INF/MANIFEST.MF} entry. */
  private static void writeMinimalValidJar(Path jar) throws IOException {
    Files.createDirectories(jar.getParent());
    try (OutputStream fos = Files.newOutputStream(jar);
        JarOutputStream jos = new JarOutputStream(fos)) {
      jos.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
      jos.write("Manifest-Version: 1.0\n".getBytes());
      jos.closeEntry();
    }
  }

  @FunctionalInterface
  interface IOWriter<T> {
    void write(T out) throws IOException;
  }
}
