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
package com.percussion.hooks.webservices;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * GH-4411 bumped {@code com.sun.mail:jakarta.mail} to 2.x ({@code jakarta.mail.*}). Unused {@code
 * javax.mail} imports in {@link PSWebServices} broke perc-system compile.
 */
public class PSWebServicesJakartaMailNamespaceTest {

  @Test
  public void jakartaMailImplIsOnCompileClasspath() throws ClassNotFoundException {
    assertNotNull(Class.forName("jakarta.mail.internet.MimeBodyPart"));
  }

  @Test
  public void soapServletSourceDoesNotImportJavaxMail() throws Exception {
    Path src = locateWebServicesSource();
    assertTrue(Files.isRegularFile(src), () -> "missing " + src.toAbsolutePath());
    String text = Files.readString(src, StandardCharsets.UTF_8).replace("\r\n", "\n");
    assertFalse(
        text.contains("import javax.mail."),
        "PSWebServices must not import javax.mail after jakarta.mail 2.x");
  }

  private static Path locateWebServicesSource() {
    Path rel =
        Path.of(
            "webservices",
            "src",
            "com",
            "percussion",
            "webservices",
            "servlets",
            "hooks",
            "PSWebServices.java");
    Path fromModule = Path.of("").toAbsolutePath().normalize().resolve(rel);
    if (Files.isRegularFile(fromModule)) {
      return fromModule;
    }
    Path fromRepo = Path.of("").toAbsolutePath().normalize().resolve("system").resolve(rel);
    if (Files.isRegularFile(fromRepo)) {
      return fromRepo;
    }
    throw new AssertionError(
        "PSWebServices.java not found from cwd " + Path.of("").toAbsolutePath());
  }
}
