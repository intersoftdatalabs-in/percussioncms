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
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.sf.oval.constraint.NotBlank;
import net.sf.oval.constraint.NotNull;

/**
 * A data object that represents a region on the template/page. Contains region ID, tags, CSS class,
 * children, and attributes.
 *
 * @author adamgent
 */
@XmlType(
    name = "",
    propOrder = {"regionId", "startTag", "attributes", "children", "cssClass", "endTag"})
public abstract class PSAbstractRegion extends PSRegionNode {

  private static final long serialVersionUID = 1L;

  @NotNull @NotBlank private String regionId;

  /** Start XHTML tag of region. May be {@code null}. */
  private String startTag;

  /** End tag of region. May be {@code null}. */
  private String endTag;

  /** CSS class for the region. May be {@code null}. */
  private String cssClass;

  private ArrayList<PSRegionNode> children = new ArrayList<>();
  private ArrayList<PSRegionAttribute> attributes = new ArrayList<>();

  /**
   * Gets the children of this region, which are either {@link PSRegionCode} or {@link PSRegion}.
   *
   * @return never {@code null}
   */
  @XmlElementWrapper(name = "children")
  @XmlElements({
    @XmlElement(name = "region", type = PSRegion.class),
    @XmlElement(name = "code", type = PSRegionCode.class)
  })
  public List<PSRegionNode> getChildren() {
    return children;
  }

  @SuppressWarnings("unchecked")
  public void setChildren(List<PSRegionNode> children) {
    if (children == null) {
      this.children = null;
    } else if (children instanceof ArrayList) {
      this.children = (ArrayList<PSRegionNode>) children;
    } else {
      this.children = new ArrayList<>(children);
    }
  }

  @NotNull
  @NotBlank
  public String getRegionId() {
    return regionId;
  }

  public void setRegionId(String id) {
    this.regionId = id;
  }

  /**
   * Gets the start tag of the region. Should include the entire opening tag, e.g. &lt;div
   * id="regionId" class="perc-region"&gt;.
   *
   * @return may be {@code null} or empty.
   */
  public String getStartTag() {
    return startTag;
  }

  public void setStartTag(String startTag) {
    this.startTag = startTag;
  }

  /**
   * Gets the end tag of the region, e.g. &lt;/div&gt;.
   *
   * @return may be {@code null} or empty.
   */
  public String getEndTag() {
    return endTag;
  }

  public void setEndTag(String endTag) {
    this.endTag = endTag;
  }

  /**
   * Gets the user-defined CSS class selector for the region. The selector may or may not be in the
   * {@link #getStartTag()} class attribute, but in general it should be.
   *
   * @return may be empty or {@code null}.
   */
  public String getCssClass() {
    return cssClass;
  }

  public void setCssClass(String rootClass) {
    this.cssClass = rootClass;
  }

  /**
   * Gets the user-defined attributes for the region.
   *
   * @return may be empty or {@code null}.
   */
  @XmlElementWrapper(name = "attributes")
  @XmlElements({@XmlElement(name = "attribute", type = PSRegionAttribute.class)})
  public List<PSRegionAttribute> getAttributes() {
    return attributes;
  }

  @SuppressWarnings("unchecked")
  public void setAttributes(List<PSRegionAttribute> attributes) {
    if (attributes == null) {
      this.attributes = null;
    } else if (attributes instanceof ArrayList) {
      this.attributes = (ArrayList<PSRegionAttribute>) attributes;
    } else {
      this.attributes = new ArrayList<>(attributes);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PSAbstractRegion)) return false;
    PSAbstractRegion that = (PSAbstractRegion) o;
    return Objects.equals(getRegionId(), that.getRegionId())
        && Objects.equals(getStartTag(), that.getStartTag())
        && Objects.equals(getEndTag(), that.getEndTag())
        && Objects.equals(getCssClass(), that.getCssClass())
        && Objects.equals(getChildren(), that.getChildren())
        && Objects.equals(getAttributes(), that.getAttributes());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getRegionId(), getStartTag(), getEndTag(), getCssClass(), getChildren(), getAttributes());
  }

  @Override
  public String toString() {
    return "PSAbstractRegion{"
        + "regionId='"
        + regionId
        + '\''
        + ", startTag='"
        + startTag
        + '\''
        + ", endTag='"
        + endTag
        + '\''
        + ", cssClass='"
        + cssClass
        + '\''
        + ", children="
        + children
        + ", attributes="
        + attributes
        + '}';
  }
}
