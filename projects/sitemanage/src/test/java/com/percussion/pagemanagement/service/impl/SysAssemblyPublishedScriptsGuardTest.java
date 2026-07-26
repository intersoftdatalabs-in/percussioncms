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

package com.percussion.pagemanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Regression for GH-749 / v8.1.7 PR #762: global {@code includeOnPublishedPage != "no"} guards
 * around shared page scripts (jquery-ui path selection, non-jquery script links, delivery service
 * helpers, mobile preview, cataloged-link suppress, print_jqueryUI wrapper) must not gate published
 * pages. Widget-level {@code includeOnPublishedPage} property usage remains valid elsewhere.
 */
class SysAssemblyPublishedScriptsGuardTest {

  private static final Path REL =
      Path.of("system/cms/content/applications/sys_resources/ApplicationFiles/vm/sys_assembly.vm");

  @Test
  void bodyCloseAndDeliveryScriptsNotGatedByIncludeOnPublishedPage() throws Exception {
    String vm = readAssemblyVm();

    // jquery-ui edit/preview branches must not require includeOnPublishedPage
    assertFalse(
        Pattern.compile("isEditMode\\(\\)\\s*&&\\s*\\(\\$includeOnPublishedPage\\s*!=\\s*\"no\"\\)")
            .matcher(vm)
            .find(),
        "edit-mode jquery-ui branch must not gate on includeOnPublishedPage");
    assertFalse(
        Pattern.compile(
                "isPreviewMode\\(\\)\\s*&&\\s*\\(\\$includeOnPublishedPage\\s*!=\\s*\"no\"\\)")
            .matcher(vm)
            .find(),
        "preview-mode jquery-ui branch must not gate on includeOnPublishedPage");

    // non-jquery/jquery-ui script emission must not require includeOnPublishedPage
    assertFalse(
        Pattern.compile(
                "endsWith\\(\"/jquery-ui\\.js\"\\)\\)\\s*&&\\s*\\(\\$includeOnPublishedPage")
            .matcher(vm)
            .find(),
        "generic script loop must not gate on includeOnPublishedPage");

    // delivery helpers always emit with jQuery guard
    assertTrue(
        vm.contains("if (typeof jQuery !== 'undefined') { jQuery.getDeliveryServiceBase"),
        "perc_addDeliveryJSFunctions must guard jQuery and always emit");
    assertFalse(
        Pattern.compile(
                "#macro\\(perc_addDeliveryJSFunctions\\)[\\s\\S]*?#if\\(\\$includeOnPublishedPage\\s*!=\\s*\"no\"\\)")
            .matcher(vm)
            .find(),
        "perc_addDeliveryJSFunctions must not wrap in includeOnPublishedPage");
  }

  @Test
  void printJqueryUiMacroNotWrappedByIncludeOnPublishedPage() throws Exception {
    String vm = readAssemblyVm();
    // Macro body must start processing instances without an outer includeOnPublishedPage if
    assertTrue(
        Pattern.compile("#macro\\(print_jqueryUI\\s+\\$location_name\\)##\\s*#set\\(\\$instances")
            .matcher(vm)
            .find(),
        "print_jqueryUI must not open with includeOnPublishedPage guard");
    // Widget-level UI flag must remain the control for published jquery-ui
    assertTrue(
        vm.contains("includeUIOnPublishedPage"),
        "print_jqueryUI must still honor includeUIOnPublishedPage widget property");
  }

  @Test
  void previewOnlyMacrosNotGatedByIncludeOnPublishedPage() throws Exception {
    String vm = readAssemblyVm();
    assertFalse(
        Pattern.compile(
                "#macro\\(suppressCatalogedLinks\\)##[\\s\\S]*?#if\\(\\$includeOnPublishedPage\\s*!=\\s*\"no\"\\)")
            .matcher(vm)
            .find(),
        "suppressCatalogedLinks must not gate on includeOnPublishedPage");
    assertFalse(
        Pattern.compile(
                "#macro\\(addMobilePreviewToolbar\\)##[\\s\\S]*?#if\\(\\$includeOnPublishedPage\\s*!=\\s*\"no\"\\)")
            .matcher(vm)
            .find(),
        "addMobilePreviewToolbar must not gate on includeOnPublishedPage");
  }

  @Test
  void widgetLevelIncludeOnPublishedPageStillPresent() throws Exception {
    String vm = readAssemblyVm();
    // Widget property plumbing must remain (auto-list / jquery widgets still honor per-widget flag)
    assertTrue(
        vm.contains("$widgetitem.getProperties().get(\"includeOnPublishedPage\")"),
        "widget-level includeOnPublishedPage property still required");
  }

  private static String readAssemblyVm() throws Exception {
    Path root = resolveRepoRoot();
    Path vm = root.resolve(REL);
    if (!Files.isRegularFile(vm)) {
      fail("expected sys_assembly.vm at " + vm.toAbsolutePath());
    }
    return Files.readString(vm, StandardCharsets.UTF_8);
  }

  private static Path resolveRepoRoot() {
    Path cwd = Path.of("").toAbsolutePath().normalize();
    Path candidate = cwd.resolve("../..").normalize();
    if (Files.isDirectory(candidate.resolve("system"))
        && Files.isDirectory(candidate.resolve("WebUI"))) {
      return candidate;
    }
    if (Files.isDirectory(cwd.resolve("system")) && Files.isDirectory(cwd.resolve("WebUI"))) {
      return cwd;
    }
    fail("could not resolve monorepo root; tried " + candidate + " and " + cwd);
    return cwd;
  }
}
