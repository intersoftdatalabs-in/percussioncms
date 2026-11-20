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
    // #4096 UI-09 community CX new-search defaults (nested under /communities/{id}/…)
    "restCommunityNewSearchDefaultsResource",
    "restViewResource",
    // #4722 Explorer session list-column overlay
    "restExplorerListColumnsResource",
    "restServerConfigsResource",
    // #4289 / #4288 SY-05 application CMS/resource files (+ pipelines app picker)
    "restApplicationFilesResource",
    // #4325 File Explorer allow-listed browse
    "restFileExplorerResource",
    // #4343 Database Explorer allow-listed JDBC catalog browse
    "restDatabaseExplorerResource",
    // #4345 Problems panel design validation (Workbench §12.4)
    "restProblemsResource",
    "restRelationshipTypeResource",
    // Pipelines Slice A/B — CXF 404 without explicit rest-jax-rs ref (#4304/#4314/#4316)
    "restPipelinesResource",
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
    // #4644 EditorHost checkout / check-in
    "restEditorItemLockResource",
    // #6783 Explorer row visibility identity batch
    "restExplorerItemIdentityResource",
    // AS-09 / #4305 / #4307 Velocity tools + snippet catalog
    "restVelocityResource",
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

  /** Admin action-menu create POST #4123 — must precede jacksonProvider. */
  private static final String ACTION_MENU_JSON_READER = "actionMenuJsonReader";

  /** UI-04 children PUT #4204 — must precede jacksonProvider. */
  private static final String ACTION_MENU_LIST_JSON_READER = "actionMenuListJsonReader";

  /** CD-18 auto-translation PUT wrap / bare array (#4028) — must precede jacksonProvider. */
  private static final String AUTO_TRANSLATION_ROWS_JSON_READER =
      "autoTranslationRowsJsonReader";

  /** UI-05 display format communities PUT empty array (#4098) — must precede jacksonProvider. */
  private static final String DISPLAY_FORMAT_JSON_READER = "displayFormatJsonReader";

  /** #4465 template associatedContentTypes:[] clear vs omit — must precede jacksonProvider. */
  private static final String TEMPLATE_DETAIL_JSON_READER = "templateDetailJsonReader";

  /** #4470 relationship-type cloneOverrides:[] clear vs omit — must precede jacksonProvider. */
  private static final String RELATIONSHIP_TYPE_JSON_READER = "relationshipTypeJsonReader";

  @Test
  void restJaxRsServiceBeansIncludeDeveloperCatalogResources() throws Exception {
    Path root = resolveRepoRoot();
    Path beans = sitemanageBeansXml(root);
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
          indexOfBeanRef(restBlock, bean) >= 0,
          "rest-jax-rs serviceBeans must ref " + bean + " (missing → CXF 404 for catalog REST)");
    }
  }

  @Test
  void restJaxRsProvidersIncludeViewExecuteRequestJsonReaderBeforeJackson() throws Exception {
    Path root = resolveRepoRoot();
    Path beans = sitemanageBeansXml(root);
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
    int reader = indexOfBeanRef(providerBlock, VIEW_EXECUTE_JSON_READER);
    int searchReader = indexOfBeanRef(providerBlock, SEARCH_EXECUTE_JSON_READER);
    int aclReader = indexOfBeanRef(providerBlock, ACL_LIST_JSON_READER);
    int communityListReader = indexOfBeanRef(providerBlock, COMMUNITY_LIST_JSON_READER);
    int guidListReader = indexOfBeanRef(providerBlock, GUID_LIST_JSON_READER);
    int addFolderReader = indexOfBeanRef(providerBlock, ADD_FOLDER_JSON_READER);
    int findTypesReader = indexOfBeanRef(providerBlock, FIND_TYPES_JSON_READER);
    int actionMenuReader = indexOfBeanRef(providerBlock, ACTION_MENU_JSON_READER);
    int actionMenuListReader = indexOfBeanRef(providerBlock, ACTION_MENU_LIST_JSON_READER);
    int autoTranslationReader = indexOfBeanRef(providerBlock, AUTO_TRANSLATION_ROWS_JSON_READER);
    int displayFormatReader = indexOfBeanRef(providerBlock, DISPLAY_FORMAT_JSON_READER);
    int templateDetailReader = indexOfBeanRef(providerBlock, TEMPLATE_DETAIL_JSON_READER);
    int relationshipTypeReader = indexOfBeanRef(providerBlock, RELATIONSHIP_TYPE_JSON_READER);
    int jackson = indexOfBeanRef(providerBlock, "jacksonProvider");
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
        actionMenuReader >= 0,
        "rest-jax-rs providers must ref "
            + ACTION_MENU_JSON_READER
            + " (missing → Admin POST /actions JAXB allowedWorkflowTransitionsRequest)");
    assertTrue(
        actionMenuListReader >= 0,
        "rest-jax-rs providers must ref "
            + ACTION_MENU_LIST_JSON_READER
            + " (missing → Admin PUT /actions/{id}/children ArrayList / bare array 400)");
    assertTrue(
        autoTranslationReader >= 0,
        "rest-jax-rs providers must ref "
            + AUTO_TRANSLATION_ROWS_JSON_READER
            + " (missing → auto-translation PUT wrap/array empty or 400)");
    assertTrue(
        displayFormatReader >= 0,
        "rest-jax-rs providers must ref "
            + DISPLAY_FORMAT_JSON_READER
            + " (missing → display format PUT allowedCommunities:[] treated as omit)");
    assertTrue(
        templateDetailReader >= 0,
        "rest-jax-rs providers must ref "
            + TEMPLATE_DETAIL_JSON_READER
            + " (missing → template PUT associatedContentTypes:[] treated as omit)");
    assertTrue(
        relationshipTypeReader >= 0,
        "rest-jax-rs providers must ref "
            + RELATIONSHIP_TYPE_JSON_READER
            + " (missing → relationship-type PUT cloneOverrides:[] treated as omit)");
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
        actionMenuReader < jackson,
        ACTION_MENU_JSON_READER + " must be listed before jacksonProvider");
    assertTrue(
        actionMenuListReader < jackson,
        ACTION_MENU_LIST_JSON_READER + " must be listed before jacksonProvider");
    assertTrue(
        autoTranslationReader < jackson,
        AUTO_TRANSLATION_ROWS_JSON_READER + " must be listed before jacksonProvider");
    assertTrue(
        displayFormatReader < jackson,
        DISPLAY_FORMAT_JSON_READER + " must be listed before jacksonProvider");
    assertTrue(
        templateDetailReader < jackson,
        TEMPLATE_DETAIL_JSON_READER + " must be listed before jacksonProvider");
    assertTrue(
        relationshipTypeReader < jackson,
        RELATIONSHIP_TYPE_JSON_READER + " must be listed before jacksonProvider");
    assertTrue(
        restBlock.contains("skip.default.json.provider.registration\" value=\"true\""),
        "rest-jax-rs must skip Jettison JSONProvider with value=\"true\" (POST /actions JAXB "
            + "allowedWorkflowTransitionsRequest)");
  }

  /**
   * Portable join for the sitemanage Spring beans file under the monorepo root. Multi-arg
   * {@link Path#of} so no separator is baked into a literal (resolves the same file on Windows
   * and Unix) — behaviour is identical to the previous single-literal form on Linux.
   */
  private static Path sitemanageBeansXml(Path root) {
    return root.resolve(
        Path.of(
            "projects",
            "sitemanage",
            "src",
            "main",
            "resources",
            "Rhythmyx",
            "AppServer",
            "server",
            "rx",
            "deploy",
            "rxapp.ear",
            "rxapp.war",
            "WEB-INF",
            "config",
            "spring",
            "projects",
            "sitemanage-beans.xml"));
  }

  /**
   * Index of a {@code bean="..."} ref inside a CXF config block, or -1 when absent. Centralises
   * the XML attribute matcher so the quote-escape backslash lives on one line: it escapes the
   * Java string quote and is not a filesystem separator (same class as the deployer
   * XML-attribute precedent, adjudicated in issue #6831).
   */
  private static int indexOfBeanRef(String block, String bean) {
    // mkd-code-review[paths.hardcoded_sep]: XML content matcher, not a filesystem path —
    // the backslash escapes the Java string quote (issue #6831).
    return block.indexOf("bean=\"" + bean + "\"");
  }

  private static Path resolveRepoRoot() {
    Path cwd = Path.of("").toAbsolutePath().normalize();
    // Standalone: cd projects/sitemanage && ../../mvnw clean install
    // (chained parent resolves, so no separator is hardcoded)
    Path fromModule = cwd.resolve("..").resolve("..").normalize();
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
