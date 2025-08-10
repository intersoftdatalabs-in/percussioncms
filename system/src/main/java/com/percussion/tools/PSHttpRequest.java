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

package com.percussion.tools;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;

/**
 * Encapsulates an HTTP GET or POST request and the results of the request. We use this class
 * because we want more flexibility over the behavior of the HTTP transaction than classes like
 * java.net.HttpURLConnection gives us. For example, we may want to send garbage or unencoded URLs.
 */
public class PSHttpRequest implements IPSHTTPConstants {

  public PSHttpRequest(URL url) {
    this(url.toString(), "GET", null);
  }

  /**
   * Constructs a request to the given URL with the given method. If request content is non-null, it
   * will be sent after the request headers.
   *
   * @param url the URL as a string
   * @param reqMethod the HTTP method
   * @param reqContent request content, may be null
   */
  public PSHttpRequest(String url, String reqMethod, InputStream reqContent) {
    init(url, reqMethod, reqContent);
  }

  private void init(String url, String reqMethod, InputStream reqContent) {
    reqUrl = url;
    reqMethod = reqMethod;
    reqContent = reqContent;
  }

  /**
   * Sets the outgoing request content for this request. If an existing content had been specified
   * (and it is not the same content stream as the argument to this method), the existing content
   * will be closed first.
   *
   * @author chadloder
   * @version 1.3 1999/11/03
   * @param content
   */
  public void setRequestContent(InputStream content) {
    if (reqContent != null && content != reqContent) {
      try {
        reqContent.close();
      } catch (IOException e) {
        /* ignore */
      }
    }
    reqContent = content;
  }

  /**
   * Sets the request's hostname. Usually this value will be extracted from the URL, but if the host
   * is not available in the URL, then you will to set the host using this method.
   *
   * <p>If this value is set, then it will override any setting in the URL.
   *
   * @author chad loder
   * @version 1.0 1999/8/20
   * @param hostName
   */
  public void setRequestHost(String hostName) {
    reqHost = hostName;
  }

  /**
   * Sets the request's port. Usually this value will be extracted from the URL, but if the port is
   * not available in the URL, then you will need to set the port using this method.
   *
   * <p>If this value is set, then it will override any setting in the URL.
   *
   * @author chad loder
   * @version 1.0 1999/8/20
   * @param port
   */
  public void setRequestPort(int port) {
    reqPort = port;
  }

  /**
   * Gets the request method, usually "GET" or "POST".
   *
   * @author chad loder
   * @version 1.0 1999/8/20
   * @return String
   */
  public String getRequestMethod() {
    return reqMethod;
  }

  /**
   * Enables tracing status to the given PrintWriter.
   *
   * @author chad loder
   * @version 1.0 1999/8/20
   * @param logger
   */
  public void enableTrace(LogSink logger) {
    logger = logger;
  }

  /**
   * Sets the HTTP version to declare when making a request. The default is 1.0
   *
   * @author chad loder
   * @version 1.0 1999/8/20
   * @param major
   * @param minor
   */
  public void setRequestHttpVersion(int major, int minor) {
    reqHttpVersion = "" + major + "." + minor;
  }

  /**
   * Sets the request URL.
   *
   * @author chad loder
   * @version 1.0 1999/8/20
   * @param URL
   */
  public void setRequestURL(String URL) {
    reqUrl = URL;
  }

  public void addRequestHeaders(PSHttpHeaders headers) {
    reqHeaders.addAll(headers);
  }

  /**
   * Adds a header that will be sent along with the request.
   *
   * @author chad loder
   * @version 1.0 1999/8/20
   * @param headerName
   * @param headerValue
   */
  public void addRequestHeader(String headerName, String headerValue) {
    reqHeaders.replaceHeader(headerName, headerValue);
  }

  /**
   * Adds a response header that was present in the response.
   *
   * @author chad loder
   * @version 1.0 1999/8/20
   * @param headerName
   * @param headerValue
   */
  protected void addResponseHeader(String headerName, String headerValue) {
    respHeaders.addHeader(headerName, headerValue);
  }

  /**
   * Returns the response headers.
   *
   * @author chad loder
   * @version 1.0 1999/8/20
   * @return Iterator
   */
  public PSHttpHeaders getResponseHeaders() {
    return respHeaders;
  }

  /**
   * Sends the request and parses the response. If request content was supplied in the constructor,
   * it will be sent.
   *
   * <p>The request content stream (if specified in the constructor) is guaranteed to be closed
   * after this method is called, even if exceptions are thrown from this method.
   *
   * @author chad loder
   * @version 1.0 1999/8/20
   * @throws IOException
   */
  public void sendRequest() throws IOException {
    PSHttpRequestTimings timings = new PSHttpRequestTimings();
    sendRequest(timings);
    timingsStats = timings;
  }

  /**
   * Sends the request and parses the response. If request content was supplied in the constructor,
   * it will be sent.
   *
   * <p>The request content stream (if specified in the constructor) is guaranteed to be closed
   * after this method is called, even if exceptions are thrown from this method.
   *
   * @param timer Can be <CODE>null</CODE>.
   * @author chad loder
   * @version 1.0 1999/8/20
   * @throws IOException
   */
  private void sendRequest(PSHttpRequestTimings timings) throws IOException {
    try {
      // TODO: don't do this if we support keep-alive connections
      if (sock != null) {
        disconnect();
      }

      String host = reqHost;
      if (host == null) {
        URL u = new URL(reqUrl);
        host = u.getHost();
      }

      int port = reqPort;
      if (port <= 0) {
        URL u = new URL(reqUrl);
        port = u.getPort();
      }

      if (port <= 0) {
        port = 80;
      }

      timings.beforeConnect(System.currentTimeMillis());

      // connect the socket
      connect(host, port);

      timings.afterConnect(System.currentTimeMillis());

      OutputStream out = sock.getOutputStream();

      reqWriter = new BufferedWriter(new OutputStreamWriter(out));

      // send the request line
      sendRequestLine(reqWriter);

      // send the request headers
      sendRequestHeaders(reqWriter);

      reqWriter.flush();

      // if applicable, send the additional content
      if (reqContent != null) {
        sendReqContent(reqContent, out);
        reqContent.close();
        reqContent = null;
      }

      timings.afterRequest(System.currentTimeMillis());

      // prepare to read the response
      respIn = new PSInputStreamReader(sock.getInputStream());

      // read the response header
      long hdrBytes = parseResponse(respIn);

      timings.afterHeaders(System.currentTimeMillis());
      timings.headerBytes(hdrBytes);
    } finally {
      if (reqContent != null) {
        reqContent.close();
        reqContent = null;
      }
    }
  }

  protected void connect(String host, int port) throws IOException {
    log("Connecting...");
    sock = new Socket(host, port);
  }

  protected void sendReqContent(InputStream in, OutputStream out) throws IOException {
    long bytesSent = PSCopyStream.copyStream(in, out);
    log("Sent " + bytesSent + " bytes of content");
  }

  /**
   * Gets the approximate number of milliseconds we had to wait for the first byte of the response
   * to become available from the server.
   *
   * @author chad loder
   * @version 1.0 1999/8/20
   * @return long
   */
  public long getResponseLatency() {
    return respLatencyMs;
  }

  public PSHttpRequestTimings getTimings() throws CloneNotSupportedException {
    return (PSHttpRequestTimings) timingsStats.clone();
  }

  /**
   * Gets the response content stream, which may be null or empty if getResponseCode() returns
   * anything other than 2xx. If we are currently waiting for data to become available over the
   * connection, this method will block until either we have timed out or until data becomes
   * available.
   *
   * @author chad loder
   * @version 1.0 1999/8/20
   * @return InputStream
   */
  public InputStream getResponseContent() {
    return respIn;
  }

  /**
   * Gets the HTTP response code.
   *
   * @author chad loder
   * @version 1.0 1999/8/20
   * @return int
   */
  public int getResponseCode() {
    return respHttpCode;
  }

  /**
   * Closes the request. Any pending results are discarded, and the response content is no longer
   * valid.
   *
   * @author chad loder
   * @version 1.0 1999/8/20
   * @throws Exception;
   */
  public void disconnect() throws IOException {
    if (respIn != null || sock != null) {
      log("Disconnecting...");
    }

    if (respIn != null) {
      respIn.close();
    }

    if (sock != null) {
      sock.close();
    }

    respIn = null;
    sock = null;
  }

  protected void finalize() throws Throwable {
    super.finalize();
    disconnect();
  }

  /**
   * Gets the response message, that is any text following the status code on the response header
   * line.
   *
   * @author chad loder
   * @version 1.0 1999/8/20
   * @return String
   */
  public String getResponseMessage() {
    return respMsg;
  }

  /** send the HTTP request line to the given writer */
  protected void sendRequestLine(Writer writer) throws IOException {
    // this logic allows us to send invalid URLs
    String sendUrl = reqUrl;
    try {
      URL u = new URL(reqUrl);
      sendUrl = u.getFile();
    } catch (MalformedURLException e) {
      // ignore, just send the invalid URL as is
    }

    String reqLine = reqMethod + " " + sendUrl + " HTTP/" + reqHttpVersion;
    writer.write(reqLine + "\r\n");

    log("Sent request line " + reqLine);
  }

  /** sends the request headers to the given writer, followed by a blank line */
  protected void sendRequestHeaders(Writer writer) throws IOException {
    log("Sending request headers...");
    Collection<String> keySet = reqHeaders.getHeaderNames();
    for (Iterator<String> i = keySet.iterator(); i.hasNext(); ) {
      String headerName = i.next();
      for (Iterator<String> j = reqHeaders.getHeaders(headerName); j.hasNext(); ) {
        String val = headerName + ": " + j.next();
        writer.write(val + "\r\n");
        log("  Sent header " + val);
      }
    }

    writer.write("\r\n"); // blank line to terminate the headers

    log("Finished sending headers");
  }

  protected long parseResponse(PSInputStreamReader reader) throws IOException {
    long bytes = parseResponseStatus(reader);
    bytes += parseResponseHeaders(reader);
    return bytes;
  }

  /** Read the HTTP status code, which looks like "HTTP/1.1 nnn:blah" where nnn is the code */
  protected long parseResponseStatus(PSInputStreamReader reader) throws IOException {
    String statusLine = reader.readLine();

    log("Server status: " + statusLine);

    {
      if (statusLine == null || statusLine.length() < 8) {
        throw new IOException("Malformed HTTP status line \"" + statusLine + "\"");
      }

      int spacePos = statusLine.indexOf(' ');
      if (spacePos < 5) {
        throw new IOException("Malformed HTTP status line \"" + statusLine + "\"");
      }

      int startCode = spacePos + 1; // points at first char of code
      int endCode = startCode + 3; // points one past the code
      respHttpCode = Integer.parseInt(statusLine.substring(startCode, endCode));
      respMsg = statusLine.substring(endCode).trim();
    }

    // status line is ASCII bytes (don't forget 2 bytes for CR+LF)
    return statusLine.length() + 2;
  }

  /**
   * Parses the headers, and positions the reader on first byte of actual data.
   *
   * @author chad loder
   * @version 1.0 1999/8/20
   */
  protected long parseResponseHeaders(PSInputStreamReader reader) throws IOException {
    long bytes = 0L;

    log("Parsing response headers...");

    // now read each header
    for (String line = reader.readLine(); line != null; line = reader.readLine()) {
      // line is ASCII bytes (don't forget 2 bytes for CR+LF)
      bytes += line.length() + 2;

      if (line.length() == 0) {
        // this is the last (empty) line in the headers
        break;
      }

      int pos = line.indexOf(':');
      if (pos < 1 || pos == line.length()) {
        throw new IOException("Malformed result header line \"" + line + "\"");
      }

      log("\tParsed header " + line);

      String name = line.substring(0, pos).trim();
      String val = line.substring(pos + 1, line.length()).trim();

      addResponseHeader(name, val);
    }

    log("Finished parsing response headers");

    return bytes;
  }

  public void logException(Throwable t) {
    if (logger != null) {
      logger.log(t);
    }
  }

  public void log(String message) {
    if (logger != null) {
      logger.log(message);
    }
  }

  protected Socket sock;
  protected LogSink logger;

  /* response */
  protected long respLatencyMs;
  protected String respMsg;
  protected PSInputStreamReader respIn;
  protected int respHttpCode = -1;
  protected PSHttpHeaders respHeaders = new PSHttpHeaders();

  /* request */
  protected String reqHost;
  protected int reqPort = -1;
  protected String reqMethod;
  protected InputStream reqContent;
  protected Writer reqWriter;
  protected PSHttpHeaders reqHeaders = new PSHttpHeaders();
  protected String reqHttpVersion = "1.0";
  protected String reqUrl;

  /* statistics */
  protected PSHttpRequestTimings timingsStats;
}
