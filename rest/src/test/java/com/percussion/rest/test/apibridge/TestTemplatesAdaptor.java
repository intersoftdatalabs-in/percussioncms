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

import com.percussion.rest.templates.ITemplatesAdaptor;
import com.percussion.rest.templates.TemplateDetail;
import com.percussion.rest.templates.TemplateFilter;
import com.percussion.rest.templates.TemplateSummary;
import java.net.URI;
import java.util.List;
import org.springframework.stereotype.Component;

/** Test adaptor for Templates API bridge. */
@Component
public class TestTemplatesAdaptor implements ITemplatesAdaptor {

  @Override
  public List<TemplateSummary> listAllTemplateSummaries(URI baseUri) {
    return List.of();
  }

  @Override
  public List<TemplateSummary> listTemplateSummaries(URI baseUri, TemplateFilter filter) {
    return null;
  }

  @Override
  public TemplateDetail getTemplate(URI baseUri, String idOrName) {
    return null;
  }

  @Override
  public TemplateDetail updateTemplate(URI baseUri, String idOrName, TemplateDetail body) {
    return null;
  }
}
