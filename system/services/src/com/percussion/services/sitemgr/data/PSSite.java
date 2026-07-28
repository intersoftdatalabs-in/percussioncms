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
import com.percussion.util.PSXMLDomUtil;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.xml.IPSXmlSerialization;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Collections;

import com.percussion.utils.xml.IPSXmlErrors;
import com.percussion.utils.xml.PSInvalidXmlException;
import com.percussion.utils.xml.PSXmlUtils;

import static com.percussion.util.PSBase64Decoder.decode;
import static com.percussion.util.PSBase64Encoder.encode;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Modern site data entity representing a logical (and physical) place to publish
 * content using Java 11 features. The site is associated with a portion of the
 * site folder tree in the repository and publishes to a specific publisher.
 *
 * <h2>Java 11 Enhancements</h2>
 * <ul>
 * <li>Enhanced validation with Objects.requireNonNull</li>
 * <li>Optional-based safe access for nullable properties</li>
 * <li>Stream API for efficient collection processing</li>
 * <li>Improved error handling patterns</li>
 * </ul>
 *
 * @author dougrand (original)
 * @author Sunny Sal (Java 11 refactoring)
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSSite")
@NaturalIdCache
@Table(name = "RXSITES")
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
    public Set<PSSiteProperty> getProperties() {
        return properties == null ? Collections.emptySet() : properties;
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
        if (siteId != null) {
            throw new IllegalStateException("siteId is already set");
        }
        siteId = newguid.longValue();
    }

    @Override
    public Long getSiteId() {
        return siteId;
    }

    public void setSiteId(Long siteId) {
        this.siteId = siteId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
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
    public String getPreviousName() {
        return previousName;
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
    public String getBaseUrl() {
        return baseUrl;
    }

    @Override
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public String getIpAddress() {
        return ipAddress;
    }

    @Override
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @Override
    public String getFolderRoot() {
        return folderRoot;
    }

    @Override
    public void setFolderRoot(String folderRoot) {
        this.folderRoot = folderRoot;
    }

    @Override
    public String getGlobalTemplate() {
        return globalTemplate;
    }

    @Override
    public void setGlobalTemplate(String globalTemplate) {
        this.globalTemplate = globalTemplate;
    }

    @Override
    public Integer getPort() {
        return port;
    }

    @Override
    public void setPort(Integer port) {
        this.port = port;
    }

    @Override
    public String getRoot() {
        return root;
    }

    @Override
    public void setRoot(String root) {
        this.root = root;
    }

    @Override
    public Set<IPSAssemblyTemplate> getAssociatedTemplates() {
        return new HashSet<>(templates);
    }

    public void setAssociatedTemplates(Set<IPSAssemblyTemplate> templates) {
        this.templates = Optional.ofNullable(templates)
            .orElse(Collections.emptySet());
    }

    @Override
    public Long getDefaultPubServer() {
        return defaultPubServer;
    }

    @Override
    public void setDefaultPubServer(Long defaultPubServer) {
        this.defaultPubServer = defaultPubServer;
    }

    @Override
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
    public String getSiteAdditionalHeadContent() {
        return siteAdditionalHeadContent;
    }

    @Override
    public void setSiteAdditionalHeadContent(String siteAdditionalHeadContent) {
        this.siteAdditionalHeadContent = siteAdditionalHeadContent;
    }

    @Override
    public String getSiteBeforeBodyCloseContent() {
        return siteBeforeBodyCloseContent;
    }

    @Override
    public void setSiteBeforeBodyCloseContent(String siteBeforeBodyCloseContent) {
        this.siteBeforeBodyCloseContent = siteBeforeBodyCloseContent;
    }

    @Override
    public String getSiteAfterBodyOpenContent() {
        return siteAfterBodyOpenContent;
    }

    @Override
    public void setSiteAfterBodyOpenContent(String siteAfterBodyOpenContent) {
        this.siteAfterBodyOpenContent = siteAfterBodyOpenContent;
    }

    @Override
    public String getLoginPage() {
        return loginPage;
    }

    @Override
    public void setLoginPage(String loginPage) {
        this.loginPage = loginPage;
    }

    @Override
    public String getRegistrationPage() {
        return registrationPage;
    }

    @Override
    public void setRegistrationPage(String registrationPage) {
        this.registrationPage = registrationPage;
    }

    @Override
    public String getRegistrationConfirmationPage() {
        return registrationConfirmationPage;
    }

    @Override
    public void setRegistrationConfirmationPage(String registrationConfirmationPage) {
        this.registrationConfirmationPage = registrationConfirmationPage;
    }

    @Override
    public String getResetPage() {
        return resetPage;
    }

    @Override
    public void setResetPage(String resetPage) {
        this.resetPage = resetPage;
    }

    @Override
    public String getResetRequestPasswordPage() {
        return resetRequestPasswordPage;
    }

    @Override
    public void setResetRequestPasswordPage(String resetRequestPasswordPage) {
        this.resetRequestPasswordPage = resetRequestPasswordPage;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getPrivateKey() {
        return this.privateKey;
    }

    @Override
    public void setNavTheme(String navTheme) {
        this.navTheme = navTheme;
    }

    @Override
    public String getNavTheme() {
        return this.navTheme;
    }

    @Override
    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getUnpublishFlags() {
        return StringUtils.isBlank(unpublishFlags) ? "u" : unpublishFlags;
    }

    public void setUnpublishFlags(String flags) {
        if (StringUtils.isBlank(flags))
            throw new IllegalArgumentException("flags may not be null or empty.");

        unpublishFlags = flags;
    }

    @Override
    public boolean isMobilePreviewEnabled() {
        return Boolean.TRUE.equals(mobilePreviewEnabled);
    }

    @Override
    public void setMobilePreviewEnabled(boolean mobilePreviewEnabled) {
        this.mobilePreviewEnabled = mobilePreviewEnabled;
    }

    @Override
    public boolean isOverrideSystemJQuery() {
        return Boolean.TRUE.equals(overrideSystemJQuery);
    }

    @Override
    public void setOverrideSystemJQuery(boolean overrideSystemJQuery) {
        this.overrideSystemJQuery = overrideSystemJQuery;
    }

    @Override
    public boolean isOverrideSystemFoundation() {
        return Boolean.TRUE.equals(overrideSystemFoundation);
    }

    @Override
    public void setOverrideSystemFoundation(boolean overrideSystemFoundation) {
        this.overrideSystemFoundation = overrideSystemFoundation;
    }

    @Override
    public boolean isOverrideSystemJQueryUI() {
        return Boolean.TRUE.equals(overrideSystemJQueryUI);
    }

    @Override
    public void setOverrideSystemJQueryUI(boolean overrideSystemJQueryUI) {
        this.overrideSystemJQueryUI = overrideSystemJQueryUI;
    }

    @Override
    public boolean isPageBased() {
        return Boolean.TRUE.equals(pageBased);
    }

    @Override
    public void setPageBased(boolean pageBasedSite) {
        this.pageBased = pageBasedSite;
    }

    @Override
    public boolean isSecure() {
        return BooleanToTFCharConverter.isTruthy(is_secure);
    }

    @Override
    public void setSecure(boolean isSecure) {
        // CHAR(1) column — must be T/F, not Boolean.toString()
        this.is_secure = BooleanToTFCharConverter.toChar(isSecure);
    }

    @Override
    public boolean isCanonical() {
        return BooleanToTFCharConverter.isTruthy(is_canonical);
    }

    @Override
    public void setCanonical(boolean isCanonical) {
        // CHAR(1) column — must be T/F, not Boolean.toString() ("true" is 4 chars)
        this.is_canonical = BooleanToTFCharConverter.toChar(isCanonical);
    }

    @Override
    public boolean isCanonicalReplace() {
        return BooleanToTFCharConverter.isTruthy(is_canonical_replace);
    }

    @Override
    public void setAllowedNamespaces(String allowedNamespaces) {
        this.allowedNamespaces = allowedNamespaces;
    }

    @Override
    public String getAllowedNamespaces() {
        return allowedNamespaces;
    }

    @Override
    public void setCanonicalReplace(boolean isCanonicalReplace) {
        // CHAR(1) column — must be T/F, not Boolean.toString()
        this.is_canonical_replace = BooleanToTFCharConverter.toChar(isCanonicalReplace);
    }

    @Override
    public String getCanonicalDist() {
        return canonicalDist;
    }

    @Override
    public void setCanonicalDist(String canonicalDist) {
        this.canonicalDist = canonicalDist;
    }

    @Override
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
    public boolean isGenerateSitemap() {
        return BooleanToTFCharConverter.isTruthy(this.generateSiteMap);
    }

    @Override
    @Deprecated
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
    public String getGenerateSiteMapOptions() {
        return this.generateSiteMapOptions;
    }

    @Override
    public void setSiteProtocol(String siteProtocol) {
        this.siteProtocol = siteProtocol;
    }

    @Override
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

    // XML serialization constants
    public static final String XML_NODE_NAME = "PSXSite";

    // private XML constants
    private static final String NAME_ATTR = "name";
    private static final String DESCRIPTION_ATTR = "description";
    private static final String BASEURL_ATTR = "baseUrl";
    private static final String ROOT_ATTR = "root";
    private static final String IPADDRESS_ATTR = "ipAddress";
    private static final String PORT_ATTR = "port";
    private static final String FOLDERROOT_ATTR = "folderRoot";
    private static final String NAVTHEME_ATTR = "navTheme";
    private static final String GLOBALTEMPLATE_ATTR = "globalTemplate";
    private static final String IS_PAGE_BASED_ATTR = "isPageBased";

    @Override
    public void fromXML(String xmlsource) throws IOException, SAXException, PSInvalidXmlException {
        if (xmlsource == null || xmlsource.trim().isEmpty()) {
            throw new IllegalArgumentException("xmlsource may not be null or empty");
        }

        Reader r = new StringReader(xmlsource);
        Document doc = PSXmlDocumentBuilder.createXmlDocument(r, false);
        NodeList nodes = doc.getElementsByTagName(XML_NODE_NAME);
        if (nodes.getLength() == 0) {
            throw new PSInvalidXmlException(IPSXmlErrors.XML_ELEMENT_MISSING, XML_NODE_NAME);
        }

        Element elem = (Element) nodes.item(0);

        String nameAttr = PSXmlUtils.checkAttribute(elem, NAME_ATTR, true);
        setName(nameAttr);

        String descr = PSXmlUtils.checkAttribute(elem, DESCRIPTION_ATTR, false);
        setDescription(descr.length() > 0 ? descr : null);

        setBaseUrl(PSXmlUtils.checkAttribute(elem, BASEURL_ATTR, false));
        setRoot(PSXmlUtils.checkAttribute(elem, ROOT_ATTR, false));
        setIpAddress(PSXmlUtils.checkAttribute(elem, IPADDRESS_ATTR, false));
        String portAttr = PSXmlUtils.checkAttribute(elem, PORT_ATTR, false);
        if (portAttr.length() > 0) {
            try {
                setPort(Integer.valueOf(portAttr));
            } catch (NumberFormatException ignore) {
                setPort(null);
            }
        }

        setFolderRoot(PSXmlUtils.checkAttribute(elem, FOLDERROOT_ATTR, false));
        setNavTheme(PSXmlUtils.checkAttribute(elem, NAVTHEME_ATTR, false));
        setGlobalTemplate(PSXmlUtils.checkAttribute(elem, GLOBALTEMPLATE_ATTR, false));

        setPageBased(Boolean.parseBoolean(PSXmlUtils.checkAttribute(elem, IS_PAGE_BASED_ATTR, false)));
    }

    @Override
    public String toXML() throws IOException, SAXException {
        Document doc = PSXmlDocumentBuilder.createXmlDocument();
        Element root = doc.createElement(XML_NODE_NAME);

        root.setAttribute(NAME_ATTR, getName());
        if (getDescription() != null) root.setAttribute(DESCRIPTION_ATTR, getDescription());
        if (getBaseUrl() != null) root.setAttribute(BASEURL_ATTR, getBaseUrl());
        if (getRoot() != null) root.setAttribute(ROOT_ATTR, getRoot());
        if (getIpAddress() != null) root.setAttribute(IPADDRESS_ATTR, getIpAddress());
        if (getPort() != null) root.setAttribute(PORT_ATTR, String.valueOf(getPort()));
        if (getFolderRoot() != null) root.setAttribute(FOLDERROOT_ATTR, getFolderRoot());
        if (getNavTheme() != null) root.setAttribute(NAVTHEME_ATTR, getNavTheme());
        if (getGlobalTemplate() != null) root.setAttribute(GLOBALTEMPLATE_ATTR, getGlobalTemplate());

        root.setAttribute(IS_PAGE_BASED_ATTR, Boolean.toString(isPageBased()));

        doc.appendChild(root);
        return PSXmlDocumentBuilder.toString(doc);
    }
}
