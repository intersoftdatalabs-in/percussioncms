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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Adaptor for managing templates in Percussion CMS. */
@PSSiteManageBean
public class TemplateAdaptor implements ITemplatesAdaptor {

  private static final Logger log = LogManager.getLogger(TemplateAdaptor.class);

  /** API capability notes shared by every detail payload (not per-template data). */
  static final List<String> TEMPLATE_DESIGN_GAPS =
      List.of(
          "Create / delete / lock not supported via this API",
          "Content-type associations not listed on this payload");

  private final IPSAssemblyService asmSvc;
  private final IPSContentWs contentwsService;

  public TemplateAdaptor() {
    this(
        PSAssemblyServiceLocator.getAssemblyService(),
        PSContentWsLocator.getContentWebservice());
  }

  /** Package-visible for unit tests that inject a fake assembly service. */
  TemplateAdaptor(IPSAssemblyService asmSvc, IPSContentWs contentwsService) {
    this.asmSvc = asmSvc;
    this.contentwsService = contentwsService;
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
    } catch (IllegalArgumentException e) {
      // Bad id form — surface as 400 via resource mapping if desired; treat as not found for now
      log.debug("Invalid template idOrName {}: {}", idOrName, e.getMessage());
      return null;
    } catch (Exception e) {
      log.error(
          "Failed to load template {} ({}): {}",
          idOrName,
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, 500);
    }
  }

  @Override
  public TemplateDetail updateTemplate(URI baseUri, String idOrName, TemplateDetail body) {
    if (StringUtils.isBlank(idOrName)) {
      throw new IllegalArgumentException("idOrName is required");
    }
    if (body == null) {
      throw new IllegalArgumentException("body is required");
    }
    try {
      IPSAssemblyTemplate t = resolveTemplate(idOrName.trim());
      if (t == null) {
        return null;
      }
      if (body.getLabel() != null) {
        t.setLabel(body.getLabel());
      }
      if (body.getDescription() != null) {
        t.setDescription(body.getDescription());
      }
      if (body.getTemplateSource() != null) {
        t.setTemplate(body.getTemplateSource());
      }
      // null = leave unchanged; non-null list (including empty) = full replace
      if (body.getBindings() != null) {
        t.setBindings(toBindings(body.getBindings()));
      }
      if (body.getSlots() != null) {
        t.setSlots(toSlots(body.getSlots()));
      }
      asmSvc.saveTemplate(t);
      IPSAssemblyTemplate reloaded = resolveTemplate(idOrName.trim());
      return reloaded != null ? toDetail(reloaded) : toDetail(t);
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (PSAssemblyException e) {
      log.error("Failed to save template {}: {}", idOrName, e.getMessage(), e);
      throw new IllegalStateException("Failed to save template", e);
    } catch (Exception e) {
      log.error(
          "Failed to update template {} ({}): {}",
          idOrName,
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new IllegalStateException("Failed to update template", e);
    }
  }

  private List<PSTemplateBinding> toBindings(List<TemplateBindingSummary> summaries) {
    List<PSTemplateBinding> out = new ArrayList<>();
    if (summaries == null) {
      return out;
    }
    int i = 0;
    for (TemplateBindingSummary s : summaries) {
      i++;
      if (s == null) {
        throw new IllegalArgumentException("bindings[" + (i - 1) + "] is null");
      }
      if (StringUtils.isBlank(s.getVariable())) {
        throw new IllegalArgumentException("bindings[" + (i - 1) + "].variable is required");
      }
      if (StringUtils.isBlank(s.getExpression())) {
        throw new IllegalArgumentException("bindings[" + (i - 1) + "].expression is required");
      }
      int order = s.getExecutionOrder() != null && s.getExecutionOrder() > 0
          ? s.getExecutionOrder()
          : i;
      out.add(new PSTemplateBinding(order, s.getVariable().trim(), s.getExpression().trim()));
    }
    return out;
  }

  private Set<IPSTemplateSlot> toSlots(List<TemplateSlotSummary> summaries)
      throws PSAssemblyException {
    Set<IPSTemplateSlot> out = new HashSet<>();
    if (summaries == null) {
      return out;
    }
    int i = 0;
    for (TemplateSlotSummary s : summaries) {
      i++;
      if (s == null) {
        throw new IllegalArgumentException("slots[" + (i - 1) + "] is null");
      }
      IPSTemplateSlot slot = resolveSlotRef(s);
      if (slot == null) {
        String key =
            s.getName() != null
                ? s.getName()
                : (s.getGuid() != null && s.getGuid().getStringValue().isPresent()
                    ? s.getGuid().getStringValue().get()
                    : "?");
        throw new IllegalArgumentException("slots[" + (i - 1) + "] not found: " + key);
      }
      out.add(slot);
    }
    return out;
  }

  private IPSTemplateSlot resolveSlotRef(TemplateSlotSummary s) throws PSAssemblyException {
    if (s.getGuid() != null && s.getGuid().getStringValue().isPresent()) {
      String sv = s.getGuid().getStringValue().get();
      if (StringUtils.isNotBlank(sv)) {
        try {
          return asmSvc.loadSlot(ApiUtils.convertGuid(s.getGuid()));
        } catch (Exception e) {
          log.debug("Slot GUID load failed for {}: {}", sv, e.getMessage());
        }
      }
    }
    if (StringUtils.isNotBlank(s.getName())) {
      try {
        return asmSvc.findSlotByName(s.getName().trim());
      } catch (PSAssemblyException e) {
        return null;
      }
    }
    return null;
  }

  /**
   * Resolve by: pure numeric uuid → typed GUID string (digits and dashes only) → name. Names that
   * contain dashes but are not pure GUID digit/dash forms fall through to name lookup.
   */
  private IPSAssemblyTemplate resolveTemplate(String idOrName) throws Exception {
    if (StringUtils.isNumeric(idOrName)) {
      long uuid = Long.parseLong(idOrName);
      IPSGuid g = new PSGuid(PSTypeEnum.TEMPLATE, uuid);
      try {
        return asmSvc.loadTemplate(g, true);
      } catch (PSNotFoundException e) {
        return null;
      }
    }
    // GUID-shaped only (e.g. 0-10-347), not arbitrary names with dashes
    if (idOrName.matches("\\d+-\\d+(-\\d+)?")) {
      try {
        PSGuid g = new PSGuid(idOrName);
        if (g.getType() == 0) {
          g = new PSGuid(PSTypeEnum.TEMPLATE, g.getUUID());
        }
        return asmSvc.loadTemplate(g, true);
      } catch (PSNotFoundException e) {
        return null;
      } catch (IllegalArgumentException e) {
        log.debug("Not a template GUID, trying name: {}", idOrName);
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
          log.warn("Could not convert slot GUID for {}: {}", slot.getName(), e.getMessage(), e);
        }
        s.setName(slot.getName());
        s.setLabel(StringUtils.defaultIfBlank(slot.getLabel(), slot.getName()));
        s.setDescription(slot.getDescription());
        slots.add(s);
      }
      slots.sort(
          Comparator.comparing(
              TemplateSlotSummary::getLabel,
              Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
    }
    d.setSlots(slots);
    d.setDesignGaps(new ArrayList<>(TEMPLATE_DESIGN_GAPS));
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
      throw new WebApplicationException(e, 500);
    } catch (PSNotFoundException e) {
      throw new WebApplicationException("Not Found", 404);
    }
  }
}
