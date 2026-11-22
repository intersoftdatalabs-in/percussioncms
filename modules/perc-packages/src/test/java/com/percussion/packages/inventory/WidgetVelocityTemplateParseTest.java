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
package com.percussion.packages.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Parses every packaged widget/page Velocity template ({@code *.vm} under
 * {@code Packages/}) with the same Velocity engine version and parser
 * settings the runtime uses.
 *
 * <p>Nothing else in the build loads these templates, so a structural
 * template bug — for example the duplicated {@code #foreach} in
 * {@code percCalendarTwoSnippet.vm} proposed by PR #7061 (29 block openers
 * against 28 {@code #end}) — would otherwise pass every check (issue
 * #7080). Re-adding a stray block opener to any widget {@code .vm} must fail
 * {@code mvn -pl modules/perc-packages test} with the file named.
 *
 * <p>Parse only: no render context is needed. The authoritative gate is the
 * engine parser ({@link #everyPackagedTemplateParses}); the cheap
 * directive-balance lint ({@link #blockDirectivesBalance}) names the file and
 * the counts for the unclosed-directive mistake class.
 */
class WidgetVelocityTemplateParseTest {

  /** Block directives that consume exactly one {@code #end}. */
  private static final Pattern BLOCK_OPENER =
      Pattern.compile("#(foreach|if|macro|define)\\b");

  private static final Pattern BLOCK_CLOSER = Pattern.compile("#end\\b");

  private static final Pattern BLOCK_COMMENT = Pattern.compile("#\\*.*?\\*#", Pattern.DOTALL);

  /** Regression file for the #7061 duplicated-{@code #foreach} defect. */
  private static final String CALENDAR_TWO_SNIPPET =
      "perc.widget.calendar/widgets/percCalendarTwo/templates/percCalendarTwoSnippet.vm";

  private static Path packagesRoot;
  private static VelocityEngine engine;

  @BeforeAll
  static void initEngine() throws IOException {
    packagesRoot = packagesRoot();
    engine = new VelocityEngine();
    Properties runtimeProps = runtimeVelocityProperties();
    if (!runtimeProps.isEmpty()) {
      for (String name : runtimeProps.stringPropertyNames()) {
        engine.setProperty(name, runtimeProps.getProperty(name));
      }
    }
    engine.setProperty(RuntimeConstants.RESOURCE_LOADERS, "file");
    engine.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, packagesRoot.toString());
    engine.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_CACHE, "false");
    engine.init();
  }

  @Test
  void everyPackagedTemplateParses() throws IOException {
    List<Path> templates = packagedTemplates();
    assertTrue(!templates.isEmpty(), "no *.vm templates found under " + packagesRoot);
    List<String> failures = new ArrayList<>();
    for (Path template : templates) {
      String rel = rel(template);
      try {
        engine.getTemplate(rel);
      } catch (Exception e) {
        failures.add(rel + " -> " + e.getClass().getSimpleName() + ": " + firstLine(e.getMessage()));
      }
    }
    assertTrue(
        failures.isEmpty(),
        () ->
            failures.size()
                + " packaged Velocity template(s) failed to parse:\n  "
                + String.join("\n  ", failures));
  }

  @Test
  void blockDirectivesBalance() throws IOException {
    List<String> failures = new ArrayList<>();
    for (Path template : packagedTemplates()) {
      int openers = 0;
      int closers = 0;
      for (String codeLine : codeLines(template)) {
        openers += count(BLOCK_OPENER, codeLine);
        closers += count(BLOCK_CLOSER, codeLine);
      }
      if (openers != closers) {
        failures.add(
            rel(template) + " has " + openers + " block opener(s) vs " + closers + " #end");
      }
    }
    assertTrue(
        failures.isEmpty(),
        () ->
            "unbalanced block directives (stray #foreach/#if/#macro/#define or #end):\n  "
                + String.join("\n  ", failures));
  }

  @Test
  void calendarTwoSnippetRegressionPin() throws IOException {
    Path snippet = packagesRoot.resolve(CALENDAR_TWO_SNIPPET);
    assertTrue(Files.isRegularFile(snippet), "regression file missing: " + CALENDAR_TWO_SNIPPET);
    // Authoritative: the engine parser rejects the #7061 shape (unclosed #foreach).
    try {
      engine.getTemplate(CALENDAR_TWO_SNIPPET);
    } catch (Exception e) {
      fail(
          CALENDAR_TWO_SNIPPET
              + " failed to parse (issue #7080 pin for the #7061 defect): "
              + firstLine(e.getMessage()));
    }
    // Cheap pin: block openers must equal #end closers in this file.
    int openers = 0;
    int closers = 0;
    for (String codeLine : codeLines(snippet)) {
      openers += count(BLOCK_OPENER, codeLine);
      closers += count(BLOCK_CLOSER, codeLine);
    }
    assertEquals(
        closers,
        openers,
        CALENDAR_TWO_SNIPPET + " block directives drifted (issue #7080 pin for the #7061 defect)");
  }

  private static List<Path> packagedTemplates() throws IOException {
    try (Stream<Path> walk = Files.walk(packagesRoot)) {
      return walk
          .filter(p -> p.getFileName().toString().endsWith(".vm"))
          .filter(Files::isRegularFile)
          .sorted(Comparator.comparing(Path::toString))
          .toList();
    }
  }

  private static String rel(Path template) {
    return packagesRoot.relativize(template).toString().replace('\\', '/');
  }

  /**
   * Template source lines with Velocity comments removed: {@code #* ... *#}
   * blocks first, then {@code ##} line comments.
   */
  private static List<String> codeLines(Path template) throws IOException {
    String text = Files.readString(template, StandardCharsets.UTF_8);
    String noBlockComments = BLOCK_COMMENT.matcher(text).replaceAll("");
    List<String> code = new ArrayList<>();
    for (String line : noBlockComments.split("\\R")) {
      int comment = line.indexOf("##");
      code.add(comment < 0 ? line : line.substring(0, comment));
    }
    return code;
  }

  private static int count(Pattern pattern, String line) {
    Matcher matcher = pattern.matcher(line);
    int found = 0;
    while (matcher.find()) {
      found++;
    }
    return found;
  }

  private static String firstLine(String message) {
    if (message == null) {
      return "<no message>";
    }
    int end = message.indexOf('\n');
    return end < 0 ? message : message.substring(0, end);
  }

  private static Path packagesRoot() {
    List<Path> candidates =
        List.of(
            Path.of(System.getProperty("user.dir"), "src", "main", "resources", "Packages"),
            Path.of(
                System.getProperty("user.dir"),
                "modules",
                "perc-packages",
                "src",
                "main",
                "resources",
                "Packages"));
    for (Path candidate : candidates) {
      if (Files.isDirectory(candidate)) {
        return candidate;
      }
    }
    fail("Packages payload root not found under user.dir=" + System.getProperty("user.dir"));
    throw new IllegalStateException("unreachable");
  }

  /**
   * Parser-relevant settings the runtime ships
   * ({@code system/Tools/velocity/config/velocity.properties}), resolved by
   * walking up from the module checkout so the test parses exactly like the
   * runtime. Empty when the file is absent (engine defaults apply).
   */
  private static Properties runtimeVelocityProperties() {
    Path dir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
    while (dir != null) {
      Path props = dir.resolve(Path.of("system", "Tools", "velocity", "config", "velocity.properties"));
      if (Files.isRegularFile(props)) {
        Properties loaded = new Properties();
        try (InputStream in = Files.newInputStream(props)) {
          loaded.load(in);
        } catch (IOException e) {
          fail("cannot read runtime velocity.properties at " + props + ": " + e.getMessage());
        }
        return loaded;
      }
      dir = dir.getParent();
    }
    return new Properties();
  }
}
