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
package com.percussion.webui.gadget.servlets;

import com.percussion.security.ToDoVulnerability;
import com.percussion.security.error.PSExceptionUtils;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * @author erikserating
 */
public class GadgetSettingsFormServlet extends HttpServlet {
  private static final Logger log = LogManager.getLogger(GadgetSettingsFormServlet.class.getName());

  /**
   * A convenience method which can be overridden so that there's no need to call <code>
   * super.init(config)</code>.
   *
   * <p>Instead of overriding {@link # init(ServletConfig)}, simply override this method and it will
   * be called by <code>GenericServlet.init(ServletConfig config)</code>. The <code>ServletConfig
   * </code> object can still be retrieved via {@link #getServletConfig}.
   *
   * @throws ServletException if an exception occurs that interrupts the servlet's normal operation
   */
  @Override
  public void init() throws ServletException {
    super.init();
  }

  /* (non-Javadoc)
   * @see jakarta.servlet.http.HttpServlet#doGet(
   *    jakarta.servlet.http.HttpServletRequest, jakarta.servlet.http.HttpServletResponse)
   */
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
    try {
      var gadgetUrl = req.getParameter("gurl");
      if (!PSGadgetUtils.isValidGadgetPathInUrl(req, new URI(gadgetUrl))) {
        resp.sendError(404);
        return;
      }
      var moduleId = req.getParameter("mid");
      resp.setContentType("application/javascript");
      var out = resp.getWriter();
      if (gadgetUrl != null) {
        var meta = getGadgetMeta(req, resp, gadgetUrl, moduleId);
        var prefs = extractUserPrefs(meta);
        var formContent =
            new PSUserPrefFormContent(prefs, moduleId, getUpParams(req), this, req, resp);
        out.println(formContent.toJavaScript());
      } else {
        out.println("// Gadget URL must be specified.");
      }
    } catch (IOException | URISyntaxException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      try {
        resp.sendError(404);
      } catch (IOException ioException) {
        resp.reset();
        resp.setStatus(404);
      }
    }
  }

  /** Validate input parameters for sane inputs. */
  private void validateInputParameters() {}

  /**
   * Calls the gadget metadata service to get information for the specified gadget url.
   *
   * @param req the servlet request, assumed not <code>null</code>.
   * @param url the gadget.xml url, assumed not <code>null</code> or empty.
   * @param moduleId The Module Id
   * @return A json object containing the Gadget metadata
   * @throws IOException
   */
  @SuppressWarnings("unchecked")
  private JSONObject getGadgetMeta(
      HttpServletRequest req, HttpServletResponse response, String url, String moduleId)
      throws IOException {
    var obj = new JSONObject();
    var gadget = new JSONObject();
    var context = new JSONObject();
    context.put("country", "US");
    context.put("language", "en");
    context.put("view", "default");
    context.put("container", "default");
    var gadgets = new JSONArray();
    var upParams = getUpParams(req);
    if (moduleId == null) moduleId = "0";
    gadgets.add(gadget);
    obj.put("context", context);
    obj.put("gadgets", gadgets);
    gadget.put("url", url);
    gadget.put("moduleId", moduleId);
    if (!upParams.isEmpty()) {
      var ups = new JSONObject();
      upParams.forEach(ups::put);
      gadget.put("prefs", ups);
    }
    String result = null;
    try {
      var dispatcher = this.getServletContext().getRequestDispatcher(METADATA_SERVICE_URL);
      var sw = new StringWriter();
      var pw = new PrintWriter(sw);
      var responseWrapper =
          new HttpServletResponseWrapper(response) {
            @Override
            public PrintWriter getWriter() throws IOException {
              return pw;
            }
          };
      var requestWrapper =
          new HttpServletRequestWrapper(req) {
            @Override
            public String getMethod() {
              return "POST";
            }

            final byte[] bytes = obj.toString().getBytes(StandardCharsets.UTF_8);

            @Override
            public ServletInputStream getInputStream() throws IOException {
              return new ServletInputStream() {
                private int lastIndexRetrieved = -1;
                private ReadListener readListener = null;

                @Override
                public boolean isFinished() {
                  return (lastIndexRetrieved == bytes.length - 1);
                }

                @Override
                public boolean isReady() {
                  return isFinished();
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                  this.readListener = readListener;
                  if (!isFinished()) {
                    try {
                      readListener.onDataAvailable();
                    } catch (IOException e) {
                      readListener.onError(e);
                    }
                  } else {
                    try {
                      readListener.onAllDataRead();
                    } catch (IOException e) {
                      readListener.onError(e);
                    }
                  }
                }

                @Override
                public int read() throws IOException {
                  int i;
                  if (!isFinished()) {
                    i = bytes[lastIndexRetrieved + 1];
                    lastIndexRetrieved++;
                    if (isFinished() && (readListener != null)) {
                      try {
                        readListener.onAllDataRead();
                      } catch (IOException ex) {
                        readListener.onError(ex);
                        throw ex;
                      }
                    }
                    return i;
                  } else {
                    return -1;
                  }
                }
              };
            }
          };
      dispatcher.include(requestWrapper, responseWrapper);
      result = sw.toString();
    } catch (ServletException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
    }
    if (result != null) {
      var parser = new JSONParser();
      try {
        var meta = (JSONObject) parser.parse(new StringReader(result));
        return meta;
      } catch (Exception e) {
        log.error(PSExceptionUtils.getMessageForLog(e));
        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        throw new IOException("Problem retrieving metadata.");
      }
    } else {
      throw new IOException("Unable to retrieve metadata.");
    }
  }

  /**
   * @param meta a de-serialized gadget metadata entry json object
   * @return A list of JSON objects representing userPrefs
   */
  private List<JSONObject> extractUserPrefs(JSONObject meta) {
    var results = new ArrayList<JSONObject>();
    var gArr = (JSONArray) meta.get("gadgets");
    if (gArr != null) {
      var prefs = (JSONObject) ((JSONObject) gArr.get(0)).get("userPrefs");
      if (!prefs.isEmpty()) {
        var keys = new ArrayList<String>();
        for (var k : prefs.keySet()) keys.add((String) k);
        Collections.sort(keys);
        for (var key : keys) {
          var vals = (JSONObject) prefs.get(key);
          vals.put("fieldname", key);
          results.add(vals);
        }
      }
    }
    return results;
  }

  /**
   * Helper method to retrieve all user preference value params from the request.
   *
   * @param req assumed not <code>null</code>.
   * @return map of user pref params.
   */
  @ToDoVulnerability // This needs to validate the parameters for injection
  private Map<String, String> getUpParams(HttpServletRequest req) {
    var params = new HashMap<String, String>();
    var en = req.getParameterNames();
    while (en.hasMoreElements()) {
      var name = en.nextElement();
      if (name.startsWith("up_")) params.put(name, req.getParameter(name));
    }
    return params;
  }

  /**
   * Retrieve the pssessionid value from the request header.
   *
   * @param request the request assumed not <code>null</code>.
   * @return the pssessionid value or <code>null</code> if not found.
   */
  private String getPSSessionId(HttpServletRequest request) {
    var cookies = request.getCookies();
    for (var cookie : cookies) {
      if (cookie.getName().equals(PSSESSIONID)) return cookie.getValue();
    }
    return null;
  }

  private static final String METADATA_SERVICE_URL = "/cm/gadgets/metadata";
  private static final String PSSESSIONID = "JSESSIONID";
}
