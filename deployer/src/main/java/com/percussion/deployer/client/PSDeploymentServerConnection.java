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

package com.percussion.deployer.client;

import com.percussion.conn.PSServerException;
import com.percussion.deployer.objectstore.PSDbmsInfo;
import com.percussion.deployer.objectstore.PSDeploymentServerConnectionInfo;
import com.percussion.design.objectstore.PSUnknownNodeTypeException;
import com.intsof.percussioncms.auditlog.codes.DeploymentErrorCodes;
import com.percussion.error.PSDeployException;
import com.percussion.error.PSDeployNonUniqueException;
import com.percussion.error.PSLockedException;
import com.percussion.security.PSAuthenticationFailedException;
import com.percussion.security.PSAuthenticationRequiredException;
import com.percussion.security.PSAuthorizationException;
import com.percussion.security.PSEncryptionException;
import com.percussion.security.PSEncryptor;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.server.PSServerLockException;
import com.percussion.server.job.PSJobException;
import com.percussion.system.utils.PSFormatVersion;
import com.percussion.util.PSCharSetsConstants;
import com.percussion.util.PSPurgableTempFile;
import com.percussion.utils.io.PathUtils;
import com.percussion.utils.server.IPSCgiVariables;
import com.percussion.xml.PSXmlDocumentBuilder;
import com.percussion.xml.PSXmlTreeWalker;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Represents a connection to the Rx server and a session theirin. Is used to execute requests to
 * the deployment and job handlers on the server.
 */
public final class PSDeploymentServerConnection {
  /**
   * Constructs a connection using the http protocol. Calls
   *
   * @param info contains the connection details, assumed not <code>null</code>
   * @param overrideLock If <code>true</code>, then the lock is acquired by the user no matter what,
   *     if <code>false</code> then sessionid has to match for lock acquisition.
   * @param mode The operating mode.
   * @throws IllegalArgumentException if any param is invalid.
   * @throws PSAuthenticationFailedException if authentication fails.
   * @throws PSAuthorizationException If the user is not authorized to access the server for
   *     deployment operations.
   * @throws PSServerException if there are any errors communicating with the server.
   * @throws PSDeployException For any other errors.
   */
  public PSDeploymentServerConnection(
      PSDeploymentServerConnectionInfo info,
      boolean overrideLock,
      IPSDeployConstants.OperatingMode mode)
      throws PSAuthenticationFailedException,
          PSAuthorizationException,
          PSServerException,
          PSDeployException {
    this(
        "http",
        info.getServer(),
        info.getPort(),
        info.getUserid(),
        info.getPassword(),
        info.isPwdEncrypted(),
        overrideLock,
        mode);
  }

  /**
   * Construct a connection using the http protocol. Calls
   *
   * @param server The name of the server to connect to.
   * @param port The port on the server.
   * @param userid The user id to connect using.
   * @param password The password.
   * @param isPwdEncrypted Whether the password is encrypted.
   * @param overrideLock Whether to override an existing lock.
   * @param mode The operating mode.
   * @throws PSAuthenticationFailedException if authentication fails.
   * @throws PSAuthorizationException if not authorized.
   * @throws PSServerException if the server cannot be reached.
   * @throws PSDeployException for any other errors.
   */
  public PSDeploymentServerConnection(
      String server,
      int port,
      String userid,
      String password,
      boolean isPwdEncrypted,
      boolean overrideLock,
      IPSDeployConstants.OperatingMode mode)
      throws PSAuthenticationFailedException,
          PSAuthorizationException,
          PSServerException,
          PSDeployException {
    this("http", server, port, userid, password, isPwdEncrypted, overrideLock, mode);
  }

  /**
   * Creates a connection to the server. Use {@link #disconnect()} to disconnect from the server.
   * This connection may not be used again after it has been disconnected. A daemon locker thread is
   * started which renews the lock 2 minutes before the lock expiration {@link
   * IPSDeployConstants#LOCK_EXPIRATION_DURATION}. If the lock cannot be extended locker thread
   * dies.
   *
   * @param protocol The protocol to use, may not be <code>null</code> or empty. Currently only
   *     'http' and 'https' are supported.
   * @param server The name of the server to connect to, may not be <code>null</code> or empty.
   * @param port The port on the server. Must be greater than 0.
   * @param userid The user id to connect using, may not be <code>null</code> or empty.
   * @param password The password, may be <code>null</code> or empty. If <code>null</code>, and
   *     empty <code>String</code> is stored.
   * @param isPwdEncrypted If <code>true</code>, the password will be treated as encrypted.
   *     Otherwise, it is assumed to be clear text and will be encryted for storage or serialization
   *     to the server.
   * @param overrideLock If <code>true</code>, then the lock is acquired by the user no matter what,
   *     if <code>false</code> then sessionid has to match for lock acquisition.
   * @param mode The operating mode.
   * @throws IllegalArgumentException if any param is invalid.
   * @throws PSAuthenticationFailedException If the user cannot be authenticated by the server.
   * @throws PSAuthorizationException If the user is not authorized to access the server for
   *     deployment operations.
   * @throws PSServerException if there are any errors communicating with the server.
   * @throws PSDeployException For any other errors.
   */
  public PSDeploymentServerConnection(
      String protocol,
      String server,
      int port,
      String userid,
      String password,
      boolean isPwdEncrypted,
      boolean overrideLock,
      IPSDeployConstants.OperatingMode mode)
      throws PSAuthenticationFailedException,
          PSAuthorizationException,
          PSServerException,
          PSDeployException {
    if (protocol == null || protocol.trim().length() == 0)
      throw new IllegalArgumentException("protocol may not be null or empty");

    if (server == null || server.trim().length() == 0)
      throw new IllegalArgumentException("server may not be null or empty");

    if (port <= 0) throw new IllegalArgumentException("port must be greater than zero");

    if (userid == null || userid.trim().length() == 0)
      throw new IllegalArgumentException("userid may not be null or empty");

    m_server = server;
    m_port = port;
    m_uid = userid;
    operatingMode = mode;
    m_password = password == null ? "" : password;

    // create the connection object. Only 'http' and 'https' are supported; anything
    // else used to surface as a vendored ProtocolNotSuppException and is mapped to the
    // same UNEXPECTED_ERROR contract here.
    if (!protocol.equalsIgnoreCase("http") && !protocol.equalsIgnoreCase("https")) {
      throw new PSDeployException(
          DeploymentErrorCodes.UNEXPECTED_ERROR, "Unsupported protocol: " + protocol);
    }
    m_baseUri =
        URI.create(protocol.toLowerCase(Locale.ROOT) + "://" + m_server + ":" + m_port);
    m_basicAuthHeader =
        "Basic "
            + Base64.getEncoder()
                .encodeToString(
                    (m_uid + ":" + m_password).getBytes(StandardCharsets.ISO_8859_1));
    // Per-instance cookie jar, replacing the vendored client's setContext(this) association.
    // ACCEPT_ALL mirrors the vendored default of storing whatever the server sets.
    m_httpClient =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .build();

    m_isConnected = true;
    Document respDoc = connect(overrideLock);

    PSXmlTreeWalker tree = new PSXmlTreeWalker(respDoc);
    Element root = respDoc.getDocumentElement();
    tree.setCurrent(root);

    String deployVersion = root.getAttribute("deployVersion");
    int deployInterface = -1;
    try {
      deployInterface = Integer.parseInt(deployVersion);
    } catch (NumberFormatException e) {
      m_isConnected = false;
      Object[] args = {"connect", "deployVersion", deployVersion};
      throw new PSDeployException(DeploymentErrorCodes.SERVER_RESPONSE_ELEMENT_INVALID, args);
    }

    // The client's deployment version must be greater than or equal to the
    // server's.  This allows forward compatiblity to be handled by the
    // server.
    if (deployInterface > DEPLOYMENT_INTERFACE_VERSION) {
      m_isConnected = false;
      throw new PSDeployException(
          DeploymentErrorCodes.SERVER_VERSION_INVALID, m_version.getVersionString());
    }

    String licensed = root.getAttribute("licensed");
    if ((licensed != null) && (licensed.trim().length() > 0))
      m_bLicensed = "yes".equalsIgnoreCase(licensed.trim());

    Element versionEl =
        tree.getNextElement(PSFormatVersion.NODE_TYPE, PSXmlTreeWalker.GET_NEXT_ALLOW_CHILDREN);
    if (versionEl == null) {
      m_isConnected = false;
      Object[] args = {"connect", PSFormatVersion.NODE_TYPE};
      throw new PSDeployException(DeploymentErrorCodes.SERVER_RESPONSE_ELEMENT_MISSING, args);
    }
    m_version = PSFormatVersion.createFromXml(versionEl);
    if (m_version == null) {
      m_isConnected = false;
      Object[] args = {"connect", PSFormatVersion.NODE_TYPE, "unknown"};
      throw new PSDeployException(DeploymentErrorCodes.SERVER_RESPONSE_ELEMENT_INVALID, args);
    }
    // This will have to be reworked once we allow remote install of the package manager for cougar.
    // It will need to know the difference of cougar and rhythmyx
    // currently versions prior to 6.0 are not supported
    // TODO: Fix me so the version checking is real
    if (m_version.getMajorVersion() < 6) {
      m_isConnected = false;
      throw new PSDeployException(
          DeploymentErrorCodes.SERVER_VERSION_INVALID, m_version.getVersionString());
    }

    Element repositoryEl =
        tree.getNextElement(PSDbmsInfo.XML_NODE_NAME, PSXmlTreeWalker.GET_NEXT_ALLOW_SIBLINGS);
    if (repositoryEl == null) {
      m_isConnected = false;
      Object[] args = {"connect", PSDbmsInfo.XML_NODE_NAME};
      throw new PSDeployException(DeploymentErrorCodes.SERVER_RESPONSE_ELEMENT_MISSING, args);
    }
    try {
      m_serverRepositoryInfo = new PSDbmsInfo(repositoryEl);
    } catch (PSUnknownNodeTypeException e) {
      m_isConnected = false;
      Object[] args = {"connect", PSDbmsInfo.XML_NODE_NAME, e.getLocalizedMessage()};
      throw new PSDeployException(DeploymentErrorCodes.SERVER_RESPONSE_ELEMENT_INVALID, args);
    }

    // locker thread tries to extend the lock 2 minutes before expiration.
    final int lockTimeoutBuffer = 2;
    Runnable runnable =
        new Runnable() {
          public void run() {
            try {
              while (m_isConnected) {
                // sleep for lock expiration time less 2 minutes
                Thread.sleep(
                    ((IPSDeployConstants.LOCK_EXPIRATION_DURATION - lockTimeoutBuffer)
                        * 60
                        * 1000));
                extendLock();
              }
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            } catch (PSDeployException e) {
              /*do nothing*/
            }
          }
        };
    // Don't even bother to start if locking time is less than 2 minutes.
    if (IPSDeployConstants.LOCK_EXPIRATION_DURATION > lockTimeoutBuffer) {
      m_lockerThread = new Thread(runnable);
      m_lockerThread.setDaemon(true);
      m_lockerThread.start();
    }
  }

  /**
   * Flags this connection as disconnected and it is no longer usable. Any session information is
   * discarded. Kills the locker thread started in the ctor.
   *
   * @throws PSDeployException if errors occur executing the request or if disconnection is being
   *     made when the connection has already been lost.
   */
  public void disconnect() throws PSDeployException {
    try {
      doDisconnect();
    } catch (PSDeployException e) {
      Object[] args = {m_server + ":" + m_port, e.getLocalizedMessage()};
      throw new PSDeployException(DeploymentErrorCodes.LOCK_NOT_RELEASED, args);
    } finally {
      if (m_lockerThread != null) m_lockerThread.interrupt();
      m_isConnected = false;
      m_sessionId = "";
    }
  }

  /**
   * Sends a disconnect request to the server so the user's lock may be released if currently held.
   *
   * @throws PSDeployException If there are any errors.
   */
  private void doDisconnect() throws PSDeployException {
    try {
      Document reqDoc = PSXmlDocumentBuilder.createXmlDocument();
      Element root = PSXmlDocumentBuilder.createRoot(reqDoc, "PSXDeployDisconnectRequest");
      root.setAttribute("userId", m_uid);
      root.setAttribute("password", m_password);
      execute(DEPLOY_REQUEST + "disconnect", reqDoc);
    } catch (PSAuthorizationException e) {
      throw new PSDeployException(e);
    } catch (PSServerException e) {
      throw new PSDeployException(e);
    } catch (PSAuthenticationFailedException e) {
      throw new PSDeployException(e);
    } catch (PSServerLockException e) {
      throw new PSDeployException(e);
    }
  }

  /**
   * Requests lock extension for the locked user.
   *
   * @throws PSDeployException if errors occur executing the request.
   */
  public void extendLock() throws PSDeployException {
    try {
      Document reqDoc = PSXmlDocumentBuilder.createXmlDocument();
      PSXmlDocumentBuilder.createRoot(reqDoc, "PSXDeployExtendLockRequest");
      execute(DEPLOY_REQUEST + "extendlock", reqDoc);
    } catch (PSAuthorizationException
        | PSServerException
        | PSAuthenticationFailedException
        | PSServerLockException e) {
      throw new PSDeployException(e);
    }
  }

  /**
   * Executes the specified request against the server. Convenience version that calls {@link
   * #execute(String, Document, Map) execute(type, req, null)}
   *
   * @param type the request type, may not be <code>null</code> or empty.
   * @param req the request document, may not be <code>null</code>.
   * @return the response document, may be <code>null</code>.
   * @throws PSAuthenticationFailedException if authentication fails.
   * @throws PSAuthorizationException if not authorized.
   * @throws PSServerException if the server cannot be reached.
   * @throws PSDeployException for any other errors.
   * @throws PSServerLockException if the server lock cannot be acquired.
   */
  public Document execute(String type, Document req)
      throws PSAuthenticationFailedException,
          PSAuthorizationException,
          PSServerException,
          PSDeployException,
          PSServerLockException {
    if (type == null || type.trim().length() == 0)
      throw new IllegalArgumentException("type may not be null or empty");

    if (!type.startsWith(DEPLOY_REQUEST) && !type.startsWith(JOB_REQUEST))
      throw new IllegalArgumentException("invalid type");

    if (req == null) throw new IllegalArgumentException("req may not be null");

    return execute(type, req, null);
  }

  /**
   * Executes the specified request against the server.
   *
   * @param type Specifies the request type. Must be a type expected by the handler specified by the
   *     type prefix. May not be <code>null</code> or empty and must begin with one of the supported
   *     handler prefixes. Currently supported handler prefixes are <code>"deploy-"</code> and
   *     <code>"job-"</code>.
   * @param req The body of the request, the format of which is defined by the handler and request
   *     type. May not be <code>null</code>.
   * @param params Map of http params to include in the request. Key is the param name as a <code>
   *     String</code>, value is the param value as a <code>String</code>. May be <code>null</code>
   *     or empty, may not contain an entry with a <code>null</code> key.
   * @return The response document from the request, never <code>null</code>.
   * @throws IllegalArgumentException if any param is invalid.
   * @throws PSAuthenticationFailedException If the user cannot be authenticated by the server.
   * @throws PSAuthorizationException If the user is not authorized to access the server for
   *     deployment operations.
   * @throws PSServerException if there are any errors communicating with the server.
   * @throws PSServerLockException if a required server lock cannot be obtained.
   * @throws PSDeployException if the connection is not valid or any other errors occur executing
   *     the request.
   */
  public Document execute(String type, Document req, Map<String, String> params)
      throws PSAuthenticationFailedException,
          PSAuthorizationException,
          PSServerException,
          PSDeployException,
          PSServerLockException {
    if (type == null || type.trim().length() == 0)
      throw new IllegalArgumentException("type may not be null or empty");

    if (req == null) throw new IllegalArgumentException("req may not be null");

    if (!m_isConnected) {
      throw new PSDeployException(DeploymentErrorCodes.NOT_CONNECTED_ERROR, m_server);
    }
    return execute(type, req, params, true);
  }

  /**
   * Executes the specified request against the server. Private version of {@link #execute(String,
   * Document, Map)} with an additional <code>reconnect</code> parameter described below.
   *
   * @param reconnect If <code>true</code>, an attempt is made to reconnect and re-execute the
   *     request if an exception is returned by the server that could indicate that the user's
   *     session has timed out. If <code>false</code>, no attempt to reconnect and re-execute is
   *     made.
   */
  private Document execute(String type, Document req, Map<String, String> params, boolean reconnect)
      throws PSAuthenticationFailedException,
          PSAuthorizationException,
          PSServerException,
          PSDeployException,
          PSServerLockException {
    String requestPage = getRequestPage(type);

    // add session id if we have one
    Element root = req.getDocumentElement();
    root.setAttribute("sessionId", m_sessionId);

    // make request
    byte[] respData = null;
    Document respDoc = null;
    int status = -1;
    PSPurgableTempFile reqFile = null;
    byte[] data = null;
    HttpResponse<byte[]> resp = null;
    // Content-Type is set by the multipart encoder below; the request-type header rides along.
    Map<String, String> headers = new LinkedHashMap<>();
    try {
      // add the params
      Map<String, String> formFields = getFormFields(params);

      // add the request doc as an attachment with explicit XML content type
      // so PSFormContentParser always sets the input document (NULL_INPUT_DOC)
      reqFile = createAttachmentFile(req);

      MultipartBody encoded = encodeXmlDocumentMultipart(formFields, reqFile);
      headers.put("Content-Type", encoded.getContentType());
      headers.put(IPSCgiVariables.CGI_PS_REQUEST_TYPE, type);
      data = encoded.getBody();

      // send the request to the Rx server
      synchronized (m_mutexObject) {
        resp = postBytes(requestPage, data, headers);
      }
      // get the response code
      status = resp.statusCode();
      respData = resp.body();
    } catch (IOException ioe) {
      log.error(ioe.getMessage());
      log.debug(ioe.getMessage(), ioe);
      System.out.println("3. IOException occurred, rePOSTing: " + ioe.getLocalizedMessage());
      try {
        synchronized (m_mutexObject) {
          resp = postBytes(requestPage, data, headers);
        }
        status = resp.statusCode();
        respData = resp.body();
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        throw new PSDeployException(
            DeploymentErrorCodes.UNEXPECTED_ERROR, ie.getLocalizedMessage());
      } catch (Exception e) {
        log.error(PSExceptionUtils.getMessageForLog(e));
        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        if (data != null && log.isDebugEnabled()) {
          try {
            // log the request data to make debugging easier
            log.debug(getFormFields(params));
            String request = new String(data, "ISO-8859-1");
            log.debug(request);
          } catch (UnsupportedEncodingException uee) {
            log.error(uee.getMessage());
            log.debug(uee.getMessage(), uee);
          }
        }
        throw new PSDeployException(DeploymentErrorCodes.UNEXPECTED_ERROR, e.getLocalizedMessage());
      }
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      throw new PSDeployException(DeploymentErrorCodes.UNEXPECTED_ERROR, ie.getLocalizedMessage());
    } catch (Exception e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      if (data != null && log.isDebugEnabled()) {
        try {
          // log the request data to make debugging easier
          log.debug(getFormFields(params));
          String request = new String(data, "ISO-8859-1");
          log.debug(request);
        } catch (UnsupportedEncodingException uee) {
          log.error(uee.getMessage());
          log.debug(uee.getMessage(), uee);
        }
      }

      throw new PSDeployException(DeploymentErrorCodes.UNEXPECTED_ERROR, e.getLocalizedMessage());
    } finally {
      if (reqFile != null) reqFile.release();
    }

    // handle error response
    try {
      respDoc = parseServerResponse(type, status, respData);
    } catch (PSAuthorizationException e) {
      if (reconnect && m_sessionId.trim().length() > 0) {
        reconnect();
        respDoc = execute(type, req, params, false);
      } else throw e;
    } catch (PSLockedException e) {
      if (reconnect && m_sessionId.trim().length() > 0) {
        reconnect();
        respDoc = execute(type, req, params, false);
      } else throw e;
    }

    return respDoc;
  }

  /**
   * Executes the request, gets the body from the supplied input stream, not the request.
   *
   * @param type Specifies the request type. Must be a type expected by the handler specified by the
   *     type prefix. May not be <code>null</code> or empty and must begin with one of the supported
   *     handler prefixes. Currently supported handler prefixes are <code>"deploy-"</code> and
   *     <code>"job-"</code>.
   * @param params Map of http params to include in the request. Key is the param name as a <code>
   *     String</code>, value is the param value as a <code>String</code>. May be <code>null</code>,
   *     may not contain an entry with a <code>null</code> key.
   * @param body The file to post. May not be <code>null</code> and must reference an existing file.
   *     Data will be sent to the server as an attachment using the file name minus the extension as
   *     the parameter name.
   * @param controller The file job controller to set the input stream on before copying the file.
   *     May not be <code>null</code>.
   * @return The response document from the request, never <code>null</code>.
   * @throws IllegalArgumentException if any param is invalid.
   * @throws PSAuthenticationFailedException If the user cannot be authenticated by the server.
   * @throws PSAuthorizationException If the user is not authorized to access the server for
   *     deployment operations.
   * @throws PSServerException if there are any errors communicating with the server.
   * @throws PSServerLockException if a required server lock cannot be obtained.
   * @throws PSDeployException if the connection is not valid or any other errors occur executing
   *     the request.
   */
  public Document execute(
      String type, Map<String, String> params, File body, PSDeployFileJobControl controller)
      throws PSAuthenticationFailedException,
          PSAuthorizationException,
          PSServerException,
          PSDeployException,
          PSServerLockException {
    if (type == null || type.trim().length() == 0)
      throw new IllegalArgumentException("type may not be null or empty");

    if (body == null || !body.exists())
      throw new IllegalArgumentException("body may not be null and must exist");

    if (controller == null) throw new IllegalArgumentException("controller may not be null");

    if (!m_isConnected) {
      throw new PSDeployException(DeploymentErrorCodes.NOT_CONNECTED_ERROR, m_server);
    }

    return execute(type, params, body, controller, true, true);
  }

  /**
   * Executes the specified request against the server. Private version of {@link #execute(String,
   * Document, OutputStream, PSDeployFileJobControl)} with an additional <code>reconnect</code>
   * parameter described below.
   *
   * @param reconnect If <code>true</code>, an attempt is made to reconnect and re-execute the
   *     request if an exception is returned by the server that could indicate that the user's
   *     session has timed out. If <code>false</code>, no attempt to reconnect and re-execute is
   *     made.
   * @param repost If <code>true</code>, an attempt is made to re-execute the request if an IO
   *     exception is returned by the server. If <code>false</code>, no attempt to re-execute is
   *     made.
   */
  private Document execute(
      String type,
      Map<String, ?> params,
      File body,
      PSDeployFileJobControl controller,
      boolean reconnect,
      boolean repost)
      throws PSAuthenticationFailedException,
          PSAuthorizationException,
          PSServerException,
          PSDeployException,
          PSServerLockException {
    String requestPage = getRequestPage(type);

    // make request
    byte[] respData = null;
    Document respDoc = null;
    int status = -1;
    // Content-Type is set by the multipart encoder below; the request-type header rides along.
    // (The vendored client also sent "Connection: Keep-Alive"; java.net.http manages persistence
    // itself and forbids that restricted header, so it is not forwarded.)
    Map<String, String> headers = new LinkedHashMap<>();

    HttpResponse<byte[]> resp = null;
    boolean doRepost = false;

    // add the params
    Map<String, String> formFields = getFormFields(params);

    // Read in file and create multipart form. Content type mirrors the vendored encoder's
    // URLConnection filename guess, defaulting to application/octet-stream when unknown.
    String fileContentType = URLConnection.guessContentTypeFromName(body.getName());
    if (fileContentType == null) fileContentType = "application/octet-stream";

    headers.put(IPSCgiVariables.CGI_PS_REQUEST_TYPE, type);
    try {
      byte[] fileBytes = Files.readAllBytes(body.toPath());
      MultipartBody encoded =
          encodeMultipart(
              formFields,
              List.of(
                  new MultipartFileUpload(
                      body.getName(), body.getName(), fileContentType, fileBytes)));
      headers.put("Content-Type", encoded.getContentType());
      byte[] data = encoded.getBody();

      // send the request to the Rx server, streaming the buffered body through the progress
      // counter the same way the vendored copyStream pump did
      PSInputStreamCounter counter = new PSInputStreamCounter(new ByteArrayInputStream(data));
      controller.setStream(counter, data.length);
      synchronized (m_mutexObject) {
        resp = postUpload(requestPage, headers, cancellableUploadStream(counter, controller));
      }
      // get the response code
      status = resp.statusCode();
      respData = resp.body();
    } catch (IOException ioe) {
      if (repost) {
        log.error("6. IOException occurred rePOSTing: {}", ioe.getLocalizedMessage());
        log.debug(ioe.getMessage(), ioe);
        doRepost = true;
      } else {
        log.error(ioe.getMessage());
        log.debug(ioe.getMessage(), ioe);
        throw new PSDeployException(
            DeploymentErrorCodes.UNEXPECTED_ERROR, ioe.getLocalizedMessage());
      }
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      if (repost) {
        log.error("6. Interrupted rePOSTing: {}", PSExceptionUtils.getMessageForLog(ie));
        log.debug(PSExceptionUtils.getDebugMessageForLog(ie));
        doRepost = true;
      } else {
        throw new PSDeployException(
            DeploymentErrorCodes.UNEXPECTED_ERROR, ie.getLocalizedMessage());
      }
    } catch (Exception e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new PSDeployException(DeploymentErrorCodes.UNEXPECTED_ERROR, e.getLocalizedMessage());
    }

    if (doRepost) {
      return execute(type, params, body, controller, reconnect, false);
    }

    // handle error response
    try {
      respDoc = parseServerResponse(type, status, respData);
    } catch (PSAuthorizationException e) {
      // String type, Map params, File body, PSDeployFileJobControl controller
      if (reconnect && m_sessionId.trim().length() > 0) {
        reconnect();
        respDoc = execute(type, params, body, controller, false, repost);
      } else throw e;
    } catch (PSLockedException e) {
      if (reconnect && m_sessionId.trim().length() > 0) {
        reconnect();
        respDoc = execute(type, params, body, controller, false, repost);
      } else throw e;
    }

    return respDoc;
  }

  /**
   * Executes the request, writing the results to the supplied output stream rather than returning a
   * document.
   *
   * @param type Specifies the request type. Must be a type expected by the handler specified by the
   *     type prefix. May not be <code>null</code> or empty and must begin with one of the supported
   *     handler prefixes. Currently supported handler prefixes are <code>"deploy-"</code> and
   *     <code>"job-"</code>. The request handler on the server must return a non-200 result if the
   *     resulting content contains an exception document rather than the expected content.
   * @param req The body of the request, the format of which is defined by the handler and request
   *     type. May not be <code>null</code>.
   * @param out The stream to which the body of the response is written, may not be <code>null
   *     </code> or closed. Will be closed by this method.
   * @param controller The file job controller to set the output stream on before copying the file.
   *     May not be <code>null</code>.
   * @throws IllegalArgumentException if any param is invalid.
   * @throws PSAuthenticationFailedException If the user cannot be authenticated by the server.
   * @throws PSAuthorizationException If the user is not authorized to access the server for
   *     deployment operations.
   * @throws PSServerException if there are any errors communicating with the server.
   * @throws PSServerLockException if a required server lock cannot be obtained.
   * @throws PSDeployException if the connection is not valid or any other errors occur executing
   *     the request.
   */
  public void execute(
      String type, Document req, OutputStream out, PSDeployFileJobControl controller)
      throws PSAuthenticationFailedException,
          PSAuthorizationException,
          PSServerException,
          PSDeployException,
          PSServerLockException {
    if (type == null) throw new IllegalArgumentException(" may not be null");

    if (req == null) throw new IllegalArgumentException("req may not be null");

    if (out == null) throw new IllegalArgumentException("out may not be null");

    if (controller == null) throw new IllegalArgumentException("controller may not be null");

    execute(type, req, out, controller, true);
  }

  /**
   * Executes the specified request against the server. Private version of {@link #execute(String,
   * Document, OutputStream, PSDeployFileJobControl)} with an additional <code>reconnect</code>
   * parameter described below.
   *
   * @param reconnect If <code>true</code>, an attempt is made to reconnect and re-execute the
   *     request if an exception is returned by the server that could indicate that the user's
   *     session has timed out. If <code>false</code>, no attempt to reconnect and re-execute is
   *     made.
   */
  private void execute(
      String type,
      Document req,
      OutputStream out,
      PSDeployFileJobControl controller,
      boolean reconnect)
      throws PSAuthenticationFailedException,
          PSAuthorizationException,
          PSServerException,
          PSDeployException,
          PSServerLockException {
    String requestPage = getRequestPage(type);

    // add session id if we have one
    Element root = req.getDocumentElement();
    root.setAttribute("sessionId", m_sessionId);

    // make request
    byte[] respData = null;
    InputStream in = null;
    int status = -1;

    // add the request doc as an attachment with explicit XML content type
    try (PSPurgableTempFile reqFile = createAttachmentFile(req)) {

      // Content-Type is set by the multipart encoder below; the request-type header rides along.
      Map<String, String> headers = new LinkedHashMap<>();
      headers.put(IPSCgiVariables.CGI_PS_REQUEST_TYPE, type);
      MultipartBody encoded = encodeXmlDocumentMultipart(null, reqFile);
      headers.put("Content-Type", encoded.getContentType());
      byte[] data = encoded.getBody();

      // send the request to the Rx server
      HttpResponse<InputStream> resp;
      synchronized (m_mutexObject) {
        resp = postDownload(requestPage, data, headers);
      }
      // get the response code
      status = resp.statusCode();
      int contentLength = parseContentLength(resp);
      if (status == 200) {
        in = resp.body();
        PSInputStreamCounter cIn = new PSInputStreamCounter(in);
        controller.setStream(cIn, contentLength);
        copyStream(cIn, out, controller);
      } else {
        // in this case we should have gotten back an exception in an XML
        // doc
        try (InputStream errIn = resp.body()) {
          respData = errIn.readAllBytes();
        }
      }
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      throw new PSDeployException(
          DeploymentErrorCodes.UNEXPECTED_ERROR, ie.getLocalizedMessage());
    } catch (Exception e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new PSDeployException(DeploymentErrorCodes.UNEXPECTED_ERROR, e.getLocalizedMessage());
    } finally {
      if (in != null)
        try {
          in.close();
        } catch (IOException e) {
        }
      if (out != null)
        try {
          out.close();
        } catch (IOException e) {
        }
    }

    if (status != 200 || respData != null) {
      try {
        parseServerResponse(type, status, respData);
      } catch (PSAuthorizationException e) {
        if (reconnect && m_sessionId.trim().length() > 0) {
          reconnect();
          execute(type, req, out, controller, false);
        } else throw e;
      } catch (PSLockedException e) {
        if (reconnect && m_sessionId.trim().length() > 0) {
          reconnect();
          execute(type, req, out, controller, false);
        } else throw e;
      }
    }
  }

  /**
   * Disconnects, clears the sessionid, and calls {@link #connect(boolean) connect(false)}.
   *
   * @throws PSAuthenticationFailedException If the user cannot be authenticated by the server.
   * @throws PSAuthorizationException If the user is not authorized to access the server for
   *     deployment operations.
   * @throws PSServerException if there are any errors communicating with the server.
   * @throws PSDeployException for any other errors.
   */
  private void reconnect()
      throws PSAuthenticationFailedException,
          PSAuthorizationException,
          PSServerException,
          PSDeployException {
    doDisconnect();
    m_sessionId = "";
    connect(false);
  }

  /**
   * Determine if this connection is still valid for use.
   *
   * @return <code>true</code> if the connection is valid for use, <code>false</code> if it is not.
   */
  public boolean isConnected() {
    return m_isConnected;
  }

  /**
   * Get the server version
   *
   * @return The server version, may be <code>null</code> if not connected, never empty.
   */
  public String getServerVersion() {
    return m_isConnected ? m_version.getVersionString() : null;
  }

  /**
   * Gets the server build number.
   *
   * @return The number, may be <code>null</code> if not connected, never empty.
   */
  public String getServerBuildNumber() {
    return m_isConnected ? m_version.getBuildNumber() : null;
  }

  /**
   * Gets the server's build date.
   *
   * @return The date, may be <code>null</code> if not connected.
   */
  public Date getServerBuildDate() {
    return m_isConnected ? m_version.getBuildDate() : null;
  }

  /**
   * Gets the password used to connect to the server.
   *
   * @param encrypted If <code>true</code>, the password will be returned in its encrypted form.
   *     Otherwise, the clear text version of the password is returned.
   * @return The password supplied by the ctor, possibly encrypted, never <code>null</code>, may be
   *     empty.
   */
  public String getPassword(boolean encrypted) {
    return encrypted ? m_password : decryptPwd(m_uid, m_password);
  }

  /**
   * Get the dbms info for the server's cms repository.
   *
   * @return The dbms info. UserId and Password will be empty to allow comparisons without regard to
   *     credentials. May be <code>null</code> if not connected.
   */
  public PSDbmsInfo getRepositoryInfo() {
    return m_isConnected ? m_serverRepositoryInfo : null;
  }

  /**
   * Returns a boolean indicating whether the Rx server to which this object represents a connection
   * is licensed for Enterprise Manager.
   *
   * @return <code>true</code> if the Rx server to which this object represents a connection is
   *     licensed for Enterprise Manager, <code>false</code> otherwise.
   */
  public boolean isServerLicensed() {
    return m_bLicensed;
  }

  /**
   * Encrypts the supplied password if it is non-<code>null</code> and not empty.
   *
   * <p>Encryption always uses {@code PSEncryptor} (AES/GCM). The legacy lasagna/DES fallback was
   * removed in issue #6057: {@code PSEncryptionKeyFactory} unconditionally rejects DES, so {@code
   * PSCryptographer} can no longer decrypt anything and the old fallback only threw {@code
   * IllegalArgumentException} for non-AES input.
   *
   * @param uid The user id, may be <code>null</code> or empty.
   * @param pwd The password to encrypt, may be <code>null</code> or empty.
   * @return The encrypted password, or an empty string if the supplied password is <code>null
   *     </code> or empty.
   */
  public static String encryptPwd(String uid, String pwd) {
    if (pwd == null || pwd.trim().length() == 0) return "";

    try {
      return PSEncryptor.encryptString(
          PathUtils.getRxDir(null).getAbsolutePath().concat(PSEncryptor.SECURE_DIR), pwd);

    } catch (PSEncryptionException e) {
      log.error("Error encrypting password: {}", PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      return "";
    }
  }

  /**
   * Decrypts the supplied password if it is non-<code>null</code> and not empty.
   *
   * <p>Decryption uses {@code PSEncryptor} (AES/GCM) only. The legacy lasagna/DES fallback was
   * removed in issue #6057: {@code PSEncryptionKeyFactory} unconditionally rejects DES, so the
   * old fallback could never decrypt pre-AES artifacts — it only threw {@code
   * IllegalArgumentException} for input the AES path rejects. Undecryptable input now yields an
   * empty string, matching this method's documented contract. Restoring true legacy decryption
   * needs a human/owner accept-risk decision (same bar as PSAesCBC, issue #1347).
   *
   * @param uid The user id, may be <code>null</code> or empty.
   * @param pwd The password to decrypt, may be <code>null</code> or empty.
   * @return The decrypted password, or an empty string if the supplied password is <code>null
   *     </code> or empty, or cannot be decrypted.
   */
  public static String decryptPwd(String uid, String pwd) {
    if (pwd == null || pwd.trim().length() == 0) return "";

    try {
      return PSEncryptor.decryptString(
          PathUtils.getRxDir().getAbsolutePath().concat(PSEncryptor.SECURE_DIR), pwd);
    } catch (PSEncryptionException e) {
      log.error("Error decrypting password: {}", PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      return "";
    }
  }

  /**
   * Makes connection request, stores the returned session id, and returns the resulting response
   * document.
   *
   * @param overrideLock If <code>true</code>, then the lock is acquired by the user no matter what,
   *     if <code>false</code> then sessionid has to match for lock acquisition.
   * @return The response doc, never <code>null</code>.
   * @throws PSAuthenticationFailedException If the user cannot be authenticated by the server.
   * @throws PSAuthorizationException If the user is not authorized to access the server for
   *     deployment operations.
   * @throws PSServerException if there are any errors communicating with the server.
   * @throws PSDeployException For any other errors.
   */
  private Document connect(boolean overrideLock)
      throws PSAuthenticationFailedException,
          PSAuthorizationException,
          PSServerException,
          PSDeployException {
    var reqDoc = PSXmlDocumentBuilder.createXmlDocument();
    var root = PSXmlDocumentBuilder.createRoot(reqDoc, "PSXDeployConnectRequest");
    root.setAttribute("userId", m_uid);
    root.setAttribute("password", m_password);
    root.setAttribute("overrideLock", overrideLock ? "yes" : "no");
    root.setAttribute("enforceLicense", "no");

    Document respDoc;
    try {
      respDoc = execute(DEPLOY_REQUEST + "connect", reqDoc);
    } catch (PSServerLockException e) {
      throw new PSDeployException(e);
    }

    root = respDoc.getDocumentElement();
    m_sessionId = root.getAttribute("sessionId");

    return respDoc;
  }

  /**
   * Writes doc out to a temp file to be used as an attachment on a request.
   *
   * @param doc The document to store in the file. Assumed not <code>null</code>.
   * @return The file, never <code>null</code>.
   * @throws IOException If there are any errors.
   */
  private PSPurgableTempFile createAttachmentFile(Document doc) throws Exception {

    PSPurgableTempFile reqFile = new PSPurgableTempFile("dpl_", ".xml", null);
    try (FileOutputStream out = new FileOutputStream(reqFile)) {
      PSXmlDocumentBuilder.write(doc, out);
    }
    return reqFile;
  }

  /**
   * Multipart Content-Type forced on XML request document parts so the server {@code
   * PSFormContentParser} classifies the part as XML and calls {@code setInputDocument}. Relying
   * only on {@code URLConnection.guessContentTypeFromName} can omit or mis-classify the type on
   * some JREs/platforms and surfaces as {@code DeploymentErrorCodes.NULL_INPUT_DOC}.
   */
  public static final String XML_REQUEST_CONTENT_TYPE = "application/xml";

  /**
   * One multipart {@code form-data} file part: field name, filename, part Content-Type (may be
   * {@code null}, in which case no part Content-Type header is written, mirroring the vendored
   * encoder's filename-guess behavior), and raw bytes.
   *
   * <p>Package-private for unit tests.
   */
  static final class MultipartFileUpload {
    private final String fieldName;
    private final String fileName;
    private final String contentType;
    private final byte[] data;

    MultipartFileUpload(String fieldName, String fileName, String contentType, byte[] data) {
      if (fieldName == null || fieldName.isEmpty())
        throw new IllegalArgumentException("fieldName may not be null or empty");
      if (fileName == null || fileName.isEmpty())
        throw new IllegalArgumentException("fileName may not be null or empty");
      if (data == null) throw new IllegalArgumentException("data may not be null");
      this.fieldName = fieldName;
      this.fileName = fileName;
      this.contentType = contentType;
      this.data = data;
    }

    String getFieldName() {
      return fieldName;
    }

    String getFileName() {
      return fileName;
    }

    String getContentType() {
      return contentType;
    }

    byte[] getData() {
      return data;
    }
  }

  /**
   * An encoded {@code multipart/form-data} body plus the {@code Content-Type} header value
   * (including boundary) to send with it.
   *
   * <p>Package-private for unit tests.
   */
  static final class MultipartBody {
    private final byte[] body;
    private final String contentType;

    MultipartBody(byte[] body, String contentType) {
      if (body == null) throw new IllegalArgumentException("body may not be null");
      if (contentType == null) throw new IllegalArgumentException("contentType may not be null");
      this.body = body;
      this.contentType = contentType;
    }

    byte[] getBody() {
      return body;
    }

    String getContentType() {
      return contentType;
    }
  }

  /**
   * Builds a multipart file part for an XML request body with an explicit {@link
   * #XML_REQUEST_CONTENT_TYPE} Content-Type.
   *
   * <p>Package-private for unit tests.
   *
   * @param fieldName form field / disposition name; if {@code null} or blank, {@code
   *     dpl_request.xml} is used. A {@code .xml} suffix is ensured for filename-based fallbacks.
   * @param xmlBytes raw XML bytes, not {@code null}
   * @return the file part, never {@code null}
   */
  static MultipartFileUpload createXmlRequestFileData(String fieldName, byte[] xmlBytes) {
    if (xmlBytes == null) {
      throw new IllegalArgumentException("xmlBytes may not be null");
    }
    String partName =
        (fieldName == null || fieldName.trim().isEmpty()) ? "dpl_request.xml" : fieldName.trim();
    // Use a simple basename (no path separators) for Content-Disposition filename.
    int slash = Math.max(partName.lastIndexOf('/'), partName.lastIndexOf('\\'));
    if (slash >= 0 && slash < partName.length() - 1) {
      partName = partName.substring(slash + 1);
    }
    if (!partName.toLowerCase(Locale.ROOT).endsWith(".xml")) {
      partName = partName + ".xml";
    }
    return new MultipartFileUpload(partName, partName, XML_REQUEST_CONTENT_TYPE, xmlBytes);
  }

  /**
   * Multipart-encodes form fields plus an XML request document file with explicit {@link
   * #XML_REQUEST_CONTENT_TYPE} on the file part.
   *
   * <p>Package-private for unit tests.
   *
   * @param fields form field map, may be {@code null}
   * @param xmlFile temp file containing the XML document, not {@code null}
   * @return encoded multipart body, never {@code null}
   * @throws IOException if the file cannot be read
   */
  static MultipartBody encodeXmlDocumentMultipart(Map<String, String> fields, File xmlFile)
      throws IOException {
    if (xmlFile == null) {
      throw new IllegalArgumentException("xmlFile may not be null");
    }
    byte[] xmlBytes = Files.readAllBytes(xmlFile.toPath());
    MultipartFileUpload file = createXmlRequestFileData(xmlFile.getName(), xmlBytes);
    return encodeMultipart(fields, List.of(file));
  }

  /**
   * Multipart-encodes form fields plus an in-memory XML document with explicit {@link
   * #XML_REQUEST_CONTENT_TYPE} on the file part. Used by unit tests and as a convenience for
   * callers that already have a {@link Document}.
   *
   * <p>Package-private for unit tests.
   *
   * @param fields form field map, may be {@code null}
   * @param doc XML request document, not {@code null}
   * @return encoded multipart body, never {@code null}
   * @throws Exception if the document cannot be serialized
   */
  static MultipartBody encodeXmlDocumentMultipart(Map<String, String> fields, Document doc)
      throws Exception {
    if (doc == null) {
      throw new IllegalArgumentException("doc may not be null");
    }
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    PSXmlDocumentBuilder.write(doc, bos);
    byte[] xmlBytes = bos.toByteArray();
    MultipartFileUpload file = createXmlRequestFileData("dpl_request.xml", xmlBytes);
    return encodeMultipart(fields, List.of(file));
  }

  /**
   * Encodes form fields and file parts into a {@code multipart/form-data} body, using the same
   * part layout the vendored {@code Codecs.mpFormDataEncode} produced: field parts as {@code
   * Content-Disposition: form-data; name="..."} followed by file parts with an explicit filename
   * and, when known, a part Content-Type. Field names/values are encoded ISO-8859-1, matching the
   * vendored default.
   *
   * <p>Package-private for unit tests.
   *
   * @param fields form field map, may be {@code null} or empty
   * @param files file parts, may be {@code null} or empty
   * @return encoded body plus Content-Type header value, never {@code null}
   */
  static MultipartBody encodeMultipart(
      Map<String, String> fields, List<MultipartFileUpload> files) {
    String boundary = "-----------------------------" + Long.toUnsignedString(System.nanoTime());
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    boolean hasPart = false;

    if (fields != null) {
      for (Map.Entry<String, String> field : fields.entrySet()) {
        if (field == null) continue;
        hasPart = true;
        writeFieldPart(out, boundary, field.getKey(), field.getValue());
      }
    }

    if (files != null) {
      for (MultipartFileUpload file : files) {
        if (file == null) continue;
        hasPart = true;
        writeFilePart(out, boundary, file);
      }
    }

    if (!hasPart) {
      return new MultipartBody(new byte[0], "application/octet-stream");
    }
    writeAscii(out, "--" + boundary + "--\r\n");
    return new MultipartBody(
        out.toByteArray(), "multipart/form-data; boundary=" + boundary);
  }

  /**
   * Writes one {@code form-data} field part. Header tokens ({@code Content-Disposition}, CRLF)
   * are RFC 2046 wire literals, identical on every OS.
   */
  private static void writeFieldPart(
      ByteArrayOutputStream out, String boundary, String name, String value) {
    writeAscii(out, "--" + boundary + "\r\n");
    // mkd-code-review[paths.hardcoded_sep]: RFC 2046 wire literal (CRLF, quoted disposition
    // name), not a filesystem path join — byte-identical on all platforms by protocol.
    writeAscii(out, "Content-Disposition: form-data; name=\"" + name + "\"\r\n");
    writeAscii(out, "\r\n");
    byte[] valueBytes = value == null ? new byte[0] : value.getBytes(StandardCharsets.ISO_8859_1);
    out.write(valueBytes, 0, valueBytes.length);
    writeAscii(out, "\r\n");
  }

  /**
   * Writes one {@code form-data} file part, with a part Content-Type only when known. Header
   * tokens are RFC 2046 wire literals, identical on every OS.
   */
  private static void writeFilePart(
      ByteArrayOutputStream out, String boundary, MultipartFileUpload file) {
    writeAscii(out, "--" + boundary + "\r\n");
    // mkd-code-review[paths.hardcoded_sep]: RFC 2046 wire literal (CRLF, quoted disposition
    // name/filename), not a filesystem path join — byte-identical on all platforms by protocol.
    String disposition = String.format("Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"\r\n", file.getFieldName(), file.getFileName());
    writeAscii(out, disposition);
    if (file.getContentType() != null) {
      writeAscii(out, "Content-Type: " + file.getContentType() + "\r\n");
    }
    writeAscii(out, "\r\n");
    byte[] data = file.getData();
    out.write(data, 0, data.length);
    writeAscii(out, "\r\n");
  }

  /**
   * Sends a buffered request body and buffers the full response, mirroring the vendored {@code
   * Post(byte[], headers)} call shape. The per-instance client carries the preemptive {@code
   * Authorization} header and the connection cookie jar on every request.
   *
   * @param requestPage request path, resolved against the connection base URI
   * @param body buffered request body, never {@code null}
   * @param headers additional request headers, never {@code null}
   * @return the response with a buffered body, never {@code null}
   * @throws IOException if the send fails
   * @throws InterruptedException if the send is interrupted
   */
  private HttpResponse<byte[]> postBytes(
      String requestPage, byte[] body, Map<String, String> headers)
      throws IOException, InterruptedException {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(m_baseUri.resolve(requestPage))
            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
            .header("Authorization", m_basicAuthHeader);
    for (Map.Entry<String, String> header : headers.entrySet()) {
      builder.header(header.getKey(), header.getValue());
    }
    return m_httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
  }

  /**
   * Sends a streaming upload body (progress counted through the supplied stream) and buffers the
   * response, replacing the vendored {@code Post(HttpOutputStream, headers)} + {@code copyStream}
   * pump. The body is sent chunked; servlet containers de-chunk transparently before the
   * multipart parser sees it.
   *
   * @param requestPage request path, resolved against the connection base URI
   * @param headers additional request headers, never {@code null}
   * @param upload body stream (progress-counting and cancellation-aware), never {@code null}
   * @return the response with a buffered body, never {@code null}
   * @throws IOException if the send fails
   * @throws InterruptedException if the send is interrupted
   */
  private HttpResponse<byte[]> postUpload(
      String requestPage, Map<String, String> headers, InputStream upload)
      throws IOException, InterruptedException {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(m_baseUri.resolve(requestPage))
            .POST(HttpRequest.BodyPublishers.ofInputStream(() -> upload))
            .header("Authorization", m_basicAuthHeader);
    for (Map.Entry<String, String> header : headers.entrySet()) {
      builder.header(header.getKey(), header.getValue());
    }
    return m_httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
  }

  /**
   * Sends a buffered request body and streams the response, replacing the vendored {@code
   * Post(byte[], headers)} + {@code getInputStream} download shape. Callers own the response
   * stream.
   *
   * @param requestPage request path, resolved against the connection base URI
   * @param body buffered request body, never {@code null}
   * @param headers additional request headers, never {@code null}
   * @return the response with a streaming body, never {@code null}
   * @throws IOException if the send fails
   * @throws InterruptedException if the send is interrupted
   */
  private HttpResponse<InputStream> postDownload(
      String requestPage, byte[] body, Map<String, String> headers)
      throws IOException, InterruptedException {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(m_baseUri.resolve(requestPage))
            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
            .header("Authorization", m_basicAuthHeader);
    for (Map.Entry<String, String> header : headers.entrySet()) {
      builder.header(header.getKey(), header.getValue());
    }
    return m_httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
  }

  /**
   * Reads the response {@code Content-Length}, throwing {@link NumberFormatException} when the
   * header is absent or unparseable — the same contract the vendored {@code getHeaderAsInt} had,
   * so the surrounding error mapping is unchanged.
   */
  private static int parseContentLength(HttpResponse<?> resp) {
    String value = resp.headers().firstValue("Content-Length").orElse(null);
    if (value == null) throw new NumberFormatException("null");
    return Integer.parseInt(value);
  }

  /**
   * Wraps the upload progress stream so a controller cancellation aborts the send with an {@link
   * IOException}, mirroring the vendored {@code copyStream} pump which stopped (and failed the
   * request) once {@code getCancelledStatus()} left {@code -1}.
   */
  private static InputStream cancellableUploadStream(
      InputStream in, PSDeployFileJobControl controller) {
    return new FilterInputStream(in) {
      private void checkCancelled() throws IOException {
        if (controller.getCancelledStatus() != -1) throw new IOException("Upload cancelled");
      }

      @Override
      public int read() throws IOException {
        checkCancelled();
        return super.read();
      }

      @Override
      public int read(byte[] b, int off, int len) throws IOException {
        checkCancelled();
        return super.read(b, off, len);
      }
    };
  }

  /**
   * Writes an ASCII header line to the multipart buffer. Header names, boundaries, and disposition
   * attributes are protocol ASCII by construction.
   */
  private static void writeAscii(ByteArrayOutputStream out, String text) {
    byte[] bytes = text.getBytes(StandardCharsets.US_ASCII);
    out.write(bytes, 0, bytes.length);
  }

  /**
   * Returns {@code true} if the multipart body contains a part Content-Type of {@link
   * #XML_REQUEST_CONTENT_TYPE} or {@code text/xml}. Package-private for unit tests.
   *
   * @param multipartBody encoded multipart bytes, not {@code null}
   * @return whether an XML content-type header is present for a part
   */
  static boolean multipartContainsXmlContentType(byte[] multipartBody) {
    if (multipartBody == null) {
      return false;
    }
    // Multipart header bytes are ISO-8859-1 by construction (see encodeMultipart).
    String body = new String(multipartBody, StandardCharsets.ISO_8859_1);
    String lower = body.toLowerCase(Locale.ROOT);
    return lower.contains("content-type: application/xml")
        || lower.contains("content-type: text/xml");
  }

  /**
   * Constant to indicate the deployment version. Used to determine compatibility between the client
   * and server. If the server is modified such that it is no longer compatible with older clients,
   * this value should be incremented.
   */
  public static final int DEPLOYMENT_INTERFACE_VERSION = 8;

  /** Constant for prefix when making deployment handler requests. */
  public static final String DEPLOY_REQUEST = "deploy-";

  /** Constant for prefix when making job handler requests. */
  public static final String JOB_REQUEST = "job-";

  /**
   * Attempts to parse the response returned by the server. Checks the supplied <code>status</code>
   * and <code>response</code> to see if the server has returned any errors and if so throws the
   * appropriate exception. If no errors are found, the result document is returned.
   *
   * @param type The request type, assumed not <code>null</code> or empty.
   * @param status The status returned by the request.
   * @param response The response to check, may be <code>null</code>.
   * @return The result document, never <code>null</code>.
   * @throws PSAuthenticationFailedException If the response is an Xml doc containing a
   *     PSXDeployException element with this exception nested within it.
   * @throws PSAuthorizationException If the response is an Xml doc containing a PSXDeployException
   *     element with this exception nested within it.
   * @throws PSAuthorizationException If the response is an Xml doc containing a PSXDeployException
   *     element with this exception nested within it.
   * @throws PSServerLockException if a required server lock cannot be obtained.
   * @throws PSDeployException If the the <code>respDoc</code> is <code>null</code>, the status is
   *     not <code>200</code>, or the data contains a PSXDeployException element with none of the
   *     other exceptions nested within it.
   */
  private Document parseServerResponse(String type, int status, byte[] response)
      throws PSAuthenticationFailedException,
          PSAuthorizationException,
          PSServerException,
          PSDeployException,
          PSServerLockException {
    Document respDoc = null;

    // handle case if server is still starting
    if (status == 503) {
      throw new PSDeployException(DeploymentErrorCodes.SERVER_NOT_AVAILABLE);
    }

    // make sure we got back some kind of response
    if (response == null || response.length == 0) {
      Object[] args = {type, String.valueOf(status)};
      throw new PSDeployException(DeploymentErrorCodes.SERVER_RESPONSE_EMPTY, args);
    }

    // try to parse the response as XML

    try (ByteArrayInputStream bIn = new ByteArrayInputStream(response)) {
      respDoc = PSXmlDocumentBuilder.createXmlDocument(bIn, false);
    } catch (Exception e) {
      // not valid xml
      if (status == 200) {
        // must have valid response for a 200 error.
        Object[] args = {type, e.getLocalizedMessage()};
        throw new PSDeployException(DeploymentErrorCodes.SERVER_RESPONSE_PARSE_ERROR, args);
      }
    }

    // if we parsed the response and status is 200, we're all done.
    if (status == 200) return respDoc;

    // see if we've got an error from the server that wasn't handled by the
    // deployment or job handlers.
    if (respDoc == null) {
      // can't handle it, so throw exception with the body as a string
      String msg = "";
      try {
        msg = new String(response, PSCharSetsConstants.rxJavaEnc());
      } catch (UnsupportedEncodingException e) {
        // we've done our best
      }

      Object[] args = {type, String.valueOf(status), msg};
      throw new PSDeployException(DeploymentErrorCodes.SERVER_ERROR_RESPONSE, args);
    }

    // We must have an error that we can handle in some way.
    Element root = respDoc.getDocumentElement();
    PSDeployException de = null;
    String className = null;
    int errorCode = -1;
    Object[] errorArgs = null;
    if (root.getNodeName().equals(PSDeployException.XML_NODE_NAME)) {
      try {
        de = new PSDeployException(root);
      } catch (PSUnknownNodeTypeException e) {
        // malformed exception xml (should not happen)
        Object[] args = {type, PSDeployException.XML_NODE_NAME, e.getLocalizedMessage()};
        throw new PSDeployException(DeploymentErrorCodes.SERVER_RESPONSE_ELEMENT_INVALID, args);
      }

      className = de.getOriginalExceptionClass();
      if (className != null) {
        errorCode = de.getErrorCode();
        errorArgs = de.getErrorArguments();
      }
    } else if (root.getNodeName().equals(PSJobException.XML_NODE_NAME)) {
      PSJobException je = null;
      try {
        je = new PSJobException(root);
        de = new PSDeployException(DeploymentErrorCodes.UNEXPECTED_ERROR, je.getLocalizedMessage());
      } catch (PSUnknownNodeTypeException e) {
        // malformed exception xml (should not happen)
        Object[] args = {type, PSDeployException.XML_NODE_NAME, e.getLocalizedMessage()};
        throw new PSDeployException(DeploymentErrorCodes.SERVER_RESPONSE_ELEMENT_INVALID, args);
      }

      className = je.getOriginalExceptionClass();
      if (className != null) {
        errorCode = je.getErrorCode();
        errorArgs = je.getErrorArguments();
      }
    } else {
      Object[] args = {type, String.valueOf(status), PSXmlDocumentBuilder.toString(respDoc)};
      throw new PSDeployException(DeploymentErrorCodes.SERVER_ERROR_RESPONSE, args);
    }

    if (PSAuthenticationFailedException.class.getName().equals(className)) {
      throw new PSAuthenticationFailedException(errorCode, errorArgs);
    } else if (PSAuthorizationException.class.getName().equals(className)) {
      throw new PSAuthorizationException(errorCode, errorArgs);
    } else if (PSAuthenticationRequiredException.class.getName().equals(className)) {
      throw new PSAuthenticationRequiredException(errorCode, errorArgs);
    } else if (PSServerException.class.getName().equals(className)) {
      throw new PSServerException(errorCode, errorArgs);
    } else if (PSDeployNonUniqueException.class.getName().equals(className)) {
      throw new PSDeployNonUniqueException(errorCode, errorArgs);
    } else if (PSLockedException.class.getName().equals(className)) {
      throw new PSLockedException(errorCode, errorArgs);
    } else if (PSServerLockException.class.getName().equals(className)) {
      throw new PSServerLockException(errorCode, errorArgs);
    } else if (de != null) {
      throw de;
    }

    // should never get here, but compiler complains without it
    return respDoc;
  }

  /**
   * Copies the supplied input stream to the output stream, using the controller to detect
   * cancellation and to mark copy as completed.
   *
   * @param in The input stream, assumed not <code>null</code>.
   * @param out The output stream, assumed not <code>null</code>.
   * @param controller The job controller, with the appropriate stream already set on it, assumed
   *     not <code>null</code>.
   * @throws IOException If any errors occur.
   */
  private void copyStream(InputStream in, OutputStream out, PSDeployFileJobControl controller)
      throws IOException {
    byte[] buf = new byte[8192];
    while (true) {
      int read = in.read(buf);
      if (read < 0 || controller.getCancelledStatus() != -1) break;
      out.write(buf, 0, read);
    }
    out.flush();
    out.close();
    out = null;
    in.close();
  }

  /**
   * Gets the appropriate request page for the given request type.
   *
   * @param requestType The type, may not be <code>null</code> or empty, and must be a known type.
   * @return The page, never <code>null</code> or empty.
   * @throws IllegalArgumentException if <code>requestType</code> is invalid.
   */
  private String getRequestPage(String requestType) {
    if (requestType == null || requestType.trim().length() == 0)
      throw new IllegalArgumentException("requestType may not be null or empty");

    if (m_reqRoot == null) {
      String reqRoot = System.getProperty(IPSDeployConstants.PROP_RX_ROOT);
      if (reqRoot == null || reqRoot.trim().length() == 0) reqRoot = "/Rhythmyx";
      else {
        // make sure it starts with a slash, and doesn't end with one
        String slash = "/";
        if (!reqRoot.startsWith(slash)) reqRoot = slash + reqRoot;
        if (reqRoot.endsWith(slash)) reqRoot = reqRoot.substring(0, reqRoot.length() - 1);
      }
      m_reqRoot = reqRoot;
    }

    String page;
    if (requestType.startsWith(DEPLOY_REQUEST)) page = m_reqRoot + DEPLOY_REQUEST_PAGE;
    else if (requestType.startsWith(JOB_REQUEST)) page = m_reqRoot + JOB_REQUEST_PAGE;
    else throw new IllegalArgumentException("invalid type");

    return page;
  }

  /**
   * Converts params to an insertion-ordered form-field map for multipart encoding.
   *
   * @param params Map of params, where param name is key as <code>String</code> and value is the
   *     param value. May be <code>null</code>, may not contain a <code>null</code> key.
   * @return The field map, will be <code>null</code> if <code>params</code> is <code>null</code> or
   *     empty.
   */
  private Map<String, String> getFormFields(Map<String, ?> params) {
    // add the params
    Map<String, String> fields = null;
    if (params != null && params.size() > 0) {
      fields = new LinkedHashMap<>();
      for (Map.Entry<String, ?> entry : params.entrySet()) {
        if (entry.getKey() == null)
          throw new IllegalArgumentException("params may not contain a null key");

        String val = entry.getValue() != null ? entry.getValue().toString() : "";

        fields.put(entry.getKey().toString(), val);
      }
    }

    return fields;
  }

  /** Reference to Log4j singleton object used to log any errors or debug info. */
  private static final Logger log = LogManager.getLogger(PSDeploymentServerConnection.class);

  /** Constant for the page to use when executing deployment requests against the server. */
  private static final String DEPLOY_REQUEST_PAGE = "/sys_deployerHandler";

  private static final String MSM_DEPLOY_REQUEST_PAGE = "/sys_deploymentHandler";

  /** Constant for the page to use when executing job requests against the server. */
  private static final String JOB_REQUEST_PAGE = "/sys_jobHandler";

  /**
   * Determines if this object can be used to execute requests against the server. Intialized during
   * the ctor, may be modified by calls to {@link #reconnect()} and {@link #disconnect()}.
   */
  private volatile boolean m_isConnected;

  /**
   * The name of the server to connect to, set during ctor, never <code>null</code>, empty or
   * modified after that.
   */
  private String m_server;

  /** The port on the server to connect to, set during ctor, never modified after that. */
  private int m_port;

  /**
   * The server version info. Obtained during connection attempt from ctor, may be <code>null</code>
   * if connection request fails, may be reset on a subsequent attempt to reconnect.
   */
  private PSFormatVersion m_version;

  /**
   * The session id obtained from the intial connection. May be modified if the user's session on
   * the server times out and we must reconnect.
   */
  private String m_sessionId = "";

  /**
   * The server's repository information. Obtained during connection attempt from ctor, may be
   * <code>null</code> if connection request fails, may be reset on a subsequent attempt to
   * reconnect.
   */
  private PSDbmsInfo m_serverRepositoryInfo;

  /** The user's id. Set during ctor, never <code>null</code> or empty or modified after that. */
  private String m_uid;

  /**
   * The user's password. Set during ctor, may be empty, never <code>null</code> or modified after
   * that.
   */
  private String m_password;

  /**
   * Base URI of the Rx server (scheme + host + port), set during ctor, never <code>null</code> or
   * modified after that. Request pages from {@link #getRequestPage(String)} are resolved against
   * it.
   */
  private final URI m_baseUri;

  /**
   * Preemptive {@code Authorization} header value ({@code Basic} userid:password), replacing the
   * vendored client's {@code addBasicAuthorization}. Never <code>null</code> or modified after
   * ctor.
   */
  private final String m_basicAuthHeader;

  /**
   * Shared JDK HTTP client for this connection. Thread-safe, so the {@link #m_mutexObject} guard
   * around sends is retained only to preserve the original mutual-exclusion behavior. Carries a
   * per-instance cookie jar, replacing the vendored client's {@code setContext(this)} cookie
   * association. Never <code>null</code> or modified after ctor.
   */
  private final HttpClient m_httpClient;

  /**
   * Mutex preventing concurrent access to a critical section by having threads synchronize on it.
   * Here it's preventing concurrent access to <code>m_conn
   * </code>. Never modified.
   */
  private Object m_mutexObject = new Object();

  /**
   * Runs as a daemon thread. Lock is granted for 30 mins. Locker thread extends the lock after 28
   * mins. Initialised in the ctor, never <code>null
   * </code> or modified. Thread dies whenever <code>m_isConnected</code> is <code>false</code>.
   */
  private Thread m_lockerThread;

  /**
   * Rhythmyx server request root, initialzied by first call to {@link #getRequestPage(String)},
   * never <code>null</code>, empty, or modified after that.
   */
  private String m_reqRoot = null;

  /**
   * <code>true</code> if the Rx server to which this object represents a connection is licensed for
   * Enterprise Manager, <code>false</code> otherwise. Initialized to <code>true</code>, then set in
   * the constructor based on the response document obtained from the Rx Server in the <code>
   * connect()</code> method, never modified after that.
   */
  private boolean m_bLicensed = true;

  /** The operating mode for this connection, never <code>null</code>. */
  private IPSDeployConstants.OperatingMode operatingMode =
      IPSDeployConstants.OperatingMode.Packager;

  /**
   * Returns the operating mode for this connection.
   *
   * @return the operating mode, never <code>null</code>.
   */
  public IPSDeployConstants.OperatingMode getOperatingMode() {
    return operatingMode;
  }

  /**
   * Sets the operating mode for this connection.
   *
   * @param operatingMode the operating mode, may not be <code>null</code>.
   */
  public void setOperatingMode(IPSDeployConstants.OperatingMode operatingMode) {
    this.operatingMode = operatingMode;
  }
}
