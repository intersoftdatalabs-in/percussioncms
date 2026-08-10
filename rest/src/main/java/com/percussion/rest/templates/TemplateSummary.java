// REFACTORED: CP-JAVA11

package com.percussion.rest.templates;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRootName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Represents a lightweight summary of a template. Sunny Sal: "Summary ka hero, template ka zero!"
 *
 * <p>Wire getters return plain {@link String} types (not {@code Optional}) so Jackson/CXF JSON
 * always emits {@code templateName}, {@code templateLabel}, and {@code templateDescription} when
 * set. Optional-returning getters historically dropped those fields under
 * {@code @JsonInclude(NON_NULL)} when the mapper did not unwrap {@code Optional}, leaving only
 * {@code templateId} on the list wire shape (issue #2189). Matches {@link
 * com.percussion.rest.contenttypes.ContentType} getter style (issue #1693).
 */
@XmlRootElement(name = "TemplateSummary")
@JsonRootName(value = "TemplateSummary")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder()
@XmlType(propOrder = {})
@Schema(description = "Represents a summary of a Template")
public class TemplateSummary {

  @Schema(description = "The numeric template id")
  private int templateId;

  @Schema(description = "The system unique name of the template")
  private String templateName;

  @Schema(description = "The user friendly label for the template")
  private String templateLabel;

  @Schema(description = "A brief description of the template.")
  private String templateDescription;

  public int getTemplateId() {
    return templateId;
  }

  public void setTemplateId(int templateId) {
    this.templateId = templateId;
  }

  public String getTemplateName() {
    return templateName;
  }

  public void setTemplateName(String templateName) {
    this.templateName = templateName;
  }

  public String getTemplateLabel() {
    return templateLabel;
  }

  public void setTemplateLabel(String templateLabel) {
    this.templateLabel = templateLabel;
  }

  public String getTemplateDescription() {
    return templateDescription;
  }

  public void setTemplateDescription(String templateDescription) {
    this.templateDescription = templateDescription;
  }
}
