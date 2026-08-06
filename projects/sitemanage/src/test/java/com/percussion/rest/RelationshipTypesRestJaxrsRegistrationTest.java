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
package com.percussion.rest;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Regression for GH-2152 / #1694 C7 / #1714 class: {@code restRelationshipTypeResource} must be
 * listed on the {@code rest-jax-rs} {@code jaxrs:serviceBeans} block. Component-scan alone is not
 * enough — CXF returns 404 when the ref is missing.
 *
 * <p>Live H2 qa-up (2026-08-06): after adding the ref, GET /Rhythmyx/services/relationshiptypes
 * returned HTTP 200 with a Jackson {@code RelationshipType} array (design-WS findRelationshipTypes).
 */
class RelationshipTypesRestJaxrsRegistrationTest {

  /** Peers already registered (#1714) stay locked so they cannot regress silently. */
  private static final String[] REQUIRED_REFS = {
    "restRelationshipTypeResource",
    "restKeywordsResource",
    "restLocalesResource",
    "restSlotsResource",
    "restSharedFieldsResource",
    "restSystemDefResource",
    "restExtensionsResource",
  };

  @Test
  void restJaxRsServiceBeansIncludeRelationshipTypeResource() throws Exception {
    Path root = resolveRepoRoot();
    Path beans =
        root.resolve(
            "projects/sitemanage/src/main/resources/Rhythmyx/AppServer/server/rx/deploy/"
                + "rxapp.ear/rxapp.war/WEB-INF/config/spring/projects/sitemanage-beans.xml");
    assertTrue(Files.isRegularFile(beans), "missing sitemanage-beans.xml at " + beans);

    String xml = Files.readString(beans, StandardCharsets.UTF_8);
    // Bound the rest-jax-rs server block so we do not match unrelated jaxrs:server entries.
    int restServer = xml.indexOf("id=\"rest-jax-rs\"");
    assertTrue(restServer >= 0, "rest-jax-rs server must exist in sitemanage-beans.xml");
    int end = xml.indexOf("</jaxrs:server>", restServer);
    assertTrue(end > restServer, "rest-jax-rs server block must close");
    String restBlock = xml.substring(restServer, end);

    for (String bean : REQUIRED_REFS) {
      assertTrue(
          restBlock.contains("bean=\"" + bean + "\""),
          "rest-jax-rs serviceBeans must ref "
              + bean
              + " (missing → CXF 404 for catalog REST)");
    }
  }

  private static Path resolveRepoRoot() {
    Path cwd = Path.of("").toAbsolutePath().normalize();
    // Standalone: cd projects/sitemanage && ../../mvnw clean install
    Path fromModule = cwd.resolve("../..").normalize();
    if (Files.isDirectory(fromModule.resolve("system"))
        && Files.isDirectory(fromModule.resolve("projects/sitemanage"))) {
      return fromModule;
    }
    if (Files.isDirectory(cwd.resolve("system"))
        && Files.isDirectory(cwd.resolve("projects/sitemanage"))) {
      return cwd;
    }
    fail("could not resolve monorepo root from " + cwd);
    return cwd;
  }
}
