/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
package com.percussion.services.system.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.percussion.services.utils.xml.PSXmlSerializationHelper;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.xml.sax.SAXException;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * This object represents a single design object dependency.
 *
 * <p>Design-object XML root is {@code dependency}. Nested package item element is {@code
 * dependent}. Jackson opt-in property surface (issue #1993 / epic #505).
 *
 * <p><b>Nested type registration (two engines, one name):</b> Jackson binds nested items via {@link
 * JacksonXmlProperty}{@code (localName="dependent")} on {@link #getDependents()}. The static {@link
 * PSXmlSerializationHelper#addType} registration is retained for the Betwixt rollback engine and
 * for polymorphic list restore helpers that still consult the shared type map. Both paths use the
 * same element name {@code dependent} → {@link PSDependent}; they are complementary, not competing
 * owners.
 */
@JacksonXmlRootElement(localName = "dependency")
@JsonAutoDetect(
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
    creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder({"dependents", "id"})
public class PSDependency implements Serializable {
  /** Compiler generated serial version ID used for serialization. */
  private static final long serialVersionUID = -5933263029349676677L;

  /**
   * The id of the design object for which this object shows all dependencies.
   */
  private long id;

  /** All dependent design object, never <code>null</code>, may be empty. */
  private List<PSDependent> dependents = new ArrayList<>();

  static {
    // Betwixt / helper type-map path (Jackson uses @JacksonXmlProperty on getDependents()).
    // Same key+class as Jackson annotations — intentional dual registration for dual engines.
    PSXmlSerializationHelper.addType("dependent", PSDependent.class);
  }

  /** Default constructor. */
  public PSDependency() {}

  /**
   * Get the design object id for which this shows the dependencies.
   *
   * @return the design object id for which this shows the dependencies.
   */
  @JsonProperty
  public long getId() {
    return id;
  }

  /**
   * Set the new design object id for which this shows the dependencies.
   *
   * @param id the new design object id for which this shows the dependencies.
   */
  public void setId(long id) {
    this.id = id;
  }

  /**
   * Get the list with all dependents.
   *
   * @return the list with all dependents, never <code>null</code>, may be empty.
   */
  @JsonProperty
  @JacksonXmlElementWrapper(localName = "dependents")
  @JacksonXmlProperty(localName = "dependent")
  public List<PSDependent> getDependents() {
    return dependents;
  }

  /**
   * Set a new list of dependents.
   *
   * @param dependents the new list of dependents, may be <code>null</code> or empty.
   */
  public void setDependents(List<PSDependent> dependents) {
    if (dependents == null) this.dependents = new ArrayList<>();
    else this.dependents = dependents;
  }

  /**
   * Add a new dependent.
   *
   * @param dependent the new dependent to add, not <code>null</code>.
   */
  public void addDependent(PSDependent dependent) {
    if (dependent == null) throw new IllegalArgumentException("dependent cannot be null");

    dependents.add(dependent);
  }

  /**
   * Get unique list of the dependent types, comma delimited.
   *
   * @return The list, never empty, <code>null</code> if {@link #getDependents()} returns an empty
   *     list.
   */
  @JsonIgnore
  public String getDependentTypes() {
    Set<String> types = new HashSet<>();
    for (PSDependent dependent : dependents) {
      types.add(dependent.getDisplayType());
    }

    String typeList = null;
    if (!types.isEmpty()) {
      for (String type : types) {
        if (typeList == null) typeList = "";
        else typeList += ", ";

        typeList += type;
      }
    }

    return typeList;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PSDependency)) return false;
    PSDependency that = (PSDependency) o;
    return getId() == that.getId() && Objects.equals(getDependents(), that.getDependents());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getDependents());
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer("PSDependency{");
    sb.append("id=").append(id);
    sb.append(", dependents=").append(dependents);
    sb.append('}');
    return sb.toString();
  }

  /* (non-Javadoc)
   * @see IPSCatalogItem#fromXML(String)
   */
  public void fromXML(String xmlsource) throws IOException, SAXException {
    PSXmlSerializationHelper.readFromXML(xmlsource, this);
  }

  /* (non-Javadoc)
   * @see IPSCatalogItem#toXML()
   */
  public String toXML() throws IOException, SAXException {
    return PSXmlSerializationHelper.writeToXml(this);
  }
}
