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
package com.percussion.pagemanagement.assembler;

import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.PSExtensionException;
import com.percussion.pagemanagement.assembler.impl.PSAssemblyItemBridge;
import com.percussion.pagemanagement.assembler.impl.PSAssemblyItemBridge.TemplateAndPage;
import com.percussion.services.assembly.IPSAssemblyItem;
import com.percussion.services.assembly.IPSAssemblyResult;
import com.percussion.services.assembly.impl.plugin.PSVelocityAssembler;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import java.io.File;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.StopWatch;

/**
 * The entry point for the assembly service to assemble pages or templates. The assembler can
 * determine if its rendering a page or template safely.
 *
 * <p><b>Normalization note (8.2 / epic #2626):</b> This class is intentionally a thin context
 * provider on top of {@link PSVelocityAssembler} — it builds the {@code $perc} page assembly
 * context, then delegates render to Velocity. Longer term it should bind the same page context
 * for any text assembler (Velocity, HTML-first, Markdown) rather than remaining a separate
 * template species. See {@code docs/ai-generated/tasks/template-assembler-normalization/}.
 *
 * @see PSPageAssemblyContextFactory
 * @author adamgent
 */
// REFACTORED: CP-JAVA11
public class PSPageAssembler extends PSVelocityAssembler {
  private PSPageAssemblyContextFactory pageAssemblyContextFactory;
  private PSAssemblyItemBridge assemblyItemBridge;

  @Override
  public void init(IPSExtensionDef def, File file) throws PSExtensionException {
    super.init(def, file);
    PSSpringWebApplicationContextUtils.injectDependencies(this);
  }

  /**
   * Gets the template source (or content) from the specified item.
   *
   * @param assemblyItem the item used to retrieve the template.
   * @return the template source, not blank.
   */
  /**
   * Gets the template source (or content) from the specified item.
   *
   * @param assemblyItem the item used to retrieve the template.
   * @return the template source, not blank.
   */
  protected String getTemplateSource(IPSAssemblyItem assemblyItem) {
    final var template = assemblyItem.getTemplate();
    return template.getTemplate();
  }

  /**
   * This calls {@link PSPageAssemblyContextFactory#createContext(IPSAssemblyItem, TemplateAndPage,
   * boolean) PSPageAssemblyContextFactory.createContext(IPSAssemblyItem, TemplateAndPage, true)}
   */
  protected PSPageAssemblyContext createContext(
      IPSAssemblyItem assemblyItem, TemplateAndPage templateAndPage) throws Exception {
    return getPageAssemblyContextFactory().createContext(assemblyItem, templateAndPage, true);
  }

  @Override
  protected IPSAssemblyResult doAssembleSingle(IPSAssemblyItem assemblyItem) throws Exception {
    final var sw = new StopWatch("#doAssemblySingle");
    sw.start("templateAndPage");
    final var tp = assemblyItemBridge.getTemplateAndPage(assemblyItem);
    sw.stop();
    sw.start("createContext");
    final var context = createContext(assemblyItem, tp);
    sw.stop();

    final var eval = PSJexlUtils.getBindings(assemblyItem);
    eval.bind("$perc", context);
    final var templateSource = getTemplateSource(assemblyItem);
    eval.bind("$sys.template", templateSource);

    assemblyItem.setBindings(eval.getVars());

    sw.start("assemble");
    final var result = super.doAssembleSingle(assemblyItem);
    sw.stop();

    log.debug("{}", sw.prettyPrint());

    return result;
  }

  public PSPageAssemblyContextFactory getPageAssemblyContextFactory() {
    return pageAssemblyContextFactory;
  }

  public void setPageAssemblyContextFactory(
      PSPageAssemblyContextFactory pageAssemblyContextFactory) {
    this.pageAssemblyContextFactory = pageAssemblyContextFactory;
  }

  public PSAssemblyItemBridge getAssemblyItemBridge() {
    return assemblyItemBridge;
  }

  public void setAssemblyItemBridge(PSAssemblyItemBridge assemblyItemBridge) {
    this.assemblyItemBridge = assemblyItemBridge;
  }

  /** The log instance to use for this class, never <code>null</code>. */
  private static final Logger log = LogManager.getLogger(PSPageAssembler.class);
}
