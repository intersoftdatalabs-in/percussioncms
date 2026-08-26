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

package com.percussion.workflow;

import com.intsof.percussioncms.auditlog.codes.ExtensionErrorCodes;
import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.IPSResultDocumentProcessor;
import com.percussion.extension.PSExtensionException;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.extension.PSParameterMismatchException;
import com.percussion.i18n.PSI18nUtils;
import com.percussion.server.IPSRequestContext;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.services.legacy.PSCmsObjectMgrLocator;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * Result-document processor exit that augments the supplied result document with the workflow
 * transitions that are permitted for the current item. See the {@link IPSResultDocumentProcessor}
 * contract for the parameters and return value of {@link #processResultDocument(Object[],
 * IPSRequestContext, Document)}.
 */
@SuppressWarnings({"rawtypes", "unchecked", "this-escape"})
public class PSExitAddPossibleTransitions implements IPSResultDocumentProcessor {

  /** Default constructor for the extension framework. */
  public PSExitAddPossibleTransitions() {}

  /**
   * This is an inner class to encapsulate the parameters. We cannot keep these as class variables
   * due to threading issues. We instantiate this object in the main processrequest method (called
   * by server) and pass around the methods. This is meant for convenience only.
   */
  private class Params {
    /** Always {@code null} after Phase 4d-1a — retained for legacy {@code Params} API parity. */
    public Connection m_connection = null;

    public String m_userName = null;
    public String m_checkoutUserName = null;
    public String m_statusDocElementName = null;
    public String m_contentIDNodeName = null;
    public String m_contentIDName = null;
    public String m_roleNameList = "";
  }

  /** The fully qualified name of this extension. */
  private static String m_fullExtensionName = "";

  private static String ms_actionElementName = null;
  private static String ms_actionListElementName = null;
  private static String ms_actionTriggerName = "";

  public Document processResultDocument(Object[] params, IPSRequestContext request, Document resDoc)
      throws PSParameterMismatchException, PSExtensionProcessingException {
    try {
      if (null == request) {
        throw new PSExtensionProcessingException(
            m_fullExtensionName, new IllegalArgumentException("The request must not be null"));
      }
      String lang =
          (String) request.getSessionPrivateObject(PSI18nUtils.USER_SESSION_OBJECT_SYS_LANG);
      if (lang == null) lang = PSI18nUtils.DEFAULT_LANG;

      if (null == params) return resDoc; // no parameters exit with peace!

      int nParamCount = params.length;

      Params localParams = new Params();

      try {
        if (ms_correctParamCount != nParamCount) {
          String[] exParams = {Integer.toString(ms_correctParamCount), String.valueOf(nParamCount)};
          throw new PSInvalidNumberOfParametersException(
              lang, ExtensionErrorCodes.INVALID_PARAM_NUM, exParams);
        }

        if (null == params[0] || 0 == params[0].toString().trim().length()) {
          return resDoc;
          /*
           * Request by James Shultz. There may be situations with user
           * name empty and still user gets the result document, of
           * course without any workflow info.
           */
          //               throw new PSInvalidParameterTypeException(new String(
          //                  "The user name must not be empty"));
        }

        localParams.m_userName = params[0].toString();
        localParams.m_userName = PSWorkFlowUtils.filterUserName(localParams.m_userName);

        if (null == params[1] || 0 == params[1].toString().trim().length()) {
          throw new PSInvalidParameterTypeException(lang, ExtensionErrorCodes.STATUS_DOC_EMPTY);
        }
        localParams.m_statusDocElementName = params[1].toString();

        if (null == params[2] || 0 == params[2].toString().trim().length()) {
          throw new PSInvalidParameterTypeException(
              lang, ExtensionErrorCodes.CONTENTID_NODENAME_EMPTY);
        }
        localParams.m_contentIDNodeName = params[2].toString();
      } catch (PSInvalidNumberOfParametersException | PSInvalidParameterTypeException ne) {
        String language = ne.getLanguageString();
        if (language == null) language = PSI18nUtils.DEFAULT_LANG;
        throw new PSExtensionProcessingException(language, m_fullExtensionName, ne);
      }

      try {
        localParams.m_roleNameList =
            request.getUserContextInformation("Roles/RoleName", "").toString();
      } catch (Exception e) {
        localParams.m_roleNameList = "";
      }

      // Phase 4d-1a: no longer opens a second pool connection — all reads below
      // (CONTENTSTATUS, STATEROLES, CONTENTTYPES, TRANSITIONS) route through Hibernate
      // factories on the shared session.

      Element element = null;
      NodeList nodes = resDoc.getElementsByTagName(localParams.m_statusDocElementName);

      for (int i = 0; i < nodes.getLength(); i++) {
        element = (Element) nodes.item(i);
        try {
          addWorkflowInfo(resDoc, element, localParams, lang, request);
        } catch (PSXMLNodeMissingException xe) {
          request.printTraceMessage(xe.getMessage());
        } catch (SQLException se) {
          request.printTraceMessage(se.getMessage());
        } catch (PSRoleException e) {
          request.printTraceMessage(e.getMessage());
        }
      }
    } finally {
      // Phase 4d-1a: no connection to release.
    }
    return resDoc;
  }

  private void addWorkflowInfo(
      Document doc, Element elemParent, Params localParams, String lang, IPSRequestContext req)
      throws SQLException, PSXMLNodeMissingException, PSRoleException {
    int contentID = 0;
    localParams.m_contentIDName = null;
    String sContentID = null;

    if (localParams.m_contentIDNodeName.startsWith("@")) {
      localParams.m_contentIDName = localParams.m_contentIDNodeName.substring(1);

      sContentID = elemParent.getAttribute(localParams.m_contentIDName);
      if (null != sContentID) sContentID = sContentID.trim();
      if (null == sContentID || sContentID.length() < 1)
        throw new PSXMLNodeMissingException(lang, ExtensionErrorCodes.CONTENTID_NODE_MISSING_EMPTY);
      contentID = Integer.parseInt(sContentID);
    } else {
      NodeList nodes = elemParent.getElementsByTagName(localParams.m_contentIDNodeName);

      if (null == nodes || nodes.getLength() < 1)
        throw new PSXMLNodeMissingException(lang, ExtensionErrorCodes.CONTENTID_NODE_MISSING_EMPTY);
      Element elem = (Element) nodes.item(0);
      localParams.m_contentIDName = elem.getNodeName();
      sContentID = ((Text) (elem.getFirstChild())).getData();
      if (null != sContentID) sContentID = sContentID.trim();
      if (null == sContentID || sContentID.length() < 1)
        throw new PSXMLNodeMissingException(lang, ExtensionErrorCodes.CONTENTID_NODE_MISSING_EMPTY);
      contentID = Integer.parseInt(sContentID);
    }

    if (contentID < 1)
      throw new PSXMLNodeMissingException(lang, ExtensionErrorCodes.CONTENTID_NODE_MISSING);

    sContentID = Integer.toString(contentID);

    Optional<IPSWorkflowAppsContext> wacOpt;
    PSComponentSummary summary;
    int nWorkFlowAppID = -1;
    int nContentStateID;
    String sContentCheckedOutUserName;
    IPSCmsObjectMgr cms = PSCmsObjectMgrLocator.getObjectManager();
    // Phase 4d-1a: CONTENTSTATUS read via Hibernate (Phase 4b loadComponentSummary).
    summary = cms.loadComponentSummary(contentID);
    if (summary == null) {
      return;
    }
    nWorkFlowAppID = summary.getWorkflowAppId();
    nContentStateID = summary.getContentStateId();
    sContentCheckedOutUserName = summary.getCheckoutUserName();

    wacOpt = cms.loadWorkflowAppContext(nWorkFlowAppID);
    if (wacOpt.isEmpty()) {
      return;
    }

    if (wacOpt.isEmpty()) {
      return; // no workflow, nothing to add
    }
    IPSWorkflowAppsContext wac = wacOpt.get();

    // Add workflow info element and set required attributes
    Element elemWorkflowInfo = doc.createElement(ELEMENT_WORKFLOWINFO);
    elemWorkflowInfo = (Element) elemParent.appendChild(elemWorkflowInfo);
    elemWorkflowInfo.setAttribute(ATTRIB_CONTENTID, sContentID);
    elemWorkflowInfo.setAttribute(ATTRIB_WORKFLOWID, Integer.toString(nWorkFlowAppID));

    elemWorkflowInfo.setAttribute(ATTRIB_WORKFLOWNAME, wac.getWorkFlowAppName());
    //
    // Add state information element and attributes
    Element elemState = doc.createElement(ELEMENT_CURRENTSTATE);
    elemState = (Element) elemWorkflowInfo.appendChild(elemState);
    elemState.setAttribute(ATTRIB_STATEID, Integer.toString(nContentStateID));

    Optional<? extends IPSStatesContext> scOpt =
        cms.loadWorkflowState(nWorkFlowAppID, nContentStateID);
    if (scOpt.isEmpty()) {
      return; // can't determine state
    }
    IPSStatesContext sc = scOpt.get();

    String sPublishable = (sc.getIsValid()) ? "Y" : "N";
    elemState.setAttribute(ATTRIB_PUBLISHABLE, sPublishable);

    Text text = doc.createTextNode(sc.getStateName());
    elemState.appendChild(text);
    //

    // Add checkout status information element and attributes
    localParams.m_checkoutUserName = sContentCheckedOutUserName;
    if (null == localParams.m_checkoutUserName) localParams.m_checkoutUserName = "";
    else localParams.m_checkoutUserName = localParams.m_checkoutUserName.trim();

    Element elemCheckoutStatus = doc.createElement(ELEMENT_CHECKOUTSTATUS);

    elemCheckoutStatus = (Element) elemWorkflowInfo.appendChild(elemCheckoutStatus);

    elemCheckoutStatus.setAttribute(ATTRIB_CHECKOUTUSERNAME, localParams.m_checkoutUserName);

    int nCheckoutStatus = PSWorkFlowUtils.CHECKOUT_STATUS_NONE;
    if (null == localParams.m_checkoutUserName || localParams.m_checkoutUserName.length() < 1) {
      // Default above
    } else if (localParams.m_checkoutUserName.trim().equalsIgnoreCase(localParams.m_userName)) {
      nCheckoutStatus = PSWorkFlowUtils.CHECKOUT_STATUS_CURRENT_USER;
    } else {
      nCheckoutStatus = PSWorkFlowUtils.CHECKOUT_STATUS_OTHER;
    }
    text = doc.createTextNode(Integer.toString(nCheckoutStatus));
    elemCheckoutStatus.appendChild(text);

    //

    // Add user info element and attributes
    Element elemUserName = doc.createElement(ELEMENT_USERNAME);
    elemUserName = (Element) elemWorkflowInfo.appendChild(elemUserName);

    int nAssignmentType =
        PSExitAddPossibleTransitionsEx.getAssignmentType(
            nWorkFlowAppID,
            contentID,
            nContentStateID,
            localParams.m_userName,
            localParams.m_roleNameList,
            req);

    elemUserName.setAttribute(ATTRIB_ASSIGNMENTTYPE, Integer.toString(nAssignmentType));

    text = doc.createTextNode(localParams.m_userName);
    elemUserName.appendChild(text);

    //

    // Add assigned roles information
    addAssignedRolesInfo(doc, elemWorkflowInfo, nWorkFlowAppID, nContentStateID);
    //

    // Add action list
    addActions(doc, elemWorkflowInfo, contentID, nContentStateID, nWorkFlowAppID, localParams);
    //
  }

  private void addAssignedRolesInfo(
      Document doc, Element elemParent, int nWorkflowAppID, int stateid)
      throws SQLException, PSRoleException {
    Element elemAssignedRoles = doc.createElement(ELEMENT_ASSIGNEDROLES);
    elemAssignedRoles = (Element) elemParent.appendChild(elemAssignedRoles);
    Element elemAssignedRole = null;

    PSStateRolesContext src = null;

    try {
      // Phase 4d-1a: STATEROLES read via Hibernate factory (Phase 4b).
      src =
          PSStateRolesContext.loadFromHibernate(
              nWorkflowAppID, stateid, PSWorkFlowUtils.ASSIGNMENT_TYPE_NONE);
    } catch (PSEntryNotFoundException enfe) {
      // No info is added if the context does not exist
      return;
    }

    if (null == src) {
      return;
    }

    if (!src.isEmpty()) {
      Text text = null;
      Map stateRoleNameMap = src.getStateRoleNameMap();
      Map assignmentTypeMap = src.getStateRoleAssignmentTypeMap();
      List stateRoleIDs = src.getStateRoleIDs();
      Iterator iter = stateRoleIDs.iterator();
      Integer roleID = null;
      Integer assignmentType = null;
      String roleName = null;

      while (iter.hasNext()) {
        roleID = (Integer) iter.next();
        roleName = (String) stateRoleNameMap.get(roleID);
        assignmentType = (Integer) assignmentTypeMap.get(roleID);
        elemAssignedRole = doc.createElement(ELEMENT_ASSIGNEDROLE);

        elemAssignedRole.setAttribute(ATTRIB_ASSIGNMENTTYPE, assignmentType.toString());

        elemAssignedRole.setAttribute(ATTRIB_ROLEID, roleID.toString());

        text = doc.createTextNode(roleName);
        elemAssignedRole.appendChild(text);

        elemAssignedRoles.appendChild(elemAssignedRole);
      }
    }
  }

  private void addActions(
      Document doc,
      Element elemParent,
      int contentID,
      int contentStateID,
      int workflowAppID,
      Params localParams)
      throws SQLException {
    String sContentID = Integer.toString(contentID);

    PSContentTypesContext ctc = null;
    String sUpdateRequest = null;
    String sQueryRequest = null;
    try {
      // Phase 4d-1a: CONTENTTYPES read via Hibernate factory.
      ctc = PSContentTypesContext.loadFromHibernate(contentTypeIDFromSummary(contentID));
      sUpdateRequest = ctc.getContentTypeUpdateRequest();
      sQueryRequest = ctc.getContentTypeQueryRequest();
      ctc.close();
    } catch (SQLException e) {
      ctc = null;
    }

    Element elemActionList = doc.createElement(ms_actionListElementName);
    elemActionList = (Element) elemParent.appendChild(elemActionList);

    Element elemAction = null;
    Text text = null;
    String sRequestName =
        PSWorkFlowUtils.properties.getProperty(
            PSWorkFlowUtils.REQUEST_NAME, PSWorkFlowUtils.DEFAULT_REQUEST_NAME);

    String sParamSeparator = "?";

    if (null != sQueryRequest && sQueryRequest.length() > 0) {
      elemAction = doc.createElement(PSWorkFlowUtils.VIEW_ACTION_ELEMENT_NAME);
      elemAction.setAttribute(ms_actionTriggerName, "view");
      sParamSeparator = (-1 == sQueryRequest.indexOf("?")) ? "?" : "&";

      elemAction.setAttribute(
          sRequestName,
          sQueryRequest + sParamSeparator + localParams.m_contentIDName + "=" + sContentID);

      elemAction.setAttribute(localParams.m_contentIDName, sContentID);
      text = doc.createTextNode("View");
      elemAction.appendChild(text);
      elemActionList.appendChild(elemAction);
    }
    if (null != sUpdateRequest && sUpdateRequest.length() > 0) {
      elemAction = doc.createElement(PSWorkFlowUtils.EDIT_ACTION_ELEMENT_NAME);
      elemAction.setAttribute(ms_actionTriggerName, "edit");
      sParamSeparator = (-1 == sUpdateRequest.indexOf("?")) ? "?" : "&";

      elemAction.setAttribute(
          sRequestName,
          sUpdateRequest + sParamSeparator + localParams.m_contentIDName + "=" + sContentID);

      elemAction.setAttribute(localParams.m_contentIDName, sContentID);
      text = doc.createTextNode("Edit");
      elemAction.appendChild(text);
      elemActionList.appendChild(elemAction);
    }

    boolean bCheckedOut =
        ((null != localParams.m_checkoutUserName) && (localParams.m_checkoutUserName.length() > 0));

    if (bCheckedOut) {
      elemAction = doc.createElement(PSWorkFlowUtils.CHECKINOUT_ACTION_ELEMENT_NAME);

      elemAction.setAttribute(
          ms_actionTriggerName,
          PSWorkFlowUtils.properties.getProperty(PSWorkFlowUtils.TRIGGER_CHECK_IN));

      elemAction.setAttribute(localParams.m_contentIDName, sContentID);
      text = doc.createTextNode("Check-In");
      elemAction.appendChild(text);
      elemActionList.appendChild(elemAction);
    } else {
      elemAction = doc.createElement(PSWorkFlowUtils.CHECKINOUT_ACTION_ELEMENT_NAME);

      elemAction.setAttribute(
          ms_actionTriggerName,
          PSWorkFlowUtils.properties.getProperty(PSWorkFlowUtils.TRIGGER_CHECK_OUT));

      elemAction.setAttribute(localParams.m_contentIDName, sContentID);
      text = doc.createTextNode("Check-Out");
      elemAction.appendChild(text);
      elemActionList.appendChild(elemAction);
    }

    PSTransitionsContext tc = null;

    // Phase 4d-1a: TRANSITIONS read via Hibernate factory (no Connection).
    tc = PSTransitionsContext.loadAllFromHibernate(workflowAppID, contentStateID);
    if (tc.isEmpty()) {
      return;
    }

    while (true) {
      // Don't show buttons for aging transitions
      if (!tc.isAgingTransition()) {
        elemAction = doc.createElement(ms_actionElementName);
        elemAction.setAttribute(ms_actionTriggerName, tc.getTransitionActionTrigger());

        elemAction.setAttribute(localParams.m_contentIDName, sContentID);
        elemAction.setAttribute(ATTRIB_TRANSITIONID, Integer.toString(tc.getTransitionID()));

        text = doc.createTextNode(tc.getTransitionLabel());
        elemAction.appendChild(text);
        elemActionList.appendChild(elemAction);
      }

      try {
        if (false == tc.moveNext()) break;
      } catch (SQLException e) {
        break;
      }
    }
    tc.close();

    return;
  }

  /**
   * Looks up the content-type id for the supplied content id via the Spring-managed {@link
   * com.percussion.services.legacy.IPSCmsObjectMgr} (no second pool connection). Added for #1561
   * Phase 4d-1a so {@code PSContentTypesContext.loadFromHibernate} can be invoked after {@code
   * PSContentStatusContext} has been removed from the call chain.
   *
   * @param contentID the content id, must be {@code > 0}.
   * @return the content-type id; {@code 0} when no row matches.
   */
  private int contentTypeIDFromSummary(int contentID) {
    IPSCmsObjectMgr cms = PSCmsObjectMgrLocator.getObjectManager();
    PSComponentSummary summary = cms.loadComponentSummary(contentID);
    return summary == null ? 0 : (int) summary.getContentTypeId();
  }

  public boolean canModifyStyleSheet() {
    return true;
  }

  /**
   * This is used as a flag to indicate that the class hasn't been init'd yet. There are certain
   * cases where init can be called more than once on the same loaded instance of a class.
   */
  private static final int UNINITIALIZED = -1;

  private static int ms_correctParamCount = UNINITIALIZED;

  public void init(IPSExtensionDef extensionDef, @SuppressWarnings("unused") File file)
      throws PSExtensionException {
    if (UNINITIALIZED != ms_correctParamCount) return;

    ms_correctParamCount = 0;
    ms_actionTriggerName =
        PSWorkFlowUtils.properties.getProperty(
            PSWorkFlowUtils.ACTION_TRIGGER_NAME, PSWorkFlowUtils.DEFAULT_ACTION_TRIGGER_NAME);

    ms_actionListElementName =
        PSWorkFlowUtils.properties.getProperty(
            PSWorkFlowUtils.ACTION_LIST_ELEMENT_NAME,
            PSWorkFlowUtils.DEFAULT_ACTION_LIST_ELEMENT_NAME);

    ms_actionElementName =
        PSWorkFlowUtils.properties.getProperty(
            PSWorkFlowUtils.ACTION_ELEMENT_NAME, PSWorkFlowUtils.DEFAULT_ACTION_ELEMENT_NAME);

    Iterator iter = extensionDef.getRuntimeParameterNames();
    while (iter.hasNext()) {
      iter.next();
      ms_correctParamCount++;
    }
    m_fullExtensionName = extensionDef.getRef().toString();
  }

  /** Element and attribute names for the workflow information node. */
  public static final String ELEMENT_WORKFLOWINFO = "workflowinfo";

  /** Content ID attribute name. */
  public static final String ATTRIB_CONTENTID = "contentid";

  /** Workflow ID attribute name. */
  public static final String ATTRIB_WORKFLOWID = "workflowid";

  /** Workflow name attribute name. */
  public static final String ATTRIB_WORKFLOWNAME = "workflowname";

  /** Transition ID attribute name. */
  public static final String ATTRIB_TRANSITIONID = "transitionid";

  /** User name element name. */
  public static final String ELEMENT_USERNAME = "username";

  /** Assignment type attribute name. */
  public static final String ATTRIB_ASSIGNMENTTYPE = "assignmenttype";

  /** Current state element name. */
  public static final String ELEMENT_CURRENTSTATE = "currentstate";

  /** State ID attribute name. */
  public static final String ATTRIB_STATEID = "stateid";

  /** Publishable attribute name. */
  public static final String ATTRIB_PUBLISHABLE = "publishable";

  /** Check-out status element name. */
  public static final String ELEMENT_CHECKOUTSTATUS = "checkoutstatus";

  /** Check-out user name attribute name. */
  public static final String ATTRIB_CHECKOUTUSERNAME = "checkoutusername";

  /** Assigned roles container element name. */
  public static final String ELEMENT_ASSIGNEDROLES = "assignedroles";

  /** Single assigned role element name. */
  public static final String ELEMENT_ASSIGNEDROLE = "assignedrole";

  /** Role ID attribute name. */
  public static final String ATTRIB_ROLEID = "roleid";
}
