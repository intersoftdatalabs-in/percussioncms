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

package com.percussion.cms;

import com.percussion.design.objectstore.PSAttribute;
import com.percussion.design.objectstore.PSSubject;
import com.percussion.server.IPSInternalRequest;
import com.percussion.server.IPSRequestContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class PSAuthenticateUserUtils {
  /** Name of user default community properties. */
  public static final String SYS_DEFAULTCOMMUNITY = "sys_defaultCommunity";

  /**
   * Name of the internal request to get the community id with a c community name. Requires
   * parameter communityname=value, where value is a valid community name.
   */
  public static final String IREQ_COMMUNITYLOOKUP = "sys_commSupport/communityidlookup";

  /**
   * Name of the parameter requires for community id lookup. This paremeter is added when we lookup
   * the community id.
   */
  public static final String COMMUNITYNAME = "communityname";

  /**
   * Name of the element "Community" in the result document of the internal request for user
   * communities.
   */
  public static final String ELEM_COMMUNITY = "Community";

  /**
   * Name of the attribute of the communityid of the element "Community" in the result document of
   * the internal request for user communities.
   */
  public static final String ATTR_COMMID = "commid";

  /**
   * Name of the internal request to get the user communities. This is a standard Rhythmyx resource
   * meant for internal request.
   */
  public static final String IREQ_USERCOMMUNITIES = "sys_commSupport/usercommunities";

  /**
   * This method retrieves the list user's role-communities, viz. list of all communities via his
   * role membership.
   *
   * @param request <code>IPSRequestContext</code> object that is available in the extension's
   *     process request method, assumed never <code>null</code>.
   * @return list of user communities (community ids) as Java List object never <code>null</code>
   *     may be empty.
   */
  private List getUserCommunities(IPSRequestContext request) throws Exception {
    ArrayList list = new ArrayList();
    // Make an internal request to get the user roles.
    IPSInternalRequest iReq = request.getInternalRequest(IREQ_USERCOMMUNITIES);
    Document doc = null;
    try {
      iReq.makeRequest();
      doc = iReq.getResultDoc();
    } finally {
      if (iReq != null) iReq.cleanUp();
    }
    NodeList nl = doc.getElementsByTagName(ELEM_COMMUNITY);
    if (nl == null || nl.getLength() < 1) return list;

    Element elem = null;
    for (int i = 0; i < nl.getLength(); i++) {
      elem = (Element) nl.item(i);
      list.add(elem.getAttribute(ATTR_COMMID));
    }
    return list;
  }

  /**
   * This mehod retrieves the community id from "sys_commSupport/communityidlookup" by their
   * community name.
   *
   * @param request <code>IPSRequestContext</code> object that is available in the extension's
   *     process request method, assumed never <code>null</code>.
   * @param name Community name, can not be <code>null</code>
   * @return Community id.
   * @throws Exception
   */
  public static String getCommunityId(IPSRequestContext request, String name) throws Exception {
    // Backup parameters
    Map<String, Object> paramsBackup = request.getParameters();
    Document doc;
    try {
      request.setParameter(COMMUNITYNAME, name);
      IPSInternalRequest iReq = request.getInternalRequest(IREQ_COMMUNITYLOOKUP);
      try {
        iReq.makeRequest();
        doc = iReq.getResultDoc();
      } finally {
        if (iReq != null) iReq.cleanUp();
      }
    } finally {
      // restore parameters
      request.setParameters(paramsBackup);
    }
    NodeList nl = doc.getElementsByTagName(ELEM_COMMUNITY);
    Element elem = null;
    if (null != nl) elem = (Element) nl.item(0);
    return elem.getAttribute(ATTR_COMMID);
  }

  /**
   * This method retrieves the value of the given attribute for the user role. If user happens to be
   * in multiple roles the first non empty value is considered
   *
   * @param request <code>IPSRequestContext</code> object that is available in the extension's
   *     process request method, assumed never <code>null</code>.
   * @param srcAttrName Name of the role attribute to retrieve, cannot be <code>null</code>, if
   *     <code>null</code> the result will be <code>null</code>.
   * @return value of the given attribute, may be <code>null</code>
   * @throws Exception if it cannot retrieve the role attribute for any reason.
   */
  public static String getUserRoleAttribute(IPSRequestContext request, String srcAttrName)
      throws Exception {
    if (srcAttrName == null) return null;
    String attrValue = null;
    List roles = request.getSubjectRoles();
    Object role = null;
    List roleAttribs = null;
    PSAttribute attr = null;
    List attrList = null;
    String attrName = null;
    for (int i = 0; roles != null && i < roles.size(); i++) {
      role = roles.get(i);
      if (role == null) continue;
      roleAttribs = request.getRoleAttributes(role.toString().trim());
      for (int j = 0; roleAttribs != null && j < roleAttribs.size(); j++) {
        attr = (PSAttribute) roleAttribs.get(j);
        if (attr == null) continue;
        attrName = attr.getName();
        if (attrName.equals(srcAttrName)) {
          attrList = attr.getValues();
          if (attrList != null && attrList.size() > 0) {
            // we take only the first attribute
            attrValue = attrList.get(0).toString();
          }
        }
        if (attrValue != null && attrValue.length() > 0) return attrValue;
      }
    }
    return attrValue;
  }

  /**
   * First non-empty global subject attribute for the current user (issue #3508).
   *
   * <p>Used for {@link #SYS_DEFAULTCOMMUNITY} stored on the user subject rather
   * than a role. Failures return {@code null} so callers can fall back to the
   * role attribute.
   */
  public static String getUserSubjectAttribute(IPSRequestContext request, String srcAttrName) {
    if (request == null || srcAttrName == null || srcAttrName.isBlank()) {
      return null;
    }
    try {
      String userName = request.getUserName();
      if (userName == null || userName.isBlank()) {
        return null;
      }
      List<PSSubject> subjects =
          request.getSubjectGlobalAttributes(
              userName, PSSubject.SUBJECT_TYPE_USER, null, srcAttrName, false);
      return firstSubjectAttributeValue(subjects, srcAttrName);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * First non-empty value of {@code attrName} on any supplied subject.
   *
   * @param subjects may be {@code null}
   * @param attrName never {@code null} for a useful result
   * @return trimmed value, or {@code null}
   */
  public static String firstSubjectAttributeValue(List<PSSubject> subjects, String attrName) {
    if (subjects == null || attrName == null || attrName.isBlank()) {
      return null;
    }
    for (PSSubject subject : subjects) {
      if (subject == null || subject.getAttributes() == null) {
        continue;
      }
      PSAttribute attr = subject.getAttributes().getAttribute(attrName);
      if (attr == null) {
        continue;
      }
      List<?> values = attr.getValues();
      if (values == null || values.isEmpty() || values.get(0) == null) {
        continue;
      }
      String value = values.get(0).toString().trim();
      if (!value.isEmpty()) {
        return value;
      }
    }
    return null;
  }

  /**
   * Default community name for login: user-subject {@link #SYS_DEFAULTCOMMUNITY}
   * first, then the first non-empty role attribute (legacy).
   */
  public static String resolveDefaultCommunityName(IPSRequestContext request) throws Exception {
    String subjectValue = getUserSubjectAttribute(request, SYS_DEFAULTCOMMUNITY);
    if (subjectValue != null && !subjectValue.isBlank()) {
      return subjectValue.trim();
    }
    String roleValue = getUserRoleAttribute(request, SYS_DEFAULTCOMMUNITY);
    return roleValue == null ? null : roleValue.trim();
  }

  /**
   * Pure resolver for tests and callers that already loaded both stores.
   *
   * @param subjectValue user-subject attribute, may be blank
   * @param roleValue first role attribute, may be blank
   * @return trimmed subject value when present, else trimmed role value, else {@code null}
   */
  public static String resolveDefaultCommunityName(String subjectValue, String roleValue) {
    if (subjectValue != null && !subjectValue.isBlank()) {
      return subjectValue.trim();
    }
    if (roleValue != null && !roleValue.isBlank()) {
      return roleValue.trim();
    }
    return null;
  }

  /**
   * True when {@code communityName} matches an allowed membership name
   * (case-insensitive).
   */
  public static boolean isCommunityAllowed(String communityName, List<String> allowed) {
    if (communityName == null || communityName.isBlank() || allowed == null) {
      return false;
    }
    String wanted = communityName.trim();
    for (String name : allowed) {
      if (name != null && wanted.equalsIgnoreCase(name.trim())) {
        return true;
      }
    }
    return false;
  }
}
