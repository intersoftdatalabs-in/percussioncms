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
package com.percussion.design.objectstore;

import com.intsof.percussioncms.auditlog.codes.ObjectStoreErrorCodes;

import com.percussion.xml.PSXmlDocumentBuilder;
import com.percussion.xml.PSXmlTreeWalker;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Properties;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * The class that represents a row in the PSX_RXCONFIGURATIONS table, typically a rx configuration
 * with its state.
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSConfig")
@Table(name = "PSX_RXCONFIGURATIONS")
public class PSConfig extends PSComponent {

  /** Serialization id for {@link java.io.Serializable}. */
  private static final long serialVersionUID = 1L;

  /**
   * Constructs the configuration from its XML representation.
   *
   * @param source the XML element node to construct this object from, assumed not <code>null</code>
   *     . A document conforming to sys_psxServerConfig/getConfigs resource is expected.
   * @param parent the Java object which is the parent of this object, may be <code>null</code>.
   * @param parentComponents the parent objects of this object, may be <code>null</code>.
   * @throws PSUnknownNodeTypeException if the XML element node is not of the appropriate type
   */
  public PSConfig(Element source, IPSDocument parent, List<IPSComponent> parentComponents)
      throws PSUnknownNodeTypeException {
    fromXml(source, parent, parentComponents);
  }

  /** A default constructor is required by hibernate */
  private PSConfig() {}

  /**
   * The configuration name, unique accross the server.
   *
   * @return the configuration name, never <code>null</code> or empty.
   */
  public String getName() {
    return m_name;
  }

  /**
   * Status whether this configuration is locked or not.
   *
   * @return <code>true</code> if locked, <code>false</code> otherwise.
   */
  public boolean isLocked() {
    return YES.equalsIgnoreCase(m_locked);
  }

  /**
   * Is this an XML configuration?
   *
   * @return <code>true</code> if this is an XML configuration, <code>false</code> otherwise.
   */
  public boolean isXML() {
    return m_type.equalsIgnoreCase("xml");
  }

  /**
   * Is this a property configuration?
   *
   * @return <code>true</code> if this is a property configuration, <code>false</code> otherwise.
   */
  public boolean isProperty() {
    return m_type.equalsIgnoreCase("property");
  }

  /**
   * Get the name of the locker.
   *
   * @return the locker name, never <code>null</code> but might be empty if this is not locked.
   */
  public String getLocker() {
    return m_locker == null ? "" : m_locker;
  }

  /**
   * Sets the config as locked by the specified locker.
   *
   * @param locker the user holding the lock, may not be <code>null</code> or empty.
   * @throws IllegalArgumentException if locker is <code>null</code> or empty.
   */
  public void lock(String locker) {
    if (locker == null || locker.trim().length() == 0)
      throw new IllegalArgumentException("locker may not be null or empty.");

    m_locked = YES;
    m_locker = locker;
  }

  /** Sets the config as not locked. */
  public void releaseLock() {
    m_locked = NO;
    m_locker = "";
  }

  /**
   * Get the configuration.
   *
   * @return the configuration, never <code>null</code>.
   */
  public Object getConfig() {
    if (m_configString == null || m_configString.length() == 0)
      throw new IllegalStateException("m_configString must not be null or empty.");

    if (m_configObj == null) {
      try {
        setConfig(m_configString);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    return m_configObj;
  }

  /**
   * Converts the supplied configuration to either to a <code>Document</code> or <code>Properties
   * </code> object based on this configuration type.
   *
   * @param config the config as a <code>String</code>, may not be <code>null
   * </code> or empty.
   */
  public void setConfig(String config) throws IOException, SAXException {
    if (config == null || config.trim().length() == 0)
      throw new IllegalArgumentException("config may not be null or empty.");

    m_configString = config;

    if (isXML()) m_configObj = fromXmlString(config);
    else m_configObj = fromPropertyString(config);
  }

  /**
   * Sets the configuration document.
   *
   * @param doc the document containing the configuration, assumed not <code>null</code>.
   */
  public void setConfig(Document doc) {
    m_configObj = doc;
    m_configString = PSXmlDocumentBuilder.toString(doc);
  }

  /**
   * Sets the configuration properties.
   *
   * @param props the properties containing the configuration, assumed not <code>null</code>.
   */
  public void setConfig(Properties props) {
    m_configObj = props;
    m_configString = props.toString();
  }

  /**
   * Reads the configuration from its XML representation.
   *
   * @param source the XML element node to construct this object from, assumed not <code>null</code>
   *     and of correct type. An element produced by the sys_psxServerConfig/getConfigs resource is
   *     expected.
   * @param parent the Java object which is the parent of this object, may be <code>null</code>.
   * @param parentComponents the parent objects of this object, may be <code>null</code>.
   * @throws PSUnknownNodeTypeException if the XML element node is not of the appropriate type
   */
  public void fromXml(Element source, IPSDocument parent, List<IPSComponent> parentComponents)
      throws PSUnknownNodeTypeException {
    parentComponents = updateParentList(parentComponents);
    int parentSize = parentComponents.size() - 1;

    int firstFlags =
        PSXmlTreeWalker.GET_NEXT_ALLOW_CHILDREN | PSXmlTreeWalker.GET_NEXT_RESET_CURRENT;
    int nextFlags =
        PSXmlTreeWalker.GET_NEXT_ALLOW_SIBLINGS | PSXmlTreeWalker.GET_NEXT_RESET_CURRENT;

    String data = null;
    Element elem = null;
    try {
      PSXmlTreeWalker tree = new PSXmlTreeWalker(source);

      // REQUIRED: the configuration name
      elem = tree.getNextElement("NAME", firstFlags);
      if (elem == null) {
        Object[] args = {RX_CONFIGURATIONS_ELEM, "NAME", "null"};
        throw new PSUnknownNodeTypeException(ObjectStoreErrorCodes.XML_ELEMENT_INVALID_CHILD, args);
      }
      m_name = tree.getElementData(elem);

      // REQUIRED: the configuration type
      tree.setCurrent(source);
      elem = tree.getNextElement("TYPE", firstFlags);
      if (elem == null) {
        Object[] args = {RX_CONFIGURATIONS_ELEM, "TYPE", "null"};
        throw new PSUnknownNodeTypeException(ObjectStoreErrorCodes.XML_ELEMENT_INVALID_CHILD, args);
      }
      m_type = tree.getElementData(elem);
      if (!m_type.equalsIgnoreCase("xml") && !m_type.equalsIgnoreCase("property")) {
        Object[] args = {RX_CONFIGURATIONS_ELEM, "TYPE", "unsupported type: " + m_type};
        throw new PSUnknownNodeTypeException(ObjectStoreErrorCodes.XML_ELEMENT_INVALID_CHILD, args);
      }

      // OPTIONAL: the locked flag
      tree.setCurrent(source);
      elem = tree.getNextElement("LOCKED", firstFlags);
      if (elem != null) m_locked = tree.getElementData(elem);
      else m_locked = "no";

      // OPTIONAL: the locker name
      tree.setCurrent(source);
      elem = tree.getNextElement("LOCKER", firstFlags);
      if (elem != null) m_locker = tree.getElementData(elem);
      else m_locker = "";

      // REQUIRED: the configuration file
      tree.setCurrent(source);
      elem = tree.getNextElement("CONFIGURATION", firstFlags);
      if (elem == null) {
        Object[] args = {RX_CONFIGURATIONS_ELEM, "CONFIGURATION", "null"};
        throw new PSUnknownNodeTypeException(ObjectStoreErrorCodes.XML_ELEMENT_INVALID_CHILD, args);
      }
      setConfig(tree.getElementData(elem));

      // OPTIONAL: the description
      tree.setCurrent(source);
      elem = tree.getNextElement("DESCRIPTION", firstFlags);
      if (elem != null) m_description = tree.getElementData(elem);
      else m_description = "";
    } catch (IOException e) {
      Object[] args = {RX_CONFIGURATIONS_ELEM, "CONFIGURATION", e.getLocalizedMessage()};
      throw new PSUnknownNodeTypeException(ObjectStoreErrorCodes.XML_ELEMENT_INVALID_CHILD, args);
    } catch (SAXException e) {
      Object[] args = {RX_CONFIGURATIONS_ELEM, "CONFIGURATION", e.getLocalizedMessage()};
      throw new PSUnknownNodeTypeException(ObjectStoreErrorCodes.XML_ELEMENT_INVALID_CHILD, args);
    } finally {
      resetParentList(parentComponents, parentSize);
    }
  }

  /**
   * Creates a document from the supplied string.
   *
   * @param source the String to convert into a document, assumed not <code>null</code>.
   * @return the XML document created, never <code>null</code>.
   * @throws IOException if anything goes wrong reading the XML string.
   * @throws SAXException if anything goes wrong parsing the XML string.
   */
  private Document fromXmlString(String source) throws IOException, SAXException {
    String data = null;
    Element node = null;
    StringReader reader = null;

    try {
      reader = new StringReader(source);
      return PSXmlDocumentBuilder.createXmlDocument(reader, false);
    } finally {
      if (reader != null) reader.close();
    }
  }

  /**
   * Creates a properties object from the supplied string.
   *
   * @param source the String to convert into a properties object, assumed not <code>null</code>.
   * @return the Properties object create, never <code>null</code>.
   * @throws IOException if anything goes wrong loading the properties from the supplied string.
   */
  private Properties fromPropertyString(String source) throws IOException {
    String data = null;
    Element node = null;
    try (ByteArrayInputStream is = new ByteArrayInputStream(source.getBytes())) {
      Properties props = new Properties();
      props.load(is);

      return props;
    }
  }

  /**
   * This method is called to create an XML element node with the appropriate format for the given
   * object.
   *
   * @param doc the document to use to create the new XML element, assumed not <code>null</code>.
   * @return the created XML element, never <code>null</code>.
   */
  public Element toXml(Document doc) {
    Element root = doc.createElement("PSX_RXCONFIGURATIONS");

    String config = null;
    if (isXML()) config = PSXmlDocumentBuilder.toString((Document) getConfig());
    else config = ((Properties) getConfig()).toString();

    PSXmlDocumentBuilder.addElement(doc, root, "CONFIGURATION", config);
    PSXmlDocumentBuilder.addElement(doc, root, "DESCRIPTION", getLocker());
    PSXmlDocumentBuilder.addElement(doc, root, "LOCKED", isLocked() ? YES : NO);
    PSXmlDocumentBuilder.addElement(doc, root, "LOCKER", getLocker());
    PSXmlDocumentBuilder.addElement(doc, root, "NAME", getName());
    PSXmlDocumentBuilder.addElement(doc, root, "TYPE", m_type);

    return root;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    return EqualsBuilder.reflectionEquals(this, obj);
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return m_name.hashCode();
  }

  /** The configuration name. Initialized in ctor, never <code>null</code> or empty after that. */
  @Id
  @Column(name = "NAME")
  private String m_name = null;

  @Version
  @Column(name = "VERSION")
  private Integer version;

  /**
   * Hibernate optimistic-lock version for this configuration row.
   *
   * @return the version, may be {@code null} before first persist
   */
  public Integer getVersion() {
    return version;
  }

  /**
   * Sets the Hibernate optimistic-lock version. Used when re-syncing a long-lived in-memory cache
   * entry with the database before {@code merge} (e.g. package install saves the relationships
   * config many times).
   *
   * @param version the version from the database, may be {@code null}
   */
  public void setVersion(Integer version) {
    this.version = version;
  }

  /**
   * The configuration file type. Initialized in ctor, never <code>null</code> or empty after that.
   */
  @Basic
  @Column(name = "TYPE")
  private String m_type = null;

  /**
   * A flag to indicate whether or not the configuration is locked, never <code>null</code>,
   * defaults to unlocked.
   */
  @Basic
  @Column(name = "LOCKED")
  private String m_locked = NO;

  /** The name of the locker, might be empty if not locked bu never <code>null</code>. */
  @Basic
  @Column(name = "LOCKER")
  private String m_locker = "";

  /**
   * The string representation of the configuration content. Initialized in ctor, never <code>null
   * </code> after that.
   *
   * <p>Use {@link SqlTypes#LONGVARCHAR} rather than {@code @Lob}: on PostgreSQL, {@code @Lob} maps
   * to OID large-objects, but the install schema stores CONFIGURATION as TEXT (see cmsTableDef).
   * Reading OID from a TEXT column yields "Bad value for type long" when loading relationship
   * configs.
   */
  @JdbcTypeCode(SqlTypes.LONGVARCHAR)
  @Column(name = "CONFIGURATION")
  private String m_configString = null;

  /**
   * The object representation of the configuration. Initialized in ctor, never <code>null</code>
   * after that.
   */
  /** Parsed config cache; durable form is {@link #m_configString}. Java + JPA transient. */
  @Transient private transient Object m_configObj = null;

  /** A description for this server configuration, might be empty but never <code>null</code>. */
  @Basic
  @Column(name = "DESCRIPTION")
  private String m_description = "";

  /** The constant to indicate the 'yes' value for a member. */
  private static final String YES = "yes";

  /** The constant to indicate the 'no' value for a member. */
  private static final String NO = "no";

  /** The tag name of the xml element representation of this object. */
  public static final String RX_CONFIGURATIONS_ELEM = "PSX_RXCONFIGURATIONS";
}
