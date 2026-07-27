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
package com.percussion.ant.install;

import com.percussion.install.InstallUtil;
import org.apache.tools.ant.BuildException;

/**
 * Install action that aborts when a CMS (Rhythmyx) server is still running under the target root
 * (bind port / Derby Network Server heuristics via {@link InstallUtil#checkServerRunning(String)}).
 *
 * <p>Pair with {@link PSCheckRunningDtsServer} for DTS Tomcat instances.
 *
 * <pre>{@code
 * <PSCheckRunningServer rootDir="${install.dir}"/>
 * }</pre>
 *
 * @author vamsinukala
 */
public class PSCheckRunningServer extends PSAction {
  /** Creates a new running server check task. */
  public PSCheckRunningServer() {}

  @Override
  public void execute() {
    // Intentionally no super.execute(): this is a pure gate check without logger/version side
    // effects.
    String root = getRootDir();
    if (root == null || root.isBlank()) {
      // Fall back to Ant project install.dir when task omits rootDir attribute
      if (getProject() != null) {
        root = getProject().getProperty("install.dir");
      }
    }
    if (root == null || root.isBlank()) {
      throw new BuildException("PSCheckRunningServer: rootDir (or install.dir) is required");
    }
    if (InstallUtil.checkServerRunning(root)) {
      throw new BuildException(
          "A running CMS (Rhythmyx) server has been detected in the installation directory "
              + root
              + ". Stop this instance before installing or upgrading to this location"
              + " (offline only).");
    }
  }
}
