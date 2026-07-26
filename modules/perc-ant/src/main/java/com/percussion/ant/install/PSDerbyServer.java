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

package com.percussion.ant.install;

import com.percussion.install.InstallUtil;

/** Shuts down the Derby server after all database tasks have completed. */
public class PSDerbyServer extends PSAction {
  /** Creates a new Derby server shutdown task. */
  public PSDerbyServer() {}

  /** This will handle Shutting down the Derby Server after all DB tasks are done. CMS-5932. */
  public void execute() {
    InstallUtil.shutDownDerby();
  }
}
