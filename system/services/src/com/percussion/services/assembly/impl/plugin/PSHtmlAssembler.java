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

import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.assembly.IPSAssemblyItem;
import com.percussion.services.assembly.IPSAssemblyResult;
import com.percussion.services.assembly.IPSAssemblyResult.Status;
import com.percussion.services.assembly.impl.plugin.PSTextAssemblerSupport.TextAssembleOutcome;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * HTML-first text assembler: template body is mostly static HTML with {@code ${path}}
 * placeholders filled from JEXL binding results. No Velocity directives.
 *
 * <p>Extension name: {@code Java/global/percussion/assembly/htmlAssembler}
 *
 * <p>Non-trivial assemble branches live in {@link PSTextAssemblerSupport#assembleHtmlFirst} (unit
 * tested without Spring).
 *
 * @see PSBindingPlaceholderRenderer
 * @see PSVelocityAssembler
 */
public class PSHtmlAssembler extends PSAssemblerBase {

  private static final Logger log = LogManager.getLogger(PSHtmlAssembler.class);

  @Override
  public IPSAssemblyResult assembleSingle(IPSAssemblyItem item) {
    try {
      String fallback =
          item.getTemplate() != null ? item.getTemplate().getTemplate() : null;
      TextAssembleOutcome outcome =
          PSTextAssemblerSupport.assembleHtmlFirst(item.getBindings(), fallback);
      if (!outcome.success()) {
        return getFailureResult(item, outcome.errorMessage());
      }
      item.setResultData(outcome.body().getBytes(outcome.charset()));
      item.setMimeType(outcome.contentType());
      item.setStatus(Status.SUCCESS);
      return (IPSAssemblyResult) item;
    } catch (Exception e) {
      log.error(
          "HTML assembler failed for template {}: {}",
          item.getTemplate() != null ? item.getTemplate().getName() : "?",
          PSExceptionUtils.getMessageForLog(e));
      return getFailureResult(item, PSExceptionUtils.getMessageForLog(e));
    }
  }
}
