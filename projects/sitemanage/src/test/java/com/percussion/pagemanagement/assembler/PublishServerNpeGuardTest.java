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
package com.percussion.pagemanagement.assembler;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Regression for GH-847 / v8.1.7 PR #851: publishing newly created sites must resolve DTS via
 * getPublishServer() (Optional-safe) rather than raw publishServer property that can NPE.
 */
class PublishServerNpeGuardTest {

  @Test
  void deliveryPathsUseGetPublishServer() throws Exception {
    Path root = resolveRepoRoot();
    Path pageUtils =
        root.resolve(
            "projects/sitemanage/src/main/java/com/percussion/pagemanagement/assembler/PSPageUtils.java");
    Path metadata =
        root.resolve(
            "system/business/src/com/percussion/rx/delivery/impl/PSMetadataDeliveryHandler.java");
    Path pubServer =
        root.resolve(
            "system/services/src/com/percussion/services/pubserver/data/PSPubServer.java");

    String utils = Files.readString(pageUtils, StandardCharsets.UTF_8);
    // getDeliveryServer body should call getPublishServer, not raw property for publishServer
    assertTrue(utils.contains("getPublishServer()"));
    assertFalse(
        utils.contains(
            "getProperty(\"publishServer\").map(PSPubServerProperty::getValue)"),
        "getDeliveryServer must not use raw publishServer property map");

    String meta = Files.readString(metadata, StandardCharsets.UTF_8);
    assertTrue(meta.contains("getPublishServer()"));
    assertFalse(
        meta.contains("getProperty(\"publishServer\").map(PSPubServerProperty::getValue)"));

    String pub = Files.readString(pubServer, StandardCharsets.UTF_8);
    assertTrue(pub.contains("No DTS server is currently configured"));
    assertTrue(pub.contains("is not valid or not found in delivery-servers.xml"));
  }

  private static Path resolveRepoRoot() {
    Path cwd = Path.of("").toAbsolutePath().normalize();
    Path candidate = cwd.resolve("../..").normalize();
    if (Files.isDirectory(candidate.resolve("system"))) {
      return candidate;
    }
    if (Files.isDirectory(cwd.resolve("system"))) {
      return cwd;
    }
    fail("could not resolve monorepo root");
    return cwd;
  }
}
