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
package com.percussion.dashboardmanagement.data;

import com.percussion.share.data.PSAbstractDataObject;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.List;

@XmlRootElement(name = "DashboardConfig")
@XmlType(
    name = "",
    propOrder = {"gadgets"})
public class PSDashboardConfiguration extends PSAbstractDataObject {
  private static final long serialVersionUID = 1L;

  private List<PSGadget> gadgets;

  public List<PSGadget> getGadgets() {
    return gadgets;
  }

  public void setGadgets(List<PSGadget> gadgets) {
    this.gadgets = gadgets;
  }
}
