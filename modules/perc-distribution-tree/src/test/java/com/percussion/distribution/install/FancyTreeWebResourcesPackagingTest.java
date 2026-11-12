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
package com.percussion.distribution.install;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Dist ANT publish-time copies provision FancyTree to published
 * {@code web_resources/cm} (issue #6052).
 *
 * <p>The published category-list widget ({@code category.min.js}) and the delivery bundle view
 * ({@code PercCategoryListView.js}) invoke {@code .fancytree(}, which the DTS bundle does not
 * ship. The retired DynaTree publish-time copies were deleted with the vendored copy (issue
 * #5662) and nothing replaced them, so every collapsible category page 404s its tree plugin.
 * These copies restore provisioning from the npm-backed WebUI tree (Dependabot-tracked
 * {@code jquery.fancytree} in {@code WebUI/src/main/frontend/package.json}), mirroring the
 * jquery/jquery-ui/jquery.cookie precedent. Guards the copy entries, the retired-DynaTree
 * absence, and the lockstep with the widget DTS resources + the WebUI builder mapping.
 */
@Tag("UnitTest")
class FancyTreeWebResourcesPackagingTest {

  private static final Path DIST_FILES =
      Path.of("src/main/resources/installDistributionFiles.xml");

  private static final Path CATEGORY_LIST_RESOURCES =
      Path.of(
          "..",
          "perc-packages",
          "src",
          "main",
          "resources",
          "Packages",
          "perc.widget.categoryList",
          "sys__UserDependency--rxconfig",
          "Resources",
          "percCategoryList.xml");

  private static final Path LEGACY_BUNDLE_BUILDER =
      Path.of("..", "..", "WebUI", "scripts", "build-legacy-bundles.cjs");

  private static final String WAR_TREE_JS =
      "cm/jslib/profiles/3x/jquery/plugins/jquery-fancytree/jquery.fancytree-all.min.js";
  private static final String WAR_TREE_CSS =
      "cm/jslib/profiles/3x/jquery/plugins/jquery-fancytree/skin-win8/ui.fancytree.min.css";
  // build-legacy-bundles.cjs keys are relative to the cm/ webapp root.
  private static final String BUILDER_TREE_JS =
      "jslib/profiles/3x/jquery/plugins/jquery-fancytree/jquery.fancytree-all.min.js";

  @Test
  void antCopiesFancyTreeIntoPublishedWebResources() throws Exception {
    String xml = Files.readString(DIST_FILES, StandardCharsets.UTF_8);
    assertTrue(
        xml.contains("web_resources/cm/jslib/jquery.fancytree.js")
            && xml.contains(WAR_TREE_JS),
        "installDistributionFiles must publish-time copy FancyTree JS into"
            + " web_resources/cm/jslib/jquery.fancytree.js from the npm-built WAR tree");
    assertTrue(
        xml.contains("web_resources/cm/css/fancytree/skin/ui.fancytree.css")
            && xml.contains(WAR_TREE_CSS),
        "installDistributionFiles must publish-time copy FancyTree skin CSS into"
            + " web_resources/cm/css/fancytree/skin/ui.fancytree.css");
    for (String gif :
        new String[] {"icons.gif", "icons-rtl.gif", "loading.gif", "vline.gif", "vline-rtl.gif"}) {
      assertTrue(
          xml.contains(gif),
          "installDistributionFiles must copy FancyTree skin image " + gif
              + " (referenced relatively by ui.fancytree.css)");
    }
    assertTrue(
        !xml.toLowerCase().contains("dynatree"),
        "installDistributionFiles must not reference retired DynaTree (issue #6052)");
  }

  @Test
  void categoryListResources_lockstepWithPublishedCopies() throws Exception {
    assertTrue(
        Files.isRegularFile(CATEGORY_LIST_RESOURCES),
        "expected category-list DTS resources at " + CATEGORY_LIST_RESOURCES.toAbsolutePath());
    String xml = Files.readString(CATEGORY_LIST_RESOURCES, StandardCharsets.UTF_8);
    assertTrue(
        xml.contains("/web_resources/cm/jslib/jquery.fancytree.js"),
        "percCategoryList.xml must reference the published FancyTree JS copy");
    assertTrue(
        xml.contains("/web_resources/cm/css/fancytree/skin/ui.fancytree.css"),
        "percCategoryList.xml must reference the published FancyTree skin CSS copy");
    assertTrue(
        !xml.toLowerCase().contains("dynatree"),
        "percCategoryList.xml must not reference retired DynaTree (issue #6052)");
  }

  @Test
  void warTreeSources_comeFromNpmBuilderMapping() throws Exception {
    assertTrue(
        Files.isRegularFile(LEGACY_BUNDLE_BUILDER),
        "expected WebUI legacy bundle builder at " + LEGACY_BUNDLE_BUILDER.toAbsolutePath());
    String builder = Files.readString(LEGACY_BUNDLE_BUILDER, StandardCharsets.UTF_8);
    assertTrue(
        builder.contains(BUILDER_TREE_JS)
            && builder.contains("jquery.fancytree/dist/jquery.fancytree-all.min.js"),
        "WebUI builder must map the WAR-tree FancyTree JS from npm jquery.fancytree dist");
    assertTrue(
        builder.contains("skin-win8/ui.fancytree.min.css")
            && builder.contains("jquery.fancytree/dist/skin-win8/ui.fancytree.min.css"),
        "WebUI builder must map the WAR-tree FancyTree skin CSS from npm jquery.fancytree dist");
  }
}
