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
// REFACTORED: CP-JAVA11
package com.percussion.services.content.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.percussion.services.catalog.IPSCatalogItem;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.data.IPSCloneTuner;
import com.percussion.services.guidmgr.data.PSGuid;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.xml.sax.SAXException;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * This object represents a single keyword with enhanced Java 11 support.
 *
 * <p>Keywords are used for content categorization and can have associated choices.
 * The class provides comprehensive functionality for managing keyword data including
 * validation, serialization, and choice management.
 *
 * <p>Key features:
 * <ul>
 *   <li>Immutable keyword type constants</li>
 *   <li>Optional-based safe navigation</li>
 *   <li>Enhanced validation with clear error messages</li>
 *   <li>Modern equals/hashCode implementation</li>
 * </ul>
 *
 * @since Java 11 Modernization
 */
/**
 * Design-object XML root is {@code keyword} (PS/IPS strip + hyphenation). Nested package archives
 * use item element {@code choice} (not the mapped type name {@code keyword-choice}) — pinned via
 * Jackson annotations and {@link PSXmlSerializationHelper#addType(String, Class)} (issue #1888 /
 * epic #505).
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSKeyword")
@Table(name = "RXLOOKUP")
@JacksonXmlRootElement(localName = "keyword")
// Opt-in XML surface: catalog interface default methods must not leak into design XML.
@JsonAutoDetect(
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
    creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder({
  "choices",
  "description",
  "guid",
  "id",
  "keywordType",
  "label",
  "sequence",
  "value"
})
public class PSKeyword implements Serializable, IPSCatalogSummary,
   IPSCatalogItem, IPSCloneTuner {

   /**
    * Serial version UID for serialization compatibility.
    */
   private static final long serialVersionUID = -647694540051253253L;

   /**
    * Constant for all objects of type keyword.
    */
   public static final String KEYWORD_TYPE = "1";

   @Id
   @Column(name = "LOOKUPID", nullable = false)
   private long m_id;

   @Basic
   @Column(name = "LOOKUPTYPE", nullable = true, length = 50)
   private String keywordType;

   @Basic
   @Column(name = "LOOKUPVALUE", nullable = true, length = 100)
   private String value;

   @Basic
   @Column(name = "LOOKUPDISPLAY", nullable = true, length = 100)
   private String label;

   @Basic
   @Column(name = "DESCRIPTION", nullable = true, length = 255)
   private String description;

   @Basic
   @Column(name = "LOOKUPSEQUENCE", nullable = true)
   private Integer sequence = 0;

   @Version
   @Column(name = "VERSION")
   private Integer version;
   
   @Transient
   private List<PSKeywordChoice> m_choices = new ArrayList<>();

   static {
      // Package XML uses <choice> (collection singular of "choices"), not the mapped type name
      // "keyword-choice". Register once for Betwixt + Jackson type maps (not on every fromXML).
      PSXmlSerializationHelper.addType("choice", PSKeywordChoice.class);
   }

   /**
    * Default constructor required by JPA/Hibernate.
    * Use factory methods or parameterized constructors for creating new instances.
    */
   public PSKeyword() {
      // Required by JPA
   }

   /**
    * Create a new keyword with the specified parameters and an empty choice list.
    *
    * @param label the display label for the new keyword, not {@code null} or empty
    * @param description a description for the new keyword, may be {@code null} or empty
    * @param value the value for the new keyword, not {@code null}. This value will be
    *              used as the type for all choices
    * @throws IllegalArgumentException if label or value is null or empty
    */
   public PSKeyword(String label, String description, String value) {
      if (StringUtils.isBlank(label)) {
         throw new IllegalArgumentException("label cannot be null or empty");
      }
      if (StringUtils.isBlank(value)) {
         throw new IllegalArgumentException("value cannot be null or empty");
      }

      setKeywordType(KEYWORD_TYPE);
      setValue(value);
      setLabel(label);
      setDescription(description);
   }

   /**
    * Create a new keyword for the supplied id and choice using modern validation.
    *
    * @param id the GUID of the keyword, not {@code null}
    * @param choice the choice for which to create a keyword, not {@code null}
    * @return the newly created keyword for the supplied parameters, never {@code null}
    * @throws IllegalArgumentException if id or choice is null
    */
   public PSKeyword createKeyword(IPSGuid id, PSKeywordChoice choice) {
      Objects.requireNonNull(id, "id cannot be null");
      Objects.requireNonNull(choice, "choice cannot be null");

      var keyword = new PSKeyword();
      keyword.setGUID(id);
      keyword.setKeywordType(getValue());
      keyword.setLabel(choice.getLabel());
      keyword.setDescription(choice.getDescription());
      keyword.setValue(choice.getValue());
      keyword.setSequence(choice.getSequence());

      return keyword;
   }

   /**
    * Get the keyword type.  For keywords, this value will be equivalent to
    * {@link #KEYWORD_TYPE}.  For keyword choices, this value will be
    * equivalent to the value of the parent keyword.
    * 
    * @return the keyword type, never <code>null</code> or empty.
    */
   @JsonProperty
   public String getKeywordType()
   {
      return keywordType;
   }

   /**
    * Set the keyword type, this can only be done once in the lifetime of this
    * object.
    * 
    * @param keywordType the new keyword type, not <code>null</code> or empty.
    */
   public void setKeywordType(String keywordType)
   {
      if (StringUtils.isBlank(keywordType))
         throw new IllegalArgumentException(
            "keywordType cannot be null or empty");

      if (this.keywordType != null)
         throw new IllegalStateException("cannot change keyword type");

      this.keywordType = keywordType;
   }

   /**
    * Get the keyword value. This value is used as the keyword type for all
    * choices.
    * 
    * @return the keyword value, never <code>null</code>, may be empty.
    */
   @JsonProperty
   public String getValue()
   {
      return (value != null) ? value : "";
   }

   /**
    * Set the value, this can only be done once in the lifetime of this object.
    * 
    * @param value the new keyword value, not <code>null</code>.
    */
   public void setValue(String value)
   {
      if (value == null)
         throw new IllegalArgumentException("value cannot be null");

      if (!m_choices.isEmpty() && this.value != null)
         throw new IllegalStateException(
            "cannot change value if keyword defines choices");

      this.value = value;
   }

   /**
    * Get the keyword label.
    * 
    * @return the keyword label, never <code>null</code> or empty.
    */
   @JsonProperty
   public String getLabel()
   {
      return label;
   }

   /**
    * Calls {@link #setLabel(String)}.
    * 
    * @see #setLabel(String)
    */
   public void setName(String name)
   {
      setLabel(name);
   }

   /**
    * Set a new keyword label.
    * 
    * @param label the new label, not <code>null</code> or empty.
    */
   public void setLabel(String label)
   {
      if (StringUtils.isBlank(label))
         throw new IllegalArgumentException("label cannot be null or empty");

      this.label = label;
   }

   /**
    * Get the keyword description.
    * 
    * @return the keyword description, may be <code>null</code> or empty.
    */
   @JsonProperty
   public String getDescription()
   {
      return description;
   }

   /**
    * Set a new keyword description.
    * 
    * @param description the new keyword description, may be <code>null</code>
    * or empty.
    */
   public void setDescription(String description)
   {
      this.description = description;
   }

   /**
    * Get the display sequence for this keyword.
    * 
    * @return the 0 based display sequence.
    */
   @JsonProperty
   public Integer getSequence()
   {
      return sequence;
   }

   /**
    * Set a new display sequence.
    * 
    * @param sequence the new 0 based display sequence, may be <code>null</code>,
    * must be >= 0 if provided.
    */
   public void setSequence(Integer sequence)
   {
      if (sequence != null && sequence < 0)
         throw new IllegalArgumentException("sequence must be >= 0");

      this.sequence = sequence;
   }

   /**
    * Get the keyword choices with Optional wrapper for safer access.
    *
    * @return Optional containing the list of choices, never null
    */
   @IPSXmlSerialization(suppress = true)
   @JsonIgnore
   public Optional<List<PSKeywordChoice>> getChoicesOptional() {
      return Optional.ofNullable(m_choices);
   }

   /**
    * Get the keyword choices (legacy method for backward compatibility).
    *
    * <p>Jackson pins nested items to package element name {@code choice} (matches historical
    * {@code PSKeyword.betwixt} and {@link PSXmlSerializationHelper#addType} registration). Without
    * this annotation Jackson would emit the mapped type name {@code keyword-choice}.
    *
    * @return the list of choices, never {@code null}, may be empty
    */
   @JsonProperty
   @JacksonXmlElementWrapper(localName = "choices")
   @JacksonXmlProperty(localName = "choice")
   public List<PSKeywordChoice> getChoices() {
      return m_choices != null ? m_choices : List.of();
   }

   /**
    * Set the keyword choices with null safety.
    *
    * @param choices the list of choices, may be {@code null}
    */
   public void setChoices(List<PSKeywordChoice> choices) {
      this.m_choices = choices != null ? new ArrayList<>(choices) : new ArrayList<>();
   }

   /**
    * Set a keyword choice, either inserts a new one or updated an existing one
    * based on the label case insensitive.
    *
    * <p>Ignored by Jackson (conflicts with collection item name {@code choice}); use
    * {@link #setChoices(List)} for XML restore. Still used by service/API callers.
    * 
    * @param choice the keyword choice to set, not <code>null</code>.
    */
   @JsonIgnore
   public void setChoice(PSKeywordChoice choice)
   {
      if (choice == null)
         throw new IllegalArgumentException("choice cannot be null");

      for (PSKeywordChoice existingChoice : m_choices)
      {
         if (existingChoice.getLabel().equalsIgnoreCase(choice.getLabel()))
         {
            existingChoice.setDescription(choice.getDescription());
            existingChoice.setValue(choice.getValue());
            existingChoice.setSequence(choice.getSequence());

            return;
         }
      }

      m_choices.add(choice);
   }

   /**
    * Necessary for Betwixt rollback serialization ({@code PSKeyword.betwixt} updater). Jackson uses
    * {@link #setChoices(List)} instead.
    * 
    * @param choice the keyword choice to add, not {@code null}
    */
   @JsonIgnore
   public void addChoice(PSKeywordChoice choice)
   {
      if (choice == null)
         throw new IllegalArgumentException("choice cannot be null");

      for (PSKeywordChoice existingChoice : m_choices)
      {
         if (existingChoice.getLabel().equalsIgnoreCase(choice.getLabel()))
         {
            existingChoice.setDescription(choice.getDescription());
            existingChoice.setValue(choice.getValue());
            existingChoice.setSequence(choice.getSequence());

            return;
         }
      }

      m_choices.add(choice);
   }

   /**
    * Get the object version.
    * 
    * @return the object version, <code>null</code> if not initialized yet.
    */
   @IPSXmlSerialization(suppress = true)
   public Integer getVersion()
   {
      return version;
   }

   /**
    * Set the object version. The version can only be set once in the life cycle
    * of this object.
    * 
    * @param version the version of the object, must be >= 0.
    */
   public void setVersion(Integer version)
   {
      if (this.version != null && version != null)
      {
         throw new IllegalStateException("version can only be initialized "
               + "once");
      }
      
      if (version != null && version.intValue() < 0)
         throw new IllegalArgumentException("version must be >= 0");

      this.version = version;
   }
   
   /**
    * Get the lookup id for the keyword.
    * 
    * @return The lookup id, <code>null</code> if not initialized yet.
    */
   @JsonProperty
   public long getId()
   {
      return this.m_id;
   }
   
   /**
    * Set the lookup id for the keyword.
    * 
    * @param id The lookup id.
    */
   public void setId(long id)
   {
      this.m_id = id;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof PSKeyword)) return false;

      var other = (PSKeyword) obj;
      return new EqualsBuilder()
         .append(m_id, other.m_id)
         .append(keywordType, other.keywordType)
         .append(value, other.value)
         .append(label, other.label)
         .isEquals();
   }

   @Override
   public int hashCode() {
      return new HashCodeBuilder(17, 37)
         .append(m_id)
         .append(keywordType)
         .append(value)
         .append(label)
         .toHashCode();
   }

   @Override
   public String toString() {
      return new ToStringBuilder(this)
         .append("id", m_id)
         .append("keywordType", keywordType)
         .append("value", value)
         .append("label", label)
         .append("description", description)
         .append("sequence", sequence)
         .toString();
   }

   /*
    * (non-Javadoc)
    * 
    * @see IPSCatalogSummary#getGUID()
    */
   /**
    * Catalog GUID. Jackson emits/reads string form via shared {@code IPSGuid} converter in {@code
    * PSJacksonXmlSerializationHelper} (parity with Betwixt {@code PSBetwixtObjectConverter}).
    */
   @JsonProperty("guid")
   public IPSGuid getGUID()
   {
      return new PSGuid(PSTypeEnum.KEYWORD_DEF, m_id);
   }

   /*
    * (non-Javadoc)
    * 
    * @see IPSCatalogSummary#getName()
    */
   /**
    * Alias of {@link #getLabel()} for {@link IPSCatalogSummary}. Jackson omits {@code <name>}
    * (duplicate of {@code <label>}); packages historically emitted both with the same value and
    * read tolerates extra {@code name}. Betwixt rollback still emits name.
    */
   @JsonIgnore
   public String getName()
   {
      return getLabel();
   }

   /*
    * (non-Javadoc)
    * 
    * @see IPSCatalogSummary#getType()
    */
   /**
    * Catalog type name ({@code KEYWORD_DEF}). Jackson omits {@code <type>}; packages may still
    * contain a legacy {@code <type>} element which is ignored on Jackson read. Betwixt rollback
    * still emits type.
    */
   @Override
   @JsonIgnore
   public String getType()
   {
      return PSTypeEnum.KEYWORD_DEF.name();
   }

   /*
    * (non-Javadoc)
    * 
    * @see IPSCatalogItem#setGUID(IPSGuid)
    */
   public void setGUID(IPSGuid newguid) throws IllegalStateException
   {
      if (newguid == null)
         throw new IllegalArgumentException("newguid may not be null");

      // if (m_id != 0)
      // throw new IllegalStateException("cannot change existing guid");

      m_id = newguid.longValue();
   }

   /*
    * (non-Javadoc)
    * 
    * @see IPSCatalogItem#fromXML(String)
    */
   public void fromXML(String xmlsource) throws IOException, SAXException
   {
      PSXmlSerializationHelper.readFromXML(xmlsource, this);
   }

   /*
    * (non-Javadoc)
    * 
    * @see IPSCatalogItem#toXML()
    */
   public String toXML() throws IOException, SAXException
   {
      return PSXmlSerializationHelper.writeToXml(this);
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.percussion.services.data.IPSCloneTuner#tuneClone(java.lang.Object,
    * long)
    */
   public Object tuneClone(long newId)
   {
      m_id = newId;
      value = newId + "";
      return this;
   }
   
   /**
    * Performs a deep copy of the data in the supplied keyword to this
    * keyword.  All properties are copied except for id and version.
    *
    * @param other a valid {@link PSKeyword}.  Cannot be <code>null</code>.
    */
   public void copy(PSKeyword other)
   {
      if (other == null)
         throw new IllegalArgumentException("other may not be null.");
           
      description = other.description;
      keywordType = other.keywordType;
      label = other.label;
      m_choices = new ArrayList<>(other.getChoices());
      sequence = other.sequence;
      value = other.value;
   }
}
