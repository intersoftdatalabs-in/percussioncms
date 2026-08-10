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

package com.percussion.packages;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Pins the stock Directory widget membership model used to classify GH-804 / #2334.
 *
 * <p>Faculty Directory does <strong>not</strong> list people via AA children of the {@code
 * percDirectory} asset. Assembly runs a JSR-170 query over {@code percPerson} filtered by
 * organization/department fields and a hard-coded {@code sys_contentstateid != 7} clause. These
 * assertions guard that model so a future package edit cannot silently reintroduce a different
 * membership mechanism without updating the stale-publish classification.
 *
 * @see docs/ai-generated/tasks/804-faculty-directory-stale-publish/01-classification.md
 */
class PSDirectoryWidgetAutoQueryModelTest {

  private static final String DIRECTORY_WIDGET_RESOURCE =
      "/Packages/perc.widget.directory/sys__UserDependency--rxconfig/Widgets/percDirectory.xml";

  private static final Pattern PERSON_QUERY =
      Pattern.compile(
          "select\\s+rx:sys_contentid,\\s*rx:sys_folderid\\s+from\\s+rx:percPerson\\s+where\\s+[^\\\"]+",
          Pattern.CASE_INSENSITIVE);

  @Test
  void percDirectory_listsPeopleViaJcrAutoQueryNotAaChildren() throws IOException {
    String xml = readClasspathResource(DIRECTORY_WIDGET_RESOURCE);

    assertTrue(
        xml.contains("organizationSearch"),
        "Directory asset must drive membership via organizationSearch field");
    assertTrue(
        xml.contains("departmentID"),
        "Directory asset must drive membership via departmentID field");
    assertTrue(
        xml.contains("perc_AutoWidgetContentFinder"),
        "Person list must use perc_AutoWidgetContentFinder (JCR auto query)");
    assertTrue(
        xml.contains("rx:percPerson"), "Person list query must target rx:percPerson content type");
    assertTrue(
        xml.contains("rx:personOrganization"),
        "Person list must filter on personOrganization (not AA relationship to directory)");
    assertTrue(
        xml.contains("rx:sys_contentstateid != 7"),
        "Person list must retain hard-coded Archive state exclusion (!= 7) until Slice 3 changes"
            + " it");

    // Guard against accidental AA-style "children of directory asset" membership.
    assertFalse(
        xml.contains("sys_relationship") && xml.contains("percPerson") && xml.contains("owner"),
        "Directory person list must not switch to relationship-owner AA membership without doc"
            + " update");
  }

  @Test
  void percDirectory_personQueriesAllExcludeState7AndOrderByLastName() throws IOException {
    String xml = readClasspathResource(DIRECTORY_WIDGET_RESOURCE);
    Matcher m = PERSON_QUERY.matcher(xml);
    int found = 0;
    while (m.find()) {
      found++;
      String q = m.group();
      assertTrue(
          q.contains("rx:sys_contentstateid != 7"),
          "Each percPerson directory query must exclude state 7: " + q);
      assertTrue(
          q.toLowerCase().contains("order by rx:personlastname"),
          "Each percPerson directory query must order by last name: " + q);
      assertTrue(
          q.contains("rx:personOrganization"),
          "Each percPerson directory query must constrain organization: " + q);
    }
    assertTrue(
        found >= 4,
        "Expected at least 4 stock percPerson directory query variants (org/dept combinations), got"
            + " "
            + found);
  }

  private static String readClasspathResource(String resourcePath) throws IOException {
    try (InputStream in =
        PSDirectoryWidgetAutoQueryModelTest.class.getResourceAsStream(resourcePath)) {
      assertNotNull(in, "Classpath resource missing: " + resourcePath);
      String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      assertFalse(content.isBlank(), "Classpath resource empty: " + resourcePath);
      return content;
    }
  }
}
