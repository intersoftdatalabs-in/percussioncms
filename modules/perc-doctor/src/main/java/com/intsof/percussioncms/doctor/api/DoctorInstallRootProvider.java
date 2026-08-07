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
package com.intsof.percussioncms.doctor.api;

import java.nio.file.Path;

/**
 * Supplies the default CMS install root when a doctor request omits {@code installRoot}.
 *
 * <p>Server hosts typically resolve {@code rxdeploydir} / RX install directory. CLI does not use
 * this interface.
 */
@FunctionalInterface
public interface DoctorInstallRootProvider {

  /**
   * @return absolute install root path; never null
   * @throws IllegalStateException if the host cannot resolve an install root
   */
  Path getDefaultInstallRoot();
}
