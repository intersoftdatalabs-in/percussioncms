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
package com.percussion.workflow.mail;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Workflow mail plugins share perc-system's Jakarta Mail 2 classpath; {@code javax.mail} no longer
 * compiles.
 */
public class PSJavaxMailProgramJakartaNamespaceTest {

  @Test
  public void jakartaMailTypesAreOnClasspath() throws ClassNotFoundException {
    assertNotNull(Class.forName("jakarta.mail.internet.MimeMessage"));
    assertNotNull(Class.forName("org.apache.commons.mail2.jakarta.MultiPartEmail"));
  }

  @Test
  public void javaxMailProgramSourceUsesJakartaMail() throws Exception {
    Path src =
        Path.of(
            "src",
            "main",
            "java",
            "com",
            "percussion",
            "workflow",
            "mail",
            "PSJavaxMailProgram.java");
    Path fromModule = Path.of("").toAbsolutePath().normalize().resolve(src);
    Path resolved =
        Files.isRegularFile(fromModule)
            ? fromModule
            : Path.of("")
                .toAbsolutePath()
                .normalize()
                .resolve("modules")
                .resolve("extensions-workflow")
                .resolve(src);
    assertTrue(Files.isRegularFile(resolved), () -> "missing " + resolved);
    String text = Files.readString(resolved, StandardCharsets.UTF_8).replace("\r\n", "\n");
    assertFalse(text.contains("import javax.mail."));
    assertTrue(text.contains("import jakarta.mail.Session;"));
  }
}
