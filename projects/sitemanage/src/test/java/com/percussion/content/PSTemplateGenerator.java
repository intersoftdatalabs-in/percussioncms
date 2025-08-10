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
package com.percussion.content;

import com.percussion.content.data.TemplateDef;
import com.percussion.pagemanagement.data.PSTemplate;
import com.percussion.pagemanagement.data.PSTemplateSummary;
import com.percussion.pagemanagement.web.service.PSTemplateServiceClient;
import com.percussion.sitemanage.service.PSSiteTemplates;
import com.percussion.sitemanage.service.PSSiteTemplates.CreateTemplate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PSTemplateGenerator extends PSGenerator<PSTemplateServiceClient> {

  private final PSSiteTemplateRestClient siteTemplateClient;
  private final PSWidgetGenerator widgetGen;

  public PSTemplateGenerator(String baseUrl, String uid, String pw) {
    super(PSTemplateServiceClient.class, baseUrl, uid, pw);
    siteTemplateClient = new PSSiteTemplateRestClient(baseUrl);
    widgetGen = new PSWidgetGenerator(baseUrl, uid, pw);
    siteTemplateClient.login(uid, pw);
  }

  public PSTemplateSummary findTemplateByName(String name) {
    var templateSums = getRestClient().findAll();
    PSTemplateSummary templateSum = null;
    for (var sum : templateSums) {
      if (sum.getName().equalsIgnoreCase(name)) {
        templateSum = sum;
        break;
      }
    }
    return templateSum;
  }

  public PSTemplate createTemplate(TemplateDef def) {
    log.info("Creating template " + def.getName() + " in site " + def.getSiteName());
    var themeName = "percussion";

    // a blank template needs to be created first, then it is updated with all its data
    var tpls = new PSSiteTemplates();
    var ctpls = new ArrayList<CreateTemplate>();
    tpls.setCreateTemplates(ctpls);
    var ct = new CreateTemplate();
    ctpls.add(ct);
    ct.setName(def.getName());
    ct.setSiteIds(Collections.singletonList(def.getSiteName()));
    var tsum = findTemplateByName(def.getBaseTemplateName());
    ct.setSourceTemplateId(tsum.getId());
    var sums = siteTemplateClient.save(tpls);

    var template = new PSTemplate();
    template.setAdditionalHeadContent(def.getAdditionalHeadContent());
    template.setAfterBodyStartContent(def.getAfterBodyStart());
    template.setBeforeBodyCloseContent(def.getBeforeBodyClose());
    template.setName(def.getName());
    template.setId(
        sums.get(0).getId()); // must use id, same name results in new template being created
    template.setSourceTemplateName(tsum.getName());
    template.setLabel(tsum.getLabel());
    template.setTheme(themeName);
    template.setReadOnly(false);

    var widgets = def.getWidget();
    Map<String, List<String>> regionToWidgets = widgetGen.parseRegionWidget(widgets);

    var resultWithRegion = getRestClient().save(template);
    widgetGen.createAndAssignWidgets(regionToWidgets, resultWithRegion, null);

    var result = getRestClient().save(resultWithRegion);

    widgetGen.linkContent(widgets, result, null);

    log.info("Created template " + result.getId());
    return result;
  }
}
