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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.recycle.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * CMS-logical path routing for recycle paths (issue #6359).
 *
 * <p>The string joins in {@code PSRecycleService} ({@code FOLDERS + "/" + SYSTEM} root constants,
 * {@code "/" + itemName} recycle-path append, {@code FOLDERS + "/" + SYSTEM} prefix insert in
 * {@code attachPathPrefix}) concatenate CMS content-repository paths ({@code
 * //Folders/$System$/...}, {@code //Sites/...} via {@code IPSContentWs} folder paths), never OS
 * filesystem paths — so they are intentionally not {@code Path.resolve}. These tests pin the
 * logical-path routing each suppressed sink implements.
 */
class PSRecycleServiceLogicalPathsTest {

  @Test
  void rootConstantsJoinRepositoryPaths() {
    assertEquals("//Folders/$System$/Assets", PSRecycleService.ASSETS_ROOT);
    assertEquals("//Folders/$System$/Recycling", PSRecycleService.RECYCLING_ROOT);
  }

  @Test
  void checkValidPathPrefixAcceptsRepositoryRoots() {
    assertTrue(PSRecycleService.checkValidPathPrefix("//Sites/my-site"));
    assertTrue(PSRecycleService.checkValidPathPrefix("//Assets/logo.png"));
    assertTrue(
        PSRecycleService.checkValidPathPrefix(PSRecycleService.ASSETS_ROOT + "/logo.png"));
    assertTrue(
        PSRecycleService.checkValidPathPrefix(PSRecycleService.RECYCLING_ROOT + "/my-item"));
    assertTrue(PSRecycleService.checkValidPathPrefix("//Folders/$System$/Sites/1"));
  }

  @Test
  void checkValidPathPrefixRejectsNonRepositoryPaths() {
    assertFalse(PSRecycleService.checkValidPathPrefix("/tmp/foo"));
    assertFalse(PSRecycleService.checkValidPathPrefix("relative/path"));
    assertFalse(PSRecycleService.checkValidPathPrefix(""));
  }

  @Test
  void attachPathPrefixLeavesSitesPathsUnchanged() {
    StringBuilder sb = new StringBuilder("//Sites/my-site");
    assertEquals("//Sites/my-site", PSRecycleService.attachPathPrefix(sb).toString());
  }

  @Test
  void attachPathPrefixRoutesBareAssetsUnderSystemFolder() {
    // Exercises the FOLDERS + "/" + SYSTEM prefix-insert sink: a bare //Assets
    // repository path is rehomed under the //Folders/$System$ repository prefix.
    StringBuilder sb = new StringBuilder("//Assets/logo.png");
    assertEquals(
        "//Folders/$System$/Assets/logo.png",
        PSRecycleService.attachPathPrefix(sb).toString());
  }

  @Test
  void attachPathPrefixStripsSystemPrefixFromAssetsRoot() {
    StringBuilder sb = new StringBuilder(PSRecycleService.ASSETS_ROOT + "/logo.png");
    assertEquals("/Assets/logo.png", PSRecycleService.attachPathPrefix(sb).toString());
  }

  @Test
  void attachPathPrefixRejectsUnknownPrefix() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PSRecycleService.attachPathPrefix(new StringBuilder("/tmp/foo")));
  }
}
