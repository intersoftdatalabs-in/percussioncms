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
 * com.percussion.pso.workflow PublishEditionService.java
 *
 * @author DavidBenua
 *
 */
package com.percussion.pso.workflow;

// REFACTORED: CP-JAVA11
import com.percussion.rx.publisher.IPSRxPublisherService;
import com.percussion.rx.publisher.PSRxPublisherServiceLocator;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.utils.guid.IPSGuid;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;

/**
 * The PublishEdition service starts an edition. This class used to start the remote publisher via
 * HTTP, but in 6.6 we use the IPSRxPublisherService. The <code>
 * baseUrl, cmsUser, cmsPassword, listenerPort</code> and <code>retryCount</code> parameters are no
 * longer used. The getters and setters remain in place for backwards compatibility.
 *
 * @author DavidBenua
 */
public class PublishEditionService implements InitializingBean {
  /** Logger for this class */
  private static final Logger log = LogManager.getLogger(PublishEditionService.class);

  private String baseUrl = "http://127.0.0.1";
  private String listenerPort = null;
  // if local is true we expect to run as the caller, otherwise need CMS user and password
  private boolean local = true;
  private String cmsUser = null;
  private String cmsPassword = null;
  private int retryCount = 10;

  private IPSRxPublisherService rps = null;
  private IPSGuidManager gmgr = null;

  /*
   * Map of workflows
   *    Map of transitions
   *       Map of communities
   *          Value is edition
   */
  private Map<String, Map<String, Map<String, String>>> workflows =
      new HashMap<String, Map<String, Map<String, String>>>();

  /**
   * Default constructor.
   * Creates a new PublishEditionService.
   *
   */
  public PublishEditionService() {}

  private void initServices() {
    if (gmgr == null) {
      gmgr = PSGuidManagerLocator.getGuidMgr();
    }
    if (rps == null) {
      rps = PSRxPublisherServiceLocator.getRxPublisherService();
    }
  }

  /**
   * See referenced member.
   * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
   * @throws Exception if an error occurs
   */
  public void afterPropertiesSet() throws Exception {
    initServices();
  }

  /**
   * runQueuedEdition operation.
   *
   * @param ed the ed
   */
  @SuppressWarnings("deprecation")
  public void runQueuedEdition(QueuedEdition ed) {
    runEdition(ed.getEditionId());
  }

  /**
   * Runs an edition. This launches a new job asynchronously via the RxPublisherService.
   *
   * @param editionId the edition id as a simple number.
   * @since 6.6
   */
  public void runEdition(String editionId) {
    IPSGuid guid = gmgr.makeGuid(editionId, PSTypeEnum.EDITION);
    rps.startPublishingJob(guid, null);
  }

  /**
   * retryQueuedEdition operation.
   *
   * @param ed the ed
   */
  @SuppressWarnings("deprecation")
  public void retryQueuedEdition(QueuedEdition ed) {
    log.info("retryQueuedEdition is no longer used");
  }

  /**
   * findEdition operation.
   *
   * @param workflow the workflow
   * @param transition the transition
   * @param community the community
   * @return the result
   */
  public int findEdition(int workflow, int transition, int community) {
    String workKey = String.valueOf(workflow);
    Map<String, Map<String, String>> workMap = workflows.get(workKey);
    if (workMap == null) {
      String emsg = "Workflow not in configuration file " + workflow;
      log.error(emsg);
      throw new IllegalArgumentException(emsg);
    }
    String transKey = String.valueOf(transition);
    Map<String, String> transMap = workMap.get(transKey);
    if (transMap == null) {
      String emsg =
          "Transition " + transition + " not in configuration file for workflow " + workflow;
      log.error(emsg);
      throw new IllegalArgumentException(emsg);
    }
    String commKey = String.valueOf(community);
    String edition = transMap.get(commKey);
    if (edition == null) {
      String emsg =
          "Community "
              + community
              + " not in configuration file for workflow "
              + workflow
              + " and transition "
              + transition;
      log.error(emsg);
      throw new IllegalArgumentException(emsg);
    }
    return Integer.parseInt(edition);
  }

  /**
   * Makes a Queued Edition. This is no longer necessary, but supported for backwards compatibility
   *
   * @param editionId the edition id
   * @param sessionId the session id
   * @deprecated
   * @return the result
   */
  @Deprecated
  @SuppressWarnings("deprecation")
  protected QueuedEdition makeQueuedEdition(String editionId, String sessionId) {

    QueuedEdition result =
        new QueuedEdition(baseUrl, listenerPort, editionId, this.isLocal(), retryCount);

    return result;
  }

  /**
   * Gets the baseUrl. No longer used in 6.6.
   *
   * @return Returns the baseUrl.
   * @deprecated
   */
  @Deprecated
  public String getBaseUrl() {
    return baseUrl;
  }

  /**
   * No longer used in 6.6.
   *
   * @param baseUrl The baseUrl to set.
   * @deprecated
   */
  @Deprecated
  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  /**
   * No longer used in 6.6.
   *
   * @return Returns the listenerPort.
   * @deprecated
   */
  @Deprecated
  public String getListenerPort() {
    return listenerPort;
  }

  /**
   * No longer used in 6.6.
   *
   * @param listenerPort The listenerPort to set.
   * @deprecated
   */
  @Deprecated
  public void setListenerPort(String listenerPort) {
    this.listenerPort = listenerPort;
  }

  /**
   * Returns Returns the workflows..
   * @return Returns the workflows.
   */
  public Map<String, Map<String, Map<String, String>>> getWorkflows() {
    return workflows;
  }

  /**
   * Sets the workflows.
   * @param workflows The workflows to set.
   */
  public void setWorkflows(Map<String, Map<String, Map<String, String>>> workflows) {
    this.workflows = workflows;
  }

  /**
   * No longer used in 6.6.
   *
   * @return Returns the cmsPassword.
   */
  public String getCmsPassword() {
    return cmsPassword;
  }

  /**
   * No longer used in 6.6.
   *
   * @param cmsPassword The cmsPassword to set.
   */
  public void setCmsPassword(String cmsPassword) {
    this.cmsPassword = cmsPassword;
    log.debug("Setting CMS Password");
    this.local = false;
  }

  /**
   * No longer used in 6.6.
   *
   * @return Returns the cmsUser.
   */
  public String getCmsUser() {
    return cmsUser;
  }

  /**
   * No longer used in 6.6.
   *
   * @param cmsUser The cmsUser to set.
   */
  public void setCmsUser(String cmsUser) {
    this.cmsUser = cmsUser;
    log.debug("Setting CMS User " + cmsUser);
    this.local = false;
  }

  /**
   * No longer used in 6.6.
   *
   * @return Returns the local.
   */
  public boolean isLocal() {
    return local;
  }

  /**
   * No longer used in 6.6.
   *
   * @param local The local to set.
   */
  public void setLocal(boolean local) {
    this.local = local;
  }

  /**
   * Returns Returns the retryCount..
   * @return Returns the retryCount.
   * @deprecated
   */
  @Deprecated
  public int getRetryCount() {
    return retryCount;
  }

  /**
   * Sets the retryCount.
   * @param retryCount The retryCount to set.
   * @deprecated
   */
  @Deprecated
  public void setRetryCount(int retryCount) {
    this.retryCount = retryCount;
  }

  /**
   * Sets the RxPublisherService in unit test.
   *
   * @param rps the RxPublisherService to set.
   */
  protected void setRps(IPSRxPublisherService rps) {
    this.rps = rps;
  }

  /**
   * Sets the Guid Manager in unit test.
   *
   * @param gmgr the guid manager to set.
   */
  protected void setGmgr(IPSGuidManager gmgr) {
    this.gmgr = gmgr;
  }
}
