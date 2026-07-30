/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

package com.percussion.apibridge;

import com.percussion.cms.objectstore.PSSFields;
import com.percussion.cms.objectstore.PSSearch;
import com.percussion.cms.objectstore.PSSearchField;
import com.percussion.rest.Guid;
import com.percussion.rest.searches.ISearchAdaptor;
import com.percussion.rest.searches.SearchDef;
import com.percussion.rest.searches.SearchFieldSummary;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.ui.IPSUiDesignWs;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/** Read-only CX search definition catalog (UI-06). */
@PSSiteManageBean
@Lazy
public class SearchAdaptor implements ISearchAdaptor {

  private static final Logger log = LogManager.getLogger(SearchAdaptor.class);

  private static final List<String> DESIGN_GAPS =
      List.of(
          "Search create / update / delete not supported via this API",
          "Search field criterion editing not supported via this API",
          "Views are a separate catalog (Developer Views / UI-07)");

  private final IPSUiDesignWs designWs;

  @Autowired
  public SearchAdaptor(IPSUiDesignWs designWs) {
    this.designWs = designWs;
  }

  @Override
  public List<SearchDef> listSearches() {
    try {
      List<IPSCatalogSummary> summaries = designWs.findSearches(null, null);
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
      String currentUser = (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER);
      String currentSession = (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_JSESSIONID);
      List<PSSearch> loaded =
          designWs.loadSearches(guids, false, false, currentSession, currentUser);
      List<SearchDef> out = new ArrayList<>();
      if (loaded != null) {
        for (PSSearch s : loaded) {
          if (s != null) {
            out.add(toDef(s));
          }
        }
      }
      out.sort(
          Comparator.comparing(
              SearchDef::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
      return out;
    } catch (Exception e) {
      log.error("Failed to list searches", e);
      throw new IllegalStateException("Failed to list searches", e);
    }
  }

  @Override
  public SearchDef findSearchByKey(String idOrName) {
    if (!isSafeSearchKey(idOrName)) {
      return null;
    }
    String key = idOrName.trim();
    try {
      for (SearchDef s : listSearches()) {
        if (s == null) {
          continue;
        }
        if (key.equalsIgnoreCase(s.getName())) {
          return s;
        }
        if (s.getGuid() != null) {
          String gsv = s.getGuid().getStringValue().orElse(null);
          if (key.equalsIgnoreCase(gsv)) {
            return s;
          }
        }
        if (String.valueOf(s.getId()).equals(key)) {
          return s;
        }
      }
      return null;
    } catch (RuntimeException e) {
      // list failures already wrap; propagate for 500
      throw e;
    }
  }

  static SearchDef toDef(PSSearch s) {
    SearchDef d = new SearchDef();
    if (s.getGUID() != null) {
      d.setGuid(copyGuid(s.getGUID()));
    }
    d.setId(s.getId());
    d.setName(s.getName());
    d.setLabel(s.getLabel());
    d.setDescription(s.getDescription());
    d.setType(s.getType());
    d.setDisplayFormatId(s.getDisplayFormatId());
    d.setUrl(s.getUrl());
    d.setParentCategory(s.getParentCategory());
    d.setMaximumResultSize(s.getMaximumResultSize());
    d.setUserSearch(s.isUserSearch());
    d.setCustomSearch(s.isCustomSearch());
    d.setStandardSearch(s.isStandardSearch());
    d.setUserCustomizable(s.isUserCustomizable());
    d.setCaseSensitive(s.isCaseSensitive());
    d.setFields(mapFields(s.getFieldContainer()));
    d.setDesignGaps(new ArrayList<>(DESIGN_GAPS));
    return d;
  }

  static List<SearchFieldSummary> mapFields(PSSFields fields) {
    List<SearchFieldSummary> out = new ArrayList<>();
    if (fields == null) {
      return out;
    }
    for (int i = 0; i < fields.size(); i++) {
      Object o = fields.get(i);
      if (!(o instanceof PSSearchField sf) || sf == null) {
        continue;
      }
      SearchFieldSummary row = new SearchFieldSummary();
      row.setFieldName(sf.getFieldName());
      row.setDisplayName(sf.getDisplayName());
      row.setOperator(sf.getOperator());
      row.setFieldValue(sf.getFieldValue());
      row.setFieldType(sf.getFieldType());
      row.setPosition(sf.getPosition());
      out.add(row);
    }
    return out;
  }

  private static Guid copyGuid(IPSGuid guid) {
    Guid g = new Guid();
    g.setHostId(guid.getHostId());
    g.setLongValue(guid.longValue());
    g.setStringValue(guid.toString());
    g.setType(guid.getType());
    g.setUuid(guid.getUUID());
    g.setUntypedString(guid.toStringUntyped());
    return g;
  }

  /**
   * Single path component / guid token only — reject traversal and separators ({@code
   * java/path-injection}).
   */
  static boolean isSafeSearchKey(String key) {
    if (key == null || key.isBlank()) {
      return false;
    }
    return !key.contains("..")
        && key.indexOf('/') < 0
        && key.indexOf('\\') < 0
        && key.indexOf('\0') < 0;
  }
}
