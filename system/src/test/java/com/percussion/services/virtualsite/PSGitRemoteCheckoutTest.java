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
package com.percussion.services.virtualsite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.sitemgr.data.PSSite;
import com.percussion.services.sitemgr.data.PSSiteProperty;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

class PSGitRemoteCheckoutTest {

  @TempDir Path tempDir;

  @Test
  void rejectHttpAndTraversalAndOptionInjection() {
    assertThrows(
        VirtualSiteException.class,
        () -> PSGitRemoteCheckout.requireSafeRemoteUrl("http://git.example.com/repo.git"));
    assertThrows(
        VirtualSiteException.class,
        () -> PSGitRemoteCheckout.requireSafeRemoteUrl("https://git.example.com/../repo.git"));
    assertThrows(
        VirtualSiteException.class,
        () -> PSGitRemoteCheckout.requireSafeRemoteUrl("-uhttps://git.example.com/repo.git"));
    assertThrows(
        VirtualSiteException.class,
        () -> PSGitRemoteCheckout.requireSafeRemoteUrl("https://git.example.com/repo.git;rm -rf /"));
    assertThrows(
        VirtualSiteException.class, () -> PSGitRemoteCheckout.requireSafeRemoteUrl("javascript:alert(1)"));
    assertThrows(VirtualSiteException.class, () -> PSGitRemoteCheckout.requireSafeRemoteUrl("   "));
  }

  @Test
  void acceptHttpsSshFileAndScp() throws Exception {
    assertEquals(
        "https://git.example.com/org/repo.git",
        PSGitRemoteCheckout.requireSafeRemoteUrl("https://git.example.com/org/repo.git"));
    assertEquals(
        "ssh://git@git.example.com/org/repo.git",
        PSGitRemoteCheckout.requireSafeRemoteUrl("ssh://git@git.example.com/org/repo.git"));
    assertEquals(
        "git@git.example.com:org/repo.git",
        PSGitRemoteCheckout.requireSafeRemoteUrl("git@git.example.com:org/repo.git"));
    Path local = tempDir.resolve("fixture-repo");
    Files.createDirectories(local);
    String fileUrl = local.toUri().toString();
    assertEquals(fileUrl, PSGitRemoteCheckout.requireSafeRemoteUrl(fileUrl));
  }

  @Test
  void rejectUnsafeBranch() {
    assertThrows(VirtualSiteException.class, () -> PSGitRemoteCheckout.requireSafeBranch("-main"));
    assertThrows(VirtualSiteException.class, () -> PSGitRemoteCheckout.requireSafeBranch("feat/../x"));
    assertThrows(VirtualSiteException.class, () -> PSGitRemoteCheckout.requireSafeBranch("main;rm"));
    assertThrows(VirtualSiteException.class, () -> PSGitRemoteCheckout.requireSafeBranch(""));
  }

  @Test
  void acceptSafeBranch() throws Exception {
    assertEquals("main", PSGitRemoteCheckout.requireSafeBranch("main"));
    assertEquals("release/8.2", PSGitRemoteCheckout.requireSafeBranch("release/8.2"));
  }

  @Test
  void redactStripsUserinfoAndNeverLeavesToken() {
    String redacted =
        PSGitRemoteCheckout.redact("https://user:super-secret@git.example.com/org/repo.git");
    assertFalse(redacted.contains("super-secret"));
    assertTrue(redacted.contains("***"));
    assertTrue(redacted.contains("git.example.com"));
    assertEquals(
        "token=*** in log",
        PSGitRemoteCheckout.redact("token=abc123 in log"));
  }

  @Test
  void cloneUsesProcessBuilderSafeArgsAndContainedWorkDir() throws Exception {
    List<List<String>> commands = new ArrayList<>();
    PSGitRemoteCheckout checkout =
        new PSGitRemoteCheckout(
            (cwd, command, output) -> {
              commands.add(List.copyOf(command));
              if (command.contains("clone")) {
                Path dest = Path.of(command.get(command.size() - 1));
                Files.createDirectories(dest.resolve(".git"));
                Files.writeString(dest.resolve(".git").resolve("HEAD"), "ref: refs/heads/main");
              }
              return 0;
            },
            Duration.ofSeconds(5));

    Path workBase = tempDir.resolve("checkouts");
    Path discovered =
        checkout.ensureCurrent(
            "https://git.example.com/org/repo.git", "main", "Help Docs", workBase, null);

    Path expected = workBase.resolve("Help_Docs");
    assertEquals(expected.normalize(), discovered.normalize());
    assertTrue(Files.isDirectory(expected.resolve(".git")));
    assertEquals(1, commands.size());
    List<String> clone = commands.get(0);
    assertEquals("git", clone.get(0));
    assertTrue(clone.contains("clone"));
    assertTrue(clone.contains("--"));
    int dash = clone.indexOf("--");
    assertEquals("https://git.example.com/org/repo.git", clone.get(dash + 1));
    assertFalse(clone.stream().anyMatch(a -> a.startsWith("-u") && a.length() > 2 && !a.equals("--")));
    assertFalse(clone.contains(".."));
  }

  @Test
  void fetchExistingWhenGitDirPresent() throws Exception {
    Path workBase = tempDir.resolve("existing");
    Path workDir = workBase.resolve("docs");
    Files.createDirectories(workDir.resolve(".git"));
    List<List<String>> commands = new ArrayList<>();
    PSGitRemoteCheckout checkout =
        new PSGitRemoteCheckout(
            (cwd, command, output) -> {
              commands.add(List.copyOf(command));
              if (command.contains("get-url")) {
                output.append("https://git.example.com/org/repo.git");
              }
              return 0;
            },
            Duration.ofSeconds(5));

    checkout.ensureCurrent(
        "https://git.example.com/org/repo.git", "release/8.2", "docs", workBase, null);

    assertTrue(commands.stream().anyMatch(c -> c.contains("fetch")));
    assertTrue(commands.stream().anyMatch(c -> c.contains("checkout")));
    assertTrue(
        commands.stream()
            .filter(c -> c.contains("fetch"))
            .anyMatch(c -> c.contains("--") && c.contains("origin") && c.contains("release/8.2")));
  }

  @Test
  void failClosedWhenExistingOriginDoesNotMatch() throws Exception {
    Path workBase = tempDir.resolve("mismatch");
    Path workDir = workBase.resolve("docs");
    Files.createDirectories(workDir.resolve(".git"));
    PSGitRemoteCheckout checkout =
        new PSGitRemoteCheckout(
            (cwd, command, output) -> {
              if (command.contains("get-url")) {
                output.append("https://evil.example.com/other.git");
              }
              return 0;
            },
            Duration.ofSeconds(5));

    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                checkout.ensureCurrent(
                    "https://git.example.com/org/repo.git", "main", "docs", workBase, null));
    assertTrue(ex.getMessage().toLowerCase().contains("origin"));
    assertFalse(ex.getMessage().contains("evil.example.com"));
  }

  @Test
  void resolveRelativeSubPathUnderCheckout() throws Exception {
    List<List<String>> commands = new ArrayList<>();
    PSGitRemoteCheckout checkout =
        new PSGitRemoteCheckout(
            (cwd, command, output) -> {
              commands.add(List.copyOf(command));
              if (command.contains("clone")) {
                Path dest = Path.of(command.get(command.size() - 1));
                Files.createDirectories(dest.resolve(".git"));
                Files.createDirectories(dest.resolve("product-docs"));
              }
              return 0;
            },
            Duration.ofSeconds(5));
    PSSite site = siteWith(
        prop(PSVirtualSiteHelper.PROP_SOURCE_KIND, "git-filesystem"),
        prop(PSVirtualSiteHelper.PROP_REMOTE_URL, "https://git.example.com/org/repo.git"),
        prop(PSVirtualSiteHelper.PROP_ROOT_PATH, "product-docs"));

    Path discovered =
        checkout.ensureCurrent(
            "https://git.example.com/org/repo.git", "main", "docs", tempDir.resolve("sub"), site);
    assertTrue(discovered.endsWith(Path.of("product-docs")));
    assertEquals(1, commands.size());
  }

  @Test
  void neverLogsRawSecretInExceptionFromFailedClone() {
    PSGitRemoteCheckout checkout =
        new PSGitRemoteCheckout(
            (cwd, command, output) -> {
              output.append("fatal: could not read Username for 'https://user:hunter2@host/x.git'");
              return 128;
            },
            Duration.ofSeconds(5));
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                checkout.ensureCurrent(
                    "https://git.example.com/org/repo.git",
                    "main",
                    "docs",
                    tempDir.resolve("fail"),
                    null));
    assertFalse(ex.getMessage().contains("hunter2"));
    assertTrue(ex.getMessage().contains("***") || ex.getMessage().contains("failed"));
  }

  @Test
  @EnabledIf("gitAvailable")
  void clonesLocalFileRemoteAndReusesFilesystemDiscover() throws Exception {
    Path origin = createLocalGitRepo(tempDir.resolve("origin-repo"));
    String fileUrl = origin.toUri().toString();
    PSGitRemoteCheckout checkout = new PSGitRemoteCheckout();
    Path workBase = tempDir.resolve("live-checkouts");
    Path discovered = checkout.ensureCurrent(fileUrl, "main", "sample", workBase, null);

    assertTrue(Files.isRegularFile(discovered.resolve("_config.yaml")));
    assertTrue(Files.isRegularFile(discovered.resolve("8.2").resolve("index.md")));

    PSGitFilesystemVirtualSiteSource source = new PSGitFilesystemVirtualSiteSource();
    VirtualSiteConfig config = VirtualSiteConfigLoader.load(discovered, "_config.yaml", "sample");
    assertFalse(source.discover(config).isEmpty());
  }

  static boolean gitAvailable() {
    try {
      Process process = new ProcessBuilder("git", "--version").start();
      if (!process.waitFor(15, TimeUnit.SECONDS)) {
        process.destroyForcibly();
        return false;
      }
      return process.exitValue() == 0;
    } catch (Exception e) {
      return false;
    }
  }

  private static Path createLocalGitRepo(Path origin) throws Exception {
    Files.createDirectories(origin.resolve("_theme"));
    Files.createDirectories(origin.resolve("8.2"));
    Files.writeString(
        origin.resolve("_config.yaml"),
        """
        site:
          title: Remote Docs
        versions:
          - id: "8.2"
            label: "8.2"
            path: 8.2
            default: true
        theme:
          layout: page.html
        """,
        StandardCharsets.UTF_8);
    Files.writeString(
        origin.resolve("_theme").resolve("page.html"),
        "<html><body>{{content}}</body></html>",
        StandardCharsets.UTF_8);
    Files.writeString(
        origin.resolve("8.2").resolve("index.md"),
        """
        ---
        id: remote-home
        title: Home
        ---

        Remote hello.
        """,
        StandardCharsets.UTF_8);
    runGit(origin, "git", "init");
    runGit(origin, "git", "config", "user.email", "virtual-site-test@example.com");
    runGit(origin, "git", "config", "user.name", "Virtual Site Test");
    runGit(origin, "git", "add", ".");
    runGit(origin, "git", "commit", "-m", "init");
    runGit(origin, "git", "checkout", "-B", "main");
    return origin;
  }

  private static void runGit(Path cwd, String... command) throws Exception {
    Process process = new ProcessBuilder(command).directory(cwd.toFile()).start();
    if (!process.waitFor(30, TimeUnit.SECONDS)) {
      process.destroyForcibly();
      throw new IllegalStateException("git timed out: " + String.join(" ", command));
    }
    if (process.exitValue() != 0) {
      throw new IllegalStateException("git failed: " + String.join(" ", command));
    }
  }

  private static PSSiteProperty prop(String name, String value) {
    PSSiteProperty p = new PSSiteProperty();
    p.setName(name);
    p.setValue(value);
    return p;
  }

  private static PSSite siteWith(PSSiteProperty... properties) {
    PSSite site = Mockito.mock(PSSite.class);
    Set<PSSiteProperty> props = new HashSet<>();
    for (PSSiteProperty p : properties) {
      props.add(p);
    }
    Mockito.when(site.getProperties()).thenReturn(props);
    return site;
  }
}
