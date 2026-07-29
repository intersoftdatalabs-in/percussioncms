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

package com.percussion.rest.test.apibridge;

import com.percussion.rest.contenttypes.ContentType;
import com.percussion.rest.contenttypes.ContentTypeDetail;
import com.percussion.rest.contenttypes.ContentTypeFilter;
import com.percussion.rest.contenttypes.IContentTypesAdaptor;
import java.net.URI;
import java.util.List;
import org.springframework.stereotype.Component;

/** Test adaptor for ContentType API bridge. */
@Component
public class TestContentTypeAdaptor implements IContentTypesAdaptor {

  public TestContentTypeAdaptor() {
    // Default constructor
  }

  /**
   * Lists all content types available to the system.
   *
   * @param baseUri Requesting URI
   * @return A list of all available Content Types
   */
  @Override
  public List<ContentType> listContentTypes(URI baseUri) {
    return null;
  }

  /**
   * Lists ContentTypes available for the specified Site.
   *
   * @param baseUri Originating URI
   * @param siteId Site Id for Site to filter Types by
   * @return An array of ContentTypes
   */
  @Override
  public List<ContentType> listContentTypes(URI baseUri, int siteId) {
    return null;
  }

  /**
   * Lists ContentTypes available for the specified Site using a filter.
   *
   * @param baseUri Originating URI
   * @param filter A ContentTypeFilter that can be used to filter content types.
   * @return An array of ContentTypes
   */
  @Override
  public List<ContentType> listContentTypesByFilter(URI baseUri, ContentTypeFilter filter) {
    return null;
  }

  @Override
  public ContentTypeDetail getContentType(URI baseUri, String idOrName) {
    return null;
  }
}
