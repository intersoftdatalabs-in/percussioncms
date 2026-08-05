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
package com.percussion.services.ui.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.percussion.services.catalog.IPSCatalogItem;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.guidmgr.data.PSDesignGuid;
import com.percussion.services.utils.xml.PSXmlSerializationHelper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.xml.IPSXmlSerialization;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.xml.sax.SAXException;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * This object represents a single workbench hierarchy node.
 *
 * <p>Design-object XML root is {@code hierarchy-node}. Jackson opt-in property surface (issue #1920
 * / epic #505). Nested {@code properties} is a string map (key-as-element children). Catalog
 * summary aliases {@code label}/{@code description}, Hibernate {@code version}, and ordinal-only
 * {@code type-int} are suppressed. Node type is written as the enum name ({@code FOLDER} /
 * {@code PLACEHOLDER}).
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSHierarchyNode")
@Table(name = "PSX_WB_HIERARCHY_NODE")
@JacksonXmlRootElement(localName = "hierarchy-node")
@JsonAutoDetect(
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
    creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder({"guid", "name", "parentId", "properties", "type"})
public class PSHierarchyNode implements Serializable, IPSCatalogSummary, IPSCatalogItem {
  /** Compiler generated serial version ID used for serialization. */
  private static final long serialVersionUID = -5425086083061018906L;

  /** Full guid value. */
  @Id
  @Column(name = "NODE_ID", nullable = false)
  private long id;

  @Version
  @Column(name = "VERSION")
  private Integer version;

  /** Full guid value. */
  @Basic
  @Column(name = "PARENT_ID", nullable = true)
  private long parentId;

  @Basic
  @Column(name = "NAME", nullable = false, length = 250)
  private String name;

  @Basic
  @Column(name = "TYPE", nullable = false)
  private int type;

  @Transient private Map<String, String> properties = new TreeMap<>();

  /**
   * This ctor is meant for serialization only. It should not be used by clients.
   */
  public PSHierarchyNode() {}

  /**
   * This enum identifies how the node is used.
   *
   * @author paulhoward
   */
  public enum NodeType {
    /**
     * This node is a folder, meaning it can contain other nodes.
     */
    FOLDER(1),

    /**
     * This node is used to indicate where some other object should be placed in the hierarchy. It is
     * not a container.
     */
    PLACEHOLDER(2);

    /**
     * Lookup enum value by ordinal. Ordinals should be unique. If they are not unique, then the
     * first enum value with a matching ordinal is returned.
     *
     * @param s ordinal value
     * @return an enumerated value or <code>null</code> if the ordinal does not match
     */
    public static NodeType valueOf(int s) {
      NodeType types[] = values();
      for (int i = 0; i < types.length; i++) {
        if (types[i].getOrdinal() == s) return types[i];
      }
      return null;
    }

    /**
     * We want to have our own assigned numbers, so we pass that in to the ctor for each enum.
     *
     * @param ordinal Any value is OK.
     */
    private NodeType(int ordinal) {
      m_value = ordinal;
    }

    /**
     * Returns the value supplied in the ctor.
     *
     * @return See description.
     */
    public int getOrdinal() {
      return m_value;
    }

    /** See ctor. */
    private final int m_value;
  }

  /**
   * Ordinal form for persistence / legacy clients. Suppressed on design-object XML; wire uses enum
   * name via {@link #getNodeType()}.
   *
   * @return Equivalent to {@link #getNodeType()}.ordinal when known.
   */
  @IPSXmlSerialization(suppress = true)
  @JsonIgnore
  public int getTypeInt() {
    return type;
  }

  /**
   * Ordinal form for persistence / legacy clients.
   *
   * @param nodeType Like {@link #setType(NodeType)}, but takes the ordinal of the enum.
   */
  @JsonIgnore
  public void setTypeInt(int nodeType) {
    type = nodeType;
  }

  /**
   * Create a valid node.
   *
   * @param nodeName See {@link #setName(String)}.
   * @param nodeGuid See {@link #setGUID(IPSGuid)}.
   * @param nodeType See {@link #setType(NodeType)}.
   */
  public PSHierarchyNode(String nodeName, IPSGuid nodeGuid, NodeType nodeType) {
    // depend on setters for validation
    setName(nodeName);
    setGUID(nodeGuid);
    setType(nodeType);
  }

  /**
   * Get the object version.
   *
   * @return the object version, <code>null</code> if not initialized yet.
   */
  @IPSXmlSerialization(suppress = true)
  @JsonIgnore
  public Integer getVersion() {
    return version;
  }

  /**
   * Set the object version. The version can only be set once in the life cycle of this object.
   *
   * @param version the version of the object, must be >= 0.
   */
  @JsonIgnore
  public void setVersion(Integer version) {
    // Null-safe for BeanUtils copy after Jackson design-object XML restore (issue #1920).
    if (version == null) {
      return;
    }
    if (this.version != null)
      throw new IllegalStateException("version can only be initialized once");

    if (version < 0) throw new IllegalArgumentException("version must be >= 0");

    this.version = version;
  }

  /* (non-Javadoc)
   * @see IPSCatalogSummary#getName()
   */
  @JsonProperty
  public String getName() {
    return name;
  }

  /**
   * Set a new name for this hierarchy node. A node cannot have the same name as a sibling,
   * case-insensitive. This check is performed by the service, not this class.
   *
   * @param nodeName the new name, not <code>null</code> or empty.
   */
  public void setName(String nodeName) {
    if (StringUtils.isBlank(nodeName))
      throw new IllegalArgumentException("name cannot be null or empty");

    this.name = nodeName;
  }

  /* (non-Javadoc)
   * @see IPSCatalogSummary#getLabel()
   */
  @IPSXmlSerialization(suppress = true)
  @JsonIgnore
  public String getLabel() {
    return getName();
  }

  /* (non-Javadoc)
   * @see IPSCatalogSummary#getDescription()
   */
  @IPSXmlSerialization(suppress = true)
  @JsonIgnore
  public String getDescription() {
    return null;
  }

  /* (non-Javadoc)
   * @see IPSCatalogSummary#getGUID()
   */
  @JsonProperty("guid")
  public IPSGuid getGUID() {
    return new PSDesignGuid(id);
  }

  /**
   * The id of the parent of this hierarchy node, <code>null</code> if this hierarchy node does not
   * have a parent.
   *
   * @return the id of the parent hierarchy node, <code>null</code> if this is a root node.
   */
  @JsonProperty("parent-id")
  public IPSGuid getParentId() {
    return parentId == 0 ? null : new PSDesignGuid(parentId);
  }

  /**
   * Get the type of this hierarchy node (textual form for compatibility with the catalog identifier
   * API).
   *
   * @return the hierarchy node type name as a String, or {@code null} if unknown.
   */
  @JsonIgnore
  public String getType() {
    NodeType nt = NodeType.valueOf(type);
    return nt == null ? null : nt.name();
  }

  /**
   * Convenience accessor returning the enum form of the node type. Design-object XML uses this
   * enum name as {@code <type>}.
   *
   * @return the {@link NodeType} or {@code null} if unknown.
   */
  @JsonProperty("type")
  public NodeType getNodeType() {
    return NodeType.valueOf(type);
  }

  /**
   * BeanUtils / Jackson setter for {@link #getNodeType()}. Allows design-object XML restore into a
   * fresh instance (issue #1920).
   *
   * @param nt the node type, may be {@code null} (ignored)
   */
  public void setNodeType(NodeType nt) {
    if (nt == null) {
      return;
    }
    type = nt.getOrdinal();
  }

  /**
   * Set a new hierarchy node type. One-shot for client construction; design-object XML restore uses
   * {@link #setNodeType(NodeType)} / {@link #setTypeInt(int)}.
   *
   * @param nt The new hierarchy node type, not <code>null</code>.
   * @throws IllegalStateException If called more than once with a different type already set.
   */
  public void setType(NodeType nt) {
    if (nt == null) throw new IllegalArgumentException("nt cannot be null");

    // Allow idempotent re-set of the same value for BeanUtils copy after Jackson restore.
    if (type != 0 && type != nt.getOrdinal()) {
      throw new IllegalStateException("Can only set the type once.");
    }

    type = nt.getOrdinal();
  }

  /**
   * Set the id of the new parent hierarchy node.
   *
   * @param parent the id of the new hierarchy parent, <code>null</code> to make this a root node.
   */
  public void setParentId(IPSGuid parent) {
    if (null == parent) parentId = 0;
    else parentId = new PSDesignGuid(parent).getValue();
  }

  /* (non-Javadoc)
   * @see IPSCatalogItem#setGUID(IPSGuid)
   */
  public void setGUID(IPSGuid newguid) throws IllegalStateException {
    if (newguid == null) throw new IllegalArgumentException("newguid may not be null");

    if (id != 0) throw new IllegalStateException("cannot change existing guid");

    id = new PSDesignGuid(newguid).getValue();
  }

  /**
   * Get all hierarchy node properties. Sorted {@link TreeMap} for deterministic design-object XML /
   * goldens.
   *
   * @return the hierarchy node properties, never <code>null</code>, may be empty.
   */
  @JsonProperty
  public Map<String, String> getProperties() {
    if (!(properties instanceof TreeMap)) {
      properties = new TreeMap<>(properties == null ? Map.of() : properties);
    }
    return properties;
  }

  /**
   * Set new hierarchy node properties.
   *
   * @param properties the new hierarchy node properties, may be <code>null</code> or empty. All
   *     supplied properties must have a valid id. All properties will be attached to this node.
   */
  public void setProperties(Map<String, String> properties) {
    if (properties == null) this.properties = new TreeMap<>();
    else this.properties = new TreeMap<>(properties);
  }

  /**
   * Adds a new hierarchy node property or replaces an existing one.
   *
   * @param propertyName never <code>null</code> or empty.
   * @param value may be <code>null</code> or empty.
   */
  public void addProperty(String propertyName, String value) {
    if (StringUtils.isBlank(propertyName))
      throw new IllegalArgumentException("name cannot be null or empty");

    getProperties().put(propertyName, value);
  }

  /**
   * Remove the property for the specified name.
   *
   * @param propertyName the name or the property to be removed, may be <code>null</code> or empty.
   *     Nothing is done if the referenced property does not exist.
   */
  public void removeProperty(String propertyName) {
    if (!StringUtils.isBlank(propertyName)) getProperties().remove(propertyName);
  }

  /**
   * Get the property for the specified name.
   *
   * @param propertyName the name of the property to get, may be <code>null</code> or empty.
   * @return the property for the specified name, <code>null</code> if not found.
   */
  public String getProperty(String propertyName) {
    if (StringUtils.isBlank(propertyName)) return null;

    return getProperties().get(propertyName);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + (int) (parentId ^ (parentId >>> 32));
    result = prime * result + type;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    PSHierarchyNode other = (PSHierarchyNode) obj;
    if (name == null) {
      if (other.name != null) return false;
    } else if (!name.equals(other.name)) return false;
    if (parentId != other.parentId) return false;
    if (type != other.type) return false;
    return true;
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer("PSHierarchyNode{");
    sb.append("id=").append(id);
    sb.append(", version=").append(version);
    sb.append(", parentId=").append(parentId);
    sb.append(", name='").append(name).append('\'');
    sb.append(", type=").append(type);
    sb.append(", properties=").append(properties);
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
