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
/*
 * @author davidbenua
 *
 */
package com.percussion.pso.jexl;

import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.error.PSException;
import com.percussion.extension.IPSJexlExpression;
import com.percussion.extension.IPSJexlMethod;
import com.percussion.extension.IPSJexlParam;
import com.percussion.extension.PSJexlUtilBase;
import com.percussion.pso.utils.PSOItemSummaryFinder;
import com.percussion.server.PSRequest;
import com.percussion.server.PSUserSession;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.content.data.PSContentTypeSummary;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.contentmgr.IPSNode;
import com.percussion.services.contentmgr.PSContentMgrLocator;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.security.data.PSCommunity;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.services.workflow.PSWorkflowServiceLocator;
import com.percussion.services.workflow.data.PSState;
import com.percussion.system.utils.IPSHtmlParameters;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.content.PSContentWsLocator;
import com.percussion.webservices.security.IPSSecurityWs;
import com.percussion.webservices.security.PSSecurityWsLocator;
import java.util.Collections;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * JEXL function for locating various legacy objects by GUID. These functions are commonly available
 * in the Java API, but not directly accessible in JEXL.
 *
 * @author davidbenua
 */
public class PSOObjectFinder extends PSJexlUtilBase implements IPSJexlExpression, IPSOObjectFinder {
  /** Logger for this class */
  private static final Logger log = LogManager.getLogger(PSOObjectFinder.class);

  /** Content Web Service pointer. */
  private static IPSContentWs cws = null;

  private static IPSGuidManager gmgr = null;

  private static IPSContentMgr cmgr = null;

  private static IPSSecurityWs sws = null;

  private static IPSWorkflowService wf = null;

    /**
     * Creates a new PSOObjectFinder.
     */
    public PSOObjectFinder() {
    super();
  }

  /** Initialize Java services. Must be called before any Java Services are accessed. */
  private static void initServices() {
    if (cws == null) {
      cws = PSContentWsLocator.getContentWebservice();
    }

    if (wf == null) {
      wf = PSWorkflowServiceLocator.getWorkflowService();
    }

    if (gmgr == null) {
      gmgr = PSGuidManagerLocator.getGuidMgr();
    }

    if (cmgr == null) {
      cmgr = PSContentMgrLocator.getContentMgr();
    }

    if (sws == null) {
      sws = PSSecurityWsLocator.getSecurityWebservice();
    }
  }

  /**
   * @see
   *     com.percussion.pso.jexl.IPSOObjectFinder#getComponentSummary(com.percussion.utils.guid.IPSGuid)
   */
  @IPSJexlMethod(
      description = "get the Legacy Component Summary for an item",
      params = {@IPSJexlParam(name = "guid", description = "the item GUID")})
  /**
   * Returns the component summary.
   *
   * @param guid the guid
   * @return the result
   * @throws PSException if an error occurs
   */
  public PSComponentSummary getComponentSummary(IPSGuid guid) throws PSException {
    return PSOItemSummaryFinder.getSummary(guid);
  }

  /**
   * See referenced member.
   * @see com.percussion.pso.jexl.IPSOObjectFinder#getComponentSummaryById(java.lang.String)
   */
  @IPSJexlMethod(
      description = "get the Legacy Component Summary for an item",
      params = {@IPSJexlParam(name = "content", description = "the content id")})
  /**
   * Returns the component summary by id.
   *
   * @param contentid the contentid
   * @return the result
   * @throws PSException if an error occurs
   */
  public PSComponentSummary getComponentSummaryById(String contentid) throws PSException {
    return PSOItemSummaryFinder.getSummary(contentid);
  }

  /**
   * @see
   *     com.percussion.pso.jexl.IPSOObjectFinder#getContentTypeSummary(com.percussion.utils.guid.IPSGuid)
   */
  @IPSJexlMethod(
      description = "get the content type summary for a specified type",
      params = {@IPSJexlParam(name = "guid", description = "the content type GUID")})
  /**
   * Returns the content type summary.
   *
   * @param guid the guid
   * @return the result
   */
  public PSContentTypeSummary getContentTypeSummary(IPSGuid guid) {
    initServices();
    List<PSContentTypeSummary> ctypes = cws.loadContentTypes(null);
    for (PSContentTypeSummary ctype : ctypes) {
      if (ctype.getGuid().longValue() == guid.longValue()) {
        log.debug("found Content type {}", ctype.getName());
        return ctype;
      }
    }
    return null;
  }

  /**
   * See referenced member.
   * @see com.percussion.pso.jexl.IPSOObjectFinder#getJSessionId()
   */
  @IPSJexlMethod(
      description = "Get the JSESSIONID value for the current request",
      params = {})
  /**
   * Returns the jsession id.
   *
   * @return the result
   */
  public String getJSessionId() {
    String jsession = PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_JSESSIONID).toString();
    log.debug("JSESSIONID= {}", jsession);
    return jsession;
  }

  /**
   * See referenced member.
   * @see com.percussion.pso.jexl.IPSOObjectFinder#getPSSessionId()
   */
  @IPSJexlMethod(
      description = "Get the PSSESSIONID value for the current request",
      params = {})
  /**
   * Returns the pssession id.
   *
   * @return the result
   */
  public String getPSSessionId() {
    PSRequest req = (PSRequest) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_PSREQUEST);
    String sessionid = req.getUserSessionId();
    log.debug("PSSessionId={}", sessionid);
    return sessionid;
  }

  /**
   * See referenced member.
   * @see com.percussion.pso.jexl.IPSOObjectFinder#getUserLocale()
   */
  @IPSJexlMethod(
      description = "get the users current locale",
      params = {})
  /**
   * Returns the user locale.
   *
   * @return the result
   */
  public String getUserLocale() {
    PSUserSession session = getSession();
    Object obj = session.getPrivateObject(IPSHtmlParameters.SYS_LANG);
    if (obj != null) {
      return obj.toString();
    }
    return null;
  }

  /**
   * See referenced member.
   * @see com.percussion.pso.jexl.IPSOObjectFinder#getUserCommunity()
   */
  @IPSJexlMethod(
      description = "get the users current community name",
      params = {})
  /**
   * Returns the user community.
   *
   * @return the result
   */
  public String getUserCommunity() {
    PSUserSession session = getSession();
    return session.getUserCurrentCommunity();
  }

  /**
   * See referenced member.
   * @see com.percussion.pso.jexl.IPSOObjectFinder#getUserCommunityId()
   */
  @IPSJexlMethod(
      description = "get the users current community id",
      params = {})
  /**
   * Returns the user community id.
   *
   * @return the result
   */
  public String getUserCommunityId() {
    PSUserSession session = getSession();
    Object obj = session.getPrivateObject(IPSHtmlParameters.SYS_COMMUNITY);
    if (obj != null) {
      return obj.toString();
    }
    return null;
  }

  /**
   * See referenced member.
   * @see com.percussion.pso.jexl.IPSOObjectFinder#getGuidById(java.lang.String, java.lang.String)
   */
  @IPSJexlMethod(
      description = "get the GUID by Content Id and Revision",
      params = {
        @IPSJexlParam(name = "contentid", description = "the content id"),
        @IPSJexlParam(name = "revision", description = "the revision")
      })
  /**
   * Returns the guid by id.
   *
   * @param contentid the contentid
   * @param revision the revision
   * @return the result
   */
  public IPSGuid getGuidById(String contentid, String revision) {
    initServices();
    PSLocator loc = new PSLocator(contentid, revision);
    return gmgr.makeGuid(loc);
  }

  /**
   * See referenced member.
   * @see com.percussion.pso.jexl.IPSOObjectFinder#getGuidById(java.lang.String)
   */
  @IPSJexlMethod(
      description = "get the GUID by Content Id",
      params = {@IPSJexlParam(name = "contentid", description = "the content id")})
  /**
   * Returns the guid by id.
   *
   * @param contentid the contentid
   * @return the result
   */
  public IPSGuid getGuidById(String contentid) {
    initServices();
    PSLocator loc = new PSLocator(contentid);
    return gmgr.makeGuid(loc);
  }

  /**
   * See referenced member.
   * @see com.percussion.pso.jexl.IPSOObjectFinder#getNodeByGuid(com.percussion.utils.guid.IPSGuid)
   */
  @IPSJexlMethod(
      description = "get the node for a particular guid",
      params = {@IPSJexlParam(name = "guid", description = "the GUID for the item")})
  /**
   * Returns the node by guid.
   *
   * @param guid the guid
   * @return the result
   * @throws RepositoryException if an error occurs
   */
  public IPSNode getNodeByGuid(IPSGuid guid) throws RepositoryException {
    initServices();
    List<Node> nodes = cmgr.findItemsByGUID(Collections.<IPSGuid>singletonList(guid), null);
    if (nodes.size() > 0) {
      return (IPSNode) nodes.get(0);
    }
    return null;
  }

  /**
   * See referenced member.
   * @see com.percussion.pso.jexl.IPSOObjectFinder#getSiteGuid(int)
   */
  @IPSJexlMethod(
      description = "get the site guid for a given id",
      params = {@IPSJexlParam(name = "siteid", description = "the id for the site")})
  /**
   * Returns the site guid.
   *
   * @param siteid the siteid
   * @return the result
   */
  public IPSGuid getSiteGuid(int siteid) {
    initServices();
    IPSGuid guid = gmgr.makeGuid(siteid, PSTypeEnum.SITE);
    log.debug("Site guid is {}", guid);
    return guid;
  }

  /**
   * See referenced member.
   * @see com.percussion.pso.jexl.IPSOObjectFinder#getTemplateGuid(int)
   */
  @IPSJexlMethod(
      description = "get the template guid for a given id",
      params = {@IPSJexlParam(name = "template", description = "the id for the template")})
  /**
   * Returns the template guid.
   *
   * @param templateid the templateid
   * @return the result
   */
  public IPSGuid getTemplateGuid(int templateid) {
    initServices();
    IPSGuid guid = gmgr.makeGuid(templateid, PSTypeEnum.TEMPLATE);
    log.debug("Template guid is {}", guid);
    return guid;
  }

  @IPSJexlMethod(
      description = "get the community name for a  given community id",
      params = {@IPSJexlParam(name = "communityId", description = "the id for the community")})
  /**
   * Returns the community name.
   *
   * @param communityId the community id
   * @return the result
   */
  public String getCommunityName(int communityId) {
    initServices();
    List<PSCommunity> communities = sws.loadCommunities(null);
    String communityName = null;
    for (PSCommunity comm : communities) {
      if (communityId == comm.getGUID().getUUID()) {
        communityName = comm.getName();
        break;
      }
    }
    return communityName;
  }

  /***
   * Returns a PSState object for the given state and workflow.  This can be used to get
   * the publishable flag on a given workflow state.
   *
   * For Example:
   *
   * $summary = $user.psoObjectFinder.getComponentSummary($sys.assemblyItem.getId())
   * $state = $user.psoObjectFinder.getWorkflowState($summary.getContentStateId(),$summary.getWorkflowAppId())
   *
   * &lt;h1&gt;STATE CURRENT VALUE=${state.getContentValidValue()}&lt;/h1&gt;
   *
   * @param stateId the state id
   * @param workflowAppId the workflow app id
   *
   */
  @IPSJexlMethod(
      description = "Get the workflow info for a given item",
      params = {
        @IPSJexlParam(name = "stateId", description = "the stateId"),
        @IPSJexlParam(
            name = "workflowAppId",
            description = "Returns the State definition for the specified workflow state.")
      })
  /**
   * Returns the workflow state.
   *
   * @param stateId the state id
   * @param workflowAppId the workflow app id
   * @return the result
   */
  public PSState getWorkflowState(int stateId, int workflowAppId) {
    initServices();
    PSState state =
        wf.loadWorkflowState(
            new PSGuid(PSTypeEnum.WORKFLOW_STATE, stateId),
            new PSGuid(PSTypeEnum.WORKFLOW, workflowAppId));
    return state;
  }

  /**
   * Gets the user session.
   *
   * @return the user session
   */
  private PSUserSession getSession() {
    PSRequest req = (PSRequest) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_PSREQUEST);
    PSUserSession session = req.getUserSession();
    return session;
  }

  /**
   * Sets the cws.
   * @param cws The cws to set. This routine should only be used for unit testing.
   */
  public static void setCws(IPSContentWs cws) {
    PSOObjectFinder.cws = cws;
  }

  /**
   * Sets the gmgr.
   * @param gmgr the gmgr to set
   */
  public static void setGmgr(IPSGuidManager gmgr) {
    PSOObjectFinder.gmgr = gmgr;
  }

  /**
   * Sets the cmgr.
   * @param cmgr the cmgr to set
   */
  public static void setCmgr(IPSContentMgr cmgr) {
    PSOObjectFinder.cmgr = cmgr;
  }
}
