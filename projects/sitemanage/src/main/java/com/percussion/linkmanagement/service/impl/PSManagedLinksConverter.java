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
package com.percussion.linkmanagement.service.impl;

import com.percussion.data.PSConversionException;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.extension.*;
import com.percussion.linkmanagement.service.IPSManagedLinkService;
import com.percussion.pagemanagement.data.PSInlineLinkRequest;
import com.percussion.pagemanagement.data.PSInlineRenderLink;
import com.percussion.pagemanagement.service.IPSRenderLinkService;
import com.percussion.server.IPSRequestContext;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/**
 * Field input transformer to convert new style managed links in content to old style inline links.
 * If an anchor link has PERC_MANAGED_ATTR=true, calls managed link service to get the dependent id
 * and then calls renderlink service to get details for the old style managed link and adds them as
 * attributes. The inline link processor will handle creation/management of links.
 *
 * @author JaySeletz
 */
public class PSManagedLinksConverter extends PSDefaultExtension
    implements IPSFieldInputTransformer {

  public static final String RXHYPERLINK = "rxhyperlink";
  public static final String RXIMAGE = "rximage";
  public static final String INLINETYPE = "inlinetype";
  public static final String RXINLINESLOT = "rxinlineslot";
  public static final String SYS_DEPENDENTID = "sys_dependentid";
  public static final String SYS_DEPENDENTVARIANTID = "sys_dependentvariantid";
  private static final Logger log = LogManager.getLogger(PSManagedLinksConverter.class);

  private IPSManagedLinkService managedService;
  private IPSRenderLinkService renderService;

  @Override
  public void init(IPSExtensionDef def, File codeRoot) throws PSExtensionException {
    super.init(def, codeRoot);
    PSSpringWebApplicationContextUtils.injectDependencies(this);
  }

  @Override
  public Object processUdf(Object[] params, IPSRequestContext request)
      throws PSConversionException {
    var ep = new PSExtensionParams(params);
    var value = ep.getStringParam(0, null, true);
    var returnMap = params.length > 1 && Boolean.parseBoolean(ep.getStringParam(1, "false", false));
    if (StringUtils.isBlank(value)) {
      return value;
    }
    var attribs = new HashMap<String, String>();
    var updatedValue = processLinksAndImages(value, attribs);
    return returnMap ? attribs : updatedValue;
  }

  public String processLinksAndImages(String value, Map<String, String> attribs) {
    var doc = Jsoup.parseBodyFragment(value);
    var elems = doc.select(IPSManagedLinkService.A_HREF);
    var imgElems = doc.select(IPSManagedLinkService.IMG_SRC);

    if (elems.isEmpty() && imgElems.isEmpty()) {
      return value;
    }
    for (var elem : elems) {
      if (!elem.hasAttr(IPSManagedLinkService.LEGACY_INLINETYPE)
          && (managedService.doManageAll()
              || elem.attr(IPSManagedLinkService.PERC_MANAGED_ATTR)
                  .equalsIgnoreCase(IPSManagedLinkService.TRUE_VAL))) {
        convertToOldLinks(elem, RXHYPERLINK, attribs);
      }
    }
    for (var elem : imgElems) {
      if (!elem.hasAttr(IPSManagedLinkService.LEGACY_INLINETYPE)
          && (managedService.doManageAll()
              || elem.attr(IPSManagedLinkService.PERC_MANAGED_ATTR)
                  .equalsIgnoreCase(IPSManagedLinkService.TRUE_VAL))) {
        convertToOldLinks(elem, RXIMAGE, attribs);
      }
    }
    return doc.body().html();
  }

  @SuppressWarnings("deprecation")
  private void convertToOldLinks(Element elem, String type, Map<String, String> attribs) {
    int dependent = -1;
    try {
      dependent = managedService.getDependent(elem);
      if (dependent != -1) {
        var depGuid =
            PSGuidManagerLocator.getGuidMgr().makeGuid(new PSLocator(dependent)).toString();
        if (depGuid == null) {
          return;
        }
        PSInlineRenderLink renderLink;
        var path =
            "img".equalsIgnoreCase(elem.tagName())
                ? elem.attr(IPSManagedLinkService.SRC_ATTR)
                : elem.attr(IPSManagedLinkService.HREF_ATTR);
        if (RXHYPERLINK.equalsIgnoreCase(type)
            && (path.startsWith("/Sites/") || path.startsWith("//Sites/"))) {
          renderLink = renderService.renderPreviewPageLink(depGuid);
        } else {
          var linkRequest = new PSInlineLinkRequest();
          linkRequest.setTargetId(depGuid);
          renderLink = renderService.renderPreviewResourceLink(linkRequest);
        }

        if (renderLink != null) {
          if ("img".equalsIgnoreCase(elem.tagName())) {
            managedService.renderImageLink(null, elem);
            attribs.put("path", elem.attr("src"));
          } else {
            managedService.renderLink(null, elem);
            attribs.put("path", elem.attr("href"));
          }
          elem.attr(SYS_DEPENDENTVARIANTID, "" + renderLink.getLegacyDependentVariantId());
          attribs.put(SYS_DEPENDENTVARIANTID, renderLink.getLegacyDependentVariantId().toString());
          elem.attr(SYS_DEPENDENTID, "" + renderLink.getLegacyDependentId());
          attribs.put(SYS_DEPENDENTID, renderLink.getLegacyDependentId().toString());
          elem.attr(RXINLINESLOT, "" + renderLink.getLegacyRxInlineSlot());
          attribs.put(RXINLINESLOT, renderLink.getLegacyRxInlineSlot().toString());
          elem.attr(INLINETYPE, type);
          attribs.put(INLINETYPE, type);
        }
        elem.removeAttr(IPSManagedLinkService.PERC_MANAGED_ATTR);
        elem.removeAttr(IPSManagedLinkService.PERC_LINKID_ATTR);
        elem.removeAttr(IPSManagedLinkService.PERC_LINKID_OLD_ATTR);
        elem.removeAttr(IPSManagedLinkService.PERC_MANAGED_OLD_ATTR);
      }
    } catch (Exception e) {
      log.warn("Failed to convert the managed link in rich text editor.", e);
    }
  }

  /**
   * Setter for dependency injection.
   *
   * @param managedService the service to set
   */
  public void setManagedService(IPSManagedLinkService managedService) {
    this.managedService = managedService;
  }

  /**
   * Setter for dependency injection.
   *
   * @param renderService the service to set
   */
  public void setRenderService(IPSRenderLinkService renderService) {
    this.renderService = renderService;
  }
}
