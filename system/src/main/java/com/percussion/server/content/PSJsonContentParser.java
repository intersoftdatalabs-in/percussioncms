/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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

package com.percussion.server.content;

import com.intsof.percussioncms.auditlog.codes.ServerErrorCodes;

import com.percussion.content.IPSMimeContentTypes;
import com.percussion.data.PSConversionException;
import com.percussion.data.PSXmlDocumentJsonCodec;
import com.percussion.data.PSXmlFieldExtractor;
import com.percussion.server.PSRequest;
import com.percussion.server.PSRequestParsingException;
import com.percussion.util.PSBaseHttpUtils;
import com.percussion.util.PSCharSets;
import com.percussion.util.PSCharSetsConstants;
import com.percussion.util.PSInputStreamReader;
import com.percussion.util.PSPurgableTempFile;
import com.percussion.xml.PSXmlTreeWalker;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import org.w3c.dom.Document;

/**
 * Parses {@code application/json} request bodies into an input XML {@link Document} using {@link
 * PSXmlDocumentJsonCodec}, so classic update pipes and other input-document consumers can accept
 * JSON without changing mapper/exit code.
 *
 * <p>Large-body intake matches {@link PSXmlContentParser}: the request stream is copied into a
 * purgable temp file in fixed-size chunks (no single {@code byte[Content-Length]} allocation), then
 * decoded via a charset-aware {@link Reader}. There is no additional hard max-size beyond the
 * Content-Length {@code int} used by the request parser (same as the classic XML path). Nesting is
 * limited by {@link PSXmlDocumentJsonCodec#MAX_DEPTH}.
 */
public class PSJsonContentParser extends PSContentParser {

  private static final String[] ARRAY_SUPPORTED_TYPES = {IPSMimeContentTypes.MIME_TYPE_JSON};

  @Override
  public void parse(
      PSRequest request,
      String contentType,
      String charset,
      PSInputStreamReader content,
      int length)
      throws IOException, PSRequestParsingException {
    String mimeType = getMimeType(contentType);

    if (!isSupportedContentType(mimeType)) {
      Object[] args = {mimeType, Arrays.toString(ARRAY_SUPPORTED_TYPES)};
      throw new PSRequestParsingException(ServerErrorCodes.PARSER_UNSUPPORTED_CONTENT_TYPE, args);
    }

    if (length == 0) {
      return;
    }

    if (charset == null || charset.isBlank()) {
      charset = PSCharSetsConstants.rxStdEnc();
    }

    if (length < 0) {
      throw new PSRequestParsingException("Invalid Content-Length for JSON body: " + length);
    }

    Charset javaCharset;
    try {
      javaCharset = Charset.forName(PSCharSets.getJavaName(charset));
    } catch (IllegalArgumentException e) {
      throw new PSRequestParsingException("Unsupported character set for JSON body: " + charset);
    }

    /*
     * Stream body to a purgable temp file (same pattern as PSXmlContentParser) so hostile or
     * large Content-Length values do not force a single heap buffer for the entire body.
     */
    PSPurgableTempFile tempFile =
        readContentIntoPurgableTempFile("psj", ".json", null, content, length);
    try {
      long fileLength = tempFile.length();
      if (length != fileLength) {
        Object[] args = {
          request.getUserSessionId(),
          mimeType,
          String.valueOf(length),
          String.valueOf(fileLength)
        };
        com.percussion.log.PSLogManager.write(
            new com.percussion.log.PSLogServerWarning(
                ServerErrorCodes.CONTENT_LENGTH_DOES_NOT_MATCH_DATA_READ.numericCode(),
                args,
                true,
                "PSJsonContentParser"));
      }

      Document doc;
      try (Reader reader =
          new InputStreamReader(Files.newInputStream(tempFile.toPath()), javaCharset)) {
        doc = PSXmlDocumentJsonCodec.fromJson(reader);
      } catch (PSConversionException e) {
        Object[] args = {e.getLocalizedMessage()};
        throw new PSRequestParsingException(ServerErrorCodes.JSON_PARSER_ERROR, args);
      }

      /* Reject embedded file URLs (same security rule as XML content parser). */
      PSXmlTreeWalker walker = new PSXmlTreeWalker(doc);
      String str =
          walker.getElementData("@" + PSXmlFieldExtractor.XML_URL_REFERENCE_ATTRIBUTE, true);
      if (str != null) {
        doc = null;
      }

      request.setInputDocument(doc);
    } finally {
      tempFile.release();
    }
  }

  @Override
  public String[] getSupportedContentTypes() {
    return ARRAY_SUPPORTED_TYPES;
  }

  private String getMimeType(String contentType) throws PSRequestParsingException {
    try {
      return PSBaseHttpUtils.parseContentType(contentType, null);
    } catch (IllegalArgumentException e) {
      throw new PSRequestParsingException(e.getLocalizedMessage());
    }
  }
}
