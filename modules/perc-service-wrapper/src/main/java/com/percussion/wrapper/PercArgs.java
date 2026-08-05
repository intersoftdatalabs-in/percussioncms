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

package com.percussion.wrapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses the command-line arguments supplied to {@link PSServiceWrapper#main(String[])} and exposes
 * typed accessors for each recognized flag.
 *
 * <p>Recognized flags include <code>--help</code>, <code>--jettyHelp</code>, <code>--debugWrapper
 * </code>, <code>--force</code>, <code>--start</code>, <code>--startServer</code>, <code>--startDTS
 * </code>, <code>--startStagingDTS</code>, <code>--stop</code>, <code>--stopServer</code>, <code>
 * --stopDTS</code>, <code>--stopStagingDTS</code>, <code>--status</code>, and <code>--rxlt</code>.
 * Any arguments not matching a recognized flag are retained in {@link #getFilteredArgs()} and
 * forwarded to the wrapped services.
 */
public class PercArgs {
  private boolean startServer;
  private boolean startDTS;
  private boolean startStagingDTS;
  private boolean stopServer;
  private boolean stopDTS;
  private boolean stopStagingDTS;
  private boolean status;
  private boolean force;
  private boolean help;
  private boolean debugStartup;
  private boolean rxltTool;

  private List<String> filteredArgs = new ArrayList<>();

  /**
   * Parses the supplied command-line arguments and sets the corresponding flags.
   *
   * @param args the raw command-line arguments; may be <code>null</code> or empty, in which case
   *     help mode is enabled by default
   */
  public PercArgs(String[] args) {
    boolean foundArg = false;

    for (String arg : args) {
      switch (arg) {
        case "--help":
          help = true;
        case "--jettyHelp":
          filteredArgs.add("--help");
        case "--debugWrapper":
          debugStartup = true;
        case "--force":
          force = true;
        case "--start":
          foundArg = true;
          startServer = true;
          startDTS = true;
          startStagingDTS = true;
          break;
        case "--startServer":
          foundArg = true;
          startServer = true;
          break;
        case "--startDTS":
          foundArg = true;
          startDTS = true;
          break;
        case "--startStagingDTS":
          foundArg = true;
          startStagingDTS = true;
          break;
        case "--stop":
          foundArg = true;
          stopServer = true;
          stopDTS = true;
          stopStagingDTS = true;
          break;
        case "--stopServer":
          foundArg = true;
          stopServer = true;
          break;
        case "--stopDTS":
          foundArg = true;
          stopDTS = true;
          break;
        case "--stopStagingDTS":
          foundArg = true;
          stopStagingDTS = true;
          break;
        case "--status":
          foundArg = true;
          status = true;
          break;
        case "--rxlt":
          foundArg = true;
          rxltTool = true;
          break;
        default:
          filteredArgs.add(arg);
      }
    }
    // default to starting server
    if (!foundArg) {
      help = true;
    }
  }

  /**
   * Indicates whether the Jetty server should be started.
   *
   * @return <code>true</code> if <code>--startServer</code> or <code>--start</code> was supplied
   */
  public boolean isStartServer() {
    return startServer;
  }

  /**
   * Indicates whether the production DTS should be started.
   *
   * @return <code>true</code> if <code>--startDTS</code> or <code>--start</code> was supplied
   */
  public boolean isStartDTS() {
    return startDTS;
  }

  /**
   * Indicates whether the staging DTS should be started.
   *
   * @return <code>true</code> if <code>--startStagingDTS</code> or <code>--start</code> was
   *     supplied
   */
  public boolean isStartStagingDTS() {
    return startStagingDTS;
  }

  /**
   * Indicates whether the Jetty server should be stopped.
   *
   * @return <code>true</code> if <code>--stopServer</code> or <code>--stop</code> was supplied
   */
  public boolean isStopServer() {
    return stopServer;
  }

  /**
   * Indicates whether the production DTS should be stopped.
   *
   * @return <code>true</code> if <code>--stopDTS</code> or <code>--stop</code> was supplied
   */
  public boolean isStopDTS() {
    return stopDTS;
  }

  /**
   * Indicates whether the staging DTS should be stopped.
   *
   * @return <code>true</code> if <code>--stopStagingDTS</code> or <code>--stop</code> was supplied
   */
  public boolean isStopStagingDTS() {
    return stopStagingDTS;
  }

  /**
   * Indicates whether the <code>--status</code> flag was supplied.
   *
   * @return <code>true</code> if the wrapper should print the state of each managed service and
   *     exit
   */
  public boolean isStatus() {
    return status;
  }

  /**
   * Indicates whether the <code>--rxlt</code> flag was supplied.
   *
   * @return <code>true</code> if the wrapper should invoke the RxLT localization tool
   */
  public boolean isRxltTool() {
    return rxltTool;
  }

  /**
   * Converts the list of command-line arguments and converts them to a String array.
   *
   * @return the list of arguments in a String array.
   */
  public String[] getFilteredArgs() {
    return filteredArgs.toArray(new String[filteredArgs.size()]);
  }

  /**
   * Indicates whether the <code>--debugWrapper</code> flag was supplied.
   *
   * @return <code>true</code> if Jetty-startup debug logging should be enabled
   */
  public boolean isDebugStartup() {
    return debugStartup;
  }

  /**
   * Indicates whether the <code>--force</code> flag was supplied.
   *
   * @return <code>true</code> if a service that does not stop gracefully should be killed
   */
  public boolean isForce() {
    return force;
  }

  /**
   * Indicates whether the <code>--help</code> flag was supplied or no recognized flag was found.
   *
   * @return <code>true</code> if the wrapper should print help text and exit
   */
  public boolean isHelp() {
    return help;
  }
}
