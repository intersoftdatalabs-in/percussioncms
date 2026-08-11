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
package com.percussion.widgetbuilder.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.percussion.share.dao.PSSerializerUtils;
import com.percussion.share.data.PSAbstractDataObject;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Container object for a list of resource files (JS or CSS). */
@XmlRootElement(name = "WidgetBuilderResourceListData")
@JsonRootName("WidgetBuilderResourceListData")
public class PSWidgetBuilderResourceListData extends PSAbstractDataObject {

  private static final long serialVersionUID = 1L;

  private ArrayList<String> resourceList = new ArrayList<>();

  public static PSWidgetBuilderResourceListData fromXml(String resourceXml) {
    return PSSerializerUtils.unmarshal(resourceXml, PSWidgetBuilderResourceListData.class);
  }

  public String toXml() {
    return PSSerializerUtils.marshal(this);
  }

  /**
   * Get the list of resources in this list.
   *
   * @return The list, not {@code null}, may be empty.
   */
  public List<String> getResourceList() {
    return resourceList;
  }

  /**
   * Set the list of resources.
   *
   * @param resourceList The list, not {@code null}, may be empty.
   */
  @SuppressWarnings("unchecked")
  public void setResourceList(List<String> resourceList) {
    Objects.requireNonNull(resourceList, "resourceList must not be null");
    if (resourceList instanceof ArrayList) {
      this.resourceList = (ArrayList<String>) resourceList;
    } else {
      this.resourceList = new ArrayList<>(resourceList);
    }
  }

  @Override
  public String toString() {
    return "PSWidgetBuilderResourceListData{" + "resourceList=" + resourceList + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PSWidgetBuilderResourceListData)) return false;
    var that = (PSWidgetBuilderResourceListData) o;
    return Objects.equals(getResourceList(), that.getResourceList());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getResourceList());
  }
}
