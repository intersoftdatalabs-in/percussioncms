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
package com.percussion.services.assembly.impl.plugin;

import static com.percussion.cms.IPSConstants.DEFAULT_MIMETYPE;
import static com.percussion.cms.IPSConstants.SYS_PARAM_CHARSET;
import static com.percussion.cms.IPSConstants.SYS_PARAM_MIMETYPE;
import static com.percussion.cms.IPSConstants.SYS_PARAM_TEMPLATE;

import com.percussion.security.error.PSExceptionUtils;
import com.percussion.utils.jexl.IPSScript;
import com.percussion.utils.jexl.PSJexlEvaluator;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/**
 * Pure render / assemble helpers for HTML-first and Markdown assemblers (no Spring / assembly
 * service). Unit-tested without {@link PSAssemblerBase} static initialization.
 *
 * <p>{@link #assembleHtmlFirst} / {@link #assembleMarkdown} cover the non-trivial branches of
 * {@code assembleSingle} (template resolution, blank fallback, charset/mimetype defaults) so those
 * paths can be regression-tested without loading the assembly service locator.
 */
public final class PSTextAssemblerSupport {

  private static final Parser PARSER = Parser.builder().build();
  private static final HtmlRenderer RENDERER = HtmlRenderer.builder().build();

  private static final IPSScript SYS_TEMPLATE =
      PSJexlEvaluator.createStaticExpression(SYS_PARAM_TEMPLATE);
  private static final IPSScript SYS_MIMETYPE =
      PSJexlEvaluator.createStaticExpression(SYS_PARAM_MIMETYPE);
  private static final IPSScript SYS_CHARSET =
      PSJexlEvaluator.createStaticExpression(SYS_PARAM_CHARSET);

  private PSTextAssemblerSupport() {}

  /**
   * Apply {@code ${path}} placeholders only.
   *
   * @param template source, may be null
   * @param bindings assembly bindings, may be null
   * @return rendered HTML/text body
   */
  public static String renderHtmlFirst(String template, Map<String, ?> bindings) {
    return PSBindingPlaceholderRenderer.render(template, bindings);
  }

  /**
   * Apply placeholders, then CommonMark → HTML.
   *
   * @param template Markdown source, may be null
   * @param bindings assembly bindings, may be null
   * @return HTML body
   */
  public static String renderMarkdown(String template, Map<String, ?> bindings) {
    String withPlaceholders = PSBindingPlaceholderRenderer.render(template, bindings);
    Node document = PARSER.parse(withPlaceholders);
    return RENDERER.render(document);
  }

  /**
   * Full HTML-first assemble path (template resolution + render + mime/charset). Used by {@link
   * PSHtmlAssembler#assembleSingle}.
   *
   * @param bindings item bindings (may be null/empty)
   * @param fallbackTemplateSource {@code item.getTemplate().getTemplate()} when {@code
   *     $sys.template} is blank; may be null
   * @return outcome; never null
   */
  public static TextAssembleOutcome assembleHtmlFirst(
      Map<String, Object> bindings, String fallbackTemplateSource) {
    return assemble(
        bindings,
        fallbackTemplateSource,
        "no HTML template present",
        PSTextAssemblerSupport::renderHtmlFirst);
  }

  /**
   * Full Markdown assemble path (template resolution + render + mime/charset). Used by {@link
   * PSMarkdownAssembler#assembleSingle}.
   *
   * @param bindings item bindings (may be null/empty)
   * @param fallbackTemplateSource {@code item.getTemplate().getTemplate()} when {@code
   *     $sys.template} is blank; may be null
   * @return outcome; never null
   */
  public static TextAssembleOutcome assembleMarkdown(
      Map<String, Object> bindings, String fallbackTemplateSource) {
    return assemble(
        bindings,
        fallbackTemplateSource,
        "no Markdown template present",
        PSTextAssemblerSupport::renderMarkdown);
  }

  private static TextAssembleOutcome assemble(
      Map<String, Object> bindings,
      String fallbackTemplateSource,
      String missingTemplateMessage,
      BodyRenderer renderer) {
    Map<String, Object> map = bindings != null ? bindings : Map.of();
    PSJexlEvaluator eval = new PSJexlEvaluator(map);
    String template;
    try {
      template = (String) eval.evaluate(SYS_TEMPLATE);
    } catch (Exception e) {
      return TextAssembleOutcome.failure(
          "Exception retrieving template: " + PSExceptionUtils.getMessageForLog(e));
    }
    // Same combined-condition style for both assemblers (blank $sys.template → object source).
    if (StringUtils.isBlank(template) && StringUtils.isNotBlank(fallbackTemplateSource)) {
      template = fallbackTemplateSource;
    }
    if (StringUtils.isBlank(template)) {
      return TextAssembleOutcome.failure(missingTemplateMessage);
    }

    String result = renderer.render(template, map);

    String mtype;
    try {
      mtype = (String) eval.evaluate(SYS_MIMETYPE);
    } catch (Exception e) {
      mtype = null;
    }
    if (StringUtils.isBlank(mtype)) {
      mtype = DEFAULT_MIMETYPE;
    }

    String charset;
    try {
      charset = (String) eval.evaluate(SYS_CHARSET);
    } catch (Exception e) {
      charset = null;
    }
    Charset cset = StandardCharsets.UTF_8;
    if (StringUtils.isNotBlank(charset)) {
      cset = Charset.forName(charset);
      charset = cset.name();
    } else {
      charset = cset.name();
    }

    return TextAssembleOutcome.success(result, mtype, charset, cset);
  }

  @FunctionalInterface
  private interface BodyRenderer {
    String render(String template, Map<String, ?> bindings);
  }

  /**
   * Result of the pure assemble path used by HTML-first / Markdown assemblers.
   *
   * @param success whether assembly succeeded
   * @param body rendered body when successful; null on failure
   * @param mimeType base mime type without charset (e.g. text/html)
   * @param charsetName resolved charset name
   * @param charset resolved charset
   * @param errorMessage failure message when not successful
   */
  public record TextAssembleOutcome(
      boolean success,
      String body,
      String mimeType,
      String charsetName,
      Charset charset,
      String errorMessage) {

    public static TextAssembleOutcome success(
        String body, String mimeType, String charsetName, Charset charset) {
      return new TextAssembleOutcome(
          true,
          Objects.requireNonNull(body, "body"),
          Objects.requireNonNull(mimeType, "mimeType"),
          Objects.requireNonNull(charsetName, "charsetName"),
          Objects.requireNonNull(charset, "charset"),
          null);
    }

    public static TextAssembleOutcome failure(String errorMessage) {
      return new TextAssembleOutcome(
          false, null, null, null, null, Objects.requireNonNull(errorMessage, "errorMessage"));
    }

    /** Full Content-Type value including charset. */
    public String contentType() {
      return mimeType + ";charset=" + charsetName;
    }
  }
}
