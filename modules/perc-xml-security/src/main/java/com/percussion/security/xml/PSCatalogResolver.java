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

import static com.percussion.security.xml.PSSecureXMLUtils.getNoOpSource;

import com.percussion.security.error.PSExceptionUtils;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xml.resolver.Catalog;
import org.apache.xml.resolver.CatalogManager;
import org.apache.xml.resolver.tools.CatalogResolver;
import org.xml.sax.InputSource;

/**
 * A {@link CatalogResolver} tailored for Percussion CMS XML processing.
 *
 * <p>Builds a {@link CatalogManager} that prefers system IDs and uses a <em>private</em> catalog
 * instance ({@code useStaticCatalog=false}) so one resolver's parse failures cannot corrupt a
 * process-wide static catalog. Catalog file paths still come from {@code xml.catalog.files} /
 * {@code CatalogManager.properties} when set by the JVM (see {@code perc-jetty jvm.ini}). This
 * product does not ship a classpath {@code CatalogManager.properties}; {@code
 * ignoreMissingProperties=true} avoids a noisy CatalogManager warning on every resolver
 * construction. Operators still configure catalogs via the JVM system properties above.
 *
 * <p>An optional {@link IPSInternalRequestURIResolver} may be installed to short-circuit catalog
 * lookups for URIs that should be resolved by the running application.
 *
 * @author Percussion Software
 */
public class PSCatalogResolver extends CatalogResolver {

  private static final Logger log = LogManager.getLogger(PSCatalogResolver.class);
  private IPSInternalRequestURIResolver internalRequestURIResolver = null;

  /**
   * Builds a CatalogManager that prefers system IDs and uses a <em>private</em> catalog instance
   * ({@code useStaticCatalog=false}) so one resolver's parse failures cannot corrupt a process-wide
   * static catalog.
   *
   * <p>Catalog file paths still come from {@code xml.catalog.files} / CatalogManager.properties
   * when set by the JVM (see perc-jetty {@code jvm.ini}). This product does not ship a classpath
   * {@code CatalogManager.properties}; {@code ignoreMissingProperties=true} avoids a noisy
   * CatalogManager warning on every resolver construction. Operators still configure catalogs via
   * the JVM system properties above.
   */
  static CatalogManager createDefaultCatalogManager() {
    return createCatalogManager(true);
  }

  /**
   * Creates a {@link CatalogManager} configured for Percussion CMS usage.
   *
   * @param privateCatalog when {@code true}, use a non-static (per-manager) catalog; when {@code
   *     false}, allow CatalogManager's shared static catalog
   * @return a non-null, fully configured {@link CatalogManager}
   */
  static CatalogManager createCatalogManager(boolean privateCatalog) {
    CatalogManager manager = new CatalogManager();
    // No CatalogManager.properties on the product classpath — ignore is intentional (see above).
    manager.setIgnoreMissingProperties(true);
    manager.setPreferPublic(false); // prefer system (matches xml.catalog.prefer=system)
    // false => getCatalog() returns a private Catalog (see CatalogManager.getCatalog)
    manager.setUseStaticCatalog(!privateCatalog);
    manager.setVerbosity(0);
    return manager;
  }

  /**
   * Returns the optional internal-request URI resolver installed on this catalog resolver.
   *
   * @return the installed resolver, or {@code null} if no resolver has been installed
   */
  public IPSInternalRequestURIResolver getInternalRequestURIResolver() {
    return internalRequestURIResolver;
  }

  /**
   * Installs an internal-request URI resolver that will be consulted before this catalog resolver
   * performs its catalog-driven resolution.
   *
   * @param internalRequestURIResolver the resolver to install; may be {@code null} to clear any
   *     previously installed resolver
   */
  public void setInternalRequestURIResolver(
      IPSInternalRequestURIResolver internalRequestURIResolver) {
    this.internalRequestURIResolver = internalRequestURIResolver;
  }

  /**
   * Constructor. Uses a private CatalogManager so catalog parse failures (e.g. legacy TR9401
   * fallback AIOOBE on mis-formed catalogs) cannot corrupt a shared static Catalog used by other
   * resolvers.
   */
  public PSCatalogResolver() {
    super(createDefaultCatalogManager());
  }

  /**
   * Constructor.
   *
   * @param privateCatalog when {@code true} (preferred), the CatalogManager uses a private catalog
   *     instance; when {@code false}, the manager may use CatalogManager's shared static catalog
   */
  public PSCatalogResolver(boolean privateCatalog) {
    super(createCatalogManager(privateCatalog));
  }

  /**
   * Constructor
   *
   * @param manager catalog manager to use; when {@code null}, a private default manager is used
   */
  public PSCatalogResolver(CatalogManager manager) {
    super(manager != null ? manager : createDefaultCatalogManager());
  }

  /** Return the underlying catalog */
  @Override
  public Catalog getCatalog() {
    return super.getCatalog();
  }

  /**
   * Implements the guts of the <code>resolveEntity</code> method for the SAX interface.
   *
   * <p>Presented with an optional public identifier and a system identifier, this function attempts
   * to locate a mapping in the catalogs.
   *
   * <p>If such a mapping is found, it is returned. If no mapping is found, null is returned.
   *
   * @param publicId The public identifier for the entity in question. This may be null.
   * @param systemId The system identifier for the entity in question. XML requires a system
   *     identifier on all external entities, so this value is always specified.
   * @return The resolved identifier (a URI reference).
   */
  @Override
  public String getResolvedEntity(String publicId, String systemId) {
    // Only want this if debug is explicitly enabled.
    if (log.isDebugEnabled()) {
      super.getCatalog().getCatalogManager().debug.setDebug(99);
    }
    return super.getResolvedEntity(publicId, systemId);
  }

  /**
   * Implements the <code>resolveEntity</code> method for the SAX interface.
   *
   * <p>Presented with an optional public identifier and a system identifier, this function attempts
   * to locate a mapping in the catalogs.
   *
   * <p>If such a mapping is found, the resolver attempts to open the mapped value as an InputSource
   * and return it. Exceptions are ignored and null is returned if the mapped value cannot be opened
   * as an input source.
   *
   * <p>If no mapping is found (or an error occurs attempting to open the mapped value as an input
   * source), null is returned and the system will use the specified system identifier as if no
   * entityResolver was specified.
   *
   * @param publicId The public identifier for the entity in question. This may be null.
   * @param systemId The system identifier for the entity in question. XML requires a system
   *     identifier on all external entities, so this value is always specified.
   * @return An InputSource for the mapped identifier, or null.
   */
  @Override
  public InputSource resolveEntity(String publicId, String systemId) {

    // Only want this if debug is explicitly enabled.
    if (log.isDebugEnabled()) {
      super.getCatalog().getCatalogManager().debug.setDebug(99);
    }

    InputSource is = null;
    try {
      is = super.resolveEntity(publicId, systemId);

    } catch (Exception e) {
      return getNoOpSource();
    }

    if (is == null) {
      return getNoOpSource();
    } else {
      return is;
    }
  }

  /**
   * JAXP URIResolver entry point used by XSLT processors when they encounter an {@code
   * xsl:include}, {@code xsl:import}, or {@code document()} function.
   *
   * <p>If an {@link IPSInternalRequestURIResolver} is installed and returns a non-null source for
   * the given URI, that source is returned and catalog lookup is skipped. Otherwise, the underlying
   * {@link CatalogResolver} is consulted. If the catalog does not contain a mapping the caller is
   * signaled via a {@link TransformerException} so that the XSLT processor does not silently fall
   * back to network I/O.
   *
   * @param href the URI from the {@code href} attribute or {@code document()} call, may be
   *     relative; a blank or {@code null} value resolves to {@code null}
   * @param base the base URI used to resolve {@code href} when an absolute URI is required; may be
   *     {@code null}
   * @return a non-null {@link Source} for the resolved URI
   * @throws TransformerException if the URI cannot be resolved through the internal resolver or the
   *     configured catalogs
   */
  @Override
  public Source resolve(String href, String base) throws TransformerException {

    if (href == null || "".equalsIgnoreCase(href)) {
      return null;
    }
    // Only want this if debug is explicitly enabled.
    if (log.isDebugEnabled()) {
      super.getCatalog().getCatalogManager().debug.setDebug(99);
    }

    Source s = null;

    // Process any internal requests to the XML application server first
    if (internalRequestURIResolver != null) {
      s = internalRequestURIResolver.resolve(href, base);
      // If we got a result, return it.
      if (s != null) {
        return s;
      }
    }
    try {
      s = super.resolve(href, null);
    } catch (Exception e) {
      log.warn(
          "Error resolving external resource from local XML Catalog.  href: {} base: {} Error:{}",
          href,
          base,
          PSExceptionUtils.getMessageForLog(e));
      throw new TransformerException(e);
    }
    if (s == null) {
      log.warn(
          "Error resolving external resource from local XML Catalog.  href: {} base: {}",
          href,
          base);
      throw new TransformerException(
          "Resource was not resolved in the local XML catalog. Un-trusted external references are"
              + " not allowed. href:"
              + href
              + " base:"
              + base);
    }
    return s;
  }
}
