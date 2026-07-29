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

package com.percussion.apibridge;

import com.percussion.rest.keywords.IKeywordsAdaptor;
import com.percussion.rest.keywords.KeywordChoiceSummary;
import com.percussion.rest.keywords.KeywordSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.content.IPSContentService;
import com.percussion.services.content.PSContentException;
import com.percussion.services.content.PSContentServiceLocator;
import com.percussion.services.content.data.PSKeyword;
import com.percussion.services.content.data.PSKeywordChoice;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.utils.guid.IPSGuid;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Keyword design catalog + create/update/delete for the Developer module. */
@PSSiteManageBean
public class KeywordsAdaptor implements IKeywordsAdaptor {

  private static final Logger log = LogManager.getLogger(KeywordsAdaptor.class);

  private final IPSContentService contentService;

  public KeywordsAdaptor() {
    this(PSContentServiceLocator.getContentService());
  }

  /** Package-visible for unit tests that inject a fake content service. */
  KeywordsAdaptor(IPSContentService contentService) {
    this.contentService = contentService;
  }

  private IPSContentService svc() {
    return contentService;
  }

  @Override
  public List<KeywordSummary> listKeywords(URI baseUri, boolean includeChoices) {
    // baseUri reserved for HATEOAS link building (interface contract)
    List<PSKeyword> keywords = svc().findKeywordsByLabel(null, "label");
    List<KeywordSummary> out = new ArrayList<>();
    for (PSKeyword kw : keywords) {
      if (kw == null || !isKeywordDef(kw)) {
        continue;
      }
      out.add(toSummary(kw, includeChoices));
    }
    out.sort(
        Comparator.comparing(
            KeywordSummary::getLabel, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
    return out;
  }

  @Override
  public KeywordSummary getKeyword(URI baseUri, String idOrValue) {
    // baseUri reserved for HATEOAS link building (interface contract)
    if (StringUtils.isBlank(idOrValue)) {
      return null;
    }
    PSKeyword kw = resolveKeyword(svc(), idOrValue.trim());
    return kw == null ? null : toSummary(kw, true);
  }

  @Override
  public KeywordSummary createKeyword(URI baseUri, KeywordSummary body) {
    if (body == null || StringUtils.isBlank(body.getLabel())) {
      throw new IllegalArgumentException("label is required");
    }
    IPSContentService svc = svc();
    PSKeyword kw = svc.createKeyword(body.getLabel().trim(), body.getDescription());
    if (body.getSequence() != null) {
      kw.setSequence(body.getSequence());
    }
    applyChoices(kw, body.getChoices());
    svc.saveKeyword(kw);
    try {
      PSKeyword reloaded = svc.loadKeyword(kw.getGUID(), "sequence");
      return toSummary(reloaded, true);
    } catch (PSContentException e) {
      log.debug("Could not reload keyword after create: {}", e.getMessage());
      return toSummary(kw, true);
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
    IPSContentService svc = svc();
    PSKeyword kw = loadById(svc, id.trim());
    if (kw == null) {
      return null;
    }
    if (StringUtils.isNotBlank(body.getLabel())) {
      kw.setLabel(body.getLabel().trim());
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
    svc.saveKeyword(kw);
    try {
      return toSummary(svc.loadKeyword(kw.getGUID(), "sequence"), true);
    } catch (PSContentException e) {
      log.debug("Could not reload keyword after update: {}", e.getMessage());
      return toSummary(kw, true);
    }
  }

  @Override
  public void deleteKeyword(URI baseUri, String id) {
    if (StringUtils.isBlank(id)) {
      throw new IllegalArgumentException("id is required");
    }
    IPSContentService svc = svc();
    IPSGuid g = parseKeywordGuid(id.trim());
    if (g == null) {
      throw new IllegalArgumentException("Invalid keyword id: " + id);
    }
    try {
      svc.loadKeyword(g, null);
    } catch (PSContentException e) {
      throw new IllegalArgumentException("Keyword not found: " + id);
    }
    svc.deleteKeyword(g);
  }

  private static boolean isKeywordDef(PSKeyword kw) {
    return kw.getKeywordType() == null || PSKeyword.KEYWORD_TYPE.equals(kw.getKeywordType());
  }

  private static PSKeyword resolveKeyword(IPSContentService svc, String idOrValue) {
    if (StringUtils.isNumeric(idOrValue) || idOrValue.contains("-")) {
      PSKeyword byId = loadById(svc, idOrValue);
      if (byId != null) {
        return byId;
      }
    }
    // Match by value among keyword defs
    List<PSKeyword> all = svc.findKeywordsByLabel(null, null);
    for (PSKeyword kw : all) {
      if (kw != null && isKeywordDef(kw) && idOrValue.equals(kw.getValue())) {
        return kw;
      }
    }
    return null;
  }

  private static PSKeyword loadById(IPSContentService svc, String id) {
    IPSGuid g = parseKeywordGuid(id);
    if (g == null) {
      return null;
    }
    try {
      return svc.loadKeyword(g, "sequence");
    } catch (PSContentException e) {
      return null;
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
