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
package com.percussion.services.publisher.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.filter.IPSItemFilter;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.PSGuidHelper;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.publisher.IPSContentList;
import com.percussion.services.utils.xml.PSXmlSerializationHelper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.xml.IPSXmlSerialization;
import jakarta.persistence.*;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.hibernate.annotations.*;
import org.hibernate.annotations.Cache;
import org.xml.sax.SAXException;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Represents a content list in the database.
 *
 * <p>Design-object XML root is {@code content-list}. Nested package item elements are {@code
 * content-list-generator-param} and {@code template-expander-param} (registered via {@link
 * PSXmlSerializationHelper#addType}). Jackson opt-in property surface (issue #1919 / epic #505).
 *
 * @author dougrand
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSContentList")
@Table(name = "RXCONTENTLIST")
@JacksonXmlRootElement(localName = "content-list")
@JsonAutoDetect(
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
    creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder({
  "contentListId",
  "contentListType",
  "description",
  "editionType",
  "expander",
  "filterId",
  "generator",
  "guid",
  "name",
  "url",
  "generator-arguments",
  "expander-arguments"
})
public class PSContentList implements IPSContentList {

  static {
    // Nested package item element names (mapped type defaults).
    PSXmlSerializationHelper.addType(
        "content-list-generator-param", PSContentListGeneratorParam.class);
    PSXmlSerializationHelper.addType("template-expander-param", PSTemplateExpanderParam.class);
  }
    @Id
    @Column(name = "CONTENTLISTID")
    long contentListId;
    @SuppressWarnings("unused")
    @Version
    private Integer version;
    @Basic
    String name;
    @Basic
    String description;
    @Basic
    Integer type = 0;
    @Basic
    String url;
    @Basic
    String generator;
    @Basic
    String expander;
    @Basic
    @Column(name = "EDITIONTYPE")
    String editionType;
    @Basic
    @Column(name = "FILTER_ID")
    Long filterId = null;
    @OneToMany(targetEntity = PSContentListGeneratorParam.class, cascade =  {
        CascadeType.ALL}
    , fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "CONTENT_LIST_ID")
    @Fetch(FetchMode.SUBSELECT)
    Set<PSContentListGeneratorParam> generatorArguments = new HashSet<>();
    @OneToMany(targetEntity = PSTemplateExpanderParam.class, cascade =  {
        CascadeType.ALL}
    , fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "CONTENT_LIST_ID")
    @Fetch(FetchMode.SUBSELECT)
    Set<PSTemplateExpanderParam> expanderArguments = new HashSet<>();

    /**
     * The item filter as a transient object, can only exist when the
     * Content List object is loaded from service layer; otherwise it is
     * <code>null</code> (as not defined).
     */
    transient IPSItemFilter m_filter = null;

    /* (non-Javadoc)
     * @see com.percussion.services.publisher.IPSContentList#getGeneratorParams()
     */
    @JsonIgnore
    @IPSXmlSerialization(suppress = true)
    public Map<String, String> getGeneratorParams() {
        Map<String, String> rval = new HashMap<>();

        if (generatorArguments != null) {
            for (PSContentListGeneratorParam p : generatorArguments) {
                rval.put(p.getName(), p.getValue());
            }
        }

        return rval;
    }

    /**
     * Set the generator arguments. This method carefully folds the new arguments
     * into the old arguments.
     *
     * @param newargs the new arguments, never <code>null</code>
     */
    public void setGeneratorParams(Map<String, String> newargs) {
        if (newargs == null) {
            throw new IllegalArgumentException("newargs may not be null");
        }

        // before accessing, do the check if the generatorArguments is valid
        if (generatorArguments == null) {
            return;
        }

        // First remove any old argument that no longer belongs
        Set<String> removals = new HashSet<>();

        for (PSContentListGeneratorParam param : generatorArguments) {
            if (!newargs.keySet().contains(param.getName())) {
                removals.add(param.getName());
            }
        }

        for (String n : removals) {
            removeGeneratorParam(n);
        }

        // Add or modify existing, the add method takes care of this
        for (String n : newargs.keySet()) {
            String value = newargs.get(n);
            addGeneratorParam(n, value);
        }
    }

    /*
     * (non-Javadoc)
     * @see com.percussion.services.publisher.IPSContentList#getExpanderParams()
     */
    @JsonIgnore
    @IPSXmlSerialization(suppress = true)
    public Map<String, String> getExpanderParams() {
        Map<String, String> rval = new HashMap<>();

        if (expanderArguments != null) {
            for (PSTemplateExpanderParam p : expanderArguments) {
                rval.put(p.getName(), p.getValue());
            }
        }

        return rval;
    }

    /**
     * Set the expander arguments. This method carefully folds the new arguments
     * into the old arguments.
     *
     * @param newargs the new arguments, never <code>null</code>
     */
    public void setExpanderParams(Map<String, String> newargs) {
        if (newargs == null) {
            throw new IllegalArgumentException("newargs may not be null");
        }

        // before accessing, do the check if the expanderArguments is valid
        if (expanderArguments == null) {
            return;
        }

        // First remove any old argument that no longer belongs
        Set<String> removals = new HashSet<>();

        for (PSTemplateExpanderParam param : expanderArguments) {
            if (!newargs.keySet().contains(param.getName())) {
                removals.add(param.getName());
            }
        }

        for (String n : removals) {
            removeExpanderParam(n);
        }

        // Add or modify existing, the add method takes care of this
        for (String n : newargs.keySet()) {
            String value = newargs.get(n);
            addExpanderParam(n, value);
        }
    }

    /* Internal implementation required by IPSContentList */
    public void setExpanderParamsImpl(Map<String, String> newargs) {
        setExpanderParams(newargs);
    }

    /* Internal implementation required by IPSContentList */
    public void setGeneratorParamsImpl(Map<String, String> newargs) {
        setGeneratorParams(newargs);
    }

    /**
     * Internal implementation used by the interface for removing an expander param.
     */
    public void removeExpanderParamImpl(String name) {
        if (StringUtils.isBlank(name) || expanderArguments == null) {
            return;
        }
        expanderArguments.removeIf(param -> name.equals(param.getName()));
    }

    /*
     * (non-Javadoc)
     * @see com.percussion.services.publisher.IPSContentList#addGeneratorParam(java.lang.String,
     *      java.lang.String)
     */
    public void addGeneratorParam(String n, String value) {
        if (StringUtils.isBlank(n)) {
            throw new IllegalArgumentException("name may not be null or empty");
        }

        if (StringUtils.isBlank(value)) {
            removeGeneratorParam(n);

            return;
        }

        if (generatorArguments != null) {
            for (PSContentListGeneratorParam p : generatorArguments) {
                if (p.getName().equals(n)) {
                    p.setValue(value);

                    return;
                }
            }
        }

        IPSGuidManager mgr = PSGuidManagerLocator.getGuidMgr();
        PSContentListGeneratorParam newparam = new PSContentListGeneratorParam();
        newparam.setId(mgr.createGuid(PSTypeEnum.INTERNAL).longValue());
        newparam.setName(n);
        newparam.setValue(value);
        newparam.setContentList(this);

        if (generatorArguments == null) {
            generatorArguments = new HashSet<>();
        }

        generatorArguments.add(newparam);
    }

    /*
     * (non-Javadoc)
     * @see com.percussion.services.publisher.IPSContentList#removeGeneratorParam(java.lang.String)
     */
    public void removeGeneratorParam(String n) {
        if (StringUtils.isBlank(n)) {
            throw new IllegalArgumentException("name may not be null or empty");
        }

        if (generatorArguments == null) {
            return;
        }

        PSContentListGeneratorParam found = null;

        for (PSContentListGeneratorParam p : generatorArguments) {
            if (p.getName().equals(n)) {
                found = p;

                break;
            }
        }

        if (found != null) {
            generatorArguments.remove(found);
        }
    }

    /* Internal implementation required by IPSContentList */
    public void addGeneratorParamImpl(String name, String value) {
        addGeneratorParam(name, value);
    }

    /* Internal implementation required by IPSContentList */
    public void removeGeneratorParamImpl(String name) {
        removeGeneratorParam(name);
    }

    /* (non-Javadoc)
     * @see com.percussion.services.publisher.IPSContentList#addExpanderParam(java.lang.String,
     *      java.lang.String)
     */
    public void addExpanderParam(String n, String value) {
        addExpanderParamImpl(n, value);
    }

    /**
     * Internal implementation for adding expander param required by the interface.
     */
    public void addExpanderParamImpl(String n, String value) {
        if (StringUtils.isBlank(n)) {
            throw new IllegalArgumentException("name may not be null or empty");
        }

        if (StringUtils.isBlank(value)) {
            removeExpanderParam(n);

            return;
        }

        if (expanderArguments != null) {
            for (PSTemplateExpanderParam p : expanderArguments) {
                if (p.getName().equals(n)) {
                    p.setValue(value);

                    return;
                }
            }
        }

        IPSGuidManager mgr = PSGuidManagerLocator.getGuidMgr();
        PSTemplateExpanderParam newparam = new PSTemplateExpanderParam();
        newparam.setId(mgr.createGuid(PSTypeEnum.INTERNAL).longValue());
        newparam.setName(n);
        newparam.setValue(value);
        newparam.setContentList(this);

        if (expanderArguments == null) {
            expanderArguments = new HashSet<>();
        }

        expanderArguments.add(newparam);
    }

    /*
     * (non-Javadoc)
     * @see com.percussion.services.publisher.IPSContentList#removeExpanderParam(java.lang.String)
     */
    public void removeExpanderParam(String n) {
        if (StringUtils.isBlank(n)) {
            throw new IllegalArgumentException("name may not be null or empty");
        }

        if (expanderArguments == null) {
            return;
        }

        PSTemplateExpanderParam found = null;

        for (PSTemplateExpanderParam p : expanderArguments) {
            if (p.getName().equals(n)) {
                found = p;

                break;
            }
        }

        if (found != null) {
            expanderArguments.remove(found);
        }
    }

    /**
     * Get the content list id, only used in serialization
     *
     * @return the content list id, never <code>null</code> for a persisted
     *         object, may be <code>null</code> otherwise
     */
    @JsonProperty
    public long getContentListId() {
        return contentListId;
    }

    /**
     * @param contentListId
     */
    public void setContentListId(long contentListId) {
        this.contentListId = contentListId;
    }

    /**
     * Binary-compatible overload for older callers that passed {@link Integer}.
     *
     * @param contentListId may be {@code null} (treated as 0)
     */
    public void setContentListId(Integer contentListId) {
        this.contentListId = contentListId == null ? 0L : contentListId.longValue();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.percussion.services.publisher.IPSContentList#getDescription()
     */
    @JsonProperty
    public String getDescription() {
        return description;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.percussion.services.publisher.IPSContentList#setDescription(java.lang.String)
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.percussion.services.publisher.IPSContentList#getEditionType()
     */
    @JsonProperty("edition-type")
    public PSEditionType getEditionType() {
        if (editionType == null) {
            editionType = "2"; // Default
        }

        Integer et = Integer.valueOf(editionType);

        return PSEditionType.valueOf(et);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.percussion.services.publisher.IPSContentList#setEditionType(com.percussion.services.publisher.data.PSEditionType)
     */
    public void setEditionType(PSEditionType editionType) {
        this.editionType = Integer.toString(editionType.getTypeId());
    }

    /*
     * (non-Javadoc)
     *
     * @see com.percussion.services.publisher.IPSContentList#getExpander()
     */
    @JsonProperty
    public String getExpander() {
        return expander;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.percussion.services.publisher.IPSContentList#setExpander(java.lang.String)
     */
    public void setExpander(String expander) {
        this.expander = expander;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.percussion.services.publisher.IPSContentList#getGenerator()
     */
    @JsonProperty
    public String getGenerator() {
        return generator;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.percussion.services.publisher.IPSContentList#setGenerator(java.lang.String)
     */
    public void setGenerator(String generator) {
        this.generator = generator;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.percussion.services.publisher.IPSContentList#getName()
     */
    @JsonProperty
    public String getName() {
        return name;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.percussion.services.publisher.IPSContentList#setName(java.lang.String)
     */
    public void setName(String name) {
        this.name = name;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.percussion.services.publisher.IPSContentList#getUrl()
     */
    @JsonProperty
    public String getUrl() {
        return url;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.percussion.services.publisher.IPSContentList#setUrl(java.lang.String)
     */
    public void setUrl(String url) {
        if (StringUtils.isBlank(url)) {
            throw new IllegalArgumentException("url may not be null or empty");
        }

        this.url = url;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#clone()
     */
    @Override
    public PSContentList clone() {
        PSContentList newclist = new PSContentList();

        newclist.setDescription(getDescription());
        newclist.setEditionType(getEditionType());
        newclist.setExpander(getExpander());
        newclist.setGenerator(getGenerator());
        newclist.setExpanderParams(getExpanderParams());
        newclist.setGeneratorParams(getGeneratorParams());
        newclist.setFilterId(getFilterId());
        newclist.setUrl(getUrl());
        newclist.setContentListTypeImpl(getContentListType());
        newclist.setContentListId((int) PSGuidHelper.generateNextLong(
                PSTypeEnum.CONTENT_LIST));
        newclist.setName("copied" + newclist.getContentListId());

        return newclist;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object arg0) {
        if (!(arg0 instanceof PSContentList)) {
            return false;
        }

        PSContentList cl = (PSContentList) arg0;

        return new EqualsBuilder().append(type, cl.type)
                                  .append(description, cl.description)
                                  .append(editionType, cl.editionType)
                                  .append(expander, cl.expander)
                                  .append(filterId, cl.filterId)
                                  .append(generator, cl.generator)
                                  .append(expanderArguments,
            cl.expanderArguments)
                                  .append(generatorArguments,
            cl.generatorArguments).append(name, cl.name).append(url, cl.url)
                                  .isEquals();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return (name != null) ? name.hashCode() : (-1);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("PSContentList{");
        sb.append("contentListId=").append(contentListId);
        sb.append(", version=").append(version);
        sb.append(", name='").append(name).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append(", type=").append(type);
        sb.append(", url='").append(url).append('\'');
        sb.append(", generator='").append(generator).append('\'');
        sb.append(", expander='").append(expander).append('\'');
        sb.append(", editionType='").append(editionType).append('\'');
        sb.append(", filterId=").append(filterId);
        sb.append(", generatorArguments=").append(generatorArguments);
        sb.append(", expanderArguments=").append(expanderArguments);
        sb.append(", m_filter=").append(m_filter);
        sb.append('}');
        return sb.toString();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.percussion.services.catalog.IPSCatalogItem#toXML()
     */
    public String toXML() throws IOException, SAXException {
        return PSXmlSerializationHelper.writeToXml(this);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.percussion.services.catalog.IPSCatalogItem#fromXML(java.lang.String)
     */
    public void fromXML(String xmlsource) throws IOException, SAXException {
        PSXmlSerializationHelper.readFromXML(xmlsource, this);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.percussion.services.catalog.IPSCatalogItem#getGUID()
     */
    @JsonProperty("guid")
    public IPSGuid getGUID() {
        return new PSGuid(PSTypeEnum.CONTENT_LIST, contentListId);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.percussion.services.catalog.IPSCatalogItem#setGUID(com.percussion.utils.guid.IPSGuid)
     */
    public void setGUID(IPSGuid newguid) throws IllegalStateException {
        // Allow overwrite on design-object XML restore (BeanUtils + Jackson); same pattern as
        // PSKeyword#setGUID (issue #1919).
        if (newguid == null) {
            throw new IllegalArgumentException("newguid may not be null");
        }
        contentListId = newguid.longValue();
    }

    /**
     * @return Returns the version.
     */
    @JsonIgnore
    @IPSXmlSerialization(suppress = true)
    public Integer getVersion() {
        return version;
    }

    /**
     * @param version The version to set.
     */
    public void setVersion(Integer version) {
        this.version = version;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.percussion.services.publisher.IPSContentList#getFilter()
     */
    @JsonProperty("filter-id")
    public IPSGuid getFilterId() {
        if (filterId == null) {
            return null;
        } else {
            // Offline-safe assemble (avoid PSGuidUtils/locator in unit tests).
            return new PSGuid(PSTypeEnum.ITEM_FILTER, filterId);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.percussion.services.publisher.IPSContentList#setFilter(com.percussion.services.filter.IPSItemFilter)
     */
    public void setFilterId(IPSGuid filter) {
        if (filter == null) {
            this.filterId = null;
        } else {
            this.filterId = filter.longValue();
        }

        m_filter = null;
    }

    @JsonIgnore
    @IPSXmlSerialization(suppress = true)
    public IPSItemFilter getFilter() {
        return m_filter;
    }

    public void setFilter(IPSItemFilter filter) {
        if (filter == null) {
            setFilterId(null);
        } else {
            setFilterId(filter.getGUID());
        }

        m_filter = filter;
    }

    @Override
    public void setNameImpl(String name) {
        this.name = name;
    }

    /*
     * (non-Javadoc)
     * @see com.percussion.services.publisher.IPSContentList#getType()
     */
    @Override
    @JsonIgnore
    public String getType() {
        return PSTypeEnum.CONTENT_LIST.name();
    }

    @JsonProperty("content-list-type")
    public Type getContentListType() {
        int tordinal = (type == null) ? 0 : type.shortValue();
        return Type.valueOf(tordinal);
    }

    /**
     * Jackson / design-object restore for {@link #getContentListType()}.
     *
     * @param newtype never {@code null}
     */
    public void setContentListType(Type newtype) {
        setContentListTypeImpl(newtype);
    }

    @Override
    public void setUrlImpl(String url) {
        this.url = url;
    }

    @Override
    public void setContentListTypeImpl(Type newtype) {
        if (newtype == null) {
            throw new IllegalArgumentException("newtype may not be null");
        }
        type = newtype.ordinal();
    }

    /*
     * Backwards-compatible setter that remains for binary compatibility
     */
    @JsonIgnore
    public void setType(Type newtype) {
        setContentListTypeImpl(newtype);
    }

    /**
     * Generator argument beans (unordered set for Hibernate / mutators).
     *
     * @return never {@code null}
     */
    @JsonIgnore
    public Set<PSContentListGeneratorParam> getGeneratorArguments() {
        if (generatorArguments == null) {
            generatorArguments = new HashSet<>();
        }
        return generatorArguments;
    }

    /**
     * Stable-order generator arguments for design-object XML (sorted by name).
     *
     * @return never {@code null}
     */
    @JsonProperty("generator-arguments")
    @JacksonXmlElementWrapper(localName = "generator-arguments")
    @JacksonXmlProperty(localName = "content-list-generator-param")
    public java.util.List<PSContentListGeneratorParam> getGeneratorArgumentsXml() {
        return getGeneratorArguments().stream()
            .sorted(
                java.util.Comparator.comparing(
                    p -> p.getName() == null ? "" : p.getName(), String.CASE_INSENSITIVE_ORDER))
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Restore generator argument beans from design-object XML.
     *
     * @param args may be {@code null} (treated as empty)
     */
    public void setGeneratorArgumentsXml(java.util.List<PSContentListGeneratorParam> args) {
        this.generatorArguments = new HashSet<>();
        if (args != null) {
            for (PSContentListGeneratorParam p : args) {
                if (p != null) {
                    p.setContentList(this);
                    this.generatorArguments.add(p);
                }
            }
        }
    }

    /**
     * Expander argument beans (unordered set for Hibernate / mutators).
     *
     * @return never {@code null}
     */
    @JsonIgnore
    public Set<PSTemplateExpanderParam> getExpanderArguments() {
        if (expanderArguments == null) {
            expanderArguments = new HashSet<>();
        }
        return expanderArguments;
    }

    /**
     * Stable-order expander arguments for design-object XML (sorted by name).
     *
     * @return never {@code null}
     */
    @JsonProperty("expander-arguments")
    @JacksonXmlElementWrapper(localName = "expander-arguments")
    @JacksonXmlProperty(localName = "template-expander-param")
    public java.util.List<PSTemplateExpanderParam> getExpanderArgumentsXml() {
        return getExpanderArguments().stream()
            .sorted(
                java.util.Comparator.comparing(
                    p -> p.getName() == null ? "" : p.getName(), String.CASE_INSENSITIVE_ORDER))
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Restore expander argument beans from design-object XML.
     *
     * @param args may be {@code null} (treated as empty)
     */
    public void setExpanderArgumentsXml(java.util.List<PSTemplateExpanderParam> args) {
        this.expanderArguments = new HashSet<>();
        if (args != null) {
            for (PSTemplateExpanderParam p : args) {
                if (p != null) {
                    p.setContentList(this);
                    this.expanderArguments.add(p);
                }
            }
        }
    }

    /**
     * Legacy computed flag — not part of design-object XML.
     */
    @JsonIgnore
    @Override
    public boolean isLegacy() {
        return StringUtils.isBlank(generator)
            && StringUtils.isBlank(expander)
            && (filterId == null);
    }
}
