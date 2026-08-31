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
 * Regression for GH-2142 / #1694 / #1714 class: Developer catalog REST resources must be listed on
 * the {@code rest-jax-rs} {@code jaxrs:serviceBeans} block. Component-scan alone is not enough —
 * CXF returns 404 (or Spring bean-lookup 500) when the ref is missing.
 *
 * <p>Live H2 qa-up (2026-08-06): after adding the five refs below, GET
 * /Rhythmyx/services/{searches,views,cecontrols,serverconfigs,relationshiptypes} all returned 2xx.
 */
class CatalogRestJaxrsRegistrationTest {

  private static final String[] REQUIRED_REFS = {
    "restControlsResource",
    "restSearchResource",
    "restViewResource",
    "restServerConfigsResource",
    "restRelationshipTypeResource",
    // peers already registered by #1714 — keep locked so they cannot regress
    "restKeywordsResource",
    "restLocalesResource",
    // #3981 / #4028 CD-18 auto-translation set (literal path vs locales /{idOrLang})
    "restAutoTranslationsResource",
    "restSlotsResource",
    "restSharedFieldsResource",
    "restSystemDefResource",
    "restExtensionsResource",
    // #2429 P-Trans create-variant / item-locale façade
    "restContentTranslationsResource",
    // #3073 content-explorer folders façade over IPSContentWs
    "restContentExplorerFoldersResource",
  };

  /** Inbox execute #3323 — must precede jacksonProvider on rest-jax-rs. */
  private static final String VIEW_EXECUTE_JSON_READER = "viewExecuteRequestJsonReader";

  /** Explorer saved-search execute #3517 — must precede jacksonProvider on rest-jax-rs. */
  private static final String SEARCH_EXECUTE_JSON_READER = "searchExecuteRequestJsonReader";

  /** Display Format Object ACL save #3378 — must precede jacksonProvider. */
  private static final String ACL_LIST_JSON_READER = "aclListJsonReader";

  /** SE-01 community bulk save/delete #4077 — must precede jacksonProvider. */
  private static final String COMMUNITY_LIST_JSON_READER = "communityListJsonReader";

  private static final String GUID_LIST_JSON_READER = "guidListJsonReader";

  /** Explorer folder create #3360 — must precede jacksonProvider. */
  private static final String ADD_FOLDER_JSON_READER = "addFolderRequestJsonReader";

  /** Explorer find/types #3855 — must precede jacksonProvider. */
  private static final String FIND_TYPES_JSON_READER =
      "allowedContentTypeMenusRequestJsonReader";

  /** CD-18 auto-translation PUT wrap / bare array (#4028) — must precede jacksonProvider. */
  private static final String AUTO_TRANSLATION_ROWS_JSON_READER =
      "autoTranslationRowsJsonReader";

  @Test
  void restJaxRsServiceBeansIncludeDeveloperCatalogResources() throws Exception {
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
          "rest-jax-rs serviceBeans must ref " + bean + " (missing → CXF 404 for catalog REST)");
    }
  }

  @Test
  void restJaxRsProvidersIncludeViewExecuteRequestJsonReaderBeforeJackson() throws Exception {
    Path root = resolveRepoRoot();
    Path beans =
        root.resolve(
            "projects/sitemanage/src/main/resources/Rhythmyx/AppServer/server/rx/deploy/"
                + "rxapp.ear/rxapp.war/WEB-INF/config/spring/projects/sitemanage-beans.xml");
    assertTrue(Files.isRegularFile(beans), "missing sitemanage-beans.xml at " + beans);

    String xml = Files.readString(beans, StandardCharsets.UTF_8);
    int restServer = xml.indexOf("id=\"rest-jax-rs\"");
    assertTrue(restServer >= 0, "rest-jax-rs server must exist in sitemanage-beans.xml");
    int end = xml.indexOf("</jaxrs:server>", restServer);
    assertTrue(end > restServer, "rest-jax-rs server block must close");
    String restBlock = xml.substring(restServer, end);
    int providers = restBlock.indexOf("<jaxrs:providers>");
    int providersEnd = restBlock.indexOf("</jaxrs:providers>");
    assertTrue(providers >= 0 && providersEnd > providers, "rest-jax-rs providers block");
    String providerBlock = restBlock.substring(providers, providersEnd);
    int reader = providerBlock.indexOf("bean=\"" + VIEW_EXECUTE_JSON_READER + "\"");
    int searchReader = providerBlock.indexOf("bean=\"" + SEARCH_EXECUTE_JSON_READER + "\"");
    int aclReader = providerBlock.indexOf("bean=\"" + ACL_LIST_JSON_READER + "\"");
    int communityListReader =
        providerBlock.indexOf("bean=\"" + COMMUNITY_LIST_JSON_READER + "\"");
    int guidListReader = providerBlock.indexOf("bean=\"" + GUID_LIST_JSON_READER + "\"");
    int addFolderReader = providerBlock.indexOf("bean=\"" + ADD_FOLDER_JSON_READER + "\"");
    int findTypesReader = providerBlock.indexOf("bean=\"" + FIND_TYPES_JSON_READER + "\"");
    int autoTranslationReader =
        providerBlock.indexOf("bean=\"" + AUTO_TRANSLATION_ROWS_JSON_READER + "\"");
    int jackson = providerBlock.indexOf("bean=\"jacksonProvider\"");
    assertTrue(
        reader >= 0,
        "rest-jax-rs providers must ref "
            + VIEW_EXECUTE_JSON_READER
            + " (missing → Inbox flat startIndex 400)");
    assertTrue(
        searchReader >= 0,
        "rest-jax-rs providers must ref "
            + SEARCH_EXECUTE_JSON_READER
            + " (missing → saved-search execute envelope/startIndex 400)");
    assertTrue(
        aclReader >= 0,
        "rest-jax-rs providers must ref "
            + ACL_LIST_JSON_READER
            + " (missing → ACL bulk save ArrayList ClassCast 400)");
    assertTrue(
        communityListReader >= 0,
        "rest-jax-rs providers must ref "
            + COMMUNITY_LIST_JSON_READER
            + " (missing → community bulk save ArrayList ClassCast 400)");
    assertTrue(
        guidListReader >= 0,
        "rest-jax-rs providers must ref "
            + GUID_LIST_JSON_READER
            + " (missing → community bulk delete GuidList ClassCast 400)");
    assertTrue(
        addFolderReader >= 0,
        "rest-jax-rs providers must ref "
            + ADD_FOLDER_JSON_READER
            + " (missing → folder create unexpected element name / AddFolderRequest)");
    assertTrue(
        findTypesReader >= 0,
        "rest-jax-rs providers must ref "
            + FIND_TYPES_JSON_READER
            + " (missing → Explorer find/types flat contentIds / GUID 400)");
    assertTrue(
        autoTranslationReader >= 0,
        "rest-jax-rs providers must ref "
            + AUTO_TRANSLATION_ROWS_JSON_READER
            + " (missing → auto-translation PUT wrap/array empty or 400)");
    assertTrue(jackson >= 0, "rest-jax-rs providers must still ref jacksonProvider");
    assertTrue(
        reader < jackson,
        VIEW_EXECUTE_JSON_READER + " must be listed before jacksonProvider");
    assertTrue(
        searchReader < jackson,
        SEARCH_EXECUTE_JSON_READER + " must be listed before jacksonProvider");
    assertTrue(
        aclReader < jackson,
        ACL_LIST_JSON_READER + " must be listed before jacksonProvider");
    assertTrue(
        communityListReader < jackson,
        COMMUNITY_LIST_JSON_READER + " must be listed before jacksonProvider");
    assertTrue(
        guidListReader < jackson,
        GUID_LIST_JSON_READER + " must be listed before jacksonProvider");
    assertTrue(
        addFolderReader < jackson,
        ADD_FOLDER_JSON_READER + " must be listed before jacksonProvider");
    assertTrue(
        findTypesReader < jackson,
        FIND_TYPES_JSON_READER + " must be listed before jacksonProvider");
    assertTrue(
        autoTranslationReader < jackson,
        AUTO_TRANSLATION_ROWS_JSON_READER + " must be listed before jacksonProvider");
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
