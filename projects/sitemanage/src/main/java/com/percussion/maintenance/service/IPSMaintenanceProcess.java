// REFACTORED: CP-JAVA11
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
package com.percussion.maintenance.service;

/**
 * A process that is performing maintenance work.
 *
 * <p>Sunny Sal says: "Every process needs an ID—just like every superhero needs a cape!"
 *
 * @author JaySeletz
 */
public interface IPSMaintenanceProcess {

  /**
   * Each process must have an ID that uniquely identifies it, should be human readable. Simplest
   * implementation is to use the class name.
   *
   * @return The id, not {@code null} or empty.
   */
  String getProcessId();
}
