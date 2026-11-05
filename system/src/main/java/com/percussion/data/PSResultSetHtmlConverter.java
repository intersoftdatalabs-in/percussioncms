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
package com.percussion.data;

import com.intsof.percussioncms.auditlog.codes.DataErrorCodes;
import com.intsof.percussioncms.auditlog.codes.HttpErrorCodes;
import com.percussion.content.IPSMimeContentTypes;
import com.percussion.design.objectstore.PSDataSet;
import com.percussion.design.objectstore.PSResultPage;
import com.percussion.error.PSCatalogException;
import com.percussion.error.PSIllegalArgumentException;
import com.percussion.error.PSNotFoundException;
import com.percussion.extension.PSExtensionException;

import com.percussion.server.PSApplicationHandler;
import com.percussion.server.PSConsole;
import com.percussion.server.PSRequest;
import com.percussion.server.PSResponse;
import com.percussion.server.PSServer;
import com.percussion.util.PSBaseHttpUtils;
import com.percussion.util.PSCollection;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Properties;
import org.w3c.dom.Document;

/**
 * The PSResultSetHtmlConverter class extends the PSResultSetXmlConverter class, providing
 * conversion to an HTML page. It extends the XML converter as the process of HTML conversion first
 * converts to an XML document and then runs the document through its style sheet to generate HTML.
 *
 * @author Tas Giakouminakis
 * @version 1.0
 * @since 1.0
 */
public class PSResultSetHtmlConverter extends PSResultSetXmlConverter {
  /**
   * Construct a ResultSet to HTML converter.
   *
   * @param app the application containing the data set
   * @param def the data set definition
   * @exception PSIllegalArgumentException if request link generation is desired but the target data
   *     set cannot be found
   */
  public PSResultSetHtmlConverter(PSApplicationHandler app, PSDataSet def)
      throws PSNotFoundException,
          PSIllegalArgumentException,
          PSCatalogException,
          PSExtensionException {
    super(app, def);
  }

  /* ********** IPSResultSetConverter Interface Implementation ********** */

  /**
   * See {@link IPSResultSetConverter#convert(PSExecutionData, IPSResultSetDataFilter) base class}
   * for full details. More specifically, this class performs the following steps during conversion:
   *
   * <ol>
   *   <li>verify reqUrl is supported
   *   <li>run the conditionals to determine which style sheet is in use. If no conditions are met,
   *       use our default style sheet.
   *   <li>let our super-class create an XML document for us as this is a required first step in
   *       building HTML.
   *   <li>initialize the output with the appropriate header info (content type, etc.)
   *   <li>if the style sheet is XSL, run it through the XSL processor for conversion to HTML
   *       (assuming the XSL is building HTML)
   *   <li>if the style sheet is of any other type (eg, CSS), set it as the style sheet for the XML
   *       data and return it (we do no processing on CSS or other such types)
   * </ol>
   */
  public void convert(PSExecutionData data, IPSResultSetDataFilter filter)
      throws PSConversionException, PSUnsupportedConversionException {
    /* if there's more than one result set on the stack, we're
     * in trouble!!! we must have missed a join.
     */
    java.util.Stack<?> stack = data.getResultSetStack();
    if (stack.size() > 1)
      throw new PSConversionException(
          DataErrorCodes.CANNOT_CONVERT_MULTIPLE_RESULT_SETS, Integer.valueOf(stack.size()));
    else if (stack.size() == 0)
      throw new PSConversionException(DataErrorCodes.NO_DATA_FOR_CONVERSION);

    PSRequest request = data.getRequest();

    String extension = request.getRequestPageExtension();
    if (extension == null) {
      extension = "";
    }
    extension = extension.toLowerCase();
    if (!extension.isEmpty() && extension.charAt(0) != '.') {
      extension = "." + extension;
    }

    /* JSON: build the same result document as XML, then encode (no XSL). */
    if (request.getRequestPageType() == PSRequest.PAGE_TYPE_JSON
        || ".json".equals(extension)) {
      convertToJson(data, filter, request);
      return;
    }

    boolean callSuper = false;

    if (m_requestor.getMimeType(extension) == null) {
      if (extension.equals(".xml") || extension.equals(".txt")) {
        /* Need to also check the request pages!!! */
        PSCollection pages = m_resultPageSet.getResultPages();
        if (pages != null) {
          boolean found = false;
          for (int i = 0; i < pages.size(); i++) {
            /* Check to see if the page explicitly uses the extension */
            PSResultPage page = (PSResultPage) pages.get(i);
            Collection<?> c = page.getExtensions();
            if ((c != null) && (!c.isEmpty())) {
              if (c.contains(extension)) found = true;
            }
          }

          /* If we didn't find the extension in any of the pages
          then we must call the super */
          if (!found) callSuper = true;
        } else {
          callSuper = true;
        }
      }
    }

    if (callSuper) {
      if (super.isSupported(request.getRequestFileURL())) {
        super.convert(data, filter); /* let the base converter do this */
        return;
      } else {
        /* There is nobody to handle this extension! */
        String pageExt = request.getRequestPageExtension();
        if (pageExt == null) pageExt = "";
        throw new PSUnsupportedConversionException(
            DataErrorCodes.HTML_CONV_EXT_NOT_SUPPORTED, pageExt);
      }
    }

    /* let our super-class create an XML document for us as this is
     * a required first step in building HTML.
     */
    Document doc = createXmlDocument(data, filter, false);

    String contentHeader = request.getContentHeaderOverride();
    String mimeType = null;
    int pageIndex = getResultPageIndex(data);

    if (contentHeader == null) {
      mimeType = getMimeTypeForRequestPage(pageIndex, data);
      if (mimeType == null) {
        mimeType = PSResultSetXmlConverter.getMimeTypeForRequestor(m_requestor, extension, data);
      }

      /* verify reqUrl is supported */
      if (mimeType == null) {
        String pageExt = request.getRequestPageExtension();
        if (pageExt == null) pageExt = "";
        throw new PSUnsupportedConversionException(
            DataErrorCodes.HTML_CONV_EXT_NOT_SUPPORTED, pageExt);
      }
    }

    /* build the response object */
    PSResponse resp = request.getResponse();
    if (resp == null) {
      /* this should never happen! */
      throw new PSConversionException(DataErrorCodes.NO_RESPONSE_OBJECT);
    }

    ByteArrayOutputStream bout = null;
    ByteArrayInputStream in = null;
    try {
      if (doc == null) {
        resp.setStatus(HttpErrorCodes.HTTP_NOT_FOUND.numericCode());
      } else {
        bout = new ByteArrayOutputStream();
        String encoding = getEncodingForRequestPage(getResultPageIndex(data));
        if (encoding == null) encoding = m_requestor.getCharacterEncoding();
        Properties serverProps = PSServer.getServerProps();
        boolean bAllowsEncodingMods =
            Boolean.valueOf(serverProps.getProperty(PSServer.PROP_ALLOW_XSL_ENCODING_MODS, "false"))
                .booleanValue();
        PSStyleSheetMerger.merge(request, doc, bout, bAllowsEncodingMods ? encoding : null);

        if (contentHeader == null) {
          contentHeader = PSBaseHttpUtils.constructContentTypeHeader(mimeType, encoding);
        }

        int contentLength = 0;
        // Cleanup Namespaces if allowed. This will
        // remove any non-xhtml compliant namespace declarations
        if (isNamespaceCleanupAllowedForResultPage(pageIndex)) {
          String mergedResults = bout.toString(encoding);
          mergedResults =
              PSStylesheetCleanupUtils.namespaceCleanup(
                  mergedResults, PSStylesheetCleanupFilter.getInstance());
          byte[] mergedAsBytes = mergedResults.getBytes(encoding);
          contentLength = mergedAsBytes.length;
          in = new ByteArrayInputStream(mergedAsBytes);
        } else {
          contentLength = bout.size();
          in = new ByteArrayInputStream(bout.toByteArray());
        }

        resp.setContent(in, contentLength, contentHeader, false);
        in = null;
      }
    } catch (UnsupportedEncodingException e) {
      PSConsole.printMsg(this.getClass().getName(), e);
      throw new RuntimeException(e);
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (java.io.IOException e) {
          /* should never happen on a byte stream */
        }
      }

      if (bout != null) {
        try {
          bout.close();
        } catch (java.io.IOException e) {
          /* should never happen on a byte stream */
        }
      }
    }
  }

  /**
   * Convert the result set to JSON: build the XML result document (including post exits), encode
   * with {@link PSXmlDocumentJsonCodec}, and write {@code application/json} to the response.
   */
  private void convertToJson(
      PSExecutionData data, IPSResultSetDataFilter filter, PSRequest request)
      throws PSConversionException, PSUnsupportedConversionException {
    Document doc = createXmlDocument(data, filter, true);

    PSResponse resp = request.getResponse();
    if (resp == null) {
      throw new PSConversionException(DataErrorCodes.NO_RESPONSE_OBJECT);
    }

    if (doc == null) {
      resp.setStatus(HttpErrorCodes.HTTP_NOT_FOUND.numericCode());
      return;
    }

    // RFC 8259: JSON text is Unicode; always encode JSON responses as UTF-8
    // (match PSQueryCommandHandler / PSUpdateHandler).
    String encoding = StandardCharsets.UTF_8.name();

    String json = PSXmlDocumentJsonCodec.toJson(doc);
    byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

    String contentHeader = request.getContentHeaderOverride();
    if (contentHeader == null) {
      contentHeader =
          PSBaseHttpUtils.constructContentTypeHeader(IPSMimeContentTypes.MIME_TYPE_JSON, encoding);
    }

    ByteArrayInputStream in = new ByteArrayInputStream(bytes);
    resp.setContent(in, bytes.length, contentHeader, false);
  }

  /**
   * What is the default MIME type for this converter?
   *
   * @return the default MIME type
   */
  public String getDefaultMimeType() {
    return null; // now it is just blah IPSMimeContentTypes.MIME_TYPE_TEXT_HTML;
  }

  /**
   * Generate the results for this request.
   *
   * @param data the execution data associated with this request. This includes all context data,
   *     result sets, etc.
   * @exception PSConversionException if the conversion fails
   * @exception PSUnsupportedConversionException if conversion to the format required by the
   *     specified request URL is not supported
   */
  public void generateResults(PSExecutionData data)
      throws PSConversionException, PSUnsupportedConversionException {
    /* simply call convert */
    convert(data, null);
  }

  /* *********************   Protected Implementation ******************** */

  /**
   * Is the request URL supported by this converter? The request URL may contain an extension. When
   * it does, this is used in defining the output which will be returned.
   *
   * @param reqPageURL the URL which was specified when making this request
   * @return <code>true</code> if conversion is supported, <code>false</code> otherwise
   */
  protected boolean isSupported(String reqPageURL) {
    /* check the URL to see if it matches the HTML conversion rules */
    if (reqPageURL == null) return false;

    reqPageURL = reqPageURL.toLowerCase();
    if (reqPageURL.endsWith(".json")) {
      return true;
    }

    int slashIndex = reqPageURL.lastIndexOf('/');
    if (slashIndex > -1) {
      String resourcePortion = reqPageURL.substring(slashIndex + 1);
      int dotIndex = resourcePortion.indexOf('.');
      if (dotIndex > -1) {
        String extension = reqPageURL.substring(dotIndex + 1);
        return m_requestor.isExtensionSupported(extension);
      }
    }

    /* if not, check if our super-class supports it */
    return super.isSupported(reqPageURL);
  }
}
