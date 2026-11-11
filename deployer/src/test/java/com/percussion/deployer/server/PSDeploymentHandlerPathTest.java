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

package com.percussion.deployer.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Portable-path coverage for {@link PSDeploymentHandler#resolveRxPath(File, String...)} (issue
 * #6038, {@code paths.hardcoded_sep}). Every assertion is separator-convention agnostic: expected
 * values are built with {@link Path#resolve} and compared after normalizing the platform
 * separator, so the tests hold on Windows, Linux, and macOS.
 */
@DisplayName("PSDeploymentHandler.resolveRxPath portable joins (#6038)")
class PSDeploymentHandlerPathTest {

  @TempDir Path tmp;

  /** Normalizes a path string so Unix and Windows forms compare equal. */
  private static String normalized(String path) {
    return path.replace('\\', '/');
  }

  private Path expected(String... names) {
    Path path = tmp;
    for (String name : names) {
      path = path.resolve(name);
    }
    return path;
  }

  @Test
  @DisplayName("forward-slash fragments resolve to the expected segments")
  void forwardSlashFragmentsResolveToExpectedSegments() {
    Path resolved =
        PSDeploymentHandler.resolveRxPath(tmp.toFile(), "sys_Packager/server", "objectstore");
    assertEquals(expected("sys_Packager", "server", "objectstore"), resolved);
  }

  @Test
  @DisplayName("backslash fragments resolve identically on every platform")
  void backslashFragmentsResolveIdentically() {
    Path resolved =
        PSDeploymentHandler.resolveRxPath(tmp.toFile(), "sys_Packager\\server", "objectstore");
    assertEquals(expected("sys_Packager", "server", "objectstore"), resolved);
  }

  @Test
  @DisplayName("mixed separators and empty fragments are skipped")
  void mixedSeparatorsAndEmptyFragmentsAreSkipped() {
    Path resolved =
        PSDeploymentHandler.resolveRxPath(
            tmp.toFile(), "sys_Packager/server\\", "", "objectstore");
    assertEquals(expected("sys_Packager", "server", "objectstore"), resolved);
  }

  @Test
  @DisplayName("OBJECTSTORE_DIR fragment resolves to its logical segments")
  void objectstoreDirFragmentResolvesToLogicalSegments() {
    Path viaConstant =
        PSDeploymentHandler.resolveRxPath(tmp.toFile(), PSDeploymentHandler.OBJECTSTORE_DIR);
    assertEquals(expected("sys_Packager", "server", "objectstore"), viaConstant);
    assertEquals(
        "sys_Packager/server/objectstore", normalized(tmp.relativize(viaConstant).toString()));
  }

  @Test
  @DisplayName("resolved paths contain only the platform separator")
  void resolvedPathsContainOnlyPlatformSeparator() {
    String resolved =
        PSDeploymentHandler.resolveRxPath(
                tmp.toFile(), "sys_Packager/server", "objectstore", "DbmsMap")
            .toString();
    char foreign = File.separatorChar == '/' ? '\\' : '/';
    assertTrue(
        resolved.indexOf(foreign) < 0,
        () -> "resolved path mixes separators: " + resolved);
    assertEquals(
        normalized(expected("sys_Packager", "server", "objectstore", "DbmsMap").toString()),
        normalized(resolved));
  }
}
