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

package com.percussion.packages.pagexml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.packages.manifest.PSComponentPackageManifest;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit + golden parity tests for shared-snippet assembly dedupe across template variants of one
 * manifest (issue #7050 / parent #2626).
 */
class PSPageXmlSharedSnippetDedupeTest {

  private static final String GOLDEN_CANONICAL =
      "/pagexml/golden/shared-snippet-assembly.txt";

  @Test
  void dedupeThreeVariants_sharesCommonBindingsRegionsAndOutputFormat() throws Exception {
    List<PSPageXmlModel> variants = threeVariants();

    PSPageXmlSharedSnippetDedupe.DedupedModels deduped =
        PSPageXmlSharedSnippetDedupe.dedupeModels(variants);

    assertEquals("promo.page", deduped.getManifestId());
    PSPageXmlSharedSnippetDedupe.SharedAssembly shared = deduped.getShared();
    assertEquals(2, shared.getCommonBindings().size());
    assertEquals("$sys.title", shared.getCommonBindings().get(0).getVariable());
    assertEquals("$sys.body", shared.getCommonBindings().get(1).getVariable());
    assertEquals(1, shared.getCommonRegionHoles().size());
    assertEquals("perc-content", shared.getCommonRegionHoles().get(0).getRegionId());
    assertEquals("Page", shared.getCommonOutputFormat());

    assertEquals(3, deduped.getVariants().size());
    for (PSPageXmlSharedSnippetDedupe.VariantView view : deduped.getVariants()) {
      assertSame(shared, view.getShared(), "every variant references the single shared instance");
    }
    assertEquals(1, deduped.getVariants().get(0).getResidualBindings().size());
    assertEquals(1, deduped.getVariants().get(0).getResidualRegionHoles().size());
    assertEquals(2, deduped.getVariants().get(1).getResidualBindings().size());
    assertEquals(1, deduped.getVariants().get(1).getResidualRegionHoles().size());
    assertTrue(deduped.getVariants().get(2).getResidualBindings().isEmpty());
    assertTrue(deduped.getVariants().get(2).getResidualRegionHoles().isEmpty());
  }

  @Test
  void expandRestoresByteIdenticalRenders() throws Exception {
    List<PSPageXmlModel> variants = threeVariants();
    PSPageXmlSharedSnippetDedupe.DedupedModels deduped =
        PSPageXmlSharedSnippetDedupe.dedupeModels(variants);

    for (int i = 0; i < variants.size(); i++) {
      PSPageXmlModel original = variants.get(i);
      PSPageXmlModel expanded = deduped.getVariants().get(i).expand();

      assertEquals(original.getBindings(), expanded.getBindings());
      assertEquals(original.getRegionHoles(), expanded.getRegionHoles());
      assertEquals(original.getOutputFormat(), expanded.getOutputFormat());

      // Render inputs through the real compiler: template source bytes plus the compiled
      // manifest entry (bindings, slots) must be identical before/after dedupe.
      PSPageXmlCompileResult before = PSPageXmlCompiler.compile(original, testContext());
      PSPageXmlCompileResult after = PSPageXmlCompiler.compile(expanded, testContext());
      assertEquals(
          before.getTextArtifacts(),
          after.getTextArtifacts(),
          "template source bytes must be byte-identical after dedupe");
      PSComponentPackageManifest.TemplateRef beforeTemplate =
          before.getManifest().getTemplates().get(0);
      PSComponentPackageManifest.TemplateRef afterTemplate =
          after.getManifest().getTemplates().get(0);
      assertEquals(beforeTemplate, afterTemplate);
      assertEquals(before.getManifest().getSlots(), after.getManifest().getSlots());
    }
  }

  @Test
  void goldenCanonicalText_matchesFixture() throws Exception {
    PSPageXmlSharedSnippetDedupe.DedupedModels deduped =
        PSPageXmlSharedSnippetDedupe.dedupeModels(threeVariants());

    String expected = normalizeNewlines(readClasspath(GOLDEN_CANONICAL));
    String actual =
        normalizeNewlines(deduped.getShared().toCanonicalText(deduped.getVariants()));
    assertEquals(expected, actual, "shared assembly golden parity");
  }

  @Test
  void dedupeManifestTemplates_commonBindingsAndDefaultsWithExpand() throws Exception {
    PSComponentPackageManifest.TemplateRef page = manifestTemplate("promo.page", "page");
    page.setMimeType("text/html");
    page.setPublishWhen("Default");
    page.setLocationSuffix(".html");
    addManifestBinding(page, "$sys.title", "$sys.item.getProperty(\"rx:title\")");
    addManifestBinding(page, "$sys.body", "$sys.item.getProperty(\"rx:body\")");
    addManifestBinding(page, "$promo.cta", "$sys.params.get(\"cta\")");

    PSComponentPackageManifest.TemplateRef snippet = manifestTemplate("promo.snippet", "snippet");
    snippet.setMimeType("text/html");
    snippet.setPublishWhen("Default");
    snippet.setLocationSuffix(".html");
    addManifestBinding(snippet, "$promo.cta", "$sys.params.get(\"cta\")");
    addManifestBinding(snippet, "$sys.title", "$sys.item.getProperty(\"rx:title\")");
    addManifestBinding(snippet, "$sys.body", "$sys.item.getProperty(\"rx:body\")");
    addManifestBinding(snippet, "$snippet.only", "$sys.params.get(\"only\")");

    PSPageXmlSharedSnippetDedupe.DedupedTemplates deduped =
        PSPageXmlSharedSnippetDedupe.dedupeManifestTemplates(List.of(page, snippet));

    assertEquals(3, deduped.getShared().getCommonBindings().size());
    assertEquals("$sys.title", deduped.getShared().getCommonBindings().get(0).getVariable());
    assertEquals("$sys.body", deduped.getShared().getCommonBindings().get(1).getVariable());
    assertEquals("$promo.cta", deduped.getShared().getCommonBindings().get(2).getVariable());
    assertEquals("text/html", deduped.getShared().getCommonMimeType());
    assertEquals("Default", deduped.getShared().getCommonPublishWhen());
    assertEquals(".html", deduped.getShared().getCommonLocationSuffix());
    assertNull(deduped.getShared().getCommonLocationPrefix());

    assertSame(
        deduped.getShared(),
        deduped.getVariants().get(0).getShared(),
        "every template references the single shared instance");
    assertTrue(deduped.getVariants().get(0).getResidualBindings().isEmpty());
    assertEquals(1, deduped.getVariants().get(1).getResidualBindings().size());
    assertEquals("$snippet.only", deduped.getVariants().get(1).getResidualBindings().get(0).getVariable());
    assertEquals(page, deduped.getVariants().get(0).expand());
    assertEquals(snippet, deduped.getVariants().get(1).expand());
  }

  @Test
  void divergentOutputFormat_commonIsNullButExpandStaysIdentical() throws Exception {
    List<PSPageXmlModel> variants = threeVariants();
    variants.get(1).setOutputFormat("Snippet");

    PSPageXmlSharedSnippetDedupe.DedupedModels deduped =
        PSPageXmlSharedSnippetDedupe.dedupeModels(variants);
    assertNull(deduped.getShared().getCommonOutputFormat());

    for (int i = 0; i < variants.size(); i++) {
      PSPageXmlModel expanded = deduped.getVariants().get(i).expand();
      assertEquals(variants.get(i).getBindings(), expanded.getBindings());
      assertEquals(variants.get(i).getRegionHoles(), expanded.getRegionHoles());
      assertEquals(variants.get(i).getOutputFormat(), expanded.getOutputFormat());
    }
  }

  @Test
  void dedupeModels_emptyOrNull_throws() {
    assertThrows(
        PSPageXmlException.class, () -> PSPageXmlSharedSnippetDedupe.dedupeModels(List.of()));
    assertThrows(
        PSPageXmlException.class, () -> PSPageXmlSharedSnippetDedupe.dedupeModels(null));
    assertThrows(
        PSPageXmlException.class,
        () ->
            PSPageXmlSharedSnippetDedupe.dedupeManifestTemplates(
                Arrays.asList(manifestTemplate("promo.page", "page"), null)));
  }

  private static List<PSPageXmlModel> threeVariants() {
    PSPageXmlModel page = new PSPageXmlModel();
    page.setName("promo.page");
    page.setOutputFormat("Page");
    page.setAssembler("Java/global/percussion/assembly/pageAssembler");
    page.setTemplateBody("#region(\"perc-content\")\n<div>page</div>");
    addBinding(page, "$sys.title", "$sys.item.getProperty(\"rx:title\")");
    addBinding(page, "$sys.body", "$sys.item.getProperty(\"rx:body\")");
    addBinding(page, "$promo.cta", "$sys.params.get(\"cta\")");
    page.getRegionHoles().add(contentHole());
    page.getRegionHoles().add(railHole());

    PSPageXmlModel snippet = new PSPageXmlModel();
    snippet.setName("promo.snippet");
    snippet.setOutputFormat("Page");
    snippet.setAssembler("Java/global/percussion/assembly/velocityAssembler");
    snippet.setTemplateBody("#region(\"perc-content\")\n<span>snippet</span>");
    addBinding(snippet, "$promo.cta", "$sys.params.get(\"cta\")");
    addBinding(snippet, "$sys.title", "$sys.item.getProperty(\"rx:title\")");
    addBinding(snippet, "$sys.body", "$sys.item.getProperty(\"rx:body\")");
    addBinding(snippet, "$snippet.only", "$sys.params.get(\"only\")");
    snippet.getRegionHoles().add(contentHole());
    snippet.getRegionHoles().add(snippetSlotHole());

    PSPageXmlModel shared = new PSPageXmlModel();
    shared.setName("promo.shared");
    shared.setOutputFormat("Page");
    shared.setAssembler("Java/global/percussion/assembly/pageAssembler");
    shared.setTemplateBody("#region(\"perc-content\")\n<div>shared</div>");
    addBinding(shared, "$sys.body", "$sys.item.getProperty(\"rx:body\")");
    addBinding(shared, "$sys.title", "$sys.item.getProperty(\"rx:title\")");
    shared.getRegionHoles().add(contentHole());

    return new ArrayList<>(List.of(page, snippet, shared));
  }

  private static PSPageXmlModel.RegionHole contentHole() {
    PSPageXmlModel.RegionHole hole = new PSPageXmlModel.RegionHole();
    hole.setRegionId("perc-content");
    hole.setCssClass("perc-region perc-vertical");
    Map<String, Object> layout = new LinkedHashMap<>();
    layout.put("orientation", "vertical");
    hole.setLayoutHints(layout);
    Map<String, Object> styles = new LinkedHashMap<>();
    styles.put("rootclass", "perc-region perc-vertical");
    hole.setStyleHints(styles);
    return hole;
  }

  private static PSPageXmlModel.RegionHole railHole() {
    PSPageXmlModel.RegionHole hole = new PSPageXmlModel.RegionHole();
    hole.setRegionId("promo-rail");
    hole.setCssClass("");
    Map<String, Object> layout = new LinkedHashMap<>();
    layout.put("orientation", "horizontal");
    hole.setLayoutHints(layout);
    hole.setStyleHints(new LinkedHashMap<>());
    return hole;
  }

  private static PSPageXmlModel.RegionHole snippetSlotHole() {
    PSPageXmlModel.RegionHole hole = new PSPageXmlModel.RegionHole();
    hole.setRegionId("snippet-slot");
    hole.setCssClass("snippet-slot");
    hole.setLayoutHints(new LinkedHashMap<>());
    hole.setStyleHints(new LinkedHashMap<>());
    return hole;
  }

  private static void addBinding(PSPageXmlModel model, String variable, String expression) {
    PSPageXmlModel.Binding binding = new PSPageXmlModel.Binding();
    binding.setVariable(variable);
    binding.setExpression(expression);
    model.getBindings().add(binding);
  }

  private static PSComponentPackageManifest.TemplateRef manifestTemplate(String name, String type) {
    PSComponentPackageManifest.TemplateRef template =
        new PSComponentPackageManifest.TemplateRef();
    template.setName(name);
    template.setType(type);
    template.setAssembler("velocityAssembler");
    template.setSourceRef("templates/" + name + ".vm");
    return template;
  }

  private static void addManifestBinding(
      PSComponentPackageManifest.TemplateRef template, String variable, String expression) {
    PSComponentPackageManifest.Binding binding = new PSComponentPackageManifest.Binding();
    binding.setVariable(variable);
    binding.setExpression(expression);
    template.getBindings().add(binding);
  }

  private static PSPageXmlPackageContext testContext() {
    PSPageXmlPackageContext ctx = new PSPageXmlPackageContext();
    ctx.setPackageId("perc.baseTemplates");
    ctx.setPackageName("perc.baseTemplates");
    ctx.setVersion("1.1.5");
    ctx.setDescription("Shared-snippet dedupe test package.");
    ctx.setPublisherName("Percussion Software Inc.");
    ctx.setPublisherUrl("http://www.percussion.com");
    ctx.setCmsMin("1.0.0");
    ctx.setCmsMax("9.0.0");
    return ctx;
  }

  private static String readClasspath(String resource) throws Exception {
    try (InputStream in =
        PSPageXmlSharedSnippetDedupeTest.class.getResourceAsStream(resource)) {
      assertNotNull(in, "missing test resource: " + resource);
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static String normalizeNewlines(String s) {
    return s == null ? null : s.replace("\r\n", "\n").replace('\r', '\n');
  }
}
