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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.extensions.general;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Regression for GH-829 / v8.1.7 PR #837: Directory Index touch workflow action is registered and
 * wired into Default Workflow Approve transitions + installer SQL.
 */
class PSDirectoryIndexTouchWorkflowActionPortTest {

  private static final String ACTION =
      "sys_DirectoryIndexTouchWorkflowAction";

  @Test
  void directoryIndexTouchActionIsRegisteredAndWired() throws Exception {
    Path root = resolveRoot();
    Path clazz =
        root.resolve(
            "modules/extensions-main/src/main/java/com/percussion/extensions/general/PSDirectoryIndexTouchWorkflowAction.java");
    Path ext =
        root.resolve("modules/extensions-main/src/main/resources/Java/Extensions.xml");
    Path wf =
        root.resolve(
            "modules/perc-packages/src/main/resources/Packages/perc.workflow/DefaultWorkflow(3).workflowDef");
    Path install =
        root.resolve(
            "modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/installRepository.xml");
    for (Path p : new Path[] {clazz, ext, wf, install}) {
      if (!Files.isRegularFile(p)) {
        fail("missing " + p);
      }
    }
    String java = Files.readString(clazz, StandardCharsets.UTF_8);
    assertTrue(java.contains("implements IPSWorkflowAction"));
    assertTrue(java.contains("percDirectory"));
    assertTrue(java.contains("touchContentTypeItems"));

    String extensions = Files.readString(ext, StandardCharsets.UTF_8);
    assertTrue(extensions.contains("name=\"" + ACTION + "\""));
    assertTrue(
        extensions.contains(
            "com.percussion.extensions.general.PSDirectoryIndexTouchWorkflowAction"));

    String workflow = Files.readString(wf, StandardCharsets.UTF_8);
    assertTrue(workflow.contains(ACTION));
    // at least the four Approve wires
    int count = workflow.split(ACTION, -1).length - 1;
    assertTrue(count >= 4, "expected >=4 Approve transition wires, got " + count);

    String installer = Files.readString(install, StandardCharsets.UTF_8);
    assertTrue(installer.contains(ACTION));
    assertTrue(installer.contains("TRANSITIONLABEL = 'Approve'"));
  }

  private static Path resolveRoot() {
    Path cwd = Path.of("").toAbsolutePath().normalize();
    Path up = cwd.resolve("../..").normalize();
    if (Files.isDirectory(up.resolve("modules/extensions-main"))) return up;
    if (Files.isDirectory(cwd.resolve("modules/extensions-main"))) return cwd;
    // surefire may run with module basedir
    Path up2 = cwd.resolve("../../..").normalize();
    if (Files.isDirectory(up2.resolve("modules/extensions-main"))) return up2;
    fail("could not resolve monorepo root from " + cwd);
    return cwd;
  }
}
