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

package com.percussion.ant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.tools.ant.launch.Launcher;

/**
 * Launches an Apache Ant build with the provided arguments by delegating to {@link
 * org.apache.tools.ant.launch.Launcher}.
 */
public class PSAntLauncher {
  /** Creates a new Ant launcher. */
  public PSAntLauncher() {}

  /**
   * Main entry point that forwards the supplied arguments to the Ant launcher.
   *
   * @param args command line arguments passed to Ant
   */
  public static void main(String[] args) {

    List<String> origArgs = new ArrayList<>(Arrays.asList(args));
    // origArgs.add("--noclasspath");
    // rigArgs.add("--nouserlib");
    // System.setProperty("ant.library.dir","antlib");
    Launcher.main(origArgs.toArray(new String[origArgs.size()]));
  }
}
