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
package com.percussion.services.sitemgr.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.assembly.PSAssemblyServiceLocator;
import com.percussion.services.assembly.data.PSAssemblyTemplate;
import com.percussion.services.catalog.IPSCatalogItem;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.PSGuidHelper;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.sitemgr.IPSPublishingContext;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.utils.xml.PSXmlSerializationHelper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.xml.IPSXmlSerialization;
import com.percussion.utils.xml.PSInvalidXmlException;
import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import org.xml.sax.SAXException;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Site data entity representing a logical (and physical) place to publish content. The site is
 * associated with a portion of the site folder tree in the repository and publishes to a specific
 * publisher.
 *
 * <p>Design-object XML root is {@code site}. Nested package element {@code site-property} and
 * {@code template-id} are registered via {@link PSXmlSerializationHelper#addType}. Associated
 * templates are suppressed as full objects; wire form uses {@link #getTemplateIds()}. Jackson
 * opt-in surface (issue #1918 / #1892 / epic #505).
 *
 * @author dougrand (original)
 * @author Sunny Sal (Java 11 refactoring)
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSSite")
@NaturalIdCache
@Table(name = "RXSITES")
@JacksonXmlRootElement(localName = "site")
@JsonAutoDetect(
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
    creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder({
  "baseUrl",
  "canonicalDist",
  "defaultDocument",
  "defaultFileExtension",
  "defaultPubServer",
  "description",
  "folderRoot",
  "generateSiteMapOptions",
  "globalTemplate",
  "guid",
  "ipAddress",
  "name",
  "navTheme",
  "port",
  "previousName",
  "properties",
  "root",
  "siteId",
  "siteProtocol",
  "templateIds",
  "unpublishFlags"
})
public class PSSite implements IPSSite, IPSCatalogItem {

    private static final long serialVersionUID = 1L;

    static {
        // Register types with XML serializer for read creation of objects
        PSXmlSerializationHelper.addType("site-property", PSSiteProperty.class);
        PSXmlSerializationHelper.addType("template-id", PSGuid.class);
    }

    @Id
    @Column(name = "SITEID")
    private Long siteId;

    @Basic
    @NaturalId(mutable = true)
    @Column(name = "SITENAME")
    private String name;

    @Basic
    @Column(name = "SITEDESC")
    private String description;

    @Basic
    @Column(name = "ROOT")
    private String root;

    @Basic
    @Column(name = "BASEURL")
    private String baseUrl;

    @Basic
    @Column(name = "IPADDRESS")
    private String ipAddress;

    @Basic
    @Column(name = "PORT")
    private Integer port;

    @Basic
    @Column(name = "USERID")
    private String userId;

    @Basic
    @Column(name = "PASSWORD")
    private String password;

    @Basic
    @Column(name = "PRIVATE_KEY")
    private String privateKey;

    @Basic
    @Column(name = "STATE")
    private Integer state;

    @Basic
    @Column(name = "NAV_THEME")
    private String navTheme;

    @Basic
    @Column(name = "FOLDER_ROOT")
    private String folderRoot;

    @Basic
    @Column(name = "GLOBALTEMPLATE")
    private String globalTemplate;

    @Basic
    @Column(name = "ALLOWED_NAMESPACES")
    private String allowedNamespaces;

    @Basic
    @Column(name = "PREVSITENAME")
    private String previousName;

    @Basic
    @Column(name = "IS_SECURE")
    private String is_secure;

    @Basic
    @Column(name = "DEFAULT_PUBSERVERID")
    private Long defaultPubServer;

    @Basic
    @Column(name = "DEFAULT_FILE_EXT")
    private String defaultFileExtention;

    @Basic
    @Column(name = "IS_CANONICAL")
    private String is_canonical;

    @Basic
    @Column(name = "SITE_PROTOCOL")
    private String siteProtocol;

    @Basic
    @Column(name = "DEFAULT_DOCUMENT")
    private String defaultDocument;

    @Basic
    @Column(name = "CANONICAL_DIST")
    private String canonicalDist;

    @Basic
    @Column(name = "IS_CANONICAL_REPLACE")
    private String is_canonical_replace;

    @Basic
    @Column(name = "ADDL_HEAD_CONTENT")
    private String siteAdditionalHeadContent;

    @Basic
    @Column(name = "BEFORE_BODY_CLOSE")
    private String siteBeforeBodyCloseContent;

    @Basic
    @Column(name = "AFTER_BODY_START")
    private String siteAfterBodyOpenContent;

    @Basic
    @Column(name = "LOGIN_PAGE")
    private String loginPage;

    @Basic
    @Column(name = "REGISTRATION_PAGE")
    private String registrationPage;

    @Basic
    @Column(name = "REGISTRATION_CONFIRMATION_PAGE")
    private String registrationConfirmationPage;

    @Basic
    @Column(name = "RESET_PAGE")
    private String resetPage;

    @Basic
    @Column(name = "RESET_REQUEST_PASSWORD_PAGE")
    private String resetRequestPasswordPage;

    @Version
    @Column(name = "VERSION")
    private Integer version;

    /**
     * Get the entity version used for optimistic locking.
     * @return the version or null if not set
     */
    @IPSXmlSerialization(suppress = true)
    @JsonIgnore
    public Integer getVersion()
    {
        return version;
    }

    /**
     * Set the version for this site (used by persistence frameworks).
     * @param version the version number
     */
    public void setVersion(Integer version)
    {
        this.version = version;
    }

    @Basic
    @Column(name = "GENERATE_SITEMAP")
    private String generateSiteMap;

    @Basic
    @Column(name = "GENERATE_SITEMAP_OPTIONS")
    private String generateSiteMapOptions;

    @Basic
    @Column(name = "ENABLE_MOBILE_PREVIEW")
    @Convert(converter = BooleanToTFCharConverter.class)
    private Boolean mobilePreviewEnabled;

    @Basic
    @Column(name = "OVERRIDE_JQUERY")
    @Convert(converter = BooleanToTFCharConverter.class)
    private Boolean overrideSystemJQuery;

    @Basic
    @Column(name = "OVERRIDE_FOUNDATION")
    @Convert(converter = BooleanToTFCharConverter.class)
    private Boolean overrideSystemFoundation;

    @Basic
    @Column(name = "OVERRIDE_JQUERYUI")
    @Convert(converter = BooleanToTFCharConverter.class)
    private Boolean overrideSystemJQueryUI;

    @ManyToMany(targetEntity = PSAssemblyTemplate.class, fetch = FetchType.LAZY)
    @JoinTable(name = "PSX_VARIANT_SITE",
            joinColumns = @JoinColumn(name = "SITEID"),
            inverseJoinColumns = @JoinColumn(name = "VARIANTID"))
    @Fetch(FetchMode.SUBSELECT)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private Set<IPSAssemblyTemplate> templates = new HashSet<>();

    @Basic
    @Column(name = "UNPUBLISH_FLAGS")
    private String unpublishFlags;

    @Basic
    @Column(name = "IS_PAGE_BASED")
    @Convert(converter = BooleanToTFCharConverter.class)
    private Boolean pageBased;

    @OneToMany(targetEntity = PSSiteProperty.class,
            cascade = {CascadeType.ALL},
            fetch = FetchType.EAGER,
            orphanRemoval = true)
    @JoinColumn(name = "SITEID")
    @Fetch(FetchMode.SUBSELECT)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private Set<PSSiteProperty> properties = new HashSet<>();

    /**
     * Get site properties as a set, never null.
     * @return set of properties, may be empty
     */
    @JsonProperty
    @JacksonXmlElementWrapper(localName = "properties")
    @JacksonXmlProperty(localName = "site-property")
    public Set<PSSiteProperty> getProperties() {
        return properties == null ? Collections.emptySet() : properties;
    }

    /**
     * Set site properties (design-object XML restore and service callers).
     *
     * @param props may be {@code null} to clear
     */
    public void setProperties(Set<PSSiteProperty> props) {
        if (props == null) {
            if (properties != null) {
                properties.clear();
            } else {
                properties = new HashSet<>();
            }
            return;
        }
        if (properties == null) {
            properties = new HashSet<>();
        } else {
            properties.clear();
        }
        for (PSSiteProperty property : props) {
            if (property != null) {
                property.setSite(this);
                addProperty(property);
            }
        }
    }

    /**
     * Default constructor.
     */
    public PSSite() {
        // Default constructor
    }

    /**
     * Enhanced copy constructor using Java 11 patterns
     */
    public void copy(IPSSite isite) {
        Objects.requireNonNull(isite, "isite cannot be null");
        if (!(isite instanceof PSSite)) {
            throw new IllegalArgumentException("isite must be an instance of PSSite");
        }

        var site = (PSSite) isite;

        // Copy primitive properties
        this.allowedNamespaces = site.allowedNamespaces;
        this.baseUrl = site.baseUrl;
        this.description = site.description;
        this.folderRoot = site.folderRoot;
        this.globalTemplate = site.globalTemplate;
        this.ipAddress = site.ipAddress;
        this.name = site.name;
        this.previousName = site.previousName;
        this.navTheme = site.navTheme;
        this.password = site.password;
        this.port = site.port;
        this.root = site.root;
        this.state = site.state;
        this.userId = site.userId;
        this.is_secure = site.is_secure;
        this.defaultPubServer = site.defaultPubServer;
        this.defaultFileExtention = site.defaultFileExtention;
        this.is_canonical = site.is_canonical;
        this.siteProtocol = site.siteProtocol;
        this.defaultDocument = site.defaultDocument;
        this.canonicalDist = site.canonicalDist;
        this.is_canonical_replace = site.is_canonical_replace;
        this.generateSiteMap = site.generateSiteMap;
        this.generateSiteMapOptions = site.generateSiteMapOptions;

        // Copy collections using streams
        this.templates = new HashSet<>();
        site.templates.stream()
                .map(IPSAssemblyTemplate::getGUID)
                .forEach(this::addTemplateGuidToCollection);

        this.properties = new HashSet<>();
        site.properties.stream()
                .forEach(prop -> setProperty(prop.getName(), prop.getContextId(), prop.getValue()));
    }

    // Enhanced getters and setters with Java 11 patterns

    @Override
    @JsonProperty("guid")
    public IPSGuid getGUID() {
        return Optional.ofNullable(siteId)
                .map(id -> new PSGuid(PSTypeEnum.SITE, id))
                .orElse(null);
    }

    @Override
    public void setGUID(IPSGuid newguid) throws IllegalStateException {
        Objects.requireNonNull(newguid, "newguid cannot be null");
        if (newguid.getType() != PSTypeEnum.SITE.getOrdinal()) {
            throw new IllegalArgumentException("newguid must be a site guid");
        }
        // Allow overwrite on design-object XML restore (BeanUtils + Jackson)
        siteId = newguid.longValue();
    }

    @Override
    @JsonProperty
    public Long getSiteId() {
        return siteId;
    }

    public void setSiteId(Long siteId) {
        this.siteId = siteId;
    }

    @Override
    @JsonProperty
    public String getName() {
        return name;
    }

    @Override
    @JsonIgnore
    public String getLabel() {
        // Default label is the site name; return empty string if name is missing
        return Optional.ofNullable(name)
                .filter(n -> !n.trim().isEmpty())
                .orElse("");
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void setPreviousName(String previousName) {
        this.previousName = previousName;
    }

    @Override
    @JsonProperty
    public String getPreviousName() {
        return previousName;
    }

    @Override
    @JsonProperty
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    @JsonProperty
    public String getBaseUrl() {
        return baseUrl;
    }

    @Override
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    @JsonProperty
    public String getIpAddress() {
        return ipAddress;
    }

    @Override
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @Override
    @JsonProperty
    public String getFolderRoot() {
        return folderRoot;
    }

    @Override
    public void setFolderRoot(String folderRoot) {
        this.folderRoot = folderRoot;
    }

    @Override
    @JsonProperty
    public String getGlobalTemplate() {
        return globalTemplate;
    }

    @Override
    public void setGlobalTemplate(String globalTemplate) {
        this.globalTemplate = globalTemplate;
    }

    @Override
    @JsonProperty
    public Integer getPort() {
        return port;
    }

    @Override
    public void setPort(Integer port) {
        this.port = port;
    }

    @Override
    @JsonProperty
    public String getRoot() {
        return root;
    }

    @Override
    public void setRoot(String root) {
        this.root = root;
    }

    @Override
    @IPSXmlSerialization(suppress = true)
    @JsonIgnore
    public Set<IPSAssemblyTemplate> getAssociatedTemplates() {
        return new HashSet<>(templates);
    }

    @JsonIgnore
    public void setAssociatedTemplates(Set<IPSAssemblyTemplate> templates) {
        this.templates = Optional.ofNullable(templates)
            .orElse(Collections.emptySet());
    }

    /**
     * Template association GUIDs for design-object XML (string form). Full {@link
     * IPSAssemblyTemplate} graphs are suppressed; restore via {@link #setTemplateIds(Set)} requires
     * the assembly service to load templates.
     *
     * @return set of template guid strings, never null
     */
    @JsonProperty
    @JacksonXmlElementWrapper(localName = "template-ids")
    @JacksonXmlProperty(localName = "template-id")
    public Set<String> getTemplateIds() {
        Set<String> ids = new HashSet<>();
        if (templates != null && !templates.isEmpty()) {
            for (IPSAssemblyTemplate tmp : templates) {
                if (tmp != null && tmp.getGUID() != null) {
                    ids.add(tmp.getGUID().toString());
                }
            }
        }
        return ids.stream().sorted().collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * Sync associated templates from design-object XML string GUID set.
     *
     * @param newT may be null or empty to clear
     */
    public void setTemplateIds(Set<String> newT) {
        if (newT == null || newT.isEmpty()) {
            if (templates != null) {
                templates.clear();
            } else {
                templates = new HashSet<>();
            }
            return;
        }
        Set<IPSGuid> newTmps = new HashSet<>();
        for (String t : newT) {
            if (StringUtils.isNotBlank(t)) {
                newTmps.add(new PSGuid(t.trim()));
            }
        }

        if (templates == null) {
            templates = new HashSet<>();
        }

        // if the current template set is empty
        if (templates.isEmpty()) {
            for (IPSGuid guid : newTmps) {
                addTemplateGuidToCollection(guid);
            }
            return;
        }
        // get all existing tmp guids associated with this site
        Set<IPSGuid> curTmps = new HashSet<>();
        for (IPSAssemblyTemplate t : templates) {
            curTmps.add(t.getGUID());
        }
        Collection<IPSGuid> common = CollectionUtils.intersection(curTmps, newTmps);
        Collection<IPSGuid> remove = CollectionUtils.subtract(curTmps, newTmps);
        curTmps.removeAll(remove);
        newTmps.removeAll(common);
        curTmps.addAll(newTmps);
        templates.clear();

        for (IPSGuid guid : curTmps) {
            addTemplateGuidToCollection(guid);
        }
    }

    /**
     * Add a template by string GUID (Betwixt/Jackson adder path).
     *
     * @param tmpId string form of the guid, never blank
     */
    @JsonIgnore
    public void addTemplateId(String tmpId) {
        if (StringUtils.isBlank(tmpId)) {
            throw new IllegalArgumentException("template guid may not be null");
        }
        addTemplateId(new PSGuid(tmpId.trim()));
    }

    /**
     * Add a template by GUID if not already associated.
     *
     * @param id template GUID, never null
     */
    @JsonIgnore
    public void addTemplateId(IPSGuid id) {
        Objects.requireNonNull(id, "template guid may not be null");
        if (templates == null) {
            templates = new HashSet<>();
        }
        for (IPSAssemblyTemplate t : templates) {
            if (t.getGUID().equals(id)) {
                return;
            }
        }
        addTemplateGuidToCollection(id);
    }

    @Override
    @JsonProperty
    public Long getDefaultPubServer() {
        return defaultPubServer;
    }

    @Override
    public void setDefaultPubServer(Long defaultPubServer) {
        this.defaultPubServer = defaultPubServer;
    }

    @Override
    @JsonProperty
    public String getDefaultFileExtension() {
        return defaultFileExtention;
    }

    @Override
    public void setDefaultFileExtension(String defaultFileExtension) {
        this.defaultFileExtention = defaultFileExtension;
    }

    /**
     * Enhanced template management using Java 11 patterns
     */
    private void addTemplateGuidToCollection(IPSGuid guid) {
        Objects.requireNonNull(guid, "guid cannot be null");

        try {
            var assemblyService = PSAssemblyServiceLocator.getAssemblyService();
            var template = assemblyService.loadUnmodifiableTemplate(guid);
            templates.add(template);
        } catch (PSAssemblyException e) {
            throw new RuntimeException("Failed to load template: " + guid, e);
        }
    }

    /**
     * Enhanced property management using Java 11 patterns
     */
    @Override
    public PSSiteProperty setProperty(String name, IPSGuid contextId, String value) {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(contextId, "contextId cannot be null");

        // Look for an existing property
        PSSiteProperty prop = properties.stream()
                .filter(p -> Objects.equals(p.getName(), name)
                        && Objects.equals(p.getContextId(), contextId))
                .findFirst()
                .orElse(null);

        if (StringUtils.isBlank(value)) {
            if (prop != null) {
                properties.remove(prop);
            }
            return null;
        }

        if (prop == null) {
            prop = new PSSiteProperty();
            prop.setPropertyId(PSGuidHelper.generateNextLong(PSTypeEnum.SITE_PROPERTY));
            prop.setContextId(contextId);
            prop.setName(name);
            prop.setSite(this);
            properties.add(prop);
        }
        prop.setValue(value);
        return prop;
    }

    @Override
    public String getProperty(String name, IPSGuid contextId) {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(contextId, "contextId cannot be null");

        return properties.stream()
                .filter(prop -> Objects.equals(prop.getName(), name) &&
                        Objects.equals(prop.getContextId(), contextId))
                .map(PSSiteProperty::getValue)
                .findFirst()
                .orElse(null);
    }

    @Override
    public Set<String> getPropertyNames(IPSGuid contextId) {
        Objects.requireNonNull(contextId, "contextId cannot be null");
        var names = new HashSet<String>();
        for (PSSiteProperty prop : properties) {
            if (Objects.equals(prop.getContextId(), contextId)) {
                names.add(prop.getName());
            }
        }
        return names;
    }

    @Override
    @Deprecated
    public Set<String> getPropertyNames(IPSPublishingContext context) {
        Objects.requireNonNull(context, "context cannot be null");
        return getPropertyNames(context.getGUID());
    }

    public String getProperty(String propname, IPSPublishingContext context) {
        return getProperty(propname, context.getGUID());
    }

    /**
     * Add a single property to the Site.
     *
     * @param prop the SiteProperty to add, may be  <code>null</code>
     */
    public void addProperty(PSSiteProperty prop) {
        if (properties != null) {
            for (PSSiteProperty p : properties) {
                if (p.getPropertyId() == prop.getPropertyId()) {
                    p.setValue(prop.getValue());
                    p.setName(prop.getName());

                    //MSM deserialization fails to finish if p.getSite() is null
                    if (p.getSite() == null)
                        p.setSite(prop.getSite());
                    else if (!p.getSite().equals(prop.getSite()))
                        p.setSite(prop.getSite());

                    if (p.getContextId() == null)
                        p.setContextId(prop.getContextId());
                    else if (!p.getContextId().equals(prop.getContextId()))
                        p.setContextId(prop.getContextId());
                    return;
                }
            }
        }
        if (properties == null)
            properties = new HashSet<>();

        properties.add(prop);
    }


    /**
     * Property to remove
     *
     * @param propname the property name, never <code>null</code>
     */
    public void removeProperty(String propname) {
        if (StringUtils.isBlank(propname)) {
            throw new IllegalArgumentException("propname may not be null or empty");
        }
        PSSiteProperty found = null;
        for (PSSiteProperty p : properties) {
            if (p.getName().equals(propname)) {
                found = p;
                break;
            }
        }
        if (found != null) {
            properties.remove(found);
        }

    }

    /**
     * Removes a property by its id. This is not exposed through {@link IPSSite}.
     *
     * @param guid the GUID of the removed property.
     */
    public void removeProperty(IPSGuid guid) {
        if (guid == null)
            throw new IllegalArgumentException("guid must not be null.");

        long id = guid.longValue();
        PSSiteProperty found = null;
        for (PSSiteProperty p : properties) {
            if (p.getPropertyId() == id) {
                found = p;
                break;
            }
        }
        if (found != null) {
            properties.remove(found);
        }
    }

    /* Removed duplicate method - consolidated into the IPSSite-matching
     * implementation above which returns a PSSiteProperty and handles
     * find-or-create semantics consistently.
     */

    /* (non-Javadoc)
     * @see com.percussion.services.sitemgr.IPSSite#removeProperty(java.lang.String, com.percussion.services.sitemgr.IPSPublishingContext)
     */
    public void removeProperty(String propname, IPSGuid contextId) {
        PSSiteProperty prop = null;
        for (PSSiteProperty p : properties) {
            if (p.getName().equals(propname) && p.getContextId().equals(contextId)) {
                prop = p;
                break;
            }
        }
        if (prop != null) {
            properties.remove(prop);
        }
    }

    @Override
    public PSSiteProperty setProperty(String propname,
                                      IPSPublishingContext context, String value) {
        return setProperty(propname, context.getGUID(), value);
    }

    public void removeProperty(String propname, IPSPublishingContext context) {
        removeProperty(propname, context.getGUID());
    }

    @Override
    @JsonProperty
    public String getSiteAdditionalHeadContent() {
        return siteAdditionalHeadContent;
    }

    @Override
    public void setSiteAdditionalHeadContent(String siteAdditionalHeadContent) {
        this.siteAdditionalHeadContent = siteAdditionalHeadContent;
    }

    @Override
    @JsonProperty
    public String getSiteBeforeBodyCloseContent() {
        return siteBeforeBodyCloseContent;
    }

    @Override
    public void setSiteBeforeBodyCloseContent(String siteBeforeBodyCloseContent) {
        this.siteBeforeBodyCloseContent = siteBeforeBodyCloseContent;
    }

    @Override
    @JsonProperty
    public String getSiteAfterBodyOpenContent() {
        return siteAfterBodyOpenContent;
    }

    @Override
    public void setSiteAfterBodyOpenContent(String siteAfterBodyOpenContent) {
        this.siteAfterBodyOpenContent = siteAfterBodyOpenContent;
    }

    @Override
    @JsonProperty
    public String getLoginPage() {
        return loginPage;
    }

    @Override
    public void setLoginPage(String loginPage) {
        this.loginPage = loginPage;
    }

    @Override
    @JsonProperty
    public String getRegistrationPage() {
        return registrationPage;
    }

    @Override
    public void setRegistrationPage(String registrationPage) {
        this.registrationPage = registrationPage;
    }

    @Override
    @JsonProperty
    public String getRegistrationConfirmationPage() {
        return registrationConfirmationPage;
    }

    @Override
    public void setRegistrationConfirmationPage(String registrationConfirmationPage) {
        this.registrationConfirmationPage = registrationConfirmationPage;
    }

    @Override
    @JsonProperty
    public String getResetPage() {
        return resetPage;
    }

    @Override
    public void setResetPage(String resetPage) {
        this.resetPage = resetPage;
    }

    @Override
    @JsonProperty
    public String getResetRequestPasswordPage() {
        return resetRequestPasswordPage;
    }

    @Override
    public void setResetRequestPasswordPage(String resetRequestPasswordPage) {
        this.resetRequestPasswordPage = resetRequestPasswordPage;
    }

    @Override
    @JsonProperty
    public String getUserId() {
        return userId;
    }

    @Override
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    @JsonProperty
    public String getPassword() {
        return this.password;
    }

    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    @JsonProperty
    public String getPrivateKey() {
        return this.privateKey;
    }

    @Override
    public void setNavTheme(String navTheme) {
        this.navTheme = navTheme;
    }

    @Override
    @JsonProperty
    public String getNavTheme() {
        return this.navTheme;
    }

    @Override
    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    @JsonProperty
    public String getUnpublishFlags() {
        return StringUtils.isBlank(unpublishFlags) ? "u" : unpublishFlags;
    }

    public void setUnpublishFlags(String flags) {
        if (StringUtils.isBlank(flags))
            throw new IllegalArgumentException("flags may not be null or empty.");

        unpublishFlags = flags;
    }

    @Override
    @JsonProperty("mobilePreviewEnabled")
    public boolean isMobilePreviewEnabled() {
        return Boolean.TRUE.equals(mobilePreviewEnabled);
    }

    @Override
    public void setMobilePreviewEnabled(boolean mobilePreviewEnabled) {
        this.mobilePreviewEnabled = mobilePreviewEnabled;
    }

    @Override
    @JsonProperty("overrideSystemJQuery")
    public boolean isOverrideSystemJQuery() {
        return Boolean.TRUE.equals(overrideSystemJQuery);
    }

    @Override
    public void setOverrideSystemJQuery(boolean overrideSystemJQuery) {
        this.overrideSystemJQuery = overrideSystemJQuery;
    }

    @Override
    @JsonProperty("overrideSystemFoundation")
    public boolean isOverrideSystemFoundation() {
        return Boolean.TRUE.equals(overrideSystemFoundation);
    }

    @Override
    public void setOverrideSystemFoundation(boolean overrideSystemFoundation) {
        this.overrideSystemFoundation = overrideSystemFoundation;
    }

    @Override
    @JsonProperty("overrideSystemJQueryUI")
    public boolean isOverrideSystemJQueryUI() {
        return Boolean.TRUE.equals(overrideSystemJQueryUI);
    }

    @Override
    public void setOverrideSystemJQueryUI(boolean overrideSystemJQueryUI) {
        this.overrideSystemJQueryUI = overrideSystemJQueryUI;
    }

    @Override
    @JsonProperty("pageBased")
    public boolean isPageBased() {
        return Boolean.TRUE.equals(pageBased);
    }

    @Override
    public void setPageBased(boolean pageBasedSite) {
        this.pageBased = pageBasedSite;
    }

    @Override
    @JsonProperty("secure")
    public boolean isSecure() {
        return BooleanToTFCharConverter.isTruthy(is_secure);
    }

    @Override
    public void setSecure(boolean isSecure) {
        // CHAR(1) column — must be T/F, not Boolean.toString()
        this.is_secure = BooleanToTFCharConverter.toChar(isSecure);
    }

    @Override
    @JsonProperty("canonical")
    public boolean isCanonical() {
        return BooleanToTFCharConverter.isTruthy(is_canonical);
    }

    @Override
    public void setCanonical(boolean isCanonical) {
        // CHAR(1) column — must be T/F, not Boolean.toString() ("true" is 4 chars)
        this.is_canonical = BooleanToTFCharConverter.toChar(isCanonical);
    }

    @Override
    @JsonProperty("canonicalReplace")
    public boolean isCanonicalReplace() {
        return BooleanToTFCharConverter.isTruthy(is_canonical_replace);
    }

    @Override
    public void setAllowedNamespaces(String allowedNamespaces) {
        this.allowedNamespaces = allowedNamespaces;
    }

    @Override
    @JsonProperty
    public String getAllowedNamespaces() {
        return allowedNamespaces;
    }

    @Override
    public void setCanonicalReplace(boolean isCanonicalReplace) {
        // CHAR(1) column — must be T/F, not Boolean.toString()
        this.is_canonical_replace = BooleanToTFCharConverter.toChar(isCanonicalReplace);
    }

    @Override
    @JsonProperty
    public String getCanonicalDist() {
        return canonicalDist;
    }

    @Override
    public void setCanonicalDist(String canonicalDist) {
        this.canonicalDist = canonicalDist;
    }

    @Override
    @JsonProperty
    public String getDefaultDocument() {
        return defaultDocument;
    }

    @Override
    public void setDefaultDocument(String defaultDocument) {
        this.defaultDocument = defaultDocument;
    }

    @Override
    public void setGenerateSitemap(boolean generateSitemap) {
        // CHAR(1) column — must be T/F, not Boolean.toString()
        this.generateSiteMap = BooleanToTFCharConverter.toChar(generateSitemap);
    }

    @Override
    @JsonProperty("generateSitemap")
    public boolean isGenerateSitemap() {
        return BooleanToTFCharConverter.isTruthy(this.generateSiteMap);
    }

    @Override
    @Deprecated
    @JsonProperty
    public Integer getState() {
        return this.state;
    }

    @Override
    @Deprecated
    public void setState(Integer state) {
        this.state = state;
    }

    @Override
    public void setGenerateSiteMapOptions(String generateSiteMapOptions) {
        this.generateSiteMapOptions = generateSiteMapOptions;
    }

    @Override
    @JsonProperty
    public String getGenerateSiteMapOptions() {
        return this.generateSiteMapOptions;
    }

    @Override
    public void setSiteProtocol(String siteProtocol) {
        this.siteProtocol = siteProtocol;
    }

    @Override
    @JsonProperty
    public String getSiteProtocol() {
        return this.siteProtocol;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        var site = (PSSite) obj;
        return Objects.equals(siteId, site.siteId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(siteId);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("siteId", siteId)
                .append("name", name)
                .append("description", description)
                .toString();
    }

    /**
     * Historical root node name of the attribute-shaped XML representation (pre-helper). Retained
     * for callers that still reference the constant; modern writes use root {@code site}.
     */
    public static final String XML_NODE_NAME = "PSXSite";

    @Override
    public void fromXML(String xmlsource) throws IOException, SAXException, PSInvalidXmlException {
        PSXmlSerializationHelper.readFromXML(xmlsource, this);
    }

    @Override
    public String toXML() throws IOException, SAXException {
        return PSXmlSerializationHelper.writeToXml(this);
    }
}
