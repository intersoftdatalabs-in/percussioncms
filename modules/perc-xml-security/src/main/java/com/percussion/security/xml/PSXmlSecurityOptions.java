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

package com.percussion.security.xml;

/**
 * Allows for the setting of XML security options.
 *
 * <p>Prefer the {@link #secure()} or {@link #secureWithDtd()} factory methods for safe defaults.
 * Note that {@link PSSecureXMLUtils} will <em>always</em> hard-disable external entities and
 * external parameter entities regardless of the values set here, as a defense-in-depth measure
 * against XXE (CWE-611).
 *
 * @author Percussion Software
 */
public class PSXmlSecurityOptions {

  /**
   * Creates a new options bundle with the given flags.
   *
   * @param enableExternalEntities whether external general entity references should be enabled;
   *     ignored by {@link PSSecureXMLUtils}, which always hard-disables them
   * @param enableDtdDeclarations whether {@code <!DOCTYPE>} declarations should be permitted by the
   *     secured parser
   * @param enableExternalDtdReferences whether the secured parser should fetch external DTDs
   * @param enableSecureProcessing whether the JAXP {@code secure-processing} feature should be
   *     turned on
   * @param enableExternalParameterEntities whether external parameter entity references should be
   *     enabled; ignored by {@link PSSecureXMLUtils}, which always hard-disables them
   * @param enableValidation whether the secured parser should perform DTD-based validation
   */
  public PSXmlSecurityOptions(
      boolean enableExternalEntities,
      boolean enableDtdDeclarations,
      boolean enableExternalDtdReferences,
      boolean enableSecureProcessing,
      boolean enableExternalParameterEntities,
      boolean enableValidation) {
    this.enableExternalEntities = enableExternalEntities;
    this.enableDtdDeclarations = enableDtdDeclarations;
    this.enableExternalDtdReferences = enableExternalDtdReferences;
    this.enableSecureProcessing = enableSecureProcessing;
    this.enableExternalParameterEntities = enableExternalParameterEntities;
    this.enableValidation = enableValidation;
  }

  /**
   * Returns the most restrictive secure defaults: no external entities, no DTDs, no external DTD
   * references, no external parameter entities, secure processing enabled, validation off.
   *
   * @return a new secure {@link PSXmlSecurityOptions} instance
   */
  public static PSXmlSecurityOptions secure() {
    return new PSXmlSecurityOptions(false, false, false, true, false, false);
  }

  /**
   * Returns secure defaults that still allow DTD declarations and local DTD loading. External
   * entities and external parameter entities remain disabled.
   *
   * <p>Use this when parsing Percussion CMS XML that relies on DTD-based entity definitions.
   *
   * @return a new secure {@link PSXmlSecurityOptions} instance with DTD support
   */
  public static PSXmlSecurityOptions secureWithDtd() {
    return new PSXmlSecurityOptions(false, true, true, true, false, false);
  }

  private boolean enableExternalEntities;
  private boolean enableDtdDeclarations;
  private boolean enableExternalDtdReferences;
  private boolean enableSecureProcessing;
  private boolean enableExternalParameterEntities;
  private boolean enableValidation;

  private String[] allowedPathsForDtdDeclarations;
  private String[] allowedPathsForExternalEntities;
  private String[] allowedPathsForImports;
  private String[] allowedPathsForIncludes;

  /**
   * Reports whether external general entity references should be enabled. Note that {@link
   * PSSecureXMLUtils} always hard-disables them regardless of this flag.
   *
   * @return {@code true} if external general entities are requested by the caller
   */
  public boolean isEnableExternalEntities() {
    return enableExternalEntities;
  }

  /**
   * Sets whether external general entity references should be enabled.
   *
   * @param enableExternalEntities {@code true} to request external general entity resolution; note
   *     that {@link PSSecureXMLUtils} overrides this to {@code false}
   */
  public void setEnableExternalEntities(boolean enableExternalEntities) {
    this.enableExternalEntities = enableExternalEntities;
  }

  /**
   * Reports whether {@code <!DOCTYPE>} declarations should be permitted by the secured parser.
   *
   * @return {@code true} if DTD declarations are enabled
   */
  public boolean isEnableDtdDeclarations() {
    return enableDtdDeclarations;
  }

  /**
   * Sets whether {@code <!DOCTYPE>} declarations should be permitted by the secured parser.
   *
   * @param enableDtdDeclarations {@code true} to allow DTD declarations
   */
  public void setEnableDtdDeclarations(boolean enableDtdDeclarations) {
    this.enableDtdDeclarations = enableDtdDeclarations;
  }

  /**
   * Reports whether the secured parser should fetch external DTDs.
   *
   * @return {@code true} if external DTD references are enabled
   */
  public boolean isEnableExternalDtdReferences() {
    return enableExternalDtdReferences;
  }

  /**
   * Sets whether the secured parser should fetch external DTDs.
   *
   * @param enableExternalDtdReferences {@code true} to enable external DTD references
   */
  public void setEnableExternalDtdReferences(boolean enableExternalDtdReferences) {
    this.enableExternalDtdReferences = enableExternalDtdReferences;
  }

  /**
   * Reports whether the JAXP {@code secure-processing} feature should be turned on.
   *
   * @return {@code true} if secure processing is enabled
   */
  public boolean isEnableSecureProcessing() {
    return enableSecureProcessing;
  }

  /**
   * Sets whether the JAXP {@code secure-processing} feature should be turned on.
   *
   * @param enableSecureProcessing {@code true} to enable secure processing
   */
  public void setEnableSecureProcessing(boolean enableSecureProcessing) {
    this.enableSecureProcessing = enableSecureProcessing;
  }

  /**
   * Reports whether external parameter entity references should be enabled. Note that {@link
   * PSSecureXMLUtils} always hard-disables them regardless of this flag.
   *
   * @return {@code true} if external parameter entities are requested by the caller
   */
  public boolean isEnableExternalParameterEntities() {
    return enableExternalParameterEntities;
  }

  /**
   * Sets whether external parameter entity references should be enabled.
   *
   * @param enableExternalParameterEntities {@code true} to request external parameter entity
   *     resolution; note that {@link PSSecureXMLUtils} overrides this to {@code false}
   */
  public void setEnableExternalParameterEntities(boolean enableExternalParameterEntities) {
    this.enableExternalParameterEntities = enableExternalParameterEntities;
  }

  /**
   * Reports whether the secured parser should perform DTD-based validation.
   *
   * @return {@code true} if validation is enabled
   */
  public boolean isEnableValidation() {
    return enableValidation;
  }

  /**
   * Sets whether the secured parser should perform DTD-based validation.
   *
   * @param enableValidation {@code true} to enable validation
   */
  public void setEnableValidation(boolean enableValidation) {
    this.enableValidation = enableValidation;
  }

  /**
   * Returns the allow-list of filesystem paths from which {@code <!DOCTYPE>} declarations may load
   * DTDs.
   *
   * @return the configured paths, or {@code null} when no allow-list is configured
   */
  public String[] getAllowedPathsForDtdDeclarations() {
    return allowedPathsForDtdDeclarations;
  }

  /**
   * Sets the allow-list of filesystem paths from which {@code <!DOCTYPE>} declarations may load
   * DTDs.
   *
   * @param allowedPathsForDtdDeclarations the paths to allow, or {@code null} to disable path
   *     filtering
   */
  public void setAllowedPathsForDtdDeclarations(String[] allowedPathsForDtdDeclarations) {
    this.allowedPathsForDtdDeclarations = allowedPathsForDtdDeclarations;
  }

  /**
   * Returns the allow-list of filesystem paths from which external entities may be resolved.
   *
   * @return the configured paths, or {@code null} when no allow-list is configured
   */
  public String[] getAllowedPathsForExternalEntities() {
    return allowedPathsForExternalEntities;
  }

  /**
   * Sets the allow-list of filesystem paths from which external entities may be resolved.
   *
   * @param allowedPathsForExternalEntities the paths to allow, or {@code null} to disable path
   *     filtering
   */
  public void setAllowedPathsForExternalEntities(String[] allowedPathsForExternalEntities) {
    this.allowedPathsForExternalEntities = allowedPathsForExternalEntities;
  }

  /**
   * Returns the allow-list of filesystem paths from which XSL {@code <xsl:import>} elements may
   * resolve.
   *
   * @return the configured paths, or {@code null} when no allow-list is configured
   */
  public String[] getAllowedPathsForImports() {
    return allowedPathsForImports;
  }

  /**
   * Sets the allow-list of filesystem paths from which XSL {@code <xsl:import>} elements may
   * resolve.
   *
   * @param allowedPathsForImports the paths to allow, or {@code null} to disable path filtering
   */
  public void setAllowedPathsForImports(String[] allowedPathsForImports) {
    this.allowedPathsForImports = allowedPathsForImports;
  }

  /**
   * Returns the allow-list of filesystem paths from which XSL {@code <xsl:include>} elements may
   * resolve.
   *
   * @return the configured paths, or {@code null} when no allow-list is configured
   */
  public String[] getAllowedPathsForIncludes() {
    return allowedPathsForIncludes;
  }

  /**
   * Sets the allow-list of filesystem paths from which XSL {@code <xsl:include>} elements may
   * resolve.
   *
   * @param allowedPathsForIncludes the paths to allow, or {@code null} to disable path filtering
   */
  public void setAllowedPathsForIncludes(String[] allowedPathsForIncludes) {
    this.allowedPathsForIncludes = allowedPathsForIncludes;
  }
}
