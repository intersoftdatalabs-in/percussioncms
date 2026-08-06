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
package com.percussion.ant.install;

import com.percussion.install.InstallUtil;
import org.apache.tools.ant.BuildException;

/**
 * Install action that aborts when a DTS Tomcat instance is still running under the target root
 * (connector ports from {@code Deployment/Server/conf/server.xml} are bound).
 *
 * <p>Use for Production ({@code rootDir} = DTS install root) and Staging ({@code rootDir} = {@code
 * <install>/Staging}). Pair with {@link PSCheckRunningServer} for CMS.
 *
 * <pre>{@code
 * <PSCheckRunningDtsServer rootDir="${install.dir}"/>
 * <PSCheckRunningDtsServer rootDir="${install.dir}/Staging"/>
 * }</pre>
 */
public class PSCheckRunningDtsServer extends PSAction {

  /** Creates a new DTS running-instance check task. */
  public PSCheckRunningDtsServer() {}

  @Override
  public void execute() {
    // Pure gate check — no super.execute() logger/version side effects.
    String root = getRootDir();
    if (root == null || root.isBlank()) {
      if (getProject() != null) {
        root = getProject().getProperty("install.dir");
      }
    }
    if (root == null || root.isBlank()) {
      throw new BuildException("PSCheckRunningDtsServer: rootDir (or install.dir) is required");
    }
    if (InstallUtil.checkTomcatServerRunning(root)) {
      throw new BuildException(
          "A running DTS (Tomcat) instance has been detected in the installation directory "
              + root
              + ". Stop this DTS instance before installing or upgrading to this location"
              + " (offline only).");
    }
  }
}
