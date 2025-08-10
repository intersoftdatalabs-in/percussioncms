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
package com.percussion.pagemanagement.assembler.impl;

import static com.percussion.share.service.exception.PSParameterValidationUtils.validateParameters;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.Validate.isTrue;
import static org.apache.commons.lang.Validate.notEmpty;
import static org.apache.commons.lang.Validate.notNull;

import com.percussion.pagemanagement.assembler.IPSRenderAssemblyBridge;
import com.percussion.pagemanagement.assembler.PSAbstractAssemblyContext.EditType;
import com.percussion.pagemanagement.assembler.PSPageAssemblyContextFactory;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.data.PSTemplate;
import com.percussion.pagemanagement.service.IPSPageService.PSPageException;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.assembly.IPSAssemblyItem;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.assembly.PSTemplateNotImplementedException;
import com.percussion.services.filter.PSFilterException;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.share.data.PSAbstractPersistantObject;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.system.utils.IPSHtmlParameters;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.content.IPSContentWs;
import java.io.IOException;
import java.util.Collections;
import javax.jcr.RepositoryException;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// REFACTORED: CP-JAVA11
@Component("renderAssemblyBridge")
public class PSRenderAssemblyBridge implements IPSRenderAssemblyBridge {

  /** The id mapper, Initialized by constructor, never <code>null</code> after that. */
  private final IPSIdMapper idMapper;

  /** The assembly service, auto wired by Spring framework */
  private final IPSAssemblyService assemblyService;

  /**
   * The content design web-service. Initialized by constructor, never <code>null</code> after that.
   */
  private final IPSContentDesignWs contentDesignWs;

  private final IPSSiteManager siteManager;

  private final IPSContentWs contentWs;

  private final IPSCmsObjectMgr cmsMgr;

  /**
   * The name of the system dispatch template for pages. Initialized in spring configuration, never
   * <code>null</code> after that.
   */
  @Value("${assemblyBridge.dispatchTemplate:perc.base.plain}")
  private String dispatchTemplate;

  @Autowired
  public PSRenderAssemblyBridge(
      IPSAssemblyService assemblyService,
      IPSContentDesignWs contentDesignWs,
      IPSContentWs contentWs,
      IPSIdMapper idMapper,
      IPSSiteManager siteManager,
      IPSCmsObjectMgr cmsMgr) {
    this.assemblyService = assemblyService;
    this.contentDesignWs = contentDesignWs;
    this.contentWs = contentWs;
    this.idMapper = idMapper;
    this.siteManager = siteManager;
    this.cmsMgr = cmsMgr;
  }

  @Override
  public String renderPage(String id, boolean editMode, boolean scriptsOff, EditType editType)
      throws PSPageException, PSValidationException {
    return render(id, editMode, scriptsOff, editType);
  }

  @Override
  public String renderPage(String id, boolean editMode, boolean scriptsOff)
      throws PSPageException, PSValidationException {
    return render(id, editMode, scriptsOff, EditType.PAGE);
  }

  @Override
  public String renderTemplate(String id, boolean scriptsOff)
      throws PSPageException, PSValidationException {
    return render(id, true, scriptsOff, EditType.TEMPLATE);
  }

  protected String render(String id, boolean editMode, boolean scriptsOff, EditType editType)
      throws PSPageException, PSValidationException {
    notEmpty(id, "id may not be blank");
    validateExistingItem(id);
    try {
      var work = getWorkItemForPreview(id, editMode, scriptsOff, editType);
      return assemble(work);
    } catch (Exception e) {
      var errorMsg = "Failed to preview page: " + id;
      log.error("{} Error: {}", errorMsg, PSExceptionUtils.getMessageForLog(e));
      throw new PSPageException(errorMsg, e);
    }
  }

  @Override
  public String renderPage(PSPage page, boolean editMode, boolean scriptsOff)
      throws PSPageException {
    notNull(page, "page");
    notEmpty(page.getId(), "page id");
    var ai = getWorkItemForPreview(page.getId(), null, page, true, scriptsOff, EditType.PAGE);
    return render(ai, page);
  }

  @Override
  public String renderTemplate(PSTemplate template, boolean scriptsOff) throws PSPageException {
    notNull(template, "template");
    notEmpty(template.getId(), "template id");
    var ai =
        getWorkItemForPreview(
            template.getId(), template, null, true, scriptsOff, EditType.TEMPLATE);
    return render(ai, template);
  }

  @Override
  public String renderTemplateWithPage(PSTemplate template, PSPage page, boolean scriptsOff)
      throws PSPageException {
    notNull(template, "template");
    notEmpty(template.getId(), "template id");
    var ai =
        getWorkItemForPreview(page.getId(), template, page, true, scriptsOff, EditType.TEMPLATE);
    return render(ai, template);
  }

  private String render(IPSAssemblyItem work, PSAbstractPersistantObject object)
      throws PSPageException {
    notNull(work, "work");
    notNull(object, "object");
    isTrue(isNotBlank(object.getId()), "id may not be blank");
    try {
      return assemble(work);
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        var errorMsg = "Failed to preview: " + object;
        log.error("{} Error: {}", errorMsg, PSExceptionUtils.getMessageForLog(e));
      }
      throw new PSPageException("Failed to preview:", e);
    }
  }

  private String assemble(IPSAssemblyItem work)
      throws RepositoryException,
          PSTemplateNotImplementedException,
          PSAssemblyException,
          PSFilterException,
          IOException {
    var results = assemblyService.assemble(Collections.singletonList(work));
    var result = results.get(0);
    var charSet = result.getTemplate().getCharset();
    return IOUtils.toString(result.getResultStream(), charSet);
  }

  private IPSAssemblyItem getWorkItemForPreview(
      String id,
      PSTemplate template,
      PSPage page,
      boolean editMode,
      boolean scriptsOff,
      EditType editType)
      throws PSPageException {
    var item = getWorkItemForPreview(id, editMode, scriptsOff, editType);
    if (template != null) {
      PSAssemblyItemBridge.setTemplate(item, template);
    }
    if (page != null) {
      PSAssemblyItemBridge.setPage(item, page);
    }
    return item;
  }

  @Override
  public IPSAssemblyItem getWorkItemForPreview(
      String id, boolean editMode, boolean scriptsOff, EditType editType) throws PSPageException {
    notEmpty(id, "id");
    var guid = (PSLegacyGuid) idMapper.getGuid(id);
    guid = (PSLegacyGuid) contentDesignWs.getItemGuid(guid);
    var work = assemblyService.createAssemblyItem();
    work.setId(guid);
    work.setParameterValue(IPSHtmlParameters.SYS_CONTENTID, String.valueOf(guid.getContentId()));
    work.setParameterValue(IPSHtmlParameters.SYS_REVISION, String.valueOf(guid.getRevision()));
    work.setParameterValue(IPSHtmlParameters.SYS_ITEMFILTER, "preview");
    work.setParameterValue(
        IPSHtmlParameters.SYS_TEMPLATE, String.valueOf(getDispatchTemplateId().getUUID()));
    work.setParameterValue(IPSHtmlParameters.SYS_CONTEXT, "0");
    if (editMode) {
      work.setParameterValue(PSPageAssemblyContextFactory.ASSEMBLY_PARAM_EDITMODE, "true");
    }
    if (scriptsOff) {
      work.setParameterValue(PSPageAssemblyContextFactory.ASSEMBLY_PARAM_SCRIPTSOFF, "true");
    }
    work.setParameterValue(PSPageAssemblyContextFactory.ASSEMBLY_PARAM_EDITTYPE, editType.name());

    // get folder ID
    var summs = contentWs.findFolderParents(guid, false);
    if (!summs.isEmpty()) {
      var folderId = summs.get(0).getGUID().getUUID();
      work.setParameterValue(IPSHtmlParameters.SYS_FOLDERID, String.valueOf(folderId));
    }

    // get site ID
    try {
      var sites = siteManager.getItemSites(guid);
      if (!sites.isEmpty()) {
        if (sites.size() > 1) {
          log.warn("Page or Template is associated with multiple sites: {}", sites);
        }
        var siteId = sites.get(0).getGUID().getUUID();
        work.setParameterValue(IPSHtmlParameters.SYS_SITEID, String.valueOf(siteId));
      } else {
        throw new PSRenderAssemblyBridgeException(
            "Page or Template with id: " + guid + "  is not in any site folder paths.");
      }
    } catch (Exception e) {
      throw new PSRenderAssemblyBridgeException("Failed to get site for page: " + id, e);
    }
    return work;
  }

  /**
   * Validates the existence of the specified item. Throws validation exception if it does not
   * exist.
   *
   * @param id the ID of the item in question, assumed not <code>null</code>.
   */
  private void validateExistingItem(String id) throws PSValidationException {
    var guid = (PSLegacyGuid) idMapper.getGuid(id);
    var summary = cmsMgr.loadComponentSummary(guid.getContentId());
    if (summary == null) {
      var builder = validateParameters("render");
      var msg =
          "Cannot render item (id=" + guid.getContentId() + ") because the item does not exist.";
      builder.reject("page.does.not.exist", msg).throwIfInvalid();
    }
  }

  public IPSGuid getDispatchTemplateId() throws PSPageException {
    try {
      var template = assemblyService.findTemplateByName(getDispatchTemplate());
      return template.getGUID();
    } catch (Exception e) {
      var error = "Failed to find dispatcher template: " + getDispatchTemplate();
      log.error("{} Error: {}", error, PSExceptionUtils.getMessageForLog(e));
      throw new PSPageException(error, e);
    }
  }

  public String getDispatchTemplate() {
    return dispatchTemplate;
  }

  public void setDispatchTemplate(String dispatchTemplate) {
    this.dispatchTemplate = dispatchTemplate;
  }

  private static final Logger log = LogManager.getLogger(PSRenderAssemblyBridge.class);
}
