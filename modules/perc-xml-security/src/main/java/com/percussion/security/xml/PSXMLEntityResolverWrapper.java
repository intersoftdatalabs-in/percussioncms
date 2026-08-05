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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLEntityResolver;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xml.resolver.tools.CatalogResolver;
import org.xml.sax.InputSource;

/**
 * Wraps a {@link CatalogResolver} as an Xerces {@link XMLEntityResolver} so that XML parsers
 * created via {@link PSSecureXMLUtils} can resolve external entities through the configured XML
 * catalog instead of falling back to the network.
 *
 * <p>When the catalog cannot resolve an entity, this wrapper returns a no-op input source rather
 * than throwing, so the parser simply sees an empty stream and proceeds.
 *
 * @author Percussion Software
 */
public class PSXMLEntityResolverWrapper implements XMLEntityResolver {
  private CatalogResolver resolver = new CatalogResolver();
  private static final Logger log = LogManager.getLogger(PSXMLEntityResolverWrapper.class);

  /**
   * Creates a new wrapper that delegates external-entity resolution to a freshly-instantiated
   * {@link CatalogResolver}.
   */
  public PSXMLEntityResolverWrapper() {
    // no-op
  }

  /**
   * Converts a SAX {@link InputSource} into an Xerces {@link XMLInputSource}, copying the public
   * id, system id, byte/character streams, and encoding.
   *
   * @param is the SAX input source to convert, assumed not {@code null}
   * @return a non-null Xerces input source equivalent to {@code is}
   */
  private XMLInputSource getXmlInput(InputSource is) {
    XMLInputSource source = new XMLInputSource(is.getPublicId(), is.getSystemId(), null);
    source.setByteStream(is.getByteStream());
    source.setCharacterStream(is.getCharacterStream());
    source.setEncoding(is.getEncoding());
    return source;
  }

  /**
   * Resolves an external parsed entity through the wrapped {@link CatalogResolver}. If the entity
   * cannot be resolved, returns a no-op {@link XMLInputSource} so the parser sees an empty stream
   * rather than failing outright.
   *
   * @param resourceIdentifier location of the XML resource to resolve, assumed not {@code null}
   * @return a non-null {@link XMLInputSource} for the resolved entity, or a no-op source when the
   *     catalog does not contain a mapping for {@code resourceIdentifier}
   * @throws XNIException if an unrecoverable error occurs while consulting the catalog
   * @see XMLResourceIdentifier
   */
  @Override
  public XMLInputSource resolveEntity(XMLResourceIdentifier resourceIdentifier)
      throws XNIException {
    InputSource is =
        resolver.resolveEntity(
            resourceIdentifier.getPublicId(), resourceIdentifier.getLiteralSystemId());
    try {
      if (is == null) {
        log.warn(
            "Unable to resolve external resource from local XML Catalog.  PUBLIC: {} SYSTEM_ID: {}",
            resourceIdentifier.getPublicId(),
            resourceIdentifier.getLiteralSystemId());
        return getXmlInput(getNoOpSource());
      }
    } catch (Exception e) {
      log.warn(
          "Error resolving external resource from local XML Catalog.  PUBLIC: {} SYSTEM_ID: {}"
              + " Error:{}",
          resourceIdentifier.getPublicId(),
          resourceIdentifier.getLiteralSystemId(),
          PSExceptionUtils.getMessageForLog(e));
      return getXmlInput(getNoOpSource());
    }

    // We were able to resolve from the local Catalog so this resource is ok to return.
    XMLInputSource source = new XMLInputSource(is.getPublicId(), is.getSystemId(), null);
    source.setByteStream(is.getByteStream());
    source.setCharacterStream(is.getCharacterStream());
    source.setEncoding(is.getEncoding());
    return source;
  }
}
