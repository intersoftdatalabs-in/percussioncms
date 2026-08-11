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
package com.percussion.cms.objectstore.client;

import com.percussion.cms.IPSCmsErrors;
import com.percussion.cms.PSCmsException;
import com.percussion.cms.objectstore.PSProcessingStatistics;
import com.percussion.cms.objectstore.PSProcessorCommon;
import com.intsof.percussioncms.auditlog.codes.ObjectStoreErrorCodes;
import com.percussion.design.objectstore.PSUnknownNodeTypeException;
import com.percussion.system.utils.PSUrlUtils;
import com.percussion.util.IPSRemoteRequester;
import com.percussion.util.PSXMLDomUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * This class contains the know how to implement the necessary processing when operating in a
 * different JVM than the server. It uses http requests to accomplish its work. All requests are
 * performed on behalf of the user specified in the config params.
 *
 * <p>Generally, this is designed for use by remote clients such as the workbench.
 *
 * @author Paul Howard
 * @version 1.0
 */
public class PSRemoteProcessor extends PSProcessorCommon {
  /**
   * Creates a processor that can fulfill database operation requests Remotely from the Rhythmyx
   * server. Implementors should not instantiate this class directly but should use the {@link
   * com.percussion.cms.objectstore.PSProcessorProxy PSProcessorProxy} class.
   *
   * <p>See base class for further details. (Note, parameters (except ssl) are the same as those of
   * the {@link com.percussion.conn.PSDesignerConnection PSDesignerConnection} class.
   *
   * <table border="1">
   * <CAPTION>Connection Parameters</CAPTION>
   *    <tr>
   *       <th>Key</th>
   *       <th>Value</th>
   *    </tr>
   *    <tr>
   *       <td>hostName</td>
   *       <td>The name of the Rx server machine, required.</td>
   *    </tr>
   *    <tr>
   *       <td>port</td>
   *       <td>The port the Rx server is listening on. If not provided,
   *          9992 is used for non-ssl and 9443 for ssl.</td>
   *    </tr>
   *    <tr>
   *       <td>loginId</td>
   *       <td>The user name to use when connecting. If empty all connections
   *          will be made anonymously.</td>
   *    </tr>
   *    <tr>
   *       <td>loginPw</td>
   *       <td>The password to use when connecting. If not provided, "" is
   *          used. Must be unencrypted.</td>
   *    </tr>
   *    <tr>
   *       <td>useSSL</td>
   *       <td>A flag to indicate whether the connection should be encrypted.
   *          If 'true', then uses an SSL socket for communication. Any other
   *          value, or absence of the property and the connection will be
   *          made without SSL. If <code>true</code>, the supplied port
   *          must accept ssl connection requests.</td>
   *    </tr>
   *    <tr>
   *       <td>serverRoot</td>
   *       <td>The server's request root. If not supplied, Rhythmyx is used.
   *          </td>
   *    </tr>
   * </table>
   *
   * @param conn Never <code>null</code>. All work is performed as the user identified with these
   *     connection parameters.
   */
  public PSRemoteProcessor(IPSRemoteRequester conn, Map<String, Map<String, Object>> procConfig) {
    super(procConfig);
    if (null == conn) {
      throw new IllegalArgumentException("Connection information must be supplied.");
    }

    m_conn = conn;
  }

  /**
   * See base class for details.
   *
   * <ul>
   *   <li>For each entry in ids, create N html parameters whose name is the name of the entry key.
   *       The value of each instance should be the value of one of the entries in the associated
   *       collection.
   *   <li>Generate an http/s request to the resource specified in loadResource.
   * </ul>
   */
  @Override
  protected Document doLoad(String resourceName, Map<String, String[]> ids) throws PSCmsException {
    String path = "";
    try {
      Map<String, Object> params = toRequestParams(ids);
      return m_conn.getDocument(resourceName, params);
    } catch (IOException ioe) {
      String[] args = {"request url: " + path, ioe.getLocalizedMessage()};
      throw new PSCmsException(IPSCmsErrors.COMM_ERROR_WITH_SERVER, args);
    } catch (SAXException se) {
      String[] args = {"request url: " + path, se.getLocalizedMessage()};
      throw new PSCmsException(IPSCmsErrors.SAX_PROCESSING_EXCEPTION, args);
    }
  }

  // see interface for description
  @Override
  protected int doDelete(String resourceName, Map<String, String[]> ids) throws PSCmsException {
    Element root = null;
    try {
      Map<String, Object> params = toRequestParams(ids);
      Document doc = m_conn.getDocument(resourceName, params);
      if (null == doc || null == doc.getDocumentElement()) {
        throw new PSCmsException(ObjectStoreErrorCodes.XML_ELEMENT_NULL, "PSXExecStatistics");
      }

      PSProcessingStatistics stats = new PSProcessingStatistics(root);
      return stats.getDeletedCount();
    } catch (IOException ioe) {
      String[] args = {"request partial url: " + resourceName, ioe.getLocalizedMessage()};
      throw new PSCmsException(IPSCmsErrors.COMM_ERROR_WITH_SERVER, args);
    } catch (SAXException se) {
      String[] args = {"request partial url: " + resourceName, se.getLocalizedMessage()};
      throw new PSCmsException(IPSCmsErrors.SAX_PROCESSING_EXCEPTION, args);
    } catch (PSUnknownNodeTypeException unte) {
      throw new PSCmsException(unte.getErrorCode(), unte.getErrorArguments());
    }
  }

  // see base class
  @Override
  protected int[] doAllocateIds(String lookup, int count) throws PSCmsException {
    String number = "";
    // used if exception occurs
    String errPath = "";
    try {
      Map<String, Object> params = new HashMap<>();
      params.put("sys_lookupkey", lookup);
      params.put("sys_idcount", "" + count);
      /*resource returns doc of form
      <PSXIdGenerator key="lookup" firstId="100", count="count or less">*/
      // String path = "/" + m_serverRoot + "/sys_psxCms/idgen.xml";
      errPath = PSUrlUtils.createUrl("/sys_psxCms/idgen.xml", params.entrySet().iterator(), null);
      Document doc = m_conn.getDocument("sys_psxCms/idgen.xml", params);
      String nodeName = "PSXIdGenerator";
      if (null == doc || null == doc.getDocumentElement()) {
        throw new PSCmsException(ObjectStoreErrorCodes.XML_ELEMENT_NULL, nodeName);
      }

      Element root = doc.getDocumentElement();
      PSXMLDomUtil.checkNode(root, nodeName);

      number = root.getAttribute("firstId");
      int firstId = Integer.parseInt(number);
      number = root.getAttribute("count");
      int returnedIds = Integer.parseInt(number);
      int[] result = new int[count];
      for (int i = 0; i < returnedIds; i++) {
        result[i] = firstId + i;
      }
      return result;
    } catch (NumberFormatException nfe) {
      throw new PSCmsException(1000, "Bad number returned by id generator app: '" + number + "'.");
    } catch (IOException ioe) {
      String[] args = {"request url: " + errPath, ioe.getLocalizedMessage()};
      throw new PSCmsException(IPSCmsErrors.COMM_ERROR_WITH_SERVER, args);
    } catch (SAXException se) {
      String[] args = {"request url: " + errPath, se.getLocalizedMessage()};
      throw new PSCmsException(IPSCmsErrors.SAX_PROCESSING_EXCEPTION, args);
    } catch (PSUnknownNodeTypeException unte) {
      throw new PSCmsException(unte.getErrorCode(), unte.getErrorArguments());
    }
  }

  // see base class for description
  @Override
  protected PSProcessingStatistics doSave(String resourceName, Document input)
      throws PSCmsException {
    Element root = null;
    // String path = "/" + m_serverRoot + "/" + resourceName;
    try {
      Document doc = m_conn.sendUpdate(resourceName, input);

      if (null == doc)
        ; // todo: throw or return all 0's?

      root = doc.getDocumentElement();
      if (null == root)
        ; // todo: throw or return all 0's?

      return new PSProcessingStatistics(doc);
    } catch (IOException ioe) {
      String[] args = {"request url (posting xml): " + resourceName, ioe.getLocalizedMessage()};
      throw new PSCmsException(IPSCmsErrors.COMM_ERROR_WITH_SERVER, args);
    } catch (SAXException se) {
      String[] args = {"request url (posting xml): " + resourceName, se.getLocalizedMessage()};
      throw new PSCmsException(IPSCmsErrors.SAX_PROCESSING_EXCEPTION, args);
    } catch (PSUnknownNodeTypeException unte) {
      throw new PSCmsException(unte.getErrorCode(), unte.getErrorArguments());
    }
  }

  /**
   * Converts key-part id arrays into request parameters: multi-valued parts become a {@link List}
   * of strings, single-valued parts become a plain string.
   */
  private static Map<String, Object> toRequestParams(Map<String, String[]> ids) {
    Map<String, Object> params = new HashMap<>();
    for (Map.Entry<String, String[]> entry : ids.entrySet()) {
      String keyPartName = entry.getKey();
      String[] idSet = entry.getValue();
      if (idSet.length > 1) {
        List<String> l = new ArrayList<>(idSet.length);
        for (int i = 0; i < idSet.length; i++) l.add(idSet[i]);
        params.put(keyPartName, l);
      } else {
        params.put(keyPartName, idSet[0]);
      }
    }
    return params;
  }

  /** Object used to make the requests to the server. Never <code>null</code> after construction. */
  private IPSRemoteRequester m_conn = null;
}
