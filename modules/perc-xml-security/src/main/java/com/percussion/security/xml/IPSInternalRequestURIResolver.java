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

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

/**
 * Marker extension of {@link URIResolver} used to delegate URI resolution to handlers that can
 * service requests internal to the Percussion CMS (for example, XSL includes / imports that should
 * be served from the running application rather than from the filesystem or network).
 *
 * <p>Implementations are installed on a {@link PSCatalogResolver} via {@link
 * PSCatalogResolver#setInternalRequestURIResolver(IPSInternalRequestURIResolver)}; when a lookup is
 * performed the catalog resolver consults the installed instance before falling back to its
 * catalog-driven logic.
 *
 * @author Percussion Software
 */
public interface IPSInternalRequestURIResolver extends URIResolver {

  /**
   * Called by the processor when it encounters an xsl:include, xsl:import, or document() function.
   *
   * @param href An href attribute, which may be relative or absolute.
   * @param base The base URI against which the first argument will be made absolute if the absolute
   *     URI is required.
   * @return A Source object, or null if the href cannot be resolved, and the processor should try
   *     to resolve the URI itself.
   * @throws TransformerException if an error occurs when trying to resolve the URI.
   */
  @Override
  Source resolve(String href, String base) throws TransformerException;
}
