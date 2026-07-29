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
import com.percussion.services.content.IPSContentService;
import com.percussion.services.content.PSContentServiceLocator;
import com.percussion.services.content.data.PSKeyword;
import com.percussion.services.content.data.PSKeywordChoice;
import com.percussion.system.utils.PSSiteManageBean;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@PSSiteManageBean
public class KeywordsAdaptor implements IKeywordsAdaptor {

  private static final Logger log = LogManager.getLogger(KeywordsAdaptor.class);

  @Override
  public List<KeywordSummary> listKeywords(URI baseUri, boolean includeChoices) {
    IPSContentService svc = PSContentServiceLocator.getContentService();
    // null label + sort by label — all keywords of type KEYWORD
    List<PSKeyword> keywords = svc.findKeywordsByLabel(null, "label");
    List<KeywordSummary> out = new ArrayList<>();
    for (PSKeyword kw : keywords) {
      if (kw == null) {
        continue;
      }
      // Skip choice rows (type != KEYWORD_TYPE); service may return both
      if (kw.getKeywordType() != null
          && !PSKeyword.KEYWORD_TYPE.equals(kw.getKeywordType())) {
        continue;
      }
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
                    KeywordChoiceSummary::getSequence,
                    Comparator.nullsLast(Integer::compareTo)));
          }
        } catch (Exception e) {
          log.debug("Could not load choices for keyword {}: {}", kw.getLabel(), e.getMessage());
        }
        sum.setChoices(choices);
      }
      out.add(sum);
    }
    out.sort(
        Comparator.comparing(
            k -> k.getLabel() != null ? k.getLabel() : "", String.CASE_INSENSITIVE_ORDER));
    return out;
  }
}
