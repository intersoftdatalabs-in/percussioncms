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

package com.percussion.preinstall;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;

/**
 * Listens for ant build output and passes the messages to the Installer if running
 * in installer mode.  For dev installs does nothing.
 *
 * See https://ant.apache.org/manual/develop.html#buildevents
 *
 * Note: InstallAnywhere is no longer used by the product. This listener is kept as a no-op
 * to maintain backward compatibility with build scripts.
 */
public class AntBuildListener implements BuildListener {

  /** Creates a no-op build listener. */
  public AntBuildListener() {}

  @Override
  public void buildStarted(BuildEvent buildEvent) {
    // No-op: InstallAnywhere is no longer used
  }

  @Override
  public void buildFinished(BuildEvent buildEvent) {
    // No-op: InstallAnywhere is no longer used
  }

  @Override
  public void targetStarted(BuildEvent buildEvent) {
    // No-op: InstallAnywhere is no longer used
  }

  @Override
  public void targetFinished(BuildEvent buildEvent) {
    // No-op: InstallAnywhere is no longer used
  }

  @Override
  public void taskStarted(BuildEvent buildEvent) {
    // No-op: InstallAnywhere is no longer used
  }

  @Override
  public void taskFinished(BuildEvent buildEvent) {
    // No-op: InstallAnywhere is no longer used
  }

  @Override
  public void messageLogged(BuildEvent buildEvent) {
    // No-op: InstallAnywhere is no longer used
  }
}
