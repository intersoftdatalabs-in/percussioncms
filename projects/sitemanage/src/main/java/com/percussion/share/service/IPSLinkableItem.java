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
package com.percussion.share.service;

import com.percussion.share.data.IPSFolderPath;

/**
 * Marker for items that are capable of having a link generated for them. See the page module for
 * more info.
 *
 * @author adamgent
 */
public interface IPSLinkableItem extends IPSFolderPath {

  /**
   * Gets the id of the linkable item.
   *
   * @return the id, never null
   */
  String getId();

  /**
   * Gets the type of the linkable item.
   *
   * @return the type, never null
   */
  String getType();
}
