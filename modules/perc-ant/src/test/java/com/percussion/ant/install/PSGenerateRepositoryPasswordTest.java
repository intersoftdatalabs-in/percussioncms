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
package com.percussion.ant.install;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.install.PSGeneratedPasswords;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests the {@code <PSGenerateRepositoryPassword>} ANT task used by silent / non-interactive
 * installs to materialise a CMS DB password (issue #548 / #1500 matrix smoke). The task must:
 *
 * <ul>
 *   <li>Generate a random password by default and store it under {@code
 *       var/config/generated/passwords} with key {@code cmdb}.
 *   <li>Store an operator-supplied value verbatim when {@code random="false"}.
 *   <li>Expose the persisted password via the {@code cmdb.password} ANT property so downstream
 *       targets can reference it.
 *   <li>Preserve unrelated keys already on disk.
 * </ul>
 */
@Tag("UnitTest")
public class PSGenerateRepositoryPasswordTest {

  @TempDir Path installRoot;

  @Test
  void randomModeGeneratesAndPersistsPassword() throws Exception {
    Project project = new Project();
    PSGenerateRepositoryPassword task = new PSGenerateRepositoryPassword();
    task.setProject(project);
    task.setRootDir(installRoot.toString());

    task.execute();

    String property = project.getProperty(PSGenerateRepositoryPassword.CMDB_PASSWORD_PROPERTY);
    assertNotNull(property, "task must publish cmdb.password ANT property");
    assertFalse(property.isEmpty());
    Path expected = PSGeneratedPasswords.passwordsFile(installRoot);
    assertTrue(Files.isRegularFile(expected));
    Properties reloaded = load(installRoot);
    assertEquals(property, reloaded.getProperty(PSGeneratedPasswords.KEY_CMDB));
  }

  @Test
  void explicitValueModeStoresOperatorSuppliedPassword() throws Exception {
    Project project = new Project();
    PSGenerateRepositoryPassword task = new PSGenerateRepositoryPassword();
    task.setProject(project);
    task.setRootDir(installRoot.toString());
    task.setRandom(false);
    task.setValue("operator-picked-secret");

    task.execute();

    assertEquals(
        "operator-picked-secret",
        project.getProperty(PSGenerateRepositoryPassword.CMDB_PASSWORD_PROPERTY));
    Properties reloaded = load(installRoot);
    assertEquals("operator-picked-secret", reloaded.getProperty(PSGeneratedPasswords.KEY_CMDB));
  }

  @Test
  void explicitValueModeRequiresValue() throws Exception {
    Project project = new Project();
    PSGenerateRepositoryPassword task = new PSGenerateRepositoryPassword();
    task.setProject(project);
    task.setRootDir(installRoot.toString());
    task.setRandom(false);
    assertThrows(BuildException.class, task::execute);
  }

  @Test
  void rootDirIsRequired() throws Exception {
    Project project = new Project();
    PSGenerateRepositoryPassword task = new PSGenerateRepositoryPassword();
    task.setProject(project);
    assertThrows(BuildException.class, task::execute);
  }

  @Test
  void existingPasswordsArePreserved() throws Exception {
    // Seed an Admin password as PSUserService would.
    Path target = PSGeneratedPasswords.passwordsFile(installRoot);
    Files.createDirectories(target.getParent());
    Properties seed = new Properties();
    seed.setProperty("Admin", "demo-admin");
    try (var out = Files.newOutputStream(target)) {
      seed.store(out, "seed");
    }

    Project project = new Project();
    PSGenerateRepositoryPassword task = new PSGenerateRepositoryPassword();
    task.setProject(project);
    task.setRootDir(installRoot.toString());

    task.execute();

    Properties reloaded = load(installRoot);
    assertEquals("demo-admin", reloaded.getProperty("Admin"));
    assertNotNull(reloaded.getProperty(PSGeneratedPasswords.KEY_CMDB));
    assertNull(
        reloaded.getProperty(PSGenerateRepositoryPassword.CMDB_PASSWORD_PROPERTY),
        "cmdb.password must not leak into the file");
  }

  @Test
  void randomModeProducesNonEmptyUniqueValuesAcrossCalls() throws Exception {
    Project project1 = new Project();
    PSGenerateRepositoryPassword task1 = new PSGenerateRepositoryPassword();
    task1.setProject(project1);
    task1.setRootDir(installRoot.toString());
    task1.execute();
    String first = project1.getProperty(PSGenerateRepositoryPassword.CMDB_PASSWORD_PROPERTY);

    Path otherRoot = installRoot.resolve("second-install");
    Files.createDirectories(otherRoot);
    Project project2 = new Project();
    PSGenerateRepositoryPassword task2 = new PSGenerateRepositoryPassword();
    task2.setProject(project2);
    task2.setRootDir(otherRoot.toString());
    task2.execute();
    String second = project2.getProperty(PSGenerateRepositoryPassword.CMDB_PASSWORD_PROPERTY);

    assertNotNull(first);
    assertNotNull(second);
    org.junit.jupiter.api.Assertions.assertNotEquals(first, second);
  }

  private static Properties load(Path installRoot) throws Exception {
    Path file = PSGeneratedPasswords.passwordsFile(installRoot);
    Properties p = new Properties();
    try (InputStream in = Files.newInputStream(file)) {
      p.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
    }
    return p;
  }
}
