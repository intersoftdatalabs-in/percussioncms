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
package com.percussion.services.virtualsite;

import java.nio.file.Path;

/**
 * CLI entry for offline Virtual Site builds.
 *
 * <p>Usage: {@code PSVirtualSiteBuildMain <siteRoot> <outputRoot> [siteKey]}
 */
public final class PSVirtualSiteBuildMain {

  private PSVirtualSiteBuildMain() {}

  public static void main(String[] args) throws Exception {
    if (args.length < 2) {
      System.err.println(
          "Usage: PSVirtualSiteBuildMain <siteRoot> <outputRoot> [siteKey]");
      System.exit(2);
    }
    Path siteRoot = Path.of(args[0]);
    Path outputRoot = Path.of(args[1]);
    String siteKey = args.length >= 3 ? args[2] : "product-docs";

    Path participantDir = outputRoot.resolve("_meta");
    IPSVirtualParticipantService participants =
        new PSInMemoryVirtualParticipantService(participantDir);
    PSVirtualSiteBuildService service =
        new PSVirtualSiteBuildService(new PSGitFilesystemVirtualSiteSource(), participants);

    PSVirtualSiteBuildResult result = service.build(siteRoot, outputRoot, siteKey);
    System.out.println(
        "Built "
            + result.pageCount()
            + " page(s) → "
            + result.outputRoot().toAbsolutePath());
    if (result.hasLinkProblems()) {
      System.err.println("Link problems (" + result.linkProblems().size() + "):");
      for (String p : result.linkProblems()) {
        System.err.println("  " + p);
      }
      System.exit(1);
    }
  }
}
