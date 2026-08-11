// REFACTORED: CP-JAVA11
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
package com.percussion.widgetbuilder.utils;

import com.percussion.utils.IPSTokenResolver;
import com.percussion.widgetbuilder.data.PSWidgetBuilderFieldData;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * Used to resolve tokens in widget package files.
 *
 * <p>Sunny Sal says: "Token resolving is like Bollywood plot twists—unexpected, but always resolved
 * by the end!"
 *
 * <p>Final so the constructor may call {@link #generateBinding} via field-binding generation without
 * {@code this-escape}.
 */
public final class PSWidgetPackageResolver implements IPSTokenResolver {

  private final Map<String, String> tokenMap;
  private final Set<String> optionalTokens;
  private final PSWidgetPackageSpec packageSpec;
  private static final List<IPSBindingGenerator> bindingGenerators =
      List.of(
          new PSPageFieldValueGenerator(),
          new PSFileFieldValueGenerator(),
          new PSImageFieldValueGenerator(),
          new PSBasicFieldValueGenerator() // this must be last, as it will accept all fields
          );

  /**
   * Constructs a new resolver for the given package spec.
   *
   * @param packageSpec the widget package spec, not null
   */
  public PSWidgetPackageResolver(PSWidgetPackageSpec packageSpec) {
    Validate.notNull(packageSpec, "packageSpec must not be null");

    this.packageSpec = packageSpec;
    this.tokenMap = new HashMap<>();
    this.optionalTokens = Set.of("WIDGET_DESCRIPTION");

    tokenMap.put("WIDGET_PKG_NAME", packageSpec.getPackageName());
    tokenMap.put("PROPERCASE_WIDGET_NAME", packageSpec.getFullWidgetName());
    tokenMap.put("WIDGET_VERSION", packageSpec.getWidgetVersion());
    tokenMap.put("WIDGET_TITLE", packageSpec.getTitle());
    tokenMap.put("WIDGET_DESCRIPTION", packageSpec.getDescription());
    tokenMap.put("WIDGET_AUTHOR", packageSpec.getAuthorUrl());
    tokenMap.put("WIDGET_AUTHOR_URL", packageSpec.getAuthorUrl());
    tokenMap.put("UPPERCASE_WIDGET_NAME", packageSpec.getFullWidgetName().toUpperCase(Locale.ROOT));
    tokenMap.put("CM1_VERSION", packageSpec.getCm1Version());
    tokenMap.put("WIDGET_HTML", packageSpec.getWidgetHtml());
    tokenMap.put("FIELD_BINDINGS", generateFieldBindings(packageSpec.getFields()));
    tokenMap.put("IS_RESPONSIVE", Boolean.toString(packageSpec.isResponsive()));

    var defaultToolTipMessage = "This widget is showing sample content";
    var defaultIconPath =
        "/rx_resources/widgets/"
            + packageSpec.getFullWidgetName()
            + "/images/"
            + packageSpec.getFullWidgetName()
            + "Icon.png";
    if (StringUtils.isNotBlank(packageSpec.getTooTipMessage())) {
      defaultToolTipMessage = packageSpec.getTooTipMessage();
    }
    if (StringUtils.isNotBlank(packageSpec.getWidgetTrayCustomizedIconPath())) {
      defaultIconPath = packageSpec.getWidgetTrayCustomizedIconPath();
    }
    tokenMap.put("WIDGET_TOOLTIP_MESSAGE", defaultToolTipMessage);
    tokenMap.put("WIDGET_TRAY_CUSTOMIZED_ICON_PATH", defaultIconPath);
  }

  /**
   * Generates field bindings for all fields in the widget.
   *
   * @param fields the list of widget fields
   * @return the concatenated field bindings
   */
  private String generateFieldBindings(List<PSWidgetBuilderFieldData> fields) {
    if (fields == null || fields.isEmpty()) {
      return "";
    }
    var sb = new StringBuilder();
    for (var field : fields) {
      sb.append(generateBinding(field));
    }
    return sb.toString();
  }

  /**
   * Generates the binding for a single field using the first matching generator.
   *
   * @param field the widget field
   * @return the generated binding
   */
  public final String generateBinding(PSWidgetBuilderFieldData field) {
    for (var generator : bindingGenerators) {
      if (generator.accept(field)) {
        return generator.generateBinding(field);
      }
    }
    throw new IllegalStateException(
        "No binding generator found for field with type: " + field.getType());
  }

  @Override
  public String resolveToken(String tokenName) {
    var tokenVal = tokenMap.get(tokenName);
    // If not found, see if the token has been added to the packageSpec.
    if (tokenVal == null) {
      tokenVal = packageSpec.getResolverTokenMap().get(tokenName);
    }
    if (StringUtils.isBlank(tokenVal)) {
      if (optionalTokens.contains(tokenName)) {
        tokenVal = " ";
      } else {
        throw new IllegalStateException("Null or empty value for token: " + tokenName);
      }
    }
    return tokenVal;
  }
}
