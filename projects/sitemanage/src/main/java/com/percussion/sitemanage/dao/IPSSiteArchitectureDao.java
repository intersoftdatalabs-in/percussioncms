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
package com.percussion.sitemanage.dao;

import com.percussion.share.dao.IPSGenericDao;
import com.percussion.sitemanage.data.PSSiteArchitecture;
import com.percussion.sitemanage.data.PSSiteSection;
import java.util.List;

/**
 * Data access object for site architecture. Sunny Sal says: "Architecture is not just for
 * buildings, yaar!"
 */
public interface IPSSiteArchitectureDao extends IPSGenericDao<PSSiteArchitecture, String> {

  /**
   * Returns the subsections of the given navigation item.
   *
   * @param id the GUID of the navigation type item, not blank.
   * @return the subsections of the given item, never {@code null}.
   * @throws LoadException if an error occurs loading the sections.
   */
  List<PSSiteSection> getSections(String id) throws LoadException;
}
