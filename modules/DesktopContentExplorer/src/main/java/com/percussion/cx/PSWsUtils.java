/*
 * Copyright (c) 2023 Intersoft Data Labs, Inc.
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
package com.percussion.cx;

import com.percussion.webservices.content.AddFolderChildrenRequest;
import com.percussion.webservices.content.AddFolderTreeRequest;
import com.percussion.webservices.content.AddFolderTreeResponse;
import com.percussion.webservices.content.CheckinItemsRequest;
import com.percussion.webservices.content.Content;
import com.percussion.webservices.content.CreateItemsRequest;
import com.percussion.webservices.content.CreateItemsResponse;
import com.percussion.webservices.content.FindFolderChildrenRequest;
import com.percussion.webservices.content.FindFolderChildrenResponse;
import com.percussion.webservices.content.FolderRef;
import com.percussion.webservices.content.LoadItemsRequest;
import com.percussion.webservices.content.LoadItemsResponse;
import com.percussion.webservices.content.PSFolder;
import com.percussion.webservices.content.PSItem;
import com.percussion.webservices.content.PSItemStatus;
import com.percussion.webservices.content.PSItemSummary;
import com.percussion.webservices.content.PrepareForEditRequest;
import com.percussion.webservices.content.PrepareForEditResponse;
import com.percussion.webservices.content.ReleaseFromEditRequest;
import com.percussion.webservices.content.SaveItemsRequest;
import com.percussion.webservices.content.SaveItemsResponse;
import com.percussion.webservices.security.data.PSLogin;
import com.percussion.webservices.securityservices.LoginRequest;
import com.percussion.webservices.securityservices.LoginResponse;
import com.percussion.webservices.securityservices.LogoutRequest;
import com.percussion.webservices.securityservices.NotAuthenticatedFaultMessage;
import com.percussion.webservices.securityservices.Security;
import com.percussion.webservices.system.TransitionItemsRequest;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.handler.MessageContext;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class demonstrating how to use the Rhythmyx webservice API via JAX-WS (CXF). This class
 * shows how to maintain sessions for both Rhythmyx and the container across all service instances.
 * The Rhythmyx session is communicated through a SOAP header, and the container session (JSESSION)
 * is communicated through the HTTP Cookie header.
 */
public class PSWsUtils {

  /** Default constructor. */
  public PSWsUtils() {
    // no-op
  }

  /**
   * Creates a Content Item of the specified content type.
   *
   * @param binding the proxy of the content service; assumed not to be <code>null</code>.
   * @param contentType the Content Type for the created item, assumed not to be <code>null</code>
   *     or empty.
   * @return the created Content Item. The Content Item is not yet persisted to the Repository.
   *     Never <code>null</code>.
   * @throws Exception if an error occurs.
   */
  public static PSItem createItem(Content binding, String contentType) throws Exception {
    CreateItemsRequest request = new CreateItemsRequest();
    request.setContentType(contentType);
    request.setCount(1);
    CreateItemsResponse response = binding.createItems(request);
    return response.getPSItem().get(0);
  }

  /**
   * Saves the specified Content Item to the repository.
   *
   * @param binding the proxy of the content service; assumed not to be <code>null</code>.
   * @param item the Content Item to be saved; assumed not to be <code>null</code>.
   * @return the ID of the saved Content Item; never <code>null</code>.
   * @throws Exception if an error occurs.
   */
  public static long saveItem(Content binding, PSItem item) throws Exception {
    SaveItemsRequest req = new SaveItemsRequest();
    req.getPSItem().add(item);
    SaveItemsResponse response = binding.saveItems(req);
    return response.getIds().getId().get(0);
  }

  /**
   * Loads the specified Content Item.
   *
   * @param binding the proxy of the content service; assumed not to be <code>null</code>.
   * @param id the ID of the Content Item to be loaded.
   * @return the specified Content Item, never <code>null</code>.
   * @throws Exception if an error occurs.
   */
  public static PSItem loadItem(Content binding, long id) throws Exception {
    LoadItemsRequest req = new LoadItemsRequest();
    req.getId().add(id);
    req.setIncludeBinary(true);
    req.setAttachBinaries(true);
    LoadItemsResponse response = binding.loadItems(req);
    return response.getPSItem().get(0);
  }

  /**
   * Checks in the specified Content Item.
   *
   * @param binding the proxy of the content service, assumed not to be <code>null</code>.
   * @param id the ID of the Content Item to be checked in.
   * @throws Exception if an error occurs.
   */
  public static void checkinItem(Content binding, long id) throws Exception {
    CheckinItemsRequest req = new CheckinItemsRequest();
    req.getId().add(id);
    binding.checkinItems(req);
  }

  /**
   * Prepares the specified Content Item for Edit.
   *
   * @param binding the proxy of the content service; assumed not to be <code>null</code>.
   * @param id the ID of the Content Item to be prepared for editing.
   * @return The status of the specified Content Item, which can be used to reverse the
   *     prepareForEdit action by calling {@link #releaseFromEdit(Content, PSItemStatus)}
   * @throws Exception if an error occurs.
   */
  public static PSItemStatus prepareForEdit(Content binding, long id) throws Exception {
    PrepareForEditRequest req = new PrepareForEditRequest();
    req.getId().add(id);
    PrepareForEditResponse response = binding.prepareForEdit(req);
    return response.getPSItemStatus().get(0);
  }

  /**
   * Releases the specified Content Item from Edit; reverse action of {@link
   * #prepareForEdit(Content, long)}.
   *
   * @param binding the proxy of the content service; assumed not to be <code>null</code>.
   * @param status the status of the Content Item to be released for edit; assumed not to be <code>
   *     null</code>.
   * @throws Exception if an error occurs.
   */
  public static void releaseFromEdit(Content binding, PSItemStatus status) throws Exception {
    ReleaseFromEditRequest req = new ReleaseFromEditRequest();
    req.getPSItemStatus().add(status);
    binding.releaseFromEdit(req);
  }

  /**
   * Performs the Workflow Transition with the specified Trigger name for the specified Content
   * Item.
   *
   * @param binding the proxy of the system service; assumed not to be <code>null</code>.
   * @param id the ID of the Content Item to Transition.
   * @param trigger the Trigger name of the Workflow Transition; assumed not to be <code>null</code>
   *     or empty.
   * @throws Exception if an error occurs.
   */
  public static void transitionItem(
      com.percussion.webservices.system.System binding, long id, String trigger) throws Exception {
    TransitionItemsRequest req = new TransitionItemsRequest();
    req.getId().add(id);
    req.setTransition(trigger);
    binding.transitionItems(req);
  }

  /**
   * Finds all immediate child Content Items and child Folders of the specified Folder.
   *
   * @param binding the proxy of the content service; assumed not to be <code>null</code>.
   * @param folderPath the path of the Folder whose children you want to find; assumed not to be
   *     <code>null</code> or empty. Provide '/' to get all root folders such as <code>Folders
   *     </code> and <code>Sites</code>.
   * @return the result of the search for child objects; never <code>null</code>, but may be empty.
   * @throws Exception if an error occurs.
   */
  public static List<PSItemSummary> findFolderChildren(Content binding, String folderPath)
      throws Exception {
    FindFolderChildrenRequest req = new FindFolderChildrenRequest();
    var folderRef = new FolderRef();
    folderRef.setPath(folderPath);
    req.setFolder(folderRef);
    FindFolderChildrenResponse response = binding.findFolderChildren(req);
    return response.getPSItemSummary();
  }

  /**
   * Associates the specified Content Items with the specified Folder.
   *
   * @param binding the proxy of the content service; assumed not to be <code>null</code>.
   * @param folderPath the path of the Folder to which you want to add the child objects; assumed
   *     not to be <code>null</code> or empty.
   * @param childIds the IDs of the objects to be associated with the Folder specified in the
   *     folderPath parameter; assumed not <code>null</code> or empty.
   * @throws Exception if an error occurs.
   */
  public static void addFolderChildren(Content binding, String folderPath, long[] childIds)
      throws Exception {
    AddFolderChildrenRequest req = new AddFolderChildrenRequest();
    var ids = new AddFolderChildrenRequest.ChildIds();
    for (long childId : childIds) {
      ids.getId().add(childId);
    }
    req.setChildIds(ids);
    var parent = new FolderRef();
    parent.setPath(folderPath);
    req.setParent(parent);
    binding.addFolderChildren(req);
  }

  /**
   * Creates Folders for the specified Folder path. Any Folders specified in the path that do not
   * exist will be created; No action is taken on any existing Folders.
   *
   * @param binding the proxy of the content service; assumed not to be <code>null</code>.
   * @param folderPath the Folder path to be updated; assumed not to be <code>null</code> or empty.
   * @return the created folder objects, never <code>null</code>, may be empty.
   * @throws Exception if an error occurs.
   */
  public static List<PSFolder> addFolderTree(Content binding, String folderPath) throws Exception {
    AddFolderTreeRequest req = new AddFolderTreeRequest();
    req.setPath(folderPath);
    AddFolderTreeResponse response = binding.addFolderTree(req);
    return response.getPSFolder();
  }

  /**
   * Sets a new server connection with the supplied parameters. The new connection information will
   * be used for subsequent calls to get the service proxies, {@link #getContentService(String)},
   * {@link #getSecurityService()} and {@link #getSystemService(String)}.
   *
   * @param protocol the protocol of the server connection; assumed not to be <code>null</code> or
   *     empty. Defaults to <code>http</code>.
   * @param host the host name of the server connection; assumed not to be <code>null</code> or
   *     empty. Defaults to <code>localhost</code>.
   * @param port the port of the server connection; Defaults to 9992
   */
  public static void setConnectionInfo(String protocol, String host, int port) {
    ms_protocol = protocol;
    ms_host = host;
    ms_port = port;
  }

  /**
   * Logs in with the specified credentials and associated parameters.
   *
   * @param binding the proxy of the security service; assumed not to be <code>null</code>. If the
   *     login attempt is successful, the session header will be set for subsequent requests.
   * @param user the login user name; assumed not to be <code>null</code> or empty.
   * @param password the password of the login user; assumed not to be <code>null</code> or empty.
   * @param community the name of the Community into which to login the user; may be <code>null
   *     </code> or empty, in which case the user is logged in to the last Community they logged in
   *     to, or, if the user has never logged in before, into the first Community in alphabetical
   *     order.
   * @param locale the name of the Locale into which to log the user; may be <code>null</code> or
   *     empty, in which case the user is logged in to the last Locale they logged in to, or, if the
   *     user has never logged in before, into the first Locale in alphabetical order.
   * @return the Rhythmyx session, never <code>null</code> or empty.
   * @throws NotAuthenticatedFaultMessage if authentication of the passed credentials fails.
   */
  public static String login(
      Security binding, String user, String password, String community, String locale)
      throws NotAuthenticatedFaultMessage {
    if (user == null || user.isEmpty()) {
      throw new IllegalArgumentException("user may not be null or empty.");
    }
    if (password == null || password.isEmpty()) {
      throw new IllegalArgumentException("password may not be null or empty.");
    }

    LoginRequest loginReq = new LoginRequest();
    loginReq.setUsername(user);
    loginReq.setPassword(password);
    loginReq.setCommunity(community);
    loginReq.setLocaleCode(locale);

    try {
      LoginResponse loginResp = binding.login(loginReq);
      PSLogin loginObj = loginResp.getPSLogin();

      String rxSession = loginObj.getSessionId();

      // Set the Rhythmyx session header for all subsequent requests from this service
      setRxSessionHeader(binding, rxSession);

      return rxSession;
    } catch (NotAuthenticatedFaultMessage e) {
      throw e;
    } catch (Exception e) {
      // not possible
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  /**
   * Logs out the specified Rhythmyx session.
   *
   * @param binding the security proxy, assumed not to be <code>null</code>.
   * @param rxSession the Rhythmyx session for which to log out
   * @throws Exception if any error occurs.
   */
  public static void logout(Security binding, String rxSession) throws Exception {
    LogoutRequest logoutReq = new LogoutRequest();
    logoutReq.setSessionId(rxSession);
    binding.logout(logoutReq);
  }

  /**
   * Creates a proxy of the content service. Must provide a valid Rhythmyx session from {@link
   * #login(Security, String, String, String, String)}.
   *
   * @param rxSession the Rhythmyx session; assumed to be a valid Rhythmyx session from {@link
   *     #login(Security, String, String, String, String)}, not <code>null</code> or empty.
   * @return the proxy of the content service; never <code>null</code>. This method uses the server
   *     connection information that is saved with this class. The connection information can be
   *     overridden by {@link #setConnectionInfo(String, String, int)}.
   * @see #setConnectionInfo(String, String, int)
   * @throws WebServiceException if failed to create the content service instance.
   */
  public static Content getContentService(String rxSession) throws WebServiceException {
    var locator = new com.percussion.webservices.rhythmyx.Content();
    Content binding = locator.getContentSOAP();

    // Set endpoint address
    setEndpointAddress(binding, getBaseUrl() + "/contentSOAP");

    // Setting to maintain one container session (JSESSION) for all requests
    ((BindingProvider) binding)
        .getRequestContext()
        .put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);

    // Set the Rhythmyx session for all requests
    setRxSessionHeader(binding, rxSession);

    return binding;
  }

  /**
   * Creates a system service instance; must provide a valid Rhythmyx session from {@link
   * #login(Security, String, String, String, String)}.
   *
   * @param rxSession the Rhythmyx session; assumed to be a valid Rhythmyx session from {@link
   *     #login(Security, String, String, String, String)}, not <code>null</code> or empty.
   * @return the proxy of the system service; never <code>null</code>. This method uses the server
   *     connection information that is saved with this class. The connection information can be
   *     overridden by {@link #setConnectionInfo(String, String, int)}.
   * @see #setConnectionInfo(String, String, int)
   * @throws WebServiceException if the method fails to create the system service instance.
   */
  public static com.percussion.webservices.system.System getSystemService(String rxSession)
      throws WebServiceException {
    var locator = new com.percussion.webservices.rhythmyx.System();
    com.percussion.webservices.system.System binding = locator.getSystemSOAP();

    // Set endpoint address
    setEndpointAddress(binding, getBaseUrl() + "/systemSOAP");

    // Setting to maintain one container session (JSESSION) for all requests
    ((BindingProvider) binding)
        .getRequestContext()
        .put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);

    // Set the Rhythmyx session for all requests
    setRxSessionHeader(binding, rxSession);

    return binding;
  }

  /**
   * Creates a proxy of the security service. It is the caller's responsibility to call {@link
   * #login(Security, String, String, String, String)} with the returned object.
   *
   * @return the created proxy of the security service, never <code>null</code>. This method uses
   *     the server connection information that is saved with this class. However, the connection
   *     information can be overridden by {@link #setConnectionInfo(String, String, int)}.
   * @see #setConnectionInfo(String, String, int)
   * @throws WebServiceException if the method fails to create the new service proxy.
   */
  public static Security getSecurityService() throws WebServiceException {
    var locator = new com.percussion.webservices.rhythmyx.Security();
    Security binding = locator.getSecuritySOAP();

    // Set endpoint address
    setEndpointAddress(binding, getBaseUrl() + "/securitySOAP");

    // Setting for maintaining container session (JSESSION)
    ((BindingProvider) binding)
        .getRequestContext()
        .put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);

    return binding;
  }

  /**
   * Sets the endpoint address for a JAX-WS port proxy.
   *
   * @param port the JAX-WS port proxy; assumed not to be <code>null</code>.
   * @param address the endpoint URL; assumed not to be <code>null</code> or empty.
   */
  private static void setEndpointAddress(Object port, String address) {
    ((BindingProvider) port)
        .getRequestContext()
        .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, address);
  }

  /**
   * Returns the base URL from the current connection info.
   *
   * @return the base URL string, never <code>null</code>.
   */
  private static String getBaseUrl() {
    return ms_protocol + "://" + ms_host + ":" + ms_port;
  }

  /**
   * Sets the Rhythmyx session as header on the specified proxy. In JAX-WS, this is done via the
   * request context HTTP headers map.
   *
   * @param binding the proxy to which to add the Rhythmyx session as header; assumed not to be
   *     <code>null</code>.
   * @param rxSession The Rhythmyx session; assumed to be a valid Rhythmyx session from {@link
   *     #login(Security, String, String, String, String)}, not <code>null</code> or empty.
   */
  public static void setRxSessionHeader(Object binding, String rxSession) {
    Map<String, List<String>> headers = new HashMap<>();
    headers.put("RxSession", Collections.singletonList(rxSession));
    ((BindingProvider) binding)
        .getRequestContext()
        .put(MessageContext.HTTP_REQUEST_HEADERS, headers);
  }

  /** The protocol of the server connection. Defaults to 'http'. */
  private static String ms_protocol = "http";

  /** The host name of the server connection. Defaults to 'localhost'. */
  private static String ms_host = "localhost";

  /** The port of the server connection. Defaults to 9992. */
  private static int ms_port = 9992;
}
