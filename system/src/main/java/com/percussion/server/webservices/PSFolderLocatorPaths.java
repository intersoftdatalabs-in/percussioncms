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
package com.percussion.server.webservices;

import com.percussion.cms.PSCmsException;
import com.percussion.design.objectstore.PSLocator;
import java.util.ArrayList;
import java.util.List;

/**
 * Typed folder-locator path collection used by {@link PSServerFolderProcessor}. Extracted so
 * ancestor-walk behavior can be unit-tested without constructing the processor (which requires a
 * live CMS object catalog).
 */
final class PSFolderLocatorPaths {

  private PSFolderLocatorPaths() {}

  /** Immediate folder parents of a locator (community filtering is the caller's concern). */
  @FunctionalInterface
  interface FolderParentLookup {
    List<PSLocator> getImmediateParents(PSLocator locator) throws PSCmsException;
  }

  /**
   * Builds all ancestor locator paths using the given immediate-parent lookup. Each path is
   * parent-first, root-last. When walking a single path toward the root, only the first immediate
   * parent is used (folders have one folder parent).
   *
   * @param itemLocator item or folder locator, never {@code null}
   * @param parentLookup immediate folder parents of a locator, never {@code null}
   * @return list of paths, never {@code null}, may be empty
   * @throws PSCmsException if the lookup fails
   */
  static List<List<PSLocator>> collect(PSLocator itemLocator, FolderParentLookup parentLookup)
      throws PSCmsException {
    if (itemLocator == null) {
      throw new IllegalArgumentException("itemLocator cannot be null");
    }
    if (parentLookup == null) {
      throw new IllegalArgumentException("parentLookup cannot be null");
    }
    List<PSLocator> immediate = parentLookup.getImmediateParents(itemLocator);
    List<List<PSLocator>> idPaths = new ArrayList<>(immediate.size());
    for (PSLocator parent : immediate) {
      List<PSLocator> path = new ArrayList<>();
      path.add(parent);
      appendAncestors(parent, path, parentLookup);
      idPaths.add(path);
    }
    return idPaths;
  }

  /**
   * Appends ancestors onto {@code path} until the lookup reports no parent. Only the first immediate
   * parent is followed at each step.
   */
  private static void appendAncestors(
      PSLocator folderLocator, List<PSLocator> path, FolderParentLookup parentLookup)
      throws PSCmsException {
    List<PSLocator> immediate = parentLookup.getImmediateParents(folderLocator);
    if (!immediate.isEmpty()) {
      PSLocator owner = immediate.get(0);
      path.add(owner);
      appendAncestors(owner, path, parentLookup);
    }
  }
}
