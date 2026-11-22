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

import com.percussion.packages.manifest.PSComponentPackageManifest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Shared-snippet assembly dedupe across the template variants of one manifest (issue #7050 /
 * parent #2626).
 *
 * <p>Manifests may declare several {@code templates[]} variants (page / snippet / shared holes)
 * that copy the same snippet assembly — common JEXL bindings, common region holes, common
 * output-format defaults — across every variant. This helper computes that shared assembly once
 * ({@link SharedAssembly}) and hands each variant a {@link VariantView} that references the
 * single shared instance plus only its own residuals. {@link VariantView#expand()} merges the
 * two back into the full per-variant definition in original order, so variant renders are
 * byte-identical before and after the dedupe pass.
 *
 * <p>Two levels are covered:
 *
 * <ul>
 *   <li>Model level ({@link #dedupeModels}): {@code PSPageXmlModel} variants — common bindings,
 *       common region holes, common output format.
 *   <li>Manifest level ({@link #dedupeManifestTemplates}): {@code TemplateRef} variants of one
 *       {@code PSComponentPackageManifest} — common bindings plus common install defaults
 *       (mime type, publish-when, location prefix/suffix). Manifest-level slots are already
 *       shared across variants, so they need no dedupe.
 * </ul>
 */
public final class PSPageXmlSharedSnippetDedupe {

  private PSPageXmlSharedSnippetDedupe() {
    // utility
  }

  /**
   * Dedupe the shared snippet assembly across model-level template variants of one manifest.
   *
   * @param variants non-null, non-empty ordered variant models (same manifest family)
   * @return deduped view; every {@link VariantView} references the same {@link SharedAssembly}
   *     instance
   * @throws PSPageXmlException when no variants are supplied or a variant entry is null
   */
  public static DedupedModels dedupeModels(List<PSPageXmlModel> variants) throws PSPageXmlException {
    if (variants == null || variants.isEmpty()) {
      throw new PSPageXmlException("Shared-snippet dedupe requires at least one template variant");
    }
    List<PSPageXmlModel> safe = new ArrayList<>(variants.size());
    for (PSPageXmlModel variant : variants) {
      if (variant == null) {
        throw new PSPageXmlException("Shared-snippet dedupe variant must not be null");
      }
      safe.add(variant);
    }

    List<PSPageXmlModel.Binding> commonBindings = commonBindings(safe);
    List<PSPageXmlModel.RegionHole> commonRegions = commonRegions(safe);
    String commonOutputFormat = commonOutputFormat(safe);
    SharedAssembly shared = new SharedAssembly(commonBindings, commonRegions, commonOutputFormat);

    List<VariantView> views = new ArrayList<>(safe.size());
    for (PSPageXmlModel variant : safe) {
      views.add(VariantView.of(variant, shared));
    }
    String manifestId = safe.get(0).pageStem();
    return new DedupedModels(manifestId, shared, views);
  }

  /**
   * Dedupe the shared assembly across the {@code templates[]} entries of one manifest.
   *
   * @param templates non-null, non-empty ordered template refs from one manifest
   * @return deduped view; every {@link TemplateVariantView} references the same {@link
   *     SharedTemplateAssembly} instance
   * @throws PSPageXmlException when no templates are supplied or a template entry is null
   */
  public static DedupedTemplates dedupeManifestTemplates(
      List<PSComponentPackageManifest.TemplateRef> templates) throws PSPageXmlException {
    if (templates == null || templates.isEmpty()) {
      throw new PSPageXmlException("Shared-snippet dedupe requires at least one manifest template");
    }
    List<PSComponentPackageManifest.TemplateRef> safe = new ArrayList<>(templates.size());
    for (PSComponentPackageManifest.TemplateRef template : templates) {
      if (template == null) {
        throw new PSPageXmlException("Shared-snippet dedupe manifest template must not be null");
      }
      safe.add(template);
    }

    List<PSComponentPackageManifest.Binding> commonBindings = commonManifestBindings(safe);
    String commonMimeType = commonText(safe, PSComponentPackageManifest.TemplateRef::getMimeType);
    String commonPublishWhen =
        commonText(safe, PSComponentPackageManifest.TemplateRef::getPublishWhen);
    String commonLocationPrefix =
        commonText(safe, PSComponentPackageManifest.TemplateRef::getLocationPrefix);
    String commonLocationSuffix =
        commonText(safe, PSComponentPackageManifest.TemplateRef::getLocationSuffix);
    SharedTemplateAssembly shared =
        new SharedTemplateAssembly(
            commonBindings,
            commonMimeType,
            commonPublishWhen,
            commonLocationPrefix,
            commonLocationSuffix);

    List<TemplateVariantView> views = new ArrayList<>(safe.size());
    for (PSComponentPackageManifest.TemplateRef template : safe) {
      views.add(TemplateVariantView.of(template, shared));
    }
    return new DedupedTemplates(shared, views);
  }

  private static List<PSPageXmlModel.Binding> commonBindings(List<PSPageXmlModel> variants) {
    List<Set<PSPageXmlModel.Binding>> perVariant = new ArrayList<>(variants.size());
    for (PSPageXmlModel variant : variants) {
      perVariant.add(new LinkedHashSet<>(safeBindings(variant)));
    }
    List<PSPageXmlModel.Binding> common = new ArrayList<>();
    for (PSPageXmlModel.Binding candidate : safeBindings(variants.get(0))) {
      if (candidate == null) {
        continue;
      }
      boolean inAll = true;
      for (int i = 1; i < perVariant.size(); i++) {
        if (!perVariant.get(i).contains(candidate)) {
          inAll = false;
          break;
        }
      }
      if (inAll) {
        common.add(copyBinding(candidate));
      }
    }
    return common;
  }

  private static List<PSPageXmlModel.RegionHole> commonRegions(List<PSPageXmlModel> variants) {
    List<Set<PSPageXmlModel.RegionHole>> perVariant = new ArrayList<>(variants.size());
    for (PSPageXmlModel variant : variants) {
      perVariant.add(new LinkedHashSet<>(safeRegions(variant)));
    }
    List<PSPageXmlModel.RegionHole> common = new ArrayList<>();
    for (PSPageXmlModel.RegionHole candidate : safeRegions(variants.get(0))) {
      if (candidate == null) {
        continue;
      }
      boolean inAll = true;
      for (int i = 1; i < perVariant.size(); i++) {
        if (!perVariant.get(i).contains(candidate)) {
          inAll = false;
          break;
        }
      }
      if (inAll) {
        common.add(copyRegion(candidate));
      }
    }
    return common;
  }

  /**
   * Single distinct output format across all variants (trimmed, case-insensitive); null when the
   * variants disagree or none declares one.
   */
  private static String commonOutputFormat(List<PSPageXmlModel> variants) {
    String common = null;
    for (PSPageXmlModel variant : variants) {
      String normalized = normalizeText(variant.getOutputFormat());
      if (normalized == null) {
        return null;
      }
      if (common == null) {
        common = normalized;
      } else if (!common.equalsIgnoreCase(normalized)) {
        return null;
      }
    }
    return common;
  }

  private static List<PSComponentPackageManifest.Binding> commonManifestBindings(
      List<PSComponentPackageManifest.TemplateRef> templates) {
    List<Set<PSComponentPackageManifest.Binding>> perTemplate = new ArrayList<>(templates.size());
    for (PSComponentPackageManifest.TemplateRef template : templates) {
      perTemplate.add(new LinkedHashSet<>(safeManifestBindings(template)));
    }
    List<PSComponentPackageManifest.Binding> common = new ArrayList<>();
    for (PSComponentPackageManifest.Binding candidate : safeManifestBindings(templates.get(0))) {
      if (candidate == null) {
        continue;
      }
      boolean inAll = true;
      for (int i = 1; i < perTemplate.size(); i++) {
        if (!perTemplate.get(i).contains(candidate)) {
          inAll = false;
          break;
        }
      }
      if (inAll) {
        PSComponentPackageManifest.Binding copy = new PSComponentPackageManifest.Binding();
        copy.setVariable(candidate.getVariable());
        copy.setExpression(candidate.getExpression());
        common.add(copy);
      }
    }
    return common;
  }

  private interface TextAccessor {
    String get(PSComponentPackageManifest.TemplateRef template);
  }

  private static String commonText(
      List<PSComponentPackageManifest.TemplateRef> templates, TextAccessor accessor) {
    String common = null;
    for (PSComponentPackageManifest.TemplateRef template : templates) {
      String normalized = normalizeText(accessor.get(template));
      if (normalized == null) {
        return null;
      }
      if (common == null) {
        common = normalized;
      } else if (!common.equals(normalized)) {
        return null;
      }
    }
    return common;
  }

  private static List<PSPageXmlModel.Binding> safeBindings(PSPageXmlModel variant) {
    return variant.getBindings() != null ? variant.getBindings() : List.of();
  }

  private static List<PSPageXmlModel.RegionHole> safeRegions(PSPageXmlModel variant) {
    return variant.getRegionHoles() != null ? variant.getRegionHoles() : List.of();
  }

  private static List<PSComponentPackageManifest.Binding> safeManifestBindings(
      PSComponentPackageManifest.TemplateRef template) {
    return template.getBindings() != null ? template.getBindings() : List.of();
  }

  private static PSPageXmlModel.Binding copyBinding(PSPageXmlModel.Binding source) {
    PSPageXmlModel.Binding copy = new PSPageXmlModel.Binding();
    copy.setVariable(source.getVariable());
    copy.setExpression(source.getExpression());
    return copy;
  }

  private static PSPageXmlModel.RegionHole copyRegion(PSPageXmlModel.RegionHole source) {
    PSPageXmlModel.RegionHole copy = new PSPageXmlModel.RegionHole();
    copy.setRegionId(source.getRegionId());
    copy.setCssClass(source.getCssClass());
    copy.setLayoutHints(
        source.getLayoutHints() != null
            ? new LinkedHashMap<>(source.getLayoutHints())
            : new LinkedHashMap<>());
    copy.setStyleHints(
        source.getStyleHints() != null
            ? new LinkedHashMap<>(source.getStyleHints())
            : new LinkedHashMap<>());
    return copy;
  }

  static String normalizeText(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private static PSComponentPackageManifest.Binding copyManifestBinding(
      PSComponentPackageManifest.Binding source) {
    PSComponentPackageManifest.Binding copy = new PSComponentPackageManifest.Binding();
    copy.setVariable(source.getVariable());
    copy.setExpression(source.getExpression());
    return copy;
  }

  /** Copies one ordered entry list position plan: shared index ({@code >= 0}) or residual slot. */
  private interface EntryCopier<T> {
    T copy(T source);
  }

  /**
   * Build the per-position plan for one ordered entry list against its shared entries: each
   * position holds the shared index ({@code >= 0}) or the residual slot (encoded {@code -1 -
   * idx}), appending deep copies of residual entries in encounter order.
   *
   * @throws PSPageXmlException when an entry is null — a null slot would desynchronize the plan
   *     from the residuals on {@code expand()} (same fail-fast contract as the variant-level
   *     null check in {@link #dedupeModels})
   */
  private static <T> List<Integer> planEntries(
      List<T> entries, List<T> sharedEntries, List<T> residuals, EntryCopier<T> copier)
      throws PSPageXmlException {
    List<Integer> plan = new ArrayList<>(entries.size());
    for (T entry : entries) {
      if (entry == null) {
        throw new PSPageXmlException("Shared-snippet dedupe entry must not be null");
      }
      int sharedIdx = sharedEntries.indexOf(entry);
      if (sharedIdx >= 0) {
        plan.add(sharedIdx);
        continue;
      }
      plan.add(-1 - residuals.size());
      residuals.add(copier.copy(entry));
    }
    return plan;
  }

  /**
   * Shared snippet assembly computed once across the variants of one manifest: common bindings
   * (first-variant order), common region holes (first-variant order), and the single distinct
   * output format (null when variants disagree or none declares one).
   */
  public static final class SharedAssembly {
    private final List<PSPageXmlModel.Binding> commonBindings;
    private final List<PSPageXmlModel.RegionHole> commonRegionHoles;
    private final String commonOutputFormat;

    private SharedAssembly(
        List<PSPageXmlModel.Binding> commonBindings,
        List<PSPageXmlModel.RegionHole> commonRegionHoles,
        String commonOutputFormat) {
      this.commonBindings = List.copyOf(commonBindings);
      this.commonRegionHoles = List.copyOf(commonRegionHoles);
      this.commonOutputFormat = commonOutputFormat;
    }

    /** Common bindings in first-variant order (never null). */
    public List<PSPageXmlModel.Binding> getCommonBindings() {
      return commonBindings;
    }

    /** Common region holes in first-variant order (never null). */
    public List<PSPageXmlModel.RegionHole> getCommonRegionHoles() {
      return commonRegionHoles;
    }

    /** Single distinct output format, or null when variants disagree or none declares one. */
    public String getCommonOutputFormat() {
      return commonOutputFormat;
    }

    /**
     * Stable canonical text for golden-file evidence: shared bindings, shared regions, shared
     * output format, then per-variant residual counts supplied by the caller.
     */
    public String toCanonicalText(List<VariantView> views) {
      StringBuilder sb = new StringBuilder();
      sb.append("shared.bindings:\n");
      for (PSPageXmlModel.Binding binding : commonBindings) {
        sb.append(binding.getVariable()).append(" = ").append(binding.getExpression()).append('\n');
      }
      sb.append("shared.regions:\n");
      for (PSPageXmlModel.RegionHole hole : commonRegionHoles) {
        sb.append(hole.getRegionId()).append('\n');
      }
      sb.append("shared.outputFormat: ")
          .append(commonOutputFormat != null ? commonOutputFormat : "<none>")
          .append('\n');
      sb.append("variants:\n");
      for (VariantView view : views) {
        sb.append("- ")
            .append(view.getVariantName())
            .append(" residualBindings=")
            .append(view.getResidualBindings().size())
            .append(" residualRegions=")
            .append(view.getResidualRegionHoles().size())
            .append('\n');
      }
      return sb.toString();
    }
  }

  /**
   * One template variant's view of the shared assembly: a reference to the single {@link
   * SharedAssembly} plus only this variant's residuals, alongside a deep copy of the original
   * model carrying every non-deduped field (name, source file, label, mime type, publish-when,
   * and friends). Position plans record, per original entry, the shared index ({@code >= 0})
   * or the residual index (encoded {@code -1 - idx}), so {@link #expand()} restores the exact
   * original model — entry order and untouched fields alike.
   */
  public static final class VariantView {
    private final String variantName;
    private final PSPageXmlModel original;
    private final SharedAssembly shared;
    private final List<PSPageXmlModel.Binding> residualBindings;
    private final List<Integer> bindingPlan;
    private final List<PSPageXmlModel.RegionHole> residualRegionHoles;
    private final List<Integer> regionPlan;

    private VariantView(
        String variantName,
        PSPageXmlModel original,
        SharedAssembly shared,
        List<PSPageXmlModel.Binding> residualBindings,
        List<Integer> bindingPlan,
        List<PSPageXmlModel.RegionHole> residualRegionHoles,
        List<Integer> regionPlan) {
      this.variantName = variantName;
      this.original = original;
      this.shared = shared;
      this.residualBindings = List.copyOf(residualBindings);
      this.bindingPlan = List.copyOf(bindingPlan);
      this.residualRegionHoles = List.copyOf(residualRegionHoles);
      this.regionPlan = List.copyOf(regionPlan);
    }

    static VariantView of(PSPageXmlModel variant, SharedAssembly shared)
        throws PSPageXmlException {
      List<PSPageXmlModel.Binding> residuals = new ArrayList<>();
      List<Integer> bindingPlan =
          planEntries(
              safeBindingsStatic(variant), shared.getCommonBindings(), residuals, VariantView::copyBindingStatic);
      List<PSPageXmlModel.RegionHole> residualRegions = new ArrayList<>();
      List<Integer> regionPlan =
          planEntries(
              safeRegionsStatic(variant),
              shared.getCommonRegionHoles(),
              residualRegions,
              VariantView::copyRegionStatic);
      // Display name only: the raw (possibly null) name travels inside the held original so a
      // nameless model does not gain one on expand().
      String display = variant.getName() != null ? variant.getName() : variant.pageStem();
      return new VariantView(
          display,
          copyModel(variant),
          shared,
          residuals,
          bindingPlan,
          residualRegions,
          regionPlan);
    }

    /** Variant template name (never null when the model has a name or source file). */
    public String getVariantName() {
      return variantName;
    }

    /** The single shared assembly every variant of this manifest references. */
    public SharedAssembly getShared() {
      return shared;
    }

    /** This variant's own bindings (shared entries excluded, original relative order). */
    public List<PSPageXmlModel.Binding> getResidualBindings() {
      return residualBindings;
    }

    /** This variant's own region holes (shared entries excluded, original relative order). */
    public List<PSPageXmlModel.RegionHole> getResidualRegionHoles() {
      return residualRegionHoles;
    }

    /** This variant's own declared output format (may be null). */
    public String getOutputFormat() {
      return original.getOutputFormat();
    }

    /**
     * Merge shared + residual state back into the full per-variant model in exact original
     * order, carrying every non-deduped field (name, source file, label, mime type,
     * publish-when, location prefix/suffix, and friends) from the held original, so the render
     * pipeline sees byte-identical inputs before and after dedupe.
     */
    public PSPageXmlModel expand() {
      PSPageXmlModel model = copyModel(original);
      List<PSPageXmlModel.Binding> bindings = new ArrayList<>(bindingPlan.size());
      for (int plan : bindingPlan) {
        if (plan >= 0) {
          bindings.add(copyBindingStatic(shared.getCommonBindings().get(plan)));
        } else {
          bindings.add(copyBindingStatic(residualBindings.get(-1 - plan)));
        }
      }
      model.setBindings(bindings);
      List<PSPageXmlModel.RegionHole> holes = new ArrayList<>(regionPlan.size());
      for (int plan : regionPlan) {
        if (plan >= 0) {
          holes.add(copyRegionStatic(shared.getCommonRegionHoles().get(plan)));
        } else {
          holes.add(copyRegionStatic(residualRegionHoles.get(-1 - plan)));
        }
      }
      model.setRegionHoles(holes);
      return model;
    }

    /**
     * Deep copy of a variant model: every scalar field plus deep copies of the entry lists, so
     * the view holds the original state minus nothing and {@link #expand()} cannot leak
     * mutations back into the caller's model.
     */
    private static PSPageXmlModel copyModel(PSPageXmlModel source) {
      PSPageXmlModel copy = new PSPageXmlModel();
      copy.setSourceFileName(source.getSourceFileName());
      copy.setName(source.getName());
      copy.setLabel(source.getLabel());
      copy.setDescription(source.getDescription());
      copy.setGuid(source.getGuid());
      copy.setAssembler(source.getAssembler());
      copy.setOutputFormat(source.getOutputFormat());
      copy.setTemplateType(source.getTemplateType());
      copy.setMimeType(source.getMimeType());
      copy.setCharset(source.getCharset());
      copy.setActiveAssemblyType(source.getActiveAssemblyType());
      copy.setPublishWhen(source.getPublishWhen());
      copy.setLocationPrefix(source.getLocationPrefix());
      copy.setLocationSuffix(source.getLocationSuffix());
      copy.setTemplateBody(source.getTemplateBody());
      List<PSPageXmlModel.Binding> bindings = new ArrayList<>();
      if (source.getBindings() != null) {
        for (PSPageXmlModel.Binding binding : source.getBindings()) {
          bindings.add(binding != null ? copyBindingStatic(binding) : null);
        }
      }
      copy.setBindings(bindings);
      List<PSPageXmlModel.RegionHole> holes = new ArrayList<>();
      if (source.getRegionHoles() != null) {
        for (PSPageXmlModel.RegionHole hole : source.getRegionHoles()) {
          holes.add(hole != null ? copyRegionStatic(hole) : null);
        }
      }
      copy.setRegionHoles(holes);
      return copy;
    }

    private static List<PSPageXmlModel.Binding> safeBindingsStatic(PSPageXmlModel variant) {
      return variant.getBindings() != null ? variant.getBindings() : List.of();
    }

    private static List<PSPageXmlModel.RegionHole> safeRegionsStatic(PSPageXmlModel variant) {
      return variant.getRegionHoles() != null ? variant.getRegionHoles() : List.of();
    }

    private static PSPageXmlModel.Binding copyBindingStatic(PSPageXmlModel.Binding source) {
      PSPageXmlModel.Binding copy = new PSPageXmlModel.Binding();
      copy.setVariable(source.getVariable());
      copy.setExpression(source.getExpression());
      return copy;
    }

    private static PSPageXmlModel.RegionHole copyRegionStatic(PSPageXmlModel.RegionHole source) {
      PSPageXmlModel.RegionHole copy = new PSPageXmlModel.RegionHole();
      copy.setRegionId(source.getRegionId());
      copy.setCssClass(source.getCssClass());
      copy.setLayoutHints(
          source.getLayoutHints() != null
              ? new LinkedHashMap<>(source.getLayoutHints())
              : new LinkedHashMap<>());
      copy.setStyleHints(
          source.getStyleHints() != null
              ? new LinkedHashMap<>(source.getStyleHints())
              : new LinkedHashMap<>());
      return copy;
    }
  }

  /** Model-level dedupe result for one manifest family. */
  public static final class DedupedModels {
    private final String manifestId;
    private final SharedAssembly shared;
    private final List<VariantView> variants;

    private DedupedModels(String manifestId, SharedAssembly shared, List<VariantView> variants) {
      this.manifestId = manifestId;
      this.shared = shared;
      this.variants = List.copyOf(variants);
    }

    /** Manifest family id (first variant stem; may be null). */
    public String getManifestId() {
      return manifestId;
    }

    /** Shared assembly referenced by every variant (single instance). */
    public SharedAssembly getShared() {
      return shared;
    }

    /** Per-variant views in input order (never null). */
    public List<VariantView> getVariants() {
      return variants;
    }
  }

  /**
   * Manifest-level shared assembly across one manifest's {@code templates[]} entries: common
   * bindings plus common install defaults (mime type, publish-when, location prefix/suffix).
   * Each default is null when the templates disagree or none declares it.
   */
  public static final class SharedTemplateAssembly {
    private final List<PSComponentPackageManifest.Binding> commonBindings;
    private final String commonMimeType;
    private final String commonPublishWhen;
    private final String commonLocationPrefix;
    private final String commonLocationSuffix;

    private SharedTemplateAssembly(
        List<PSComponentPackageManifest.Binding> commonBindings,
        String commonMimeType,
        String commonPublishWhen,
        String commonLocationPrefix,
        String commonLocationSuffix) {
      this.commonBindings = List.copyOf(commonBindings);
      this.commonMimeType = commonMimeType;
      this.commonPublishWhen = commonPublishWhen;
      this.commonLocationPrefix = commonLocationPrefix;
      this.commonLocationSuffix = commonLocationSuffix;
    }

    /** Common bindings in first-template order (never null). */
    public List<PSComponentPackageManifest.Binding> getCommonBindings() {
      return commonBindings;
    }

    /** Single distinct mime type, or null when templates disagree or none declares one. */
    public String getCommonMimeType() {
      return commonMimeType;
    }

    /** Single distinct publish-when, or null when templates disagree or none declares one. */
    public String getCommonPublishWhen() {
      return commonPublishWhen;
    }

    /** Single distinct location prefix, or null when templates disagree or none declares one. */
    public String getCommonLocationPrefix() {
      return commonLocationPrefix;
    }

    /** Single distinct location suffix, or null when templates disagree or none declares one. */
    public String getCommonLocationSuffix() {
      return commonLocationSuffix;
    }
  }

  /**
   * One manifest template's view of the shared assembly: a reference to the single {@link
   * SharedTemplateAssembly} plus only this template's residuals. The position plan records, per
   * original entry, the shared index ({@code >= 0}) or the residual index (encoded {@code -1 -
   * idx}), so {@link #expand()} restores the exact original {@code TemplateRef}.
   */
  public static final class TemplateVariantView {
    private final SharedTemplateAssembly shared;
    private final String name;
    private final String type;
    private final String assembler;
    private final String sourceRef;
    private final String contentType;
    private final String mimeType;
    private final String publishWhen;
    private final String locationSuffix;
    private final String locationPrefix;
    private final String legacyTemplateType;
    private final List<PSComponentPackageManifest.Binding> residualBindings;
    private final List<Integer> bindingPlan;

    private TemplateVariantView(
        SharedTemplateAssembly shared,
        String name,
        String type,
        String assembler,
        String sourceRef,
        String contentType,
        String mimeType,
        String publishWhen,
        String locationSuffix,
        String locationPrefix,
        String legacyTemplateType,
        List<PSComponentPackageManifest.Binding> residualBindings,
        List<Integer> bindingPlan) {
      this.shared = shared;
      this.name = name;
      this.type = type;
      this.assembler = assembler;
      this.sourceRef = sourceRef;
      this.contentType = contentType;
      this.mimeType = mimeType;
      this.publishWhen = publishWhen;
      this.locationSuffix = locationSuffix;
      this.locationPrefix = locationPrefix;
      this.legacyTemplateType = legacyTemplateType;
      this.residualBindings = List.copyOf(residualBindings);
      this.bindingPlan = List.copyOf(bindingPlan);
    }

    static TemplateVariantView of(
        PSComponentPackageManifest.TemplateRef template, SharedTemplateAssembly shared)
        throws PSPageXmlException {
      List<PSComponentPackageManifest.Binding> residuals = new ArrayList<>();
      List<Integer> plan =
          planEntries(
              template.getBindings() != null ? template.getBindings() : List.of(),
              shared.getCommonBindings(),
              residuals,
              PSPageXmlSharedSnippetDedupe::copyManifestBinding);
      return new TemplateVariantView(
          shared,
          template.getName(),
          template.getType(),
          template.getAssembler(),
          template.getSourceRef(),
          template.getContentType(),
          template.getMimeType(),
          template.getPublishWhen(),
          template.getLocationSuffix(),
          template.getLocationPrefix(),
          template.getLegacyTemplateType(),
          residuals,
          plan);
    }

    /** The single shared assembly every template of this manifest references. */
    public SharedTemplateAssembly getShared() {
      return shared;
    }

    /** This template's own bindings (shared entries excluded, original relative order). */
    public List<PSComponentPackageManifest.Binding> getResidualBindings() {
      return residualBindings;
    }

    /**
     * Merge shared + residual state back into the full template entry in exact original order,
     * so install/render sees byte-identical inputs before and after dedupe.
     */
    public PSComponentPackageManifest.TemplateRef expand() {
      PSComponentPackageManifest.TemplateRef template =
          new PSComponentPackageManifest.TemplateRef();
      template.setName(name);
      template.setType(type);
      template.setAssembler(assembler);
      template.setSourceRef(sourceRef);
      template.setContentType(contentType);
      template.setMimeType(mimeType);
      template.setPublishWhen(publishWhen);
      template.setLocationSuffix(locationSuffix);
      template.setLocationPrefix(locationPrefix);
      template.setLegacyTemplateType(legacyTemplateType);
      List<PSComponentPackageManifest.Binding> bindings = new ArrayList<>(bindingPlan.size());
      for (int plan : bindingPlan) {
        PSComponentPackageManifest.Binding source =
            plan >= 0 ? shared.getCommonBindings().get(plan) : residualBindings.get(-1 - plan);
        PSComponentPackageManifest.Binding copy = new PSComponentPackageManifest.Binding();
        copy.setVariable(source.getVariable());
        copy.setExpression(source.getExpression());
        bindings.add(copy);
      }
      template.setBindings(bindings);
      return template;
    }
  }

  /** Manifest-level dedupe result for one manifest's {@code templates[]} entries. */
  public static final class DedupedTemplates {
    private final SharedTemplateAssembly shared;
    private final List<TemplateVariantView> variants;

    private DedupedTemplates(SharedTemplateAssembly shared, List<TemplateVariantView> variants) {
      this.shared = shared;
      this.variants = List.copyOf(variants);
    }

    /** Shared assembly referenced by every template (single instance). */
    public SharedTemplateAssembly getShared() {
      return shared;
    }

    /** Per-template views in input order (never null). */
    public List<TemplateVariantView> getVariants() {
      return variants;
    }
  }
}
