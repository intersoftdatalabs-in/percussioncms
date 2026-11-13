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

package com.percussion.deployer.server.dependencies;

import com.percussion.deployer.objectstore.PSDependencyFile;
import com.percussion.deployer.objectstore.PSTransactionSummary;
import com.percussion.design.objectstore.PSContentEditorSharedDef;
import com.percussion.design.objectstore.PSSharedFieldGroup;
import com.percussion.util.PSCollection;
import com.percussion.utils.collections.PSIteratorUtils;
import java.io.File;
import java.util.Iterator;

/**
 * Pure install helpers for shared group packages (issue #6133 residual of #6058).
 *
 * <p>{@link PSSharedGroupDependencyHandler#installDependencyFiles} exceeded the repo complexity
 * thresholds (cognitive 46, cyclomatic 27). The def-file match vs. create branches now live here so
 * they are unit-testable without a live CMS; only the object-store lookups stay in the handler.
 */
public final class PSSharedGroupInstallUtils {

  private PSSharedGroupInstallUtils() {}

  /** A shared-def file found on the target server plus its loaded def. */
  public static final class SharedGroupDefEntry {
    private final File file;
    private final PSContentEditorSharedDef def;

    /**
     * @param file the def file, may not be {@code null}.
     * @param def the loaded def, may not be {@code null}.
     */
    public SharedGroupDefEntry(File file, PSContentEditorSharedDef def) {
      if (file == null) throw new IllegalArgumentException("file may not be null");
      if (def == null) throw new IllegalArgumentException("def may not be null");
      this.file = file;
      this.def = def;
    }

    /** @return the def file, never {@code null}. */
    public File file() {
      return file;
    }

    /** @return the loaded def, never {@code null}. */
    public PSContentEditorSharedDef def() {
      return def;
    }
  }

  /** Resolved install destination for an incoming shared group. */
  public static final class SharedGroupInstallTarget {
    private final PSContentEditorSharedDef def;
    private final File file;
    private final PSCollection<PSSharedFieldGroup> fieldGroups;
    private final int action;

    /**
     * @param def the def to save, may not be {@code null}.
     * @param file the file to save it to, may not be {@code null}.
     * @param fieldGroups the merged groups to set on the def, may not be {@code null}.
     * @param action one of the {@code PSTransactionSummary.ACTION_xxx} codes.
     */
    public SharedGroupInstallTarget(
        PSContentEditorSharedDef def, File file, PSCollection<PSSharedFieldGroup> fieldGroups,
        int action) {
      if (def == null) throw new IllegalArgumentException("def may not be null");
      if (file == null) throw new IllegalArgumentException("file may not be null");
      if (fieldGroups == null) throw new IllegalArgumentException("fieldGroups may not be null");
      this.def = def;
      this.file = file;
      this.fieldGroups = fieldGroups;
      this.action = action;
    }

    /** @return the def to save, never {@code null}. */
    public PSContentEditorSharedDef def() {
      return def;
    }

    /** @return the file to save it to, never {@code null}. */
    public File file() {
      return file;
    }

    /** @return the merged groups to set on the def, never {@code null}. */
    public PSCollection<PSSharedFieldGroup> fieldGroups() {
      return fieldGroups;
    }

    /** @return one of the {@code PSTransactionSummary.ACTION_xxx} codes. */
    public int action() {
      return action;
    }
  }

  /**
   * Finds the shared group XML file in an archive listing.
   *
   * @param files the archive files for the dependency, assumed not {@code null}.
   * @return the first {@code TYPE_SHARED_GROUP_XML} file, or {@code null} if none is present.
   */
  public static PSDependencyFile findSharedGroupXmlFile(Iterator<PSDependencyFile> files) {
    while (files.hasNext()) {
      PSDependencyFile file = files.next();
      if (file.getType() == PSDependencyFile.TYPE_SHARED_GROUP_XML) {
        return file;
      }
    }
    return null;
  }

  /**
   * Replaces the same-named group in the collection, or appends when absent.
   *
   * @param defCol the existing groups, may not be {@code null}.
   * @param group the incoming group, may not be {@code null}.
   */
  public static void replaceGroupInCollection(
      PSCollection<PSSharedFieldGroup> defCol, PSSharedFieldGroup group) {
    if (defCol == null) throw new IllegalArgumentException("defCol may not be null");
    if (group == null) throw new IllegalArgumentException("group may not be null");
    for (int i = 0; i < defCol.size(); i++) {
      if (group.getName().equals(defCol.get(i).getName())) {
        defCol.remove(i);
        break;
      }
    }
    defCol.add(group);
  }

  /**
   * Resolves where an incoming group must be installed.
   *
   * <ul>
   *   <li>Owner def known (original or another file holds the group): replace the same-named group
   *       in place, action {@code MODIFIED}.
   *   <li>Only the original file matches: append the group to it, action {@code MODIFIED}.
   *   <li>Neither: create a new def under the original file name, action {@code CREATED}.
   * </ul>
   *
   * @param match the original-file entry, or {@code null} when the original file is not on the
   *     target.
   * @param owner the entry holding the group, or {@code null} when no file holds it.
   * @param origFile the original file from the archive, used only when creating a new def.
   * @param group the incoming group, may not be {@code null}.
   * @return the install target, never {@code null}.
   */
  public static SharedGroupInstallTarget resolveInstallTarget(
      SharedGroupDefEntry match, SharedGroupDefEntry owner, File origFile,
      PSSharedFieldGroup group) {
    if (group == null) throw new IllegalArgumentException("group may not be null");
    if (owner != null) {
      PSCollection<PSSharedFieldGroup> defCol = typedFieldGroups(owner.def());
      replaceGroupInCollection(defCol, group);
      return new SharedGroupInstallTarget(
          owner.def(), owner.file(), defCol, PSTransactionSummary.ACTION_MODIFIED);
    }
    if (match != null) {
      PSCollection<PSSharedFieldGroup> defCol = typedFieldGroups(match.def());
      defCol.add(group);
      return new SharedGroupInstallTarget(
          match.def(), match.file(), defCol, PSTransactionSummary.ACTION_MODIFIED);
    }
    PSCollection<PSSharedFieldGroup> defCol =
        new PSCollection<>(PSIteratorUtils.iterator(group));
    PSContentEditorSharedDef tgtDef = new PSContentEditorSharedDef(defCol);
    return new SharedGroupInstallTarget(
        tgtDef, origFile, defCol, PSTransactionSummary.ACTION_CREATED);
  }

  /**
   * Copies the field groups of a shared def into a typed collection.
   *
   * <p>{@code PSContentEditorSharedDef.getFieldGroups()} (system module) is typed {@code
   * Iterator<PSSharedFieldGroup>} since #6132, so the elements transfer without a cast.
   *
   * @param def the shared def to copy from, may not be {@code null}.
   * @return a typed collection holding the def's field groups, never {@code null}.
   */
  static PSCollection<PSSharedFieldGroup> typedFieldGroups(PSContentEditorSharedDef def) {
    PSCollection<PSSharedFieldGroup> groups = new PSCollection<>(PSSharedFieldGroup.class);
    Iterator<PSSharedFieldGroup> it = def.getFieldGroups();
    while (it.hasNext()) {
      groups.add(it.next());
    }
    return groups;
  }
}
