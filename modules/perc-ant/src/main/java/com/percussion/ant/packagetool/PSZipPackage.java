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
import java.io.IOException;
import java.util.Properties;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Zip;

/**
 * Zips a Rhythmyx {@code .ppkg} package from the temporary staging directories into a single {@code
 * .ppkg} file under the destination directory.
 *
 * <p>Ant task usage:
 *
 * <pre>
 *
 * First set the taskdef:
 *
 *  <code>
 *  &lt;taskdef name="pszippackage"
 *             class="com.percussion.ant.packagetool.PSZipPackage"
 *             classpath="c:\lib"/&gt;
 *  </code>
 *
 * Now use the task to open a file creating a new changelist, the change
 * number will be put into the ${change.number} property.
 *
 *  <code>
 *  &lt;PSP4Edit  path="system/packages"
 *                env="c:/perforce.properties"
 *                description="My new changelist"
 *                packagename="perc.gadget.activity.ppkg"
 *                lock="false"/&gt;
 * </code>
 *
 *
 * Example perforce properties file:
 *
 * <code>
 * #
 * # Perforce P4 java API Properties
 * #
 *
 * # Full path to the P4 executable
 * #
 * p4.executable=C:\\Program Files\\Perforce\\P4.EXE
 *
 * # P4PORT to connect to.
 * #
 * p4.port=hera:1666
 *
 * # P4USER to run as.
 * #
 * p4.user=radharanisonnathi
 *
 * # P4CLIENT to use.
 * #
 * p4.client=rsonnathi-cml-projComments
 *
 * # P4PASSWD to use for the P4USER, if one has been set.
 * #
 * p4.password=password
 *
 *
 * </code>
 *
 *
 * </pre>
 */
public class PSZipPackage extends Zip {
  /** Creates a new zip package task. */
  public PSZipPackage() {
    super();
  }

  /**
   * Returns the destination directory into which the {@code .ppkg} file will be written.
   *
   * @return the destination directory path, may be <code>null</code>
   */
  public String getDestDirPath() {
    return destDirPath;
  }

  /**
   * Sets the destination directory into which the {@code .ppkg} file will be written.
   *
   * @param destDirPath absolute path of the destination directory
   */
  public void setDestDirPath(String destDirPath) {
    this.destDirPath = destDirPath;
  }

  /**
   * Sets the first temporary staging directory containing the package's source files.
   *
   * @param tempPath1 absolute path of the first temporary directory
   */
  public void setTempPath1(String tempPath1) {
    this.tempPath1 = tempPath1;
  }

  /**
   * Sets the second temporary staging directory used as the Ant {@code basedir}.
   *
   * @param tempPath2 absolute path of the second temporary directory
   */
  public void setTempPath2(String tempPath2) {
    this.tempPath2 = tempPath2;
  }

  /**
   * Sets the package name (without the {@code .ppkg} extension).
   *
   * @param packagename the package name, may not be <code>null</code> or empty
   */
  public void setPackageName(String packagename) {
    packageName = packagename;
  }

  /**
   * Sets the root directory under which the package's source files reside.
   *
   * @param rootDirPath absolute path of the package source root
   */
  public void setRootDirPath(String rootDirPath) {
    this.rootDirPath = rootDirPath;
  }

  @Override
  public void execute() throws BuildException {
    if (rootDirPath == null || rootDirPath.trim().length() == 0)
      throw new BuildException("The package path can not be null or empty.");

    if (packageName == null || packageName.trim().length() == 0)
      throw new BuildException("The package name can not be null or empty.");

    if (destDirPath == null || destDirPath.trim().length() == 0)
      throw new BuildException("The destination directory path can not be null or empty.");

    // Get the directory name from packagename Ex: perc.widgets.image1.ppkg -> perc.widgets.image1
    String directoryName = packageName.substring(0, packageName.lastIndexOf('.'));

    try {
      // Get the properties file for this package
      Properties prop =
          PSPackageBuildToolHelper.getPropertiesFile(
              rootDirPath + File.separator + directoryName + File.separator + directoryName);
      PSPackageBuildToolHelper.moveFilesToOriginalPaths(
          directoryName, tempPath1, new File(tempPath1), tempPath2, prop);

      File sourceDirectory = new File(tempPath2);
      File destinationDirectory = new File(destDirPath + File.separator + directoryName + ".ppkg");
      setBasedir(sourceDirectory);
      setDestFile(destinationDirectory);
      super.execute();
    } catch (IOException e) {
      throw new BuildException(e);
    }
  }

  /** Root directory path for packages */
  private String rootDirPath;

  /** Package name going to be zip up */
  private String packageName;

  /** Temporary directory path */
  private String tempPath1;

  /** Temporary directory path */
  private String tempPath2;

  /** Destination directory path */
  private String destDirPath;
}
