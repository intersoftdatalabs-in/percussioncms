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
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
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

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.percussion.util.PSBase64Decoder.decode;
import static com.percussion.util.PSBase64Encoder.encode;
import static org.apache.commons.lang.StringUtils.isBlank;

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
    @Column(name = "DESCRIPTION")
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
    @Column(name = "STATE")
    private Integer state;

    @Basic
    @Column(name = "NAVTHEME")
    private String navTheme;

    @Basic
    @Column(name = "FOLDERROOT")
    private String folderRoot;

    @Basic
    @Column(name = "GLOBALTEMPLATE")
    private String globalTemplate;

    @Basic
    @Column(name = "ALLOWEDNAMESPACES")
    private String allowedNamespaces;

    @Basic
    @Column(name = "PREVIOUSNAME")
    private String previousName;

    @Basic
    @Column(name = "ISSECURE")
    private String is_secure;

    @Basic
    @Column(name = "DEFAULTPUBSERVER")
    private String defaultPubServer;

    @Basic
    @Column(name = "DEFAULTFILEEXTENTION")
    private String defaultFileExtention;

    @Basic
    @Column(name = "ISCANONICAL")
    private String is_canonical;

    @Basic
    @Column(name = "SITEPROTOCOL")
    private String siteProtocol;

    @Basic
    @Column(name = "DEFAULTDOCUMENT")
    private String defaultDocument;

    @Basic
    @Column(name = "CANONICALDIST")
    private String canonicalDist;

    @Basic
    @Column(name = "ISCANONICALREPLACE")
    private String is_canonical_replace;

    @Basic
    @Column(name = "GENERATESITEMAP")
    private String generateSiteMap;

    @Basic
    @Column(name = "GENERATESITEMAPOPTIONS")
    private String generateSiteMapOptions;

    @Basic
    @Column(name = "MOBILEPREVIEWENABLED")
    private Boolean mobilePreviewEnabled;

    @Version
    @Column(name = "VERSION")
    private Integer version;

    @ManyToMany(targetEntity = PSAssemblyTemplate.class, fetch = FetchType.LAZY)
    @JoinTable(name = "PSX_VARIANT_SITE",
            joinColumns = @JoinColumn(name = "SITEID"),
            inverseJoinColumns = @JoinColumn(name = "VARIANTID"))
    @Fetch(FetchMode.SUBSELECT)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private Set<IPSAssemblyTemplate> templates = new HashSet<>();

    @OneToMany(targetEntity = PSSiteProperty.class,
            cascade = {CascadeType.ALL},
            fetch = FetchType.EAGER,
            orphanRemoval = true)
    @JoinColumn(name = "SITEID")
    @Fetch(FetchMode.SUBSELECT)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private Set<PSSiteProperty> properties = new HashSet<>();

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

    @Override
    public void setSiteId(Long siteId) {
        this.siteId = siteId;
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
    public String getBaseUrl() {
        return baseUrl;
    }

    @Override
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
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
    public Collection<IPSAssemblyTemplate> getAssociatedTemplates() {
        return new HashSet<>(templates);
    }

    @Override
    public void setAssociatedTemplates(Collection<IPSAssemblyTemplate> templates) {
        this.templates = Optional.ofNullable(templates)
                .map(HashSet::new)
                .orElse(new HashSet<>());
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
    public void setProperty(String name, IPSGuid contextId, String value) {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(contextId, "contextId cannot be null");

        // Remove existing property with same name and context
        properties.removeIf(prop ->
                Objects.equals(prop.getName(), name) &&
                        Objects.equals(prop.getContextId(), contextId)
        );

        // Add new property if value is not blank
        if (StringUtils.isNotBlank(value)) {
            var property = new PSSiteProperty();
            property.setName(name);
            property.setContextId(contextId);
            property.setValue(value);
            property.setSiteId(siteId);
            properties.add(property);
        }
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

    public PSSiteProperty setProperty(String propname, IPSGuid contextId,
                                      String value) {
        PSSiteProperty prop = null;
        for (PSSiteProperty p : properties) {
            if (p.getName().equals(propname) && p.getContextId().equals(contextId)) {
                prop = p;
                break;
            }
        }
        if (prop == null) {
            prop = new PSSiteProperty();
            prop.setPropertyId(
                    PSGuidHelper.generateNextLong(PSTypeEnum.SITE_PROPERTY));
            prop.setContextId(contextId);
            prop.setName(propname);
            prop.setSite(this);
            properties.add(prop);
        }
        prop.setValue(value);

        return prop;
    }

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

    public PSSiteProperty setProperty(String propname,
                                      IPSPublishingContext context, String value) {
        return setProperty(propname, context.getGUID(), value);
    }

    public void removeProperty(String propname, IPSPublishingContext context) {
        removeProperty(propname, context.getGUID());
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
}
