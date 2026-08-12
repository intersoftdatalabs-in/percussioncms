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
// REFACTORED: CP-JAVA11
package com.percussion.share.test;

import com.percussion.share.dao.PSSerializerUtils;
import com.percussion.share.validation.PSErrorCause;
import com.percussion.share.validation.PSErrors;
import com.percussion.share.validation.PSValidationErrors;
import jakarta.xml.bind.JAXB;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;
import javax.xml.stream.XMLStreamException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.io.STAXEventReader;

/**
 * A REST client that handles un/marshalling of objects from responses and to requests. The
 * serialization is done through JAXB.
 *
 * @author adamgent
 */
public class PSObjectRestClient extends PSRestClient {

  public PSObjectRestClient() {
    super();
    // Direct field seed (protected requestHeaders) — avoids this-escape method calls.
    requestHeaders.put("Accept", "text/xml");
  }

  public PSObjectRestClient(String baseUrl) {
    super(baseUrl);
    // Headers map is shared with the client created in super(baseUrl).
    requestHeaders.put("Accept", "text/xml");
  }

  public void switchCommunity(Integer id) {
    var params = new HashMap<String, String>();
    params.put("sys_community", id.toString());
    POST("/Rhythmyx/sys_welcome/login.html", params.entrySet());
  }

  public void login(String user, String password) {
    var params = new HashMap<String, String>();
    params.put("j_username", user);
    params.put("j_password", password);
    POST(
        "/Rhythmyx/login?sys_redirect=http%3a%2f%2flocalhost%3a9992%2fRhythmyx%2ftest%2fsearch.jsp",
        params.entrySet());
  }

  public void delete(String path) {
    try {
      DELETE(path);
    } catch (RestClientException e) {
      handleException(e);
    }
  }

  protected <T, R> List<R> deleteObjectFromPathAndGetObjects(String path, Class<R> responseType) {
    try {
      return objectsFromResponseBody(deleteObjectFromPath(path), responseType);
    } catch (RestClientException e) {
      return this.<List<R>>handleException(e);
    }
  }

  protected <T> String deleteObjectFromPath(String path) {
    try {
      return DELETE(path);
    } catch (RestClientException e) {
      return this.<String>handleException(e);
    }
  }

  protected <T> T getObjectFromPath(String path, Class<T> type) {
    try {
      return objectFromResponseBody(GET(path), type);
    } catch (RestClientException e) {
      return this.<T>handleException(e);
    }
  }

  protected <T> List<T> getObjectsFromPath(String path, Class<T> type) {
    try {
      return objectsFromResponseBody(GET(path), type);
    } catch (RestClientException e) {
      return this.<List<T>>handleException(e);
    }
  }

  private <T> T handleException(RestClientException e) {
    if (e.getStatus() == 400) throw new DataValidationRestClientException(e);
    else if (e.getStatus() == 500) throw new DataRestClientException(e);
    else throw e;
  }

  protected <T> String postObjectToPath(String path, T object) {
    try {
      return POST(path, objectToRequestBody(object));
    } catch (RestClientException e) {
      return this.<String>handleException(e);
    }
  }

  protected <T> String putObjectToPath(String path, T object) {
    try {
      return PUT(path, objectToRequestBody(object));
    } catch (RestClientException e) {
      return this.<String>handleException(e);
    }
  }

  protected <T, R> R putObjectToPath(String path, T object, Class<R> responseType) {
    try {
      return objectFromResponseBody(putObjectToPath(path, object), responseType);
    } catch (RestClientException e) {
      return this.<R>handleException(e);
    }
  }

  protected <T, R> List<R> postObjectToPathAndGetObjects(
      String path, T object, Class<R> responseType) {
    try {
      return objectsFromResponseBody(postObjectToPath(path, object), responseType);
    } catch (RestClientException e) {
      return this.<List<R>>handleException(e);
    }
  }

  protected <T, R> R postObjectToPath(String path, T object, Class<R> responseType) {
    try {
      return objectFromResponseBody(postObjectToPath(path, object), responseType);
    } catch (RestClientException e) {
      return this.<R>handleException(e);
    }
  }

  protected <T> String objectToRequestBody(T data) {
    var sw = new StringWriter();
    JAXB.marshal(data, sw);
    return sw.getBuffer().toString();
  }

  protected <T> T objectFromResponseBody(String response, Class<T> type) {
    try {
      var context = JAXBContext.newInstance(type);
      var u = context.createUnmarshaller();
      return type.cast(u.unmarshal(new StringReader(response)));
    } catch (JAXBException e) {
      throw new DataRestClientMarshalException("Error unmarshaling", e);
    }
  }

  public <T> String objectToJson(T object) {
    try {
      return PSSerializerUtils.getJsonXmlFromObject(object);
    } catch (Exception e) {
      throw new DataRestClientMarshalException("Error converting to JSON", e);
    }
  }

  protected <T> List<T> objectsFromResponseBody(String response, Class<T> type) {
    // JAXB can't handle lists that well without help. CXF has the help built in.
    try {
      var reader = new STAXEventReader();
      var sr = new StringReader(response);
      var doc = reader.readDocument(sr);
      var es = doc.getRootElement().elements();
      var list = new ArrayList<T>();
      for (var e : es) {
        list.add(objectFromResponseBody(e.asXML(), type));
      }
      return list;
    } catch (XMLStreamException e) {
      throw new RuntimeException(e);
    }
  }

  public static class DataValidationRestClientException extends DataRestClientException {
    private static final long serialVersionUID = 1L;
    /** Parsed validation payload; not part of Java serialization of the exception. */
    private transient PSValidationErrors errors;

    public DataValidationRestClientException(RestClientException cause) {
      super(cause);
    }

    @Override
    protected final void setErrorResponse(String response) {
      setValidationErrors(fromValidationXml(response));
    }

    @Override
    public PSValidationErrors getErrors() {
      return errors;
    }

    public final void setValidationErrors(PSValidationErrors errors) {
      this.errors = errors;
    }

    protected final PSValidationErrors fromValidationXml(String xml) {
      try {
        return JAXB.unmarshal(new StringReader(xml), PSValidationErrors.class);
      } catch (Exception e) {
        log.error("Failed to get errors object from xml");
      }
      return new PSValidationErrors();
    }

    private static final Logger log = LogManager.getLogger(DataValidationRestClientException.class);
  }

  public static class DataRestClientException extends RestClientException {
    private static final long serialVersionUID = 1L;
    /** Parsed error payload; not part of Java serialization of the exception. */
    private transient PSErrors errors;
    private RestClientException restClientException;

    /**
     * Parses response body into structured errors. Polymorphic parse hook may run before subclass
     * fields finish init — justified for intentional Throwable construction (same pattern as main
     * PSErrorsExceptionDecorator).
     */
    @SuppressWarnings("this-escape")
    public DataRestClientException(RestClientException cause) {
      super(cause);
      restClientException = cause;
      if (cause.getResponseBody() != null) {
        setErrorResponse(cause.getResponseBody());
      }
      // else: super() already filled the stack; do not call fillInStackTrace() (this-escape).
    }

    /** Chains nested error causes; {@code initCause} is intentional Throwable API use. */
    @SuppressWarnings("this-escape")
    public DataRestClientException(DataRestClientException parent, PSErrorCause ec) {
      restClientException = parent.restClientException;
      initFromErrorCause(ec);
    }

    protected final void initFromErrorCause(PSErrorCause ec) {
      applyStackTrace(ec.getStackTrace());
      var child = ec.getErrorCause();
      initFromChildCause(child);
    }

    protected final void initFromChildCause(PSErrorCause child) {
      if (child != null) {
        initCause(new DataRestClientException(this, child));
      } else if (restClientException != null) {
        initCause(restClientException);
      }
    }

    protected final void applyStackTrace(PSErrorCause ec) {
      if (ec != null) {
        setStackTrace(ec.getStackTrace());
      }
    }

    protected final void applyStackTrace(StackTraceElement[] stack) {
      if (stack != null) {
        setStackTrace(stack);
      }
    }

    @Override
    public String getMessage() {
      if (hasException()) {
        return errors.getGlobalError().getCause().getMessage();
      }
      return super.getMessage();
    }

    private boolean hasException() {
      return (errors != null
          && errors.getGlobalError() != null
          && errors.getGlobalError().getCause() != null);
    }

    protected void setErrorResponse(String response) {
      setErrors(fromXml(response));
    }

    public PSErrors getErrors() {
      return errors;
    }

    protected final void setErrors(PSErrors errors) {
      this.errors = errors;
      if (hasException()) {
        applyStackTrace(errors.getGlobalError().getCause());
        initFromChildCause(errors.getGlobalError().getCause().getErrorCause());
      }
    }

    protected final PSErrors fromXml(String xml) {
      try {
        return JAXB.unmarshal(new StringReader(xml), PSErrors.class);
      } catch (Exception e) {
        log.error("Failed to get errors object from xml");
      }
      return new PSErrors();
    }

    private static final Logger log = LogManager.getLogger(DataRestClientException.class);
  }

  public static class DataRestClientMarshalException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DataRestClientMarshalException(String message) {
      super(message);
    }

    public DataRestClientMarshalException(String message, Throwable cause) {
      super(message, cause);
    }

    public DataRestClientMarshalException(Throwable cause) {
      super(cause);
    }
  }
}
