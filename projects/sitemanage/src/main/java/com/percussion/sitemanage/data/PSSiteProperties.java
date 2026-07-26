// REFACTORED: CP-JAVA11
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
package com.percussion.sitemanage.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.percussion.pathmanagement.data.PSFolderPermission;
import com.percussion.pathmanagement.data.PSGenerateSiteMapOptions;
import com.percussion.share.data.PSAbstractPersistantObject;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Optional;

/**
 * Contains the modifiable properties for a particular site.
 *
 * @author yubingchen
 */
@XmlRootElement(name = "SiteProperties")
public class PSSiteProperties extends PSAbstractPersistantObject {

  private static final long serialVersionUID = 1L;

  /**
   * Gets the site ID.
   *
   * @return the site ID, not blank for a valid site.
   */
  @XmlElement
  @Override
  public String getId() {
    return id;
  }

  /**
   * Sets the site ID.
   *
   * @param id the new ID of the site, not blank for a valid site.
   */
  @Override
  public void setId(String id) {
    this.id = id;
  }

  public Optional<String> getName() {
    return Optional.ofNullable(name);
  }

  public void setName(String name) {
    this.name = name;
  }

  public Optional<String> getDescription() {
    return Optional.ofNullable(description);
  }

  public void setDescription(String desc) {
    description = desc;
  }

  public Optional<String> getHomePageLinkText() {
    return Optional.ofNullable(homePageLinkText);
  }

  public void setHomePageLinkText(String linkTitle) {
    this.homePageLinkText = linkTitle;
  }

  public Optional<PSFolderPermission> getFolderPermission() {
    return Optional.ofNullable(folderPermission);
  }

  public void setFolderPermission(PSFolderPermission permission) {
    folderPermission = permission;
  }

  /**
   * The relative path to the sitewide loggin page information. For now this is used by Login
   * widget, and protected region feature in templates Eg: /index
   *
   * @author federicoromanelli
   * @return the path to the sitewide login page
   */
  public Optional<String> getLoginPage() {
    return Optional.ofNullable(loginPage);
  }

  /**
   * The relative path to the sitewide loggin page information. For now this is used by Login
   * widget, and protected region feature in templates Eg: /index
   *
   * @author federicoromanelli
   * @param loginPage - the path to the sitewide login page
   */
  public void setLoginPage(String loginPage) {
    this.loginPage = loginPage;
  }

  /**
   * The relative path to the sitewide loggin error page. For now this is used by Login widget, and
   * protected region feature in templates. When user is not able to loggin with the login widget,
   * and this error page is set then he's redirected to this page. Eg: /errorpage.html
   *
   * @author federicoromanelli
   * @return the path to the sitewide login error page
   */
  public Optional<String> getLoginErrorPage() {
    return Optional.ofNullable(loginErrorPage);
  }

  /**
   * The relative path to the sitewide loggin error page. For now this is used by Login widget, and
   * protected region feature in templates. When user is not able to loggin with the login widget,
   * and this error page is set then he's redirected to this page. Eg: /errorpage.html
   *
   * @author federicoromanelli
   * @param loginErrorPage - the path to the sitewide login error page
   */
  public void setLoginErrorPage(String loginErrorPage) {
    this.loginErrorPage = loginErrorPage;
  }

  /**
   * @return <code>true</code> if the site is secure. <code>false</code> otherwise.
   */
  @XmlElement(name = "isSecure")
  public boolean isSecure() {
    return isSecure;
  }

  public void setSecure(boolean is_secure) {
    this.isSecure = is_secure;
  }

  /**
   * The relative path to the sitewide registration page information. For now this is used by
   * Registration widget, and protected region feature in templates Eg: /index
   *
   * @author rafaelsalis
   * @return the path to the sitewide registration page
   */
  public Optional<String> getRegistrationPage() {
    return Optional.ofNullable(registrationPage);
  }

  /**
   * The relative path to the sitewide registration page information. For now this is used by
   * Registration widget, and protected region feature in templates Eg: /index
   *
   * @author rafaelsalis
   * @param registrationPage the path to the sitewide registration page
   */
  public void setRegistrationPage(String registrationPage) {
    this.registrationPage = registrationPage;
  }

  /**
   * The relative path to the sitewide registration confirmation page. Used by Registration widget.
   * Eg: /registration/registration_confirmation.html
   *
   * @author jshirai
   */
  public Optional<String> getRegistrationConfirmationPage() {
    return Optional.ofNullable(registrationConfirmationPage);
  }

  /**
   * The relative path to the sitewide registration confirmation page. Used by Registration widget.
   * Eg: /registration/registration_confirmation.html
   *
   * @author jshirai
   */
  public void setRegistrationConfirmationPage(String registrationConfirmationPage) {
    this.registrationConfirmationPage = registrationConfirmationPage;
  }

  /**
   * The relative path to the sitewide reset page information. For now this is used by Registration
   * widget, and protected region feature in templates Eg: /index
   *
   * @author rafaelsalis
   * @return the path to the sitewide reset page
   */
  public Optional<String> getResetPage() {
    return Optional.ofNullable(resetPage);
  }

  /**
   * The relative path to the sitewide reset page information. For now this is used by Registration
   * widget, and protected region feature in templates Eg: /index
   *
   * @author rafaelsalis
   * @param resetPage - the path to the sitewide registration page
   */
  public void setResetPage(String resetPage) {
    this.resetPage = resetPage;
  }

  /**
   * The relative path to the sitewide reset request password page information. For now this is used
   * by Login widget, and protected region feature in templates Eg: /index
   *
   * @author rafaelsalis
   * @return the path to the sitewide reset request password page
   */
  public Optional<String> getResetRequestPasswordPage() {
    return Optional.ofNullable(resetRequestPasswordPage);
  }

  /**
   * The relative path to the sitewide reset request password page information. For now this is used
   * by Login widget, and protected region feature in templates Eg: /index
   *
   * @author rafaelsalis
   * @param resetRequestPasswordPage - the path to the sitewide reset request password page
   */
  public void setResetRequestPasswordPage(String resetRequestPasswordPage) {
    this.resetRequestPasswordPage = resetRequestPasswordPage;
  }

  /**
   * @param cssClassNames the class names used with navigation widget.
   */
  public void setCssClassNames(String cssClassNames) {
    this.cssClassNames = cssClassNames;
  }

  /**
   * Gets the css class names of the section folder.
   *
   * @return the css class names used with navigation widget.
   */
  public Optional<String> getCssClassNames() {
    return Optional.ofNullable(cssClassNames);
  }

  /**
   * @param defaultFileExtention the default file extension used when creating a new page.
   */
  @XmlElement(name = "defaultFileExtention")
  public void setDefaultFileExtention(String defaultFileExtention) {
    this.defaultFileExtention = defaultFileExtention;
  }

  /**
   * Gets the default file extension.
   *
   * @return the default file extension used when creating a new page.
   */
  public Optional<String> getDefaultFileExtention() {
    return Optional.ofNullable(defaultFileExtention);
  }

  /**
   * Determines if canonical tags should be rendered or not during the publishing.
   *
   * @return <code>true</code> if the site is (marked) to render canonical tags. <code>false</code>
   *     otherwise.
   */
  @XmlElement(name = "isCanonical")
  public boolean isCanonical() {
    return isCanonical;
  }

  /**
   * Enable or disable canonical tags rendering.
   *
   * @param is_canonical <code>true</code> if enable rendering of canonical tags; otherwise disable
   *     rendering for the site.
   */
  public void setCanonical(boolean is_canonical) {
    this.isCanonical = is_canonical;
  }

  public Optional<String> getSiteProtocol() {
    return Optional.ofNullable(siteProtocol);
  }

  /**
   * @param siteProtocol the URLs' protocol ("http" or "https") used when rendering canonical tags.
   */
  public void setSiteProtocol(String siteProtocol) {
    this.siteProtocol = siteProtocol;
  }

  public Optional<String> getDefaultDocument() {
    return Optional.ofNullable(defaultDocument);
  }

  /**
   * @param defaultDocument the site's default document (like "index.html") used when rendering
   *     canonical tags.
   */
  public void setDefaultDocument(String defaultDocument) {
    this.defaultDocument = defaultDocument;
  }

  public Optional<String> getCanonicalDist() {
    return Optional.ofNullable(canonicalDist);
  }

  /**
   * @param canonicalDist the URLs' destination ("sections" or "pages") used when rendering
   *     canonical tags.
   */
  public void setCanonicalDist(String canonicalDist) {
    this.canonicalDist = canonicalDist;
  }

  /**
   * @return <code>true</code> if the site is (marked) to replace custom canonical tags. <code>false
   *     </code> otherwise.
   */
  @XmlElement(name = "isCanonicalReplace")
  public boolean isCanonicalReplace() {
    return isCanonicalReplace;
  }

  /**
   * Enable or disable replacing custom canonical tags with rendered.
   *
   * @param is_canonical_replace <code>true</code> if enable replacing of custom canonical tags with
   *     rendered; otherwise disable replacing for the site.
   */
  public void setCanonicalReplace(boolean is_canonical_replace) {
    this.isCanonicalReplace = is_canonical_replace;
  }

  /**
   * Determine if pubservers were changed as part of a save operation
   *
   * @return <code>true</code> if changed, <code>false</code> otherwise
   */
  public boolean isPubServersChanged() {
    return isPubServerChanged;
  }

  /**
   * See {@link #isPubServersChanged()}
   *
   * @param isPubServerChanged
   */
  public void setPubServersChanged(boolean isPubServerChanged) {
    this.isPubServerChanged = isPubServerChanged;
  }

  public boolean isOverrideSystemJQuery() {
    return overrideSystemJQuery;
  }

  public void setOverrideSystemJQuery(boolean overrideSystemJQuery) {
    this.overrideSystemJQuery = overrideSystemJQuery;
  }

  public boolean isOverrideSystemFoundation() {
    return overrideSystemFoundation;
  }

  public void setOverrideSystemFoundation(boolean overrideSystemFoundation) {
    this.overrideSystemFoundation = overrideSystemFoundation;
  }

  public boolean isOverrideSystemJQueryUI() {
    return overrideSystemJQueryUI;
  }

  public void setOverrideSystemJQueryUI(boolean overrideSystemJQueryUI) {
    this.overrideSystemJQueryUI = overrideSystemJQueryUI;
  }

  public boolean isMobilePreviewEnabled() {
    return mobilePreviewEnabled;
  }

  public void setMobilePreviewEnabled(boolean mobilePreviewEnabled) {
    this.mobilePreviewEnabled = mobilePreviewEnabled;
  }

  public Optional<String> getSiteAdditionalHeadContent() {
    return Optional.ofNullable(siteAdditionalHeadContent);
  }

  public void setSiteAdditionalHeadContent(String siteAdditionalHeadContent) {
    this.siteAdditionalHeadContent = siteAdditionalHeadContent;
  }

  public Optional<String> getSiteBeforeBodyCloseContent() {
    return Optional.ofNullable(siteBeforeBodyCloseContent);
  }

  public void setSiteBeforeBodyCloseContent(String siteBeforeBodyCloseContent) {
    this.siteBeforeBodyCloseContent = siteBeforeBodyCloseContent;
  }

  public Optional<String> getSiteAfterBodyOpenContent() {
    return Optional.ofNullable(siteAfterBodyOpenContent);
  }

  public void setSiteAfterBodyOpenContent(String siteAfterBodyOpenContent) {
    this.siteAfterBodyOpenContent = siteAfterBodyOpenContent;
  }

  public boolean isGenerateSiteMap() {
    return generateSiteMap;
  }

  public void setGenerateSiteMap(boolean generateSiteMap) {
    this.generateSiteMap = generateSiteMap;
  }

  public Optional<PSGenerateSiteMapOptions> getGenerateSiteMapOptions() {
    return Optional.ofNullable(generateSiteMapOptions);
  }

  public void setGenerateSiteMapOptions(PSGenerateSiteMapOptions generateSiteMapOptions) {
    this.generateSiteMapOptions = generateSiteMapOptions;
  }

  /** The relative path to the sitewide loggin page information. */
  private String loginPage;

  /** The relative path to the sitewide loggin error page. */
  private String loginErrorPage;

  /** The relative path to the sitewide registration page. */
  private String registrationPage;

  /** The relative path to the sitewide registration confirmation page. */
  private String registrationConfirmationPage;

  /** The relative path to the sitewide reset page. */
  private String resetPage;

  /** The relative path to the sitewide reset request password page. */
  private String resetRequestPasswordPage;

  /** See {@link #getFolderPermission()} for detail. */
  private PSFolderPermission folderPermission;

  /** See {@link #getHomePageLinkText()} for detail. */
  private String homePageLinkText;

  /** See {@link #getDescription()} for detail. */
  private String description;

  /** See {@link #getName()} for detail. */
  private String name;

  /** See {@link #getId()} for detail */
  private String id;

  /** See {@link #isSecure()} for detail */
  private boolean isSecure;

  /** Field to save the css class names used when rendering navigation widgets. */
  private String cssClassNames;

  /** Field to save the default file extension used when creating a new page. */
  private String defaultFileExtention;

  /** Determines if the site is marked to render the canonical tags or not. */
  private boolean isCanonical;

  /** Determines canonical URL's protocol ("http" or "https"). */
  String siteProtocol;

  /**
   * Determines the site's default document (like "index.html") used when rendering canonical tags.
   */
  String defaultDocument;

  /**
   * Determines where canonical URL should point: "sections"(mysite.com/mysection/) or
   * "pages"(mysite.com/mysection/index.html).
   */
  private String canonicalDist;

  /** Determines if the site is marked to replace custom canonical tags with rendered or not. */
  private boolean isCanonicalReplace;

  /** Transient flag to indicate if pubserver was modified during a save operation */
  private transient boolean isPubServerChanged;

  /***
   * Indicates that the system JQuery version should not be injected into any pages or Templates.
   */
  private boolean overrideSystemJQuery;

  /***
   * Indicates the the system Foundation version should not be injected into any Pages or Templates
   */
  private boolean overrideSystemFoundation;

  /***
   * Indicates that the system JQueryUI version should not be injected into Templates
   */
  private boolean overrideSystemJQueryUI;

  /***
   * Indicates if the mobile preview control is rendered on preview.
   */
  private boolean mobilePreviewEnabled;

  /** Indicates if a sitemap should be generated on full publish. */
  private boolean generateSiteMap;

  private PSGenerateSiteMapOptions generateSiteMapOptions;

  /***
   * Indicates head content that is global to all templates and pages on a site.
   */
  @JsonInclude(JsonInclude.Include.ALWAYS)
  private String siteAdditionalHeadContent;

  /***
   * Indicates Before Body close content that is globally injected into all Pages on a site.
   */
  @JsonInclude(JsonInclude.Include.ALWAYS)
  private String siteBeforeBodyCloseContent;

  /***
   * Indicates After Body Open content that is globally injected into all Pages on a site.
   */
  @JsonInclude(JsonInclude.Include.ALWAYS)
  private String siteAfterBodyOpenContent;
}
