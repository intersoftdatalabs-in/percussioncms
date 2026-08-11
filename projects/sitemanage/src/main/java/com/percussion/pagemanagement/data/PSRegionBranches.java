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
package com.percussion.pagemanagement.data;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.sf.oval.constraint.AssertValid;
import org.apache.commons.beanutils.BeanUtils;

/**
 * Contains the page regions that will override the template's regions. Also contains the
 * region-widgets associations ({@link #getRegionWidgetAssociations()}).
 *
 * @author adamgent
 */
@XmlRootElement(name = "RegionBranches")
public class PSRegionBranches extends PSRegionWidgetAssociations {
  private static final long serialVersionUID = 1L;

  @AssertValid private ArrayList<PSRegion> regions = new ArrayList<>();

  @AssertValid
  @XmlElementWrapper(name = "regions")
  @XmlElement(name = "region")
  public List<PSRegion> getRegions() {
    return regions;
  }

  @SuppressWarnings("unchecked")
  public void setRegions(List<PSRegion> pageRegions) {
    if (pageRegions == null) {
      this.regions = null;
    } else if (pageRegions instanceof ArrayList) {
      this.regions = (ArrayList<PSRegion>) pageRegions;
    } else {
      this.regions = new ArrayList<>(pageRegions);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PSRegionBranches)) return false;
    if (!super.equals(o)) return false;
    var that = (PSRegionBranches) o;
    return Objects.equals(getRegions(), that.getRegions());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getRegions());
  }

  @Override
  public String toString() {
    return "PSRegionBranches{" + "regions=" + regions + '}';
  }

  @Override
  public PSRegionBranches clone() {
    try {
      return (PSRegionBranches) BeanUtils.cloneBean(this);
    } catch (Exception e) {
      throw new RuntimeException("Cannot clone", e);
    }
  }
}
