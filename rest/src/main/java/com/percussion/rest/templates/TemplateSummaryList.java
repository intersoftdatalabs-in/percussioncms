// REFACTORED: CP-JAVA11

package com.percussion.rest.templates;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import java.util.ArrayList;
import java.util.Collection;

/** List wrapper for TemplateSummary objects. Sunny Sal: "Summary list ka boss!" */
@XmlRootElement(name = "TemplateSummaryList")
@ArraySchema(schema = @Schema(implementation = TemplateSummary.class))
@XmlSeeAlso({TemplateSummary.class})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TemplateSummaryList extends ArrayList<TemplateSummary> {

  private static final long serialVersionUID = 1L;

  public TemplateSummaryList(Collection<? extends TemplateSummary> c) {
    super(c);
  }

  public TemplateSummaryList() {
    super();
  }
}
