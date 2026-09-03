/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

import com.percussion.rest.extensions.Extension;
import com.percussion.rest.extensions.ExtensionFilterOptions;
import com.percussion.rest.extensions.IExtensionAdaptor;
import java.net.URI;
import java.util.List;
import org.springframework.stereotype.Component;

/** Test adaptor for Extension API bridge (MainTest Spring context). */
@Component
public class TestExtensionAdaptor implements IExtensionAdaptor {

  @Override
  public List<Extension> listExtensions(URI baseURI) {
    return getExtensions(baseURI, new ExtensionFilterOptions());
  }

  @Override
  public Extension findExtensionByKey(URI baseURI, String idOrName) {
    return null;
  }

  @Override
  public List<Extension> getExtensions(URI baseURI, ExtensionFilterOptions filter) {
    return null;
  }

  @Override
  public Extension registerExtension(URI baseURI, Extension body) {
    return body;
  }

  @Override
  public Extension updateExtension(URI baseURI, String idOrName, Extension body) {
    return body;
  }

  @Override
  public boolean deleteExtension(URI baseURI, String idOrName) {
    return false;
  }
}
