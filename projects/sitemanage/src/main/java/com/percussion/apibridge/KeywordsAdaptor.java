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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.apibridge;

import com.percussion.rest.keywords.IKeywordsAdaptor;
import com.percussion.rest.keywords.KeywordChoiceSummary;
import com.percussion.rest.keywords.KeywordNotFoundException;
import com.percussion.rest.keywords.KeywordSummary;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.content.data.PSKeyword;
import com.percussion.services.content.data.PSKeywordChoice;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.PSErrorsException;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.content.PSContentWsLocator;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Keyword design catalog for Developer REST.
 *
 * <p>Workbench parity: routes through {@link IPSContentDesignWs} (same design web service {@code
 * ContentDesignSOAPImpl} uses) — find / load / create / save / delete with design locks — not a
 * parallel path via {@code IPSContentService} alone.
 */
@PSSiteManageBean
public class KeywordsAdaptor implements IKeywordsAdaptor {

  private static final Logger log = LogManager.getLogger(KeywordsAdaptor.class);

  private final IPSContentDesignWs designWs;

  public KeywordsAdaptor() {
    this(PSContentWsLocator.getContentDesignWebservice());
  }

  /** Package-visible for unit tests that inject a fake design web service. */
  KeywordsAdaptor(IPSContentDesignWs designWs) {
    this.designWs = designWs;
  }

  @Override
  public List<KeywordSummary> listKeywords(URI baseUri, boolean includeChoices) {
    try {
      List<IPSCatalogSummary> summaries = designWs.findKeywords(null);
      if (summaries == null || summaries.isEmpty()) {
        return List.of();
      }
      List<IPSGuid> guids = new ArrayList<>();
      for (IPSCatalogSummary sum : summaries) {
        if (sum != null && sum.getGUID() != null) {
          guids.add(sum.getGUID());
        }
      }
      if (guids.isEmpty()) {
        return List.of();
      }
      List<PSKeyword> loaded =
          designWs.loadKeywords(guids, false, false, currentSession(), currentUser());
      List<KeywordSummary> out = new ArrayList<>();
      if (loaded != null) {
        for (PSKeyword kw : loaded) {
          if (kw == null || !isKeywordDef(kw)) {
            continue;
          }
          out.add(toSummary(kw, includeChoices));
        }
      }
      out.sort(
          Comparator.comparing(
              KeywordSummary::getLabel, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
      return out;
    } catch (PSErrorResultsException e) {
      log.error("Failed to list keywords via content design WS", e);
      throw new IllegalStateException("Failed to list keywords", e);
    } catch (RuntimeException e) {
      log.error("Failed to list keywords via content design WS", e);
      throw e;
    }
  }

  @Override
  public KeywordSummary getKeyword(URI baseUri, String idOrValue) {
    if (StringUtils.isBlank(idOrValue)) {
      return null;
    }
    PSKeyword kw = resolveKeyword(idOrValue.trim());
    return kw == null ? null : toSummary(kw, true);
  }

  @Override
  public KeywordSummary createKeyword(URI baseUri, KeywordSummary body) {
    if (body == null || StringUtils.isBlank(body.getLabel())) {
      throw new IllegalArgumentException("label is required");
    }
    requireSessionUserForWrite();
    String label = body.getLabel().trim();
    assertLabelUnique(label, null);
    String session = currentSession();
    String user = currentUser();
    try {
      List<PSKeyword> created =
          designWs.createKeywords(Collections.singletonList(label), session, user);
      if (created == null || created.isEmpty() || created.get(0) == null) {
        throw new IllegalStateException("Design WS createKeywords returned empty");
      }
      PSKeyword kw = created.get(0);
      if (body.getDescription() != null) {
        kw.setDescription(body.getDescription());
      }
      if (body.getSequence() != null) {
        kw.setSequence(body.getSequence());
      }
      applyChoices(kw, body.getChoices());
      designWs.saveKeywords(Collections.singletonList(kw), true, session, user);
      return reloadSummary(kw.getGUID());
    } catch (PSErrorsException e) {
      log.error("Failed to create keyword via content design WS", e);
      throw new IllegalStateException("Failed to create keyword", e);
    }
  }

  @Override
  public KeywordSummary updateKeyword(URI baseUri, String id, KeywordSummary body) {
    if (StringUtils.isBlank(id)) {
      throw new IllegalArgumentException("id is required");
    }
    if (body == null) {
      throw new IllegalArgumentException("body is required");
    }
    requireSessionUserForWrite();
    IPSGuid g = parseKeywordGuid(id.trim());
    if (g == null) {
      throw new IllegalArgumentException("Invalid keyword id: " + id);
    }
    String session = currentSession();
    String user = currentUser();
    try {
      List<PSKeyword> loaded =
          designWs.loadKeywords(Collections.singletonList(g), true, false, session, user);
      if (loaded == null || loaded.isEmpty() || loaded.get(0) == null) {
        return null;
      }
      PSKeyword kw = loaded.get(0);
      if (StringUtils.isNotBlank(body.getLabel())) {
        String newLabel = body.getLabel().trim();
        assertLabelUnique(newLabel, kw.getGUID());
        kw.setLabel(newLabel);
      }
      if (body.getDescription() != null) {
        kw.setDescription(body.getDescription());
      }
      if (body.getSequence() != null) {
        kw.setSequence(body.getSequence());
      }
      if (body.getChoices() != null) {
        applyChoices(kw, body.getChoices());
      }
      designWs.saveKeywords(Collections.singletonList(kw), true, session, user);
      return reloadSummary(kw.getGUID());
    } catch (PSErrorResultsException e) {
      if (isNotFound(e, g)) {
        return null;
      }
      log.error("Failed to load keyword for update via design WS: {}", id, e);
      throw new IllegalStateException("Failed to update keyword", e);
    } catch (PSErrorsException e) {
      log.error("Failed to save keyword via content design WS: {}", id, e);
      throw new IllegalStateException("Failed to update keyword", e);
    }
  }

  @Override
  public void deleteKeyword(URI baseUri, String id) {
    if (StringUtils.isBlank(id)) {
      throw new IllegalArgumentException("id is required");
    }
    requireSessionUserForWrite();
    IPSGuid g = parseKeywordGuid(id.trim());
    if (g == null) {
      throw new IllegalArgumentException("Invalid keyword id: " + id);
    }
    String session = currentSession();
    String user = currentUser();
    // Ensure exists (design load)
    try {
      designWs.loadKeywords(Collections.singletonList(g), false, false, session, user);
    } catch (PSErrorResultsException e) {
      throw new KeywordNotFoundException("Keyword not found: " + id);
    }
    try {
      designWs.deleteKeywords(Collections.singletonList(g), false, session, user);
    } catch (PSErrorsException e) {
      log.error("Failed to delete keyword via content design WS: {}", id, e);
      throw new IllegalStateException("Failed to delete keyword", e);
    }
  }

  private KeywordSummary reloadSummary(IPSGuid guid) {
    if (guid == null) {
      return null;
    }
    try {
      List<PSKeyword> loaded =
          designWs.loadKeywords(
              Collections.singletonList(guid), false, false, currentSession(), currentUser());
      if (loaded != null && !loaded.isEmpty() && loaded.get(0) != null) {
        return toSummary(loaded.get(0), true);
      }
    } catch (PSErrorResultsException e) {
      log.debug("Could not reload keyword after write: {}", e.getMessage());
    }
    return null;
  }

  private void assertLabelUnique(String label, IPSGuid excludeGuid) {
    List<IPSCatalogSummary> matches = designWs.findKeywords(label);
    if (matches == null) {
      return;
    }
    for (IPSCatalogSummary existing : matches) {
      if (existing == null) {
        continue;
      }
      String name = existing.getName();
      String sumLabel = existing.getLabel();
      boolean labelMatch =
          label.equalsIgnoreCase(StringUtils.defaultString(name))
              || label.equalsIgnoreCase(StringUtils.defaultString(sumLabel));
      if (!labelMatch) {
        continue;
      }
      if (excludeGuid != null
          && existing.getGUID() != null
          && excludeGuid.equals(existing.getGUID())) {
        continue;
      }
      throw new IllegalArgumentException("label already in use: " + label);
    }
  }

  private static boolean isKeywordDef(PSKeyword kw) {
    return kw.getKeywordType() == null || PSKeyword.KEYWORD_TYPE.equals(kw.getKeywordType());
  }

  private PSKeyword resolveKeyword(String idOrValue) {
    if (StringUtils.isNumeric(idOrValue) || idOrValue.contains("-")) {
      IPSGuid g = parseKeywordGuid(idOrValue);
      if (g != null) {
        try {
          List<PSKeyword> loaded =
              designWs.loadKeywords(
                  Collections.singletonList(g), false, false, currentSession(), currentUser());
          if (loaded != null && !loaded.isEmpty() && loaded.get(0) != null) {
            return loaded.get(0);
          }
        } catch (PSErrorResultsException e) {
          log.debug("Keyword id not found {}: {}", idOrValue, e.getMessage());
        }
      }
    }
    // Match by value or label among all defs loaded via design WS
    try {
      List<IPSCatalogSummary> summaries = designWs.findKeywords(null);
      if (summaries == null || summaries.isEmpty()) {
        return null;
      }
      List<IPSGuid> guids = new ArrayList<>();
      for (IPSCatalogSummary sum : summaries) {
        if (sum != null && sum.getGUID() != null) {
          guids.add(sum.getGUID());
        }
      }
      if (guids.isEmpty()) {
        return null;
      }
      List<PSKeyword> loaded =
          designWs.loadKeywords(guids, false, false, currentSession(), currentUser());
      if (loaded == null) {
        return null;
      }
      for (PSKeyword kw : loaded) {
        if (kw == null || !isKeywordDef(kw)) {
          continue;
        }
        if (idOrValue.equals(kw.getValue()) || idOrValue.equalsIgnoreCase(kw.getLabel())) {
          return kw;
        }
      }
    } catch (PSErrorResultsException e) {
      log.debug("Keyword resolve failed for {}: {}", idOrValue, e.getMessage());
    }
    return null;
  }

  /**
   * True when the design WS reported an error for the specific requested keyword GUID and did not
   * return a result for that GUID. Partial multi-id results are not treated as not-found for other
   * ids.
   */
  static boolean isNotFound(PSErrorResultsException e, IPSGuid requested) {
    if (e == null || requested == null) {
      return false;
    }
    Map<IPSGuid, Object> errors = e.getErrors();
    Map<IPSGuid, Object> results = e.getResults();
    boolean errored = errors != null && errors.containsKey(requested);
    boolean hasResult = results != null && results.containsKey(requested);
    return errored && !hasResult;
  }

  private static String currentSession() {
    return (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_JSESSIONID);
  }

  private static String currentUser() {
    return (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER);
  }

  private static void requireSessionUserForWrite() {
    if (StringUtils.isBlank(currentSession()) || StringUtils.isBlank(currentUser())) {
      throw new IllegalStateException(
          "session and user are required for keyword design create/update/delete"
              + " (IPSContentDesignWs)");
    }
  }

  private static IPSGuid parseKeywordGuid(String id) {
    try {
      if (StringUtils.isNumeric(id)) {
        return new PSGuid(PSTypeEnum.KEYWORD_DEF, Long.parseLong(id));
      }
      if (id.contains("-")) {
        PSGuid g = new PSGuid(id);
        if (g.getType() == 0) {
          g = new PSGuid(PSTypeEnum.KEYWORD_DEF, g.getUUID());
        }
        return g;
      }
    } catch (Exception e) {
      log.debug("Could not parse keyword id {}: {}", id, e.getMessage());
    }
    return null;
  }

  private static void applyChoices(PSKeyword kw, List<KeywordChoiceSummary> choices) {
    List<PSKeywordChoice> mapped = new ArrayList<>();
    if (choices != null) {
      for (KeywordChoiceSummary c : choices) {
        if (c == null || StringUtils.isBlank(c.getLabel())) {
          continue;
        }
        PSKeywordChoice ch = new PSKeywordChoice();
        ch.setLabel(c.getLabel());
        ch.setValue(StringUtils.defaultString(c.getValue()));
        ch.setDescription(c.getDescription());
        ch.setSequence(c.getSequence() != null ? c.getSequence() : 0);
        mapped.add(ch);
      }
    }
    kw.setChoices(mapped);
  }

  private static KeywordSummary toSummary(PSKeyword kw, boolean includeChoices) {
    KeywordSummary sum = new KeywordSummary();
    if (kw.getGUID() != null) {
      sum.setGuid(ApiUtils.convertGuid(kw.getGUID()));
    }
    sum.setLabel(kw.getLabel());
    sum.setValue(kw.getValue());
    sum.setDescription(kw.getDescription());
    sum.setSequence(kw.getSequence());
    if (includeChoices) {
      List<KeywordChoiceSummary> choices = new ArrayList<>();
      try {
        List<PSKeywordChoice> raw = kw.getChoices();
        if (raw != null) {
          for (PSKeywordChoice ch : raw) {
            if (ch == null) continue;
            KeywordChoiceSummary c = new KeywordChoiceSummary();
            c.setLabel(ch.getLabel());
            c.setValue(ch.getValue());
            c.setDescription(ch.getDescription());
            c.setSequence(ch.getSequence());
            choices.add(c);
          }
          choices.sort(
              Comparator.comparing(
                  KeywordChoiceSummary::getSequence, Comparator.nullsLast(Integer::compareTo)));
        }
      } catch (Exception e) {
        log.debug("Could not load choices for keyword {}: {}", kw.getLabel(), e.getMessage());
      }
      sum.setChoices(choices);
    }
    return sum;
  }
}
