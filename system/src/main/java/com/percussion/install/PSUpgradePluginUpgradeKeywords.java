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
package com.percussion.install;

import com.percussion.services.content.IPSContentService;
import com.percussion.services.content.PSContentServiceLocator;
import com.percussion.services.content.data.PSKeyword;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;

/**
 * Converts keyword names to the form required by Rhythmyx 6.0 - unique, no whitespaces.
 *
 * @author Andriy Palamarchuk
 */
public class PSUpgradePluginUpgradeKeywords extends PSSpringUpgradePluginBase
// REFACTORED: CP-JAVA11
{

  public PSPluginResponse process(IPSUpgradeModule config, Element elemData) {
    m_config = config;
    log("Fixing Keyword Names");

    final List<PSKeyword> keywords = getContentService().findKeywordsByLabel(null, null);
    final List<PSKeyword> keywordsToFixNames = findKeywordsToFixNames(keywords);
    final Set<String> keywordNames = getKeywordNames(keywords);

    for (PSKeyword keyword : keywordsToFixNames) {
      keyword.setName(PSNameSpacesUtil.removeWhitespacesFromName(keyword.getName(), keywordNames));
      keywordNames.add(keyword.getName());
      getContentService().saveKeyword(keyword);
    }

    return new PSPluginResponse(PSPluginResponse.SUCCESS, "");
  }

  /** Selects keywords from the list which need their names to be fixed. */
  private List<PSKeyword> findKeywordsToFixNames(final List<PSKeyword> keywords) {
    final List<PSKeyword> keywordsToFixNames = new ArrayList<>();
    for (PSKeyword keyword : keywords) {
      final String name = keyword.getName();
      if (!name.equals(StringUtils.deleteWhitespace(name))) {
        keywordsToFixNames.add(keyword);
      }
    }
    return keywordsToFixNames;
  }

  /** Set of keyword names extracted from the provided keywords. */
  private Set<String> getKeywordNames(final List<PSKeyword> keywords) {
    final Set<String> names = new HashSet<>();
    for (PSKeyword keyword : keywords) {
      names.add(keyword.getName());
    }
    return names;
  }

  /** Convenience method to access content service. */
  private IPSContentService getContentService() {
    return PSContentServiceLocator.getContentService();
  }

  /**
   * Prints message to the log printstream if it exists or just sends it to System.out
   *
   * @param msg the message to be logged, can be <code>null</code>.
   */
  private void log(String msg) {
    if (msg == null) {
      return;
    }

    if (m_config != null) {
      m_config.getLogStream().println(msg);
    } else {
      System.out.println(msg);
    }
  }

  /** Used for logging, initialized in {@link #process(IPSUpgradeModule, Element)} */
  private IPSUpgradeModule m_config;
}
