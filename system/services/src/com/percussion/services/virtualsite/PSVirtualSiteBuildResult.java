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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Outcome of a Virtual Site static build. */
public final class PSVirtualSiteBuildResult {

  private final Path outputRoot;
  private final int pageCount;
  private final List<String> linkProblems;
  private final List<String> writtenFiles;

  public PSVirtualSiteBuildResult(
      Path outputRoot, int pageCount, List<String> linkProblems, List<String> writtenFiles) {
    this.outputRoot = outputRoot;
    this.pageCount = pageCount;
    this.linkProblems =
        linkProblems == null
            ? List.of()
            : Collections.unmodifiableList(new ArrayList<>(linkProblems));
    this.writtenFiles =
        writtenFiles == null
            ? List.of()
            : Collections.unmodifiableList(new ArrayList<>(writtenFiles));
  }

  public Path outputRoot() {
    return outputRoot;
  }

  public int pageCount() {
    return pageCount;
  }

  public List<String> linkProblems() {
    return linkProblems;
  }

  public List<String> writtenFiles() {
    return writtenFiles;
  }

  public boolean hasLinkProblems() {
    return !linkProblems.isEmpty();
  }
}
