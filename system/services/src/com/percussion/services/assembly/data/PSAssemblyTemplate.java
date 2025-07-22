/*
 * Copyright 1999-2023 Percussion Software, Inc.
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
package com.percussion.services.assembly.data;

import com.percussion.error.PSExceptionUtils;
import com.percussion.extension.IPSExtension;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.assembly.IPSTemplateSlot;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.assembly.PSAssemblyServiceLocator;
import com.percussion.services.catalog.IPSCatalogItem;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.error.PSRuntimeException;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.utils.xml.PSXmlSerializationHelper;
import com.percussion.util.PSXMLDomUtil;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.xml.IPSXmlSerialization;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.ListIndexBase;
import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.annotations.SortComparator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import javax.persistence.Version;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Assembly template entity representing a single template with enhanced Java 11 support.
 *
 * <p>Each template defines how content should be assembled and rendered, containing optional
 * bindings that map JEXL expressions to variable bindings. Templates reference specific
 * assemblers for evaluation and can contain:
 * <ul>
 *   <li>XSL stylesheets in the filesystem (legacy assembler)</li>
 *   <li>Velocity templates in content items (velocity assembler)</li>
 * </ul>
 *
 * <p>This entity replaces the legacy {@code PSContentTypeVariant} with modern features:
 * <ul>
 *   <li>Enhanced null safety with Optional wrappers</li>
 *   <li>Stream-based collection processing</li>
 *   <li>Immutable factory methods</li>
 *   <li>Comprehensive validation patterns</li>
 * </ul>
 *
 * @author dougrand
 * @since Java 11 Modernization
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSAssemblyTemplate")
@NaturalIdCache
@Table(name = "PSX_TEMPLATE")
@NamedQueries({
      @NamedQuery(name = "template.findByNameAndType",
                  query = "select t from PSAssemblyTemplate t, PSContentTemplateDesc d " +
                         "where lower(t.name) = :name and d.m_contenttypeid = :typeid and d.m_templateid = t.id"),
      @NamedQuery(name = "template.findByType",
                  query = "select d.m_templateid from PSContentTemplateDesc d where :ctype = d.m_contenttypeid"),
      @NamedQuery(name = "template.findTemplateNameToTypeInfo",
                  query = "select d.m_contenttypeid, t.id, t.name " +
                         "from PSAssemblyTemplate t, PSContentTemplateDesc d " +
                         "where d.m_templateid = t.id")
})
public class PSAssemblyTemplate implements IPSAssemblyTemplate, IPSCatalogSummary, IPSCatalogItem, Serializable {

    private static final PSExecutionOrderComparator bindingComparator = new PSExecutionOrderComparator();
    private static final Logger log = LogManager.getLogger(PSAssemblyTemplate.class);
    private static final long serialVersionUID = -1240365481092237620L;

    @Id
    @Column(name = "TEMPLATE_ID")
    private long id;

    @Version
    private Integer version;

    @Basic
    @NaturalId(mutable = true)
    @Column(name = "NAME", unique = true)
    private String name;

    @Basic
    private String label;

    @Basic
    @Column(name = "LOCATIONPREFIX")
    private String locationPrefix;

    @Basic
    @Column(name = "LOCATIONSUFFIX")
    private String locationSuffix;

    @Basic
    private String assembler;

    @Basic
    @Column(name = "ASSEMBLYURL")
    private String assemblyUrl;

    @Basic
    @Column(name = "STYLESHEETNAME")
    private String styleSheet;

    @Basic
    @Column(name = "AATYPE")
    private int aaType;

    @Basic
    @Column(name = "OUTPUTFORMAT")
    private int outputFormat;

    @Basic
    @Column(name = "PUBLISHWHEN")
    private Character publishWhen = PublishWhen.Unspecified.getValue();

    @Basic
    @Column(name = "TEMPLATE_TYPE")
    private Integer templateType = TemplateType.Shared.ordinal();

    @Basic
    private String description;

    @Lob
    @Basic(fetch = FetchType.EAGER)
    private String template;

    @Basic()
    @Column(name = "MIME_TYPE")
    private String mimeType;

    @Basic
    private String charset;

    @OneToMany(targetEntity = PSTemplateBinding.class, cascade = {CascadeType.ALL},
               fetch = FetchType.EAGER, orphanRemoval = true)
    @OrderColumn(name = "EXECUTION_ORDER")
    @ListIndexBase(1)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, region = "PSAssemblyTemplate_Bindings")
    @Fetch(FetchMode.SUBSELECT)
    @JoinColumn(name = "TEMPLATE_ID", nullable = false)
    @SortComparator(PSExecutionOrderComparator.class)
    private List<PSTemplateBinding> bindings = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER, targetEntity = PSTemplateSlot.class,
                cascade = {CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE})
    @JoinTable(name = "RXVARIANTSLOTTYPE",
               joinColumns = {@JoinColumn(name = "VARIANTID")},
               inverseJoinColumns = {@JoinColumn(name = "SLOTID")})
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, region = "PSAssemblyTemplate_Slots")
    private Set<IPSTemplateSlot> slots = new HashSet<>();

    @Basic
    @Column(name = "GLOBAL_TEMPLATE_USAGE")
    private String globalTemplateUsage;

    /**
     * Default constructor required for JPA/Hibernate.
     */
    public PSAssemblyTemplate() {
        // Required by JPA
    }

    /**
     * Create a new assembly template with specified name and assembler.
     *
     * @param name the template name, not {@code null} or empty
     * @param assembler the assembler type, not {@code null} or empty
     * @return a new PSAssemblyTemplate instance
     * @throws IllegalArgumentException if name or assembler is null or empty
     */
    public static PSAssemblyTemplate of(String name, String assembler) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("name cannot be null or empty");
        }
        if (StringUtils.isBlank(assembler)) {
            throw new IllegalArgumentException("assembler cannot be null or empty");
        }

        var template = new PSAssemblyTemplate();
        template.setName(name);
        template.setAssembler(assembler);
        return template;
    }

    /**
     * Get the template name with Optional wrapper for safer access.
     *
     * @return Optional containing the template name if present, empty otherwise
     */
    public Optional<String> getNameOptional() {
        return Optional.ofNullable(name);
    }

    /**
     * Get the template name.
     *
     * @return the template name, may be {@code null}
     */
    public String getName() {
        return name;
    }

    /**
     * Set the template name with enhanced validation.
     *
     * @param name the template name, not {@code null} or empty
     * @throws IllegalArgumentException if name is null or empty
     */
    public void setName(String name) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("name cannot be null or empty");
        }
        this.name = name;
    }

    /**
     * Get the template description with Optional wrapper.
     *
     * @return Optional containing the description if present, empty otherwise
     */
    public Optional<String> getDescriptionOptional() {
        return Optional.ofNullable(description);
    }

    /**
     * Get the template description.
     *
     * @return the description, may be {@code null}
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the template description.
     *
     * @param description the description, may be {@code null}
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get the assembler with Optional wrapper.
     *
     * @return Optional containing the assembler if present, empty otherwise
     */
    public Optional<String> getAssemblerOptional() {
        return Optional.ofNullable(assembler);
    }

    /**
     * Get the assembler type.
     *
     * @return the assembler, may be {@code null}
     */
    public String getAssembler() {
        return assembler;
    }

    /**
     * Set the assembler type with validation.
     *
     * @param assembler the assembler type, not {@code null} or empty
     * @throws IllegalArgumentException if assembler is null or empty
     */
    public void setAssembler(String assembler) {
        if (StringUtils.isBlank(assembler)) {
            throw new IllegalArgumentException("assembler cannot be null or empty");
        }
        this.assembler = assembler;
    }

    /**
     * Get template bindings as an immutable list for safer access.
     *
     * @return immutable list of template bindings, never {@code null}
     */
    public List<PSTemplateBinding> getBindings() {
        return bindings != null ? List.copyOf(bindings) : List.of();
    }

    /**
     * Get template bindings as a Stream for functional processing.
     *
     * @return Stream of template bindings, never {@code null}
     */
    public Stream<PSTemplateBinding> getBindingsStream() {
        return bindings != null ? bindings.stream() : Stream.empty();
    }

    /**
     * Set template bindings with null safety.
     *
     * @param bindings the list of bindings, may be {@code null}
     */
    public void setBindings(List<PSTemplateBinding> bindings) {
        this.bindings = bindings != null ? new ArrayList<>(bindings) : new ArrayList<>();
    }

    /**
     * Add a template binding with validation.
     *
     * @param binding the binding to add, not {@code null}
     * @throws IllegalArgumentException if binding is null
     */
    public void addBinding(PSTemplateBinding binding) {
        Objects.requireNonNull(binding, "binding cannot be null");
        if (bindings == null) {
            bindings = new ArrayList<>();
        }
        bindings.add(binding);
    }

    /**
     * Get template slots as an immutable set for safer access.
     *
     * @return immutable set of template slots, never {@code null}
     */
    public Set<IPSTemplateSlot> getSlots() {
        return slots != null ? Set.copyOf(slots) : Set.of();
    }

    /**
     * Get template slots as a Stream for functional processing.
     *
     * @return Stream of template slots, never {@code null}
     */
    public Stream<IPSTemplateSlot> getSlotsStream() {
        return slots != null ? slots.stream() : Stream.empty();
    }

    /**
     * Set template slots with null safety.
     *
     * @param slots the set of slots, may be {@code null}
     */
    public void setSlots(Set<IPSTemplateSlot> slots) {
        this.slots = slots != null ? new HashSet<>(slots) : new HashSet<>();
    }

    /**
     * Add a template slot with validation.
     *
     * @param slot the slot to add, not {@code null}
     * @throws IllegalArgumentException if slot is null
     */
    public void addSlot(IPSTemplateSlot slot) {
        Objects.requireNonNull(slot, "slot cannot be null");
        if (slots == null) {
            slots = new HashSet<>();
        }
        slots.add(slot);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PSAssemblyTemplate)) return false;

        var other = (PSAssemblyTemplate) obj;
        return Objects.equals(id, other.id) &&
               Objects.equals(name, other.name) &&
               Objects.equals(assembler, other.assembler);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, assembler);
    }

    @Override
    public String toString() {
        return String.format("PSAssemblyTemplate{id=%d, name='%s', assembler='%s', templateType=%s}",
                           id, name, assembler, templateType);
    }

    /*
     *  (non-Javadoc)
     * @see com.percussion.services.catalog.IPSCatalogSummary#getLabel()
     */
    public String getLabel()
    {
      if (StringUtils.isBlank(label))
         return getName();

      return label;
    }

    /**
     * Get the type of this object for cataloging
     * @return the type, never <code>null</code>
     */
    public PSTypeEnum getType()
    {
      return PSTypeEnum.TEMPLATE;
    }

    /*
     *  (non-Javadoc)
     * @see com.percussion.services.assembly.IPSAssemblyTemplate#getTemplateType()
     */
    public TemplateType getTemplateType()
    {
      if (templateType == null)
         return TemplateType.Shared;
      else
         return TemplateType.valueOf(templateType);
    }

    /* (non-Javadoc)
     * @see com.percussion.services.assembly.IPSAssemblyTemplate#setTemplateType(com.percussion.services.assembly.IPSAssemblyTemplate.TemplateType)
     */
    public void setTemplateType(TemplateType newTemplateType)
    {
      if (newTemplateType == null)
      {
         throw new IllegalArgumentException("newTemplateType may not be null");
      }
      templateType = newTemplateType.ordinal();
    }

    /* (non-Javadoc)
     * @see com.percussion.services.assembly.IPSAssemblyTemplate#isVariant()
     */
    public boolean isVariant()
    {
      return assembler == null
            || assembler.equals(IPSExtension.LEGACY_ASSEMBLER);
    }


    /**
     * This method does the following: 1. creates a XML document 2. from the
     * document, extract the slot ids and build a list of guids 3. returns a set
     * of guids for the slots
     *
     * @param tmpStr the original template as a XML string representation from
     *           which the slots are extracted, tmpStr is never <code>null</code>
     *           or empty
     * @return the slots as a guid collection may be empty, but never
     *         <code>null</code>
     * @throws IOException if an I/O error occurs
     * @throws SAXException if a parsing error occurs
     */
    public static Set<IPSGuid> getSlotIdsFromTemplate(String tmpStr)
          throws IOException, SAXException
    {
      if (StringUtils.isBlank(tmpStr))
         throw new IllegalArgumentException("tmpStr may not be null or empty");
      
      Set<IPSGuid> slotGuids = new HashSet<>();
      Document doc = PSXmlDocumentBuilder.createXmlDocument(new StringReader(
            tmpStr), false);
      Element root = doc.getDocumentElement();
      NodeList nTmpList = root.getElementsByTagName(XML_SLOTIDS_NAME);
      if (nTmpList != null && nTmpList.getLength() > 0)
      {
         Element tmpList = (Element) nTmpList.item(0);
         NodeList nl = tmpList.getElementsByTagName(XML_SLOTID_NAME);
         for (int i = 0; (nl != null) && (i < nl.getLength()); i++)
         {
            Element tmpId = (Element) nl.item(i);
            String tmp = PSXMLDomUtil.getElementData(tmpId);
            if (StringUtils.isBlank(tmp))
               continue;
            IPSGuid g = new PSGuid(PSTypeEnum.SLOT, tmp);
            slotGuids.add(g);
         }
      }
      return slotGuids;
    }

    /**
     * This method is specifically used by MSM to replace the slot ids from
     * the serialized data with the new ids back into the serialized data
     * @param tmpStr the original template as XML string representation,
     * never <code>null</code> or empty
     * @param newSlots the list of new slots that need to be added may
     * not be <code>null</code>, may or may not be empty
     * @return the original template with replaced template GUIDS as a an XML string
     * representation
     * @throws IOException if an I/O error occurs
     * @throws SAXException if a parsing error occurs
     */
    public static String replaceSlotIdsFromTemplate(String tmpStr,
          Set<IPSGuid> newSlots) throws IOException, SAXException
    {
      if (StringUtils.isBlank(tmpStr))
         throw new IllegalArgumentException("siteStr may not be null or empty");
      if (newSlots == null)
         throw new IllegalArgumentException("template list may not be null");
   
      Document doc = PSXmlDocumentBuilder.createXmlDocument(new StringReader(
            tmpStr), false);
      Element root = doc.getDocumentElement();
      NodeList slotIdsElem = root.getElementsByTagName(XML_SLOTIDS_NAME);
      Element oldSlotList = (Element)slotIdsElem.item(0);
      Element newSlotList = doc.createElement(XML_SLOTIDS_NAME);
      for (IPSGuid g : newSlots)
      {
         PSXmlDocumentBuilder.addElement(doc, newSlotList, XML_SLOTID_NAME,
               String.valueOf(g.getUUID()));
      }
      oldSlotList.getParentNode().replaceChild(newSlotList, oldSlotList);
      return PSXmlDocumentBuilder.toString(doc);
    }

    /**
     * Node name for the slotids list representation
     */
    private static final String XML_SLOTIDS_NAME  = "template-slot-ids";

    /**
     * Node name for the slotid that is a child of templateids list
     */
    private static final String XML_SLOTID_NAME   = "template-slot-id";

}
