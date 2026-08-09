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
import com.percussion.services.assembly.IPSAssemblyItem;
import com.percussion.services.assembly.IPSAssemblyResult;
import com.percussion.services.assembly.IPSAssemblyResult.Status;
import com.percussion.utils.jexl.IPSScript;
import com.percussion.utils.jexl.PSJexlEvaluator;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Markdown text assembler: applies {@code ${path}} placeholders from JEXL bindings, then
 * converts CommonMark Markdown to HTML.
 *
 * <p>Extension name: {@code Java/global/percussion/assembly/markdownAssembler}
 *
 * @see PSTextAssemblerSupport
 * @see PSHtmlAssembler
 */
public class PSMarkdownAssembler extends PSAssemblerBase {

  private static final Logger log = LogManager.getLogger(PSMarkdownAssembler.class);

  private static final IPSScript SYS_TEMPLATE =
      PSJexlEvaluator.createStaticExpression(SYS_PARAM_TEMPLATE);
  private static final IPSScript SYS_MIMETYPE =
      PSJexlEvaluator.createStaticExpression(SYS_PARAM_MIMETYPE);
  private static final IPSScript SYS_CHARSET =
      PSJexlEvaluator.createStaticExpression(SYS_PARAM_CHARSET);

  @Override
  @SuppressWarnings("unchecked")
  public IPSAssemblyResult assembleSingle(IPSAssemblyItem item) {
    PSJexlEvaluator eval = new PSJexlEvaluator(item.getBindings());
    try {
      String template;
      try {
        template = (String) eval.evaluate(SYS_TEMPLATE);
      } catch (Exception e) {
        return getFailureResult(
            item, "Exception retrieving template: " + PSExceptionUtils.getMessageForLog(e));
      }
      if (StringUtils.isBlank(template) && item.getTemplate() != null) {
        template = item.getTemplate().getTemplate();
      }
      if (StringUtils.isBlank(template)) {
        return getFailureResult(item, "no Markdown template present");
      }

      Map<String, Object> bindings = item.getBindings();
      String result = PSTextAssemblerSupport.renderMarkdown(template, bindings);

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

      item.setResultData(result.getBytes(cset));
      item.setMimeType(mtype + ";charset=" + charset);
      item.setStatus(Status.SUCCESS);
      return (IPSAssemblyResult) item;
    } catch (Exception e) {
      log.error(
          "Markdown assembler failed for template {}: {}",
          item.getTemplate() != null ? item.getTemplate().getName() : "?",
          PSExceptionUtils.getMessageForLog(e));
      return getFailureResult(item, PSExceptionUtils.getMessageForLog(e));
    }
  }
}
