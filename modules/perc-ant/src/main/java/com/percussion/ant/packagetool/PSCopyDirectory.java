/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

package com.percussion.ant.packagetool;

import java.io.File;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.FileSet;

/**
 * Copies a directory tree from a root directory to a destination directory based on package
 * structure.
 */
public class PSCopyDirectory extends Copy {
  /** Creates a new copy directory task. */
  public PSCopyDirectory() {}

  /**
   * Sets the root directory path.
   *
   * @param rootDirPath the root directory path, never <code>null</code>
   */
  public void setRootDirPath(String rootDirPath) {
    this.rootDirPath = rootDirPath;
  }

  /**
   * Sets the package name.
   *
   * @param packageName the package name, never <code>null</code>
   */
  public void setPackageName(String packageName) {
    this.packageName = packageName;
  }

  /**
   * Sets the destination directory.
   *
   * @param destDir the destination directory path, never <code>null</code>
   */
  public void setDestDir(String destDir) {
    this.destDir = destDir;
  }

  @Override
  public void execute() throws BuildException {
    setTodir(new File(destDir));
    FileSet fileset = new FileSet();
    String packageFolderName = packageName.substring(0, packageName.lastIndexOf('.'));
    fileset.setDir(new File(rootDirPath + File.separator + packageFolderName));
    addFileset(fileset);
    super.execute();
  }

  /** Destination directory */
  private String destDir;

  /** Root directory path for packages */
  private String rootDirPath;

  /** Package name */
  private String packageName;
}
