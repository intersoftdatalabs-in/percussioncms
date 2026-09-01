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

package com.percussion.apibridge;

import com.percussion.design.objectstore.PSControlMeta;
import com.percussion.server.PSCustomControlManager;
import com.percussion.server.PSServer;
import com.percussion.server.PSSystemControlManager;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * Production {@link UserControlIo} over {@link PSCustomControlManager} / {@link
 * PSSystemControlManager}. Path joins use {@link Path#resolve(String)} — no hardcoded OS
 * separators.
 */
final class ManagerUserControlIo implements UserControlIo {

  @Override
  public Path userControlsDirectory() {
    File rx = PSServer.getRxDir();
    if (rx == null) {
      throw new IllegalStateException("Server rx dir is not available");
    }
    return rx.toPath().resolve("rx_resources").resolve("stylesheets").resolve("controls");
  }

  @Override
  public void writeImports() {
    PSCustomControlManager.getInstance().writeImports();
  }

  @Override
  public List<PSControlMeta> loadSystemControls() {
    List<PSControlMeta> metas = PSSystemControlManager.getInstance().getAllControls();
    return metas != null ? metas : List.of();
  }

  @Override
  public List<PSControlMeta> loadUserControls() {
    List<PSControlMeta> metas = PSCustomControlManager.getInstance().getAllControls();
    return metas != null ? metas : List.of();
  }

  @Override
  public Path findUserControlFile(String name) {
    File file = PSCustomControlManager.getInstance().getControlFile(name);
    return file != null ? file.toPath() : null;
  }
}
