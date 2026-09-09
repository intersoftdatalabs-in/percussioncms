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

package com.percussion.services.pipeline.xsl;

import com.percussion.security.xml.PSSecureXMLUtils;
import com.percussion.services.pipeline.PSPipelineIrException;
import com.percussion.services.pipeline.model.PipelineExecuteRequest;
import com.percussion.services.pipeline.model.PipelineResultPageIr;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.lang3.StringUtils;

/**
 * Apply a path-safe local/bundled XSL stylesheet to pipeline query rows for HTML Test invoke. No
 * live internet fetch; missing stylesheets fail closed (no invented HTML).
 */
public final class PSPipelineXslMerge {

  public static final String BUNDLED_MARKER = "PIPE-XSL-HTML";

  static final int MAX_STYLESHEET_CHARS = 200_000;

  private static final Pattern XML_NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_.-]*");

  private PSPipelineXslMerge() {}

  /** True when the execute request asks for HTML ({@code requestExtension} or {@code accept}). */
  public static boolean wantsHtml(PipelineExecuteRequest request) {
    if (request == null) {
      return false;
    }
    String ext = request.getRequestExtension();
    if (StringUtils.isNotBlank(ext)) {
      String token = ext.trim().toLowerCase(Locale.ROOT);
      if (token.startsWith(".")) {
        token = token.substring(1);
      }
      if ("html".equals(token) || "htm".equals(token)) {
        return true;
      }
    }
    String accept = request.getAccept();
    return StringUtils.isNotBlank(accept)
        && accept.toLowerCase(Locale.ROOT).contains("text/html");
  }

  /**
   * Transform query rows with the result-page stylesheet.
   *
   * @param sandboxRoot optional application-owned files root; {@code null} means bundled only
   */
  public static String merge(
      String appName,
      PipelineResultPageIr page,
      List<Map<String, Object>> rows,
      Path sandboxRoot)
      throws PSPipelineIrException {
    Objects.requireNonNull(page, "page");
    String uri = PSPipelineResultPagePath.requireSafeStylesheetUri(page.getStylesheetUri());
    PSPipelineResultPagePath.requireSafeRequestExtension(page.getRequestExtension());
    PSPipelineResultPagePath.requireSafeMimeType(page.getMimeType());
    String xsl = loadStylesheet(appName, uri, sandboxRoot);
    String xml = rowsToXml(rows);
    try {
      TransformerFactory tf = PSSecureXMLUtils.getSecuredTransformerFactory();
      Transformer transformer = tf.newTransformer(new StreamSource(new StringReader(xsl)));
      StringWriter out = new StringWriter();
      transformer.transform(new StreamSource(new StringReader(xml)), new StreamResult(out));
      String html = out.toString();
      if (StringUtils.isBlank(html)) {
        throw new PSPipelineIrException("Result page stylesheet produced empty HTML");
      }
      return html;
    } catch (TransformerException e) {
      throw new PSPipelineIrException("Result page XSL merge failed", e);
    }
  }

  static String loadStylesheet(String appName, String uri, Path sandboxRoot)
      throws PSPipelineIrException {
    if (PSPipelineResultPagePath.isBundledFixture(uri)) {
      return readBundledFixture();
    }
    return readSandboxFile(appName, uri, sandboxRoot);
  }

  static String readBundledFixture() throws PSPipelineIrException {
    try (InputStream in =
        PSPipelineXslMerge.class.getResourceAsStream(PSPipelineResultPagePath.BUNDLED_RESOURCE)) {
      if (in == null) {
        throw new PSPipelineIrException("Result page stylesheet not found");
      }
      String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      if (text.isBlank()) {
        throw new PSPipelineIrException("Result page stylesheet not found");
      }
      if (text.length() > MAX_STYLESHEET_CHARS) {
        throw new PSPipelineIrException("Result page stylesheet exceeds size limit");
      }
      return text;
    } catch (IOException e) {
      throw new PSPipelineIrException("Failed to read bundled result page stylesheet", e);
    }
  }

  static String readSandboxFile(String appName, String relativePath, Path sandboxRoot)
      throws PSPipelineIrException {
    if (sandboxRoot == null) {
      throw new PSPipelineIrException("Result page stylesheet not found");
    }
    if (StringUtils.isBlank(appName)
        || appName.contains("..")
        || appName.indexOf('/') >= 0
        || appName.indexOf('\\') >= 0
        || appName.indexOf('\0') >= 0) {
      throw new PSPipelineIrException("Result page stylesheet not found");
    }
    Path root = sandboxRoot.toAbsolutePath().normalize();
    Path appDir = root.resolve(appName.trim()).normalize();
    if (!appDir.startsWith(root)) {
      throw new PSPipelineIrException("Result page stylesheet URI must not contain path traversal");
    }
    Path target = appDir.resolve(relativePath).normalize();
    if (!target.startsWith(appDir)) {
      throw new PSPipelineIrException("Result page stylesheet URI must not contain path traversal");
    }
    if (!Files.isRegularFile(target)) {
      throw new PSPipelineIrException("Result page stylesheet not found");
    }
    try {
      String text = Files.readString(target, StandardCharsets.UTF_8);
      if (text.isBlank()) {
        throw new PSPipelineIrException("Result page stylesheet not found");
      }
      if (text.length() > MAX_STYLESHEET_CHARS) {
        throw new PSPipelineIrException("Result page stylesheet exceeds size limit");
      }
      return text;
    } catch (IOException e) {
      throw new PSPipelineIrException("Failed to read application-owned result page stylesheet", e);
    }
  }

  static String rowsToXml(List<Map<String, Object>> rows) {
    StringBuilder sb = new StringBuilder();
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    sb.append("<rows>");
    if (rows != null) {
      for (Map<String, Object> row : rows) {
        sb.append("<row>");
        if (row != null) {
          for (Map.Entry<String, Object> entry : row.entrySet()) {
            String key = entry.getKey();
            if (!isSafeFieldName(key)) {
              continue;
            }
            sb.append('<').append(key).append('>');
            if (entry.getValue() != null) {
              sb.append(escapeXml(String.valueOf(entry.getValue())));
            }
            sb.append("</").append(key).append('>');
          }
        }
        sb.append("</row>");
      }
    }
    sb.append("</rows>");
    return sb.toString();
  }

  static boolean isSafeFieldName(String key) {
    return StringUtils.isNotBlank(key) && XML_NAME.matcher(key).matches();
  }

  static String escapeXml(String raw) {
    StringBuilder out = new StringBuilder(raw.length());
    for (int i = 0; i < raw.length(); i++) {
      char ch = raw.charAt(i);
      switch (ch) {
        case '&':
          out.append("&amp;");
          break;
        case '<':
          out.append("&lt;");
          break;
        case '>':
          out.append("&gt;");
          break;
        case '"':
          out.append("&quot;");
          break;
        case '\'':
          out.append("&apos;");
          break;
        default:
          if (ch < 0x20 && ch != '\t' && ch != '\n' && ch != '\r') {
            break;
          }
          out.append(ch);
      }
    }
    return out.toString();
  }
}
