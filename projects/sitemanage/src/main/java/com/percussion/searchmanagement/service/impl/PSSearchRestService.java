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
package com.percussion.searchmanagement.service.impl;

import com.percussion.itemmanagement.service.IPSItemService;
import com.percussion.searchmanagement.data.PSSearchCriteria;
import com.percussion.searchmanagement.error.PSSearchServiceException;
import com.percussion.searchmanagement.service.IPSSearchService;
import com.percussion.security.PSOperationContext;
import com.percussion.security.SecureStringUtils;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.system.IPSSystemService;
import com.percussion.share.data.PSPagedItemList;
import com.percussion.share.data.PSPagedItemPropertiesList;
import com.percussion.share.service.IPSDataService;
import com.percussion.share.service.exception.PSParametersValidationException;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.share.validation.PSValidationErrorsBuilder;
import com.percussion.webservices.PSWebserviceUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** REST service for search operations. */
@Path("/search")
@Component("searchRestService")
@Tag(name = "/search")
public class PSSearchRestService {
  private static final String SEARCH_TYPE_MY_PAGES = "MyPages";
  private static final Logger log = LogManager.getLogger(PSSearchRestService.class);

  private final IPSSearchService searchService;
  private final IPSItemService itemService;
  private final IPSSystemService systemService;

  @Autowired
  public PSSearchRestService(
      IPSSearchService finderSearchService,
      IPSItemService itemService,
      IPSSystemService systemService) {
    this.searchService = finderSearchService;
    this.itemService = itemService;
    this.systemService = systemService;
  }

  /** Sanitize any input for invalid characters / parameters. */
  protected void sanitizeCriteria(PSSearchCriteria criteria) {
    if (criteria != null) {
      var q = criteria.getQuery();
      if (q != null) {
        q = SecureStringUtils.sanitizeStringForHTML(q);
        q = QueryParser.escape(q);
        criteria.setQuery(q);
      }

      criteria.setSortColumn(
          SecureStringUtils.removeInvalidSQLObjectNameCharacters(criteria.getSortColumn()));
      criteria.setSearchType(
          SecureStringUtils.removeInvalidSQLObjectNameCharacters(criteria.getSearchType()));

      var fields = criteria.getSearchFields();
      if (fields != null) {
        fields.replaceAll((k, v) -> SecureStringUtils.sanitizeStringForHTML(fields.get(k)));
      }
      if (criteria.getFolderPath() != null
          && !SecureStringUtils.isValidCMSPathString(
              criteria.getFolderPath(), PSOperationContext.SEARCH)) {
        criteria.setFolderPath(null);
      }
    }
  }

  @POST
  @Path("/get")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSPagedItemList search(PSSearchCriteria criteria) throws PSSearchServiceException {
    try {
      criteria = searchService.validateSearchCriteria(criteria);
      sanitizeCriteria(criteria);

      var itemList = new PSPagedItemList();

      // Don't run a blind search for all items - require some criteria
      if (!criteria.isEmpty()) {
        if (SEARCH_TYPE_MY_PAGES.equalsIgnoreCase(criteria.getSearchType())) {
          var userItems = itemService.getUserItems(PSWebserviceUtils.getUserName());
          var contentIds = new ArrayList<Integer>();
          for (var userItem : userItems) {
            contentIds.add(userItem.getItemId());
          }
          itemList = searchService.search(criteria, contentIds);
        } else {
          try {
            itemList = searchService.search(criteria);
          } catch (PSSearchServiceException e) {
            return checkAndThrowValidationException(e, criteria);
          }
        }
      }
      return itemList;
    } catch (PSNotFoundException
        | IPSDataService.DataServiceLoadException
        | PSValidationException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  @POST
  @Path("/get/extendedresults")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSPagedItemPropertiesList extendedSearch(PSSearchCriteria criteria)
      throws PSSearchServiceException {
    criteria = searchService.validateSearchCriteria(criteria);
    sanitizeCriteria(criteria);
    try {
      return searchService.getExtendedSearchResults(criteria);
    } catch (PSSearchServiceException e) {
      try {
        return checkAndThrowValidationException(e, criteria);
      } catch (PSValidationException ve) {
        throw new WebApplicationException(ve);
      }
    }
  }

  /**
   * If the failure chain contains a Lucene {@code ParseException}, throw a field validation error;
   * otherwise rethrow the original search exception. Never returns normally.
   *
   * @param <T> dummy return type so callers can {@code return} this and satisfy control-flow
   */
  private <T> T checkAndThrowValidationException(
      PSSearchServiceException e, PSSearchCriteria criteria)
      throws PSValidationException, PSSearchServiceException {
    Throwable cause = e.getCause();
    while (cause != null) {
      if (cause instanceof org.apache.lucene.queryparser.classic.ParseException) {
        PSValidationErrorsBuilder builder = new PSValidationErrorsBuilder("PSSearchRestService");
        builder.rejectField(
            "query",
            "invalid",
            "The search query is invalid: " + cause.getMessage(),
            criteria.getQuery());
        throw new PSParametersValidationException(builder.build());
      }
      cause = cause.getCause();
    }
    throw e;
  }
}
