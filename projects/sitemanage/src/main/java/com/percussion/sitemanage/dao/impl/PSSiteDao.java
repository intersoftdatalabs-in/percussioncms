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
package com.percussion.sitemanage.dao.impl;

import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;

import com.percussion.cms.IPSConstants;
import com.percussion.error.PSException;
import com.percussion.fastforward.managednav.IPSNavigationErrors;
import com.percussion.fastforward.managednav.PSNavException;
import com.percussion.pathmanagement.service.impl.PSPathUtils;
import com.percussion.pubserver.IPSPubServerService;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.share.dao.PSFolderPathUtils;
import com.percussion.share.data.PSItemSummaryUtils;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.sitemanage.dao.IPSSiteContentDao;
import com.percussion.sitemanage.dao.IPSSitePublishDao;
import com.percussion.sitemanage.dao.IPSiteDao;
import com.percussion.sitemanage.data.PSSite;
import com.percussion.sitemanage.data.PSSitePublishProperties;
import com.percussion.sitemanage.data.PSSiteSummary;
import com.percussion.sitemanage.error.IPSSiteManageErrors;
import com.percussion.util.PSPathUtil;
import com.percussion.webservices.PSErrorsException;
import com.percussion.webservices.publishing.PSPublishingWsLocator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** The site handler which is used for site management. */
@Component("siteDao")
@Lazy
public class PSSiteDao implements IPSiteDao {

  private final IPSSiteContentDao siteContentDao;
  private final IPSSitePublishDao sitePublishDao;

  @Autowired
  public PSSiteDao(IPSSiteContentDao siteContentDao, IPSSitePublishDao sitePublishDao) {
    this.siteContentDao = siteContentDao;
    this.sitePublishDao = sitePublishDao;
  }

  @Override
  public PSSite find(String id) {
    try {
      return loadSite(id);
    } catch (LoadException | PSNavException | DeleteException e) {
      log.warn(
          "Error loading site with id: {} Error: {}.  Skipping loading site definition.",
          id,
          PSExceptionUtils.getMessageForLog(e));
      return null;
    }
  }

  @Override
  public PSSiteSummary findByLegacySiteId(String id, boolean isValidate) {
    return sitePublishDao.findByLegacySiteId(id, isValidate);
  }

  @Override
  public List<PSSiteSummary> findAllSummaries() {
    var sums = sitePublishDao.findAllSummaries();
    sums.sort(siteComp);
    return sums;
  }

  public PSSiteSummary findSummary(String id) throws LoadException {
    return sitePublishDao.findSummary(id);
  }

  @Override
  public List<PSSite> findAll() {
    var sites = new ArrayList<PSSite>();
    var sums = findAllSummaries();
    for (var pubSite : sums) {
      var name = pubSite.getName();
      var site = find(name);
      if (site != null) {
        sites.add(site);
      }
    }
    sites.sort(siteComp);
    return sites;
  }

  // legacy delete method retained for compatibility
  @Transactional
  public void delete(String id) throws DeleteException {
    try {
      deleteSite(id);
    } catch (Exception e) {
      throw new DeleteException("Error deleting site", e);
    }
  }

  @Override
  public void remove(PSSite object) throws PSDataServiceException {
    if (object == null) {
      throw new IllegalArgumentException("object must not be null");
    }
    remove(object.getName());
  }

  @Override
  public void remove(String id) throws PSDataServiceException {
    // delegate to existing delete method which throws DeleteException
    try {
      delete(id);
    } catch (DeleteException e) {
      throw e; // DeleteException is a PSDataServiceException subclass
    }
  }

  @Transactional
  public PSSite save(PSSite site) throws SaveException {
    try {
      saveSite(site, null);
      return site;
    } catch (Exception e) {
      // Log nested cause — outer Spring tx often only surfaces UnexpectedRollbackException
      log.error(
          "Error saving site name={}: {}", site != null ? site.getName() : null, e.toString(), e);
      throw new SaveException("Error saving site", e);
    }
  }

  protected PSSite loadSite(String name) throws LoadException, PSNavException, DeleteException {
    var sum = findSummary(name);
    if (sum == null) return null;
    try {
      return summaryToFull(sum);
    } catch (PSNavException e) {
      if (e.getErrorCode() == IPSNavigationErrors.NAVIGATION_SERVICE_FOLDER_ID_NOT_FOUND_FOR_PATH) {
        var ex =
            new PSException(IPSSiteManageErrors.SITE_MANAGE_SERVICE_DELETING_BAD_SITE_RECORD, name);
        log.warn(PSExceptionUtils.getMessageForLog(ex));
        log.debug(ex);
        this.delete(sum.getId());
      } else {
        throw e;
      }
    } catch (Exception e) {
      throw new LoadException("Error loading site", e);
    }
    return null;
  }

  @Transactional
  protected void deleteSite(String name) throws PSErrorsException, DeleteException {
    log.info("Starting delete of site {}", name);
    var publishWs = PSPublishingWsLocator.getPublishingWebservice();
    var site = publishWs.findSite(name);
    if (site == null) {
      throw new DeleteException("Cannot delete site because site does not exist, site: " + name);
    }
    var summary = new PSSiteSummary();
    sitePublishDao.convertToSummary(site, summary);
    siteContentDao.deleteRelatedItems(summary);
    sitePublishDao.deleteSite(name);
  }

  @Transactional
  protected void saveSite(PSSite site, PSSite origSite)
      throws IPSPubServerService.PSPubServerServiceException, PSNotFoundException {
    notNull(site, "site may not be null");
    var isNew = sitePublishDao.saveSite(site);
    if (isNew) {
      if (origSite == null) {
        siteContentDao.createRelatedItems(site);
      } else {
        siteContentDao.copy(origSite, site);
      }
    }
  }

  @Transactional
  public boolean updateSite(IPSSite site, String newName, String newDescrption)
      throws PSNotFoundException {
    return sitePublishDao.updateSite(site, newName, newDescrption);
  }

  @Transactional
  public void updateSitePublishProperties(IPSSite site, PSSitePublishProperties publishProps)
      throws PSNotFoundException {
    notNull(site, "site may not be null");
    notNull(publishProps, "publishProps may not be null");
    sitePublishDao.updateSitePublishProperties(site, publishProps);
  }

  @Transactional
  public void addPublishNow(IPSSite site) throws PSNotFoundException {
    notNull(site, "site may not be null");
    sitePublishDao.addPublishNow(site);
  }

  @Transactional
  public void addUnpublishNow(IPSSite site) throws PSNotFoundException {
    notNull(site, "site may not be null");
    sitePublishDao.addUnpublishNow(site);
  }

  public String getSiteDeliveryType(IPSSite site) throws PSNotFoundException {
    return sitePublishDao.getSiteDeliveryType(site);
  }

  @Transactional
  public PSSite createSiteWithContent(String origId, String newName)
      throws PSDataServiceException,
          IPSPubServerService.PSPubServerServiceException,
          PSNotFoundException {
    notEmpty(origId, "origId may not be blank");
    notEmpty(newName, "newName may not be blank");
    var orig = find(origId);
    if (orig != null) {
      var copy = new PSSite();
      copy.setBaseTemplateName(orig.getBaseTemplateName());
      copy.setDescription(orig.getDescription().orElse(null));
      copy.setHomePageTitle(orig.getHomePageTitle());
      copy.setName(newName);
      copy.setNavigationTitle(orig.getNavigationTitle());
      copy.setLabel(newName);
      copy.setTemplateName(orig.getTemplateName());
      saveSite(copy, orig);
      return copy;
    } else {
      return null;
    }
  }

  public PSSiteSummary findByName(String name) {
    Validate.notEmpty(name);
    var sums = findAllSummaries();
    return sums.stream()
        .filter(sum -> sum.getName().equalsIgnoreCase(name))
        .findFirst()
        .orElse(null);
  }

  public PSSiteSummary findByPath(String path) {
    Validate.notEmpty(path);
    var sums = findAllSummaries();
    if (path.startsWith("/Sites/")) {
      path = "/" + path;
    } else if (!path.startsWith(PSPathUtil.SITES_ROOT)) {
      path = PSPathUtil.SITES_ROOT + "/" + path;
    }
    var siteName = PSPathUtils.getSiteFromPath(path);
    for (var sum : sums) {
      if (sum.getFolderPath() != null
          && PSFolderPathUtils.isDescedentPath(path, sum.getFolderPath())) {
        return sum;
      } else if (sum.getName().equalsIgnoreCase(siteName)) {
        return sum;
      }
    }
    return null;
  }

  protected PSSite summaryToFull(PSSiteSummary site) throws PSNavException, PSDataServiceException {
    notNull(site, "site may not be null");
    var s = new PSSite();
    PSItemSummaryUtils.copyProperties(site, s);
    s.setSiteId(site.getSiteId().orElse(null));
    s.setDefaultFileExtention(site.getDefaultFileExtention().orElse(null));
    s.setCanonical(site.isCanonical());
    s.setCanonicalDist(site.getCanonicalDist());
    s.setCanonicalReplace(site.isCanonicalReplace());
    s.setSiteProtocol(site.getSiteProtocol());
    s.setDefaultDocument(site.getDefaultDocument());
    s.setBaseUrl(site.getSiteProtocol() + "//" + s.getName());
    s.setPageBased(site.isPageBased());
    var homepage = siteContentDao.getHomePage(site);
    var navTitle = siteContentDao.getNavTitle(site);
    if (homepage != null) {
      s.setBaseTemplateName(homepage.getTemplateId());
      s.setHomePageTitle(homepage.getTitle());
    } else {
      log.error("No homepage for site: {}", site.getName());
    }
    s.setDescription(site.getDescription().orElse(null));
    s.setNavigationTitle(navTitle);
    siteContentDao.loadTemplateInfo(s);
    return s;
  }

  public static class PSSiteSummaryComparator implements Comparator<PSSiteSummary> {
    @Override
    public int compare(PSSiteSummary s1, PSSiteSummary s2) {
      if (s1 != null && s2 != null) {
        if (s1.getName() != null && s2.getName() != null) {
          return s1.getName().compareToIgnoreCase(s2.getName());
        } else {
          if (s1.getName() == null) {
            return (s2.getName() == null) ? 0 : 1;
          } else {
            return -1;
          }
        }
      } else {
        return 0;
      }
    }
  }

  private final PSSiteSummaryComparator siteComp = new PSSiteSummaryComparator();
  private static final Logger log = LogManager.getLogger(IPSConstants.CONTENTREPOSITORY_LOG);
}
