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

package com.percussion.theme.data;

import static org.apache.commons.lang3.Validate.notNull;

import com.percussion.share.data.PSAbstractDataObject;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/** Represents a list of region CSS objects. */
@XmlRootElement(name = "RegionCssList")
public class PSRegionCssList extends PSAbstractDataObject {
  private static final long serialVersionUID = 1L;

  private ArrayList<PSRegionCSS> regions = new ArrayList<>();

  public List<PSRegionCSS> getRegions() {
    return regions;
  }

  @SuppressWarnings("unchecked")
  public void setRegions(List<PSRegionCSS> regionList) {
    notNull(regionList);
    if (regionList instanceof ArrayList) {
      regions = (ArrayList<PSRegionCSS>) regionList;
    } else {
      regions = new ArrayList<>(regionList);
    }
  }
}
