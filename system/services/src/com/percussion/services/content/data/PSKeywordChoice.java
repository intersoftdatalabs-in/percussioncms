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
import com.percussion.services.utils.xml.PSXmlSerializationHelper;
import com.percussion.utils.xml.IPSXmlSerialization;
import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xml.sax.SAXException;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Represents a single keyword choice.
 *
 * <p>Nested package item element name is {@code choice} (registered from {@link PSKeyword}), not
 * the default mapped type name {@code keyword-choice}. {@link JacksonXmlRootElement} applies to
 * standalone {@link #toXML()}/{@link #fromXML(String)} only.
 *
 * @since Java 11 Modernization
 */
@JacksonXmlRootElement(localName = "keyword-choice")
@JsonAutoDetect(
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
    creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder({"description", "label", "sequence", "value"})
public class PSKeywordChoice implements Serializable {

   /**
    * Serial version UID for serialization compatibility.
    */
   private static final long serialVersionUID = 5676687536687532224L;

   /**
    * The value for this keyword choice, never {@code null}, may be empty.
    */
   private String value = "";

   /**
    * The label for this keyword choice, never {@code null} or empty.
    */
   private String label;

   /**
    * A description for this keyword choice, may be {@code null} or empty.
    */
   private String description;

   /**
    * The 0-based display sequence for this keyword choice, always >= 0.
    */
   private Integer sequence = 0;

   /**
    * Default constructor required for serialization frameworks.
    * Use factory methods or parameterized constructors for creating new instances.
    */
   public PSKeywordChoice() {
      // Required for serialization
   }

   /**
    * Construct a new keyword choice from the supplied keyword using modern validation.
    *
    * @param keyword the keyword for which to construct a new choice, not {@code null}
    * @throws NullPointerException if keyword is null
    */
   public PSKeywordChoice(PSKeyword keyword) {
      Objects.requireNonNull(keyword, "keyword cannot be null");

      setValue(keyword.getValue());
      setLabel(keyword.getLabel());
      setDescription(keyword.getDescription());
      setSequence(keyword.getSequence());
   }

   /**
    * Create a new keyword choice with specified parameters.
    *
    * @param value the value for the choice, not {@code null}
    * @param label the label for the choice, not {@code null} or empty
    * @param description the description for the choice, may be {@code null}
    * @param sequence the display sequence, must be >= 0
    * @return a new PSKeywordChoice instance
    * @throws IllegalArgumentException if validation fails
    */
   public static PSKeywordChoice of(String value, String label, String description, Integer sequence) {
      var choice = new PSKeywordChoice();
      choice.setValue(value);
      choice.setLabel(label);
      choice.setDescription(description);
      choice.setSequence(sequence);
      return choice;
   }

   /**
    * Get the keyword choice value with Optional wrapper for safer access.
    *
    * @return Optional containing the value if non-null, empty Optional otherwise
    */
   @IPSXmlSerialization(suppress = true)
   @JsonIgnore
   public Optional<String> getValueOptional() {
      return Optional.ofNullable(value);
   }

   /**
    * Get the keyword choice value (legacy method for backward compatibility).
    *
    * @return the keyword choice value, never {@code null}, may be empty
    */
   @JsonProperty
   public String getValue() {
      return value != null ? value : "";
   }

   /**
    * Set a new keyword choice value with enhanced validation.
    *
    * @param value the new keyword choice value, not {@code null}
    * @throws NullPointerException if value is null
    */
   public void setValue(String value) {
      this.value = Objects.requireNonNull(value, "value cannot be null");
   }

   /**
    * Get the keyword choice label.
    * 
    * @return the keyword choice label, never {@code null} or empty
    */
   @JsonProperty
   public String getLabel() {
      return label;
   }

   /**
    * Set a new keyword choice label with enhanced validation.
    *
    * @param label the new keyword choice label, not {@code null} or empty
    * @throws IllegalArgumentException if label is null or empty
    */
   public void setLabel(String label) {
      if (StringUtils.isBlank(label)) {
         throw new IllegalArgumentException("label cannot be null or empty");
      }
      this.label = label;
   }

   /**
    * Get the keyword choice description with Optional wrapper.
    *
    * @return Optional containing the description if non-null, empty Optional otherwise
    */
   @IPSXmlSerialization(suppress = true)
   @JsonIgnore
   public Optional<String> getDescriptionOptional() {
      return Optional.ofNullable(description);
   }

   /**
    * Get the keyword choice description (legacy method for backward compatibility).
    *
    * @return the keyword choice description, may be {@code null} or empty
    */
   @JsonProperty
   public String getDescription() {
      return description;
   }

   /**
    * Set a new keyword choice description.
    * 
    * @param description the new keyword choice description, may be {@code null} or empty
    */
   public void setDescription(String description) {
      this.description = description;
   }

   /**
    * Get the display sequence for this keyword choice.
    *
    * @return the 0-based display sequence, never {@code null}
    */
   @JsonProperty
   public Integer getSequence() {
      return sequence != null ? sequence : 0;
   }

   /**
    * Set a new display sequence with enhanced validation.
    *
    * @param sequence the new 0-based display sequence, may be {@code null},
    *                 must be >= 0 if provided
    * @throws IllegalArgumentException if sequence is negative
    */
   public void setSequence(Integer sequence) {
      if (sequence != null && sequence < 0) {
         throw new IllegalArgumentException("sequence must be >= 0");
      }
      this.sequence = sequence;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof PSKeywordChoice)) return false;

      var other = (PSKeywordChoice) obj;
      return new EqualsBuilder()
         .append(value, other.value)
         .append(label, other.label)
         .append(description, other.description)
         .append(sequence, other.sequence)
         .isEquals();
   }

   @Override
   public int hashCode() {
      return new HashCodeBuilder(17, 37)
         .append(value)
         .append(label)
         .append(description)
         .append(sequence)
         .toHashCode();
   }

   @Override
   public String toString() {
      return new ToStringBuilder(this)
         .append("value", value)
         .append("label", label)
         .append("description", description)
         .append("sequence", sequence)
         .toString();
   }

   /**
    * Serialize this object to XML string.
    *
    * @return XML representation of this object
    * @throws IOException if serialization fails
    * @throws SAXException if XML parsing fails
    */
   public String toXML() throws IOException, SAXException {
      return PSXmlSerializationHelper.writeToXml(this);
   }

   /**
    * Deserialize this object from XML string.
    *
    * @param xmlsource the XML source string, not {@code null}
    * @throws IOException if deserialization fails
    * @throws SAXException if XML parsing fails
    * @throws IllegalArgumentException if xmlsource is null
    */
   public void fromXML(String xmlsource) throws IOException, SAXException {
      Objects.requireNonNull(xmlsource, "xmlsource cannot be null");
      PSXmlSerializationHelper.readFromXML(xmlsource, this);
   }
}
