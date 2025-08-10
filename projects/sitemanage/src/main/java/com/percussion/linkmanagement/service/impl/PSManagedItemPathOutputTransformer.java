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

package com.percussion.linkmanagement.service.impl;

import com.percussion.cms.PSContentEditorWalker;
import com.percussion.data.PSConversionException;
import com.percussion.extension.*;
import com.percussion.linkmanagement.service.IPSManagedLinkService;
import com.percussion.server.IPSRequestContext;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import com.percussion.system.utils.IPSHtmlParameters;
import java.io.File;
import java.util.Optional;
import org.apache.commons.lang.StringUtils;

/**
 * Field output transformer to update managed item paths on edit. Calls the managed link service to
 * do the actual work.
 *
 * @author JaySeletz
 */
public class PSManagedItemPathOutputTransformer extends PSDefaultExtension
    implements IPSFieldOutputTransformer {

  private IPSManagedLinkService service;

  @Override
  public Object processUdf(Object[] params, IPSRequestContext request)
      throws PSConversionException {
    var ep = new PSExtensionParams(params);
    var path = ep.getStringParam(0, null, true);
    var linkIdField = ep.getStringParam(1, null, false);

    if (StringUtils.isBlank(linkIdField)) {
      return path;
    }
    var linkId =
        (String)
            PSContentEditorWalker.getDisplayFieldValue(request.getInputDocument(), linkIdField);
    if (StringUtils.isBlank(linkId)) {
      return path;
    }

    var cid = request.getParameter(IPSHtmlParameters.SYS_CONTENTID);
    if (StringUtils.isBlank(cid) || !StringUtils.isNumeric(cid)) {
      return path;
    }
    return Optional.ofNullable(service.renderItemPath(null, linkId)).orElse(path);
  }

  @Override
  public void init(IPSExtensionDef def, File codeRoot) throws PSExtensionException {
    super.init(def, codeRoot);
    PSSpringWebApplicationContextUtils.injectDependencies(this);
  }

  /**
   * Setter for dependency injection.
   *
   * @param service the service to set
   */
  public void setService(IPSManagedLinkService service) {
    this.service = service;
  }
}
