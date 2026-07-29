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

package com.percussion.apibridge;

import com.percussion.rest.templates.ITemplatesAdaptor;
import com.percussion.rest.templates.TemplateBindingSummary;
import com.percussion.rest.templates.TemplateDetail;
import com.percussion.rest.templates.TemplateFilter;
import com.percussion.rest.templates.TemplateSlotSummary;
import com.percussion.rest.templates.TemplateSummary;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.assembly.IPSTemplateSlot;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.assembly.PSAssemblyServiceLocator;
import com.percussion.services.assembly.data.PSTemplateBinding;
import com.percussion.services.catalog.PSCatalogException;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.content.PSContentWsLocator;
import jakarta.ws.rs.WebApplicationException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Adaptor for managing templates in Percussion CMS. */
@PSSiteManageBean
public class TemplateAdaptor implements ITemplatesAdaptor {

  private static final Logger log = LogManager.getLogger(TemplateAdaptor.class);

  private final IPSAssemblyService asmSvc = PSAssemblyServiceLocator.getAssemblyService();
  private final IPSContentWs contentwsService = PSContentWsLocator.getContentWebservice();

  public TemplateAdaptor() {
    // No-op constructor for dependency injection.
  }

  @Override
  public List<TemplateSummary> listAllTemplateSummaries(URI baseUri) {
    try {
      var summaries = asmSvc.getSummaries(PSTypeEnum.TEMPLATE);
      return summaries.stream().map(ApiUtils::convertTemplateSummary).collect(Collectors.toList());
    } catch (PSCatalogException e) {
      throw new WebApplicationException(e, 500);
    } catch (PSNotFoundException e) {
      throw new WebApplicationException("Not Found", 404);
    }
  }

  @Override
  public TemplateDetail getTemplate(URI baseUri, String idOrName) {
    // baseUri reserved for HATEOAS link building
    if (StringUtils.isBlank(idOrName)) {
      return null;
    }
    try {
      IPSAssemblyTemplate t = resolveTemplate(idOrName.trim());
      if (t == null) {
        return null;
      }
      return toDetail(t);
    } catch (PSNotFoundException e) {
      return null;
    } catch (Exception e) {
      log.error(
          "Failed to load template {} ({}): {}",
          idOrName,
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new RuntimeException(
          "Failed to load template (" + e.getClass().getName() + "): " + e.getMessage(), e);
    }
  }

  private IPSAssemblyTemplate resolveTemplate(String idOrName) throws Exception {
    if (StringUtils.isNumeric(idOrName)) {
      long uuid = Long.parseLong(idOrName);
      IPSGuid g = new PSGuid(PSTypeEnum.TEMPLATE, uuid);
      return asmSvc.loadTemplate(g, true);
    }
    if (idOrName.contains("-")) {
      try {
        PSGuid g = new PSGuid(idOrName);
        if (g.getType() == 0) {
          g = new PSGuid(PSTypeEnum.TEMPLATE, g.getUUID());
        }
        return asmSvc.loadTemplate(g, true);
      } catch (Exception ignore) {
        // fall through to name
      }
    }
    try {
      return asmSvc.findTemplateByName(idOrName);
    } catch (PSNotFoundException e) {
      return null;
    }
  }

  private TemplateDetail toDetail(IPSAssemblyTemplate t) {
    TemplateDetail d = new TemplateDetail();
    if (t.getGUID() != null) {
      d.setGuid(ApiUtils.convertGuid(t.getGUID()));
      d.setTemplateId(t.getGUID().getUUID());
    }
    d.setName(t.getName());
    d.setLabel(t.getLabel());
    d.setDescription(t.getDescription());
    d.setAssembler(t.getAssembler());
    d.setAssemblyUrl(t.getAssemblyUrl());
    d.setStyleSheet(t.getStyleSheetPath());
    d.setMimeType(t.getMimeType());
    d.setCharset(t.getCharset());
    d.setLocationPrefix(t.getLocationPrefix());
    d.setLocationSuffix(t.getLocationSuffix());
    if (t.getOutputFormat() != null) {
      d.setOutputFormat(t.getOutputFormat().name());
    }
    if (t.getActiveAssemblyType() != null) {
      d.setAaType(t.getActiveAssemblyType().name());
    }
    if (t.getPublishWhen() != null) {
      d.setPublishWhen(t.getPublishWhen().name());
    }
    if (t.getTemplateType() != null) {
      d.setTemplateType(t.getTemplateType().name());
    }
    if (t.getGlobalTemplateUsage() != null) {
      d.setGlobalTemplateUsage(t.getGlobalTemplateUsage().name());
    }
    d.setVariant(t.isVariant());
    d.setTemplateSource(t.getTemplate());

    List<TemplateBindingSummary> bindings = new ArrayList<>();
    List<PSTemplateBinding> rawBindings = t.getBindings();
    if (rawBindings != null) {
      for (PSTemplateBinding b : rawBindings) {
        if (b == null) continue;
        TemplateBindingSummary s = new TemplateBindingSummary();
        s.setExecutionOrder(b.getExecutionOrder());
        s.setVariable(b.getVariable());
        s.setExpression(b.getExpression());
        bindings.add(s);
      }
      bindings.sort(
          Comparator.comparing(
              TemplateBindingSummary::getExecutionOrder,
              Comparator.nullsLast(Integer::compareTo)));
    }
    d.setBindings(bindings);

    List<TemplateSlotSummary> slots = new ArrayList<>();
    if (t.getSlots() != null) {
      for (IPSTemplateSlot slot : t.getSlots()) {
        if (slot == null) continue;
        TemplateSlotSummary s = new TemplateSlotSummary();
        try {
          if (slot.getGUID() != null) {
            s.setGuid(ApiUtils.convertGuid(slot.getGUID()));
          }
        } catch (Exception e) {
          log.debug("Could not convert slot GUID for {}: {}", slot.getName(), e.getMessage());
        }
        s.setName(slot.getName());
        s.setLabel(StringUtils.defaultIfBlank(slot.getLabel(), slot.getName()));
        s.setDescription(slot.getDescription());
        slots.add(s);
      }
      slots.sort(
          Comparator.comparing(
              x -> x.getLabel() != null ? x.getLabel() : "", String.CASE_INSENSITIVE_ORDER));
    }
    d.setSlots(slots);

    List<String> gaps = new ArrayList<>();
    gaps.add("Create / update / delete / lock not supported (read-only)");
    gaps.add("Template source editing not supported via this API");
    gaps.add("Binding/slot association edits not supported");
    gaps.add("Content-type associations not listed on this payload");
    d.setDesignGaps(gaps);
    return d;
  }

  /**
   * Returns all template summaries that match the supplied filter. NOTE: Currently only contentId
   * is implemented. TODO: Implement for all filter options.
   */
  @Override
  public List<TemplateSummary> listTemplateSummaries(URI baseUri, TemplateFilter filter) {
    if (filter == null) {
      throw new IllegalArgumentException("TemplateFilter cannot be null");
    }
    var ret = new ArrayList<TemplateSummary>();
    int contentID = filter.getContentId();

    try {
      var guids = new ArrayList<IPSGuid>();
      guids.add(PSGuidManagerLocator.getGuidMgr().makeGuid(contentID, PSTypeEnum.LEGACY_CONTENT));
      var items = contentwsService.loadItems(guids, false, false, false, false);
      if (items != null && !items.isEmpty()) {
        var item = items.get(0);
        long contentTypeId = item.getContentTypeId();
        var ctypeGuid =
            PSGuidManagerLocator.getGuidMgr().makeGuid(contentTypeId, PSTypeEnum.NODEDEF);
        var templates = asmSvc.findTemplatesByContentType(ctypeGuid);

        ret.addAll(
            templates.stream().map(ApiUtils::convertTemplateSummary).collect(Collectors.toList()));
        return ret;
      } else {
        throw new PSNotFoundException("Content Id: " + contentID + " not found.");
      }
    } catch (PSAssemblyException | PSErrorResultsException e) {
      throw new WebApplicationException(e.getMessage(), 500);
    } catch (PSNotFoundException e) {
      throw new WebApplicationException("Not Found", 404);
    }
  }
}
