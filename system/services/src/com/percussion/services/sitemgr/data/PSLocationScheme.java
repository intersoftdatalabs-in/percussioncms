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
package com.percussion.services.sitemgr.data;

import com.percussion.services.catalog.IPSCatalogIdentifier;
import com.percussion.services.catalog.IPSCatalogItem;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.PSGuidHelper;
import com.percussion.services.guidmgr.PSGuidUtils;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.sitemgr.IPSLocationScheme;
import com.percussion.services.sitemgr.IPSPublishingContext;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.string.PSStringUtils;
import com.percussion.utils.xml.IPSXmlErrors;
import com.percussion.utils.xml.PSInvalidXmlException;
import com.percussion.utils.xml.PSXmlUtils;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
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
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Modern location scheme data entity representing location generation rules
 * for content publishing using Java 11 features.
 *
 * <h2>Java 11 Enhancements</h2>
 * <ul>
 * <li>Enhanced validation with Objects.requireNonNull</li>
 * <li>Optional-based safe access for nullable properties</li>
 * <li>Stream API for efficient parameter processing</li>
 * <li>Improved error handling and validation patterns</li>
 * </ul>
 *
 * @author dougrand (original)
 * @author Sunny Sal (Java 11 refactoring)
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSLocationScheme")
@Table(name = "PSX_LOCATIONSCHEME")
public class PSLocationScheme implements IPSLocationScheme, IPSCatalogItem, IPSCatalogIdentifier {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "LOCATIONSCHEME_ID")
    private long schemeId = -1;

    @Basic
    @Column(name = "NAME")
    private String name;

    @Basic
    @Column(name = "DESCRIPTION")
    private String description;

    @Basic
    @Column(name = "GENERATOR")
    private String generator;

    @Basic
    @Column(name = "TEMPLATE_ID")
    private Long templateId;

    @Basic
    @Column(name = "CONTENT_TYPE_ID")
    private Long contentTypeId;

    @Basic
    @Column(name = "CONTEXT_ID")
    private Long contextId;

    @Version
    @Column(name = "VERSION")
    private Integer version;

    @OneToMany(targetEntity = PSLocationSchemeParameter.class,
               cascade = {CascadeType.ALL},
               fetch = FetchType.EAGER,
               orphanRemoval = true)
    @JoinColumn(name = "LOCATIONSCHEME_ID")
    @Fetch(FetchMode.SUBSELECT)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private Set<PSLocationSchemeParameter> parameters = new HashSet<>();

    /**
     * Cloning flag to track cloned instances
     */
    private transient boolean isCloned = false;

    /**
     * Default constructor.
     */
    public PSLocationScheme() {
        // Default constructor
    }

    /**
     * Enhanced copy constructor using Java 11 patterns
     */
    public PSLocationScheme(PSLocationScheme source) {
        Objects.requireNonNull(source, "source cannot be null");

        this.name = source.name;
        this.description = source.description;
        this.generator = source.generator;
        this.templateId = source.templateId;
        this.contentTypeId = source.contentTypeId;
        this.contextId = source.contextId;
        this.version = source.version;
        this.isCloned = true;

        // Deep copy parameters using streams
        this.parameters = new HashSet<>();
        source.parameters.stream()
            .map(PSLocationSchemeParameter::new)
            .forEach(this.parameters::add);
    }

    @Override
    public IPSGuid getGUID() {
        return schemeId == -1 ? null : new PSGuid(PSTypeEnum.LOCATION_SCHEME, schemeId);
    }

    @Override
    public void setGUID(IPSGuid guid) throws IllegalStateException {
        Objects.requireNonNull(guid, "guid cannot be null");
        if (guid.getType() != PSTypeEnum.LOCATION_SCHEME.getOrdinal()) {
            throw new IllegalArgumentException("guid must be a location scheme guid");
        }
        if (schemeId != -1) {
            throw new IllegalStateException("schemeId is already set");
        }
        this.schemeId = guid.longValue();
    }

    @Override
    public long getId() {
        return schemeId;
    }

    @Override
    public void setId(long id) {
        this.schemeId = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getGenerator() {
        return generator;
    }

    @Override
    public void setGenerator(String generator) {
        this.generator = generator;
    }

    @Override
    public IPSGuid getTemplateId() {
        return Optional.ofNullable(templateId)
            .map(id -> new PSGuid(PSTypeEnum.TEMPLATE, id))
            .orElse(null);
    }

    @Override
    public void setTemplateId(IPSGuid templateId) {
        this.templateId = Optional.ofNullable(templateId)
            .map(IPSGuid::longValue)
            .orElse(null);
    }

    @Override
    public IPSGuid getContentTypeId() {
        return Optional.ofNullable(contentTypeId)
            .map(id -> new PSGuid(PSTypeEnum.NODEDEF, id))
            .orElse(null);
    }

    @Override
    public void setContentTypeId(IPSGuid contentTypeId) {
        this.contentTypeId = Optional.ofNullable(contentTypeId)
            .map(IPSGuid::longValue)
            .orElse(null);
    }

    @Override
    public IPSGuid getContextId() {
        return Optional.ofNullable(contextId)
            .map(id -> new PSGuid(PSTypeEnum.CONTEXT, id))
            .orElse(null);
    }

    @Override
    public void setContextId(IPSGuid contextId) {
        this.contextId = Optional.ofNullable(contextId)
            .map(IPSGuid::longValue)
            .orElse(null);
    }

    @Override
    public Set<PSLocationSchemeParameter> getParameterSet() {
        return new HashSet<>(parameters);
    }

    @Override
    public void setParameterSet(Set<PSLocationSchemeParameter> parameters) {
        this.parameters = Optional.ofNullable(parameters)
            .map(HashSet::new)
            .orElse(new HashSet<>());
    }

    /**
     * Enhanced parameter management using Java 11 patterns
     */
    @Override
    public String getParameter(String name) {
        Objects.requireNonNull(name, "name cannot be null");

        return parameters.stream()
            .filter(param -> Objects.equals(param.getName(), name))
            .map(PSLocationSchemeParameter::getValue)
            .findFirst()
            .orElse(null);
    }

    @Override
    public void setParameter(String name, String value) {
        Objects.requireNonNull(name, "name cannot be null");

        // Remove existing parameter with same name
        parameters.removeIf(param -> Objects.equals(param.getName(), name));

        // Add new parameter if value is not blank
        if (StringUtils.isNotBlank(value)) {
            var parameter = new PSLocationSchemeParameter();
            parameter.setName(name);
            parameter.setValue(value);
            parameter.setLocationSchemeId(schemeId);
            parameters.add(parameter);
        }
    }

    /**
     * Check if this is a cloned instance
     */
    public boolean isCloned() {
        return isCloned;
    }

    /**
     * Mark this instance as cloned
     */
    public void setCloned(boolean cloned) {
        this.isCloned = cloned;
    }

    @Override
    public Integer getVersion() {
        return version;
    }

    @Override
    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        var scheme = (PSLocationScheme) obj;
        return new EqualsBuilder()
            .append(schemeId, scheme.schemeId)
            .append(name, scheme.name)
            .append(generator, scheme.generator)
            .append(templateId, scheme.templateId)
            .append(contentTypeId, scheme.contentTypeId)
            .append(contextId, scheme.contextId)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(schemeId)
            .append(name)
            .append(generator)
            .append(templateId)
            .append(contentTypeId)
            .append(contextId)
            .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("schemeId", schemeId)
            .append("name", name)
            .append("generator", generator)
            .append("templateId", templateId)
            .append("contentTypeId", contentTypeId)
            .append("contextId", contextId)
            .toString();
    }
}
