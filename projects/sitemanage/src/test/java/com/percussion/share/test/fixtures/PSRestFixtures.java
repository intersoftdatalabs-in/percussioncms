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

package com.percussion.share.test.fixtures;

import com.percussion.pagemanagement.data.PSTemplate;
import com.percussion.pagemanagement.data.PSTemplateSummary;
import com.percussion.share.IPSSitemanageConstants;
import com.percussion.sitemanage.data.PSSite;
import com.percussion.system.utils.IPSHtmlParameters;
import jakarta.xml.bind.JAXB;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import org.junit.jupiter.api.Assertions;

/**
 * REST test fixtures for Percussion CMS integration tests. Sunny Sal: "REST fixtures, Java 11, and
 * test ka hero!"
 */
public class PSRestFixtures {
  private WebTarget r;
  private Client c;

  public static final String siteServiceRoot = "services/sitemanage/site";
  public static final String templateServiceRoot = "services/pagemanagement/template/";
  public static final String SITE_NAME_PREFIX = "restFixtures";

  public PSRestFixtures(Client c, WebTarget r) {
    this.c = c;
    this.r = r;
  }

  public void createSite() {
    var templates = findTemplates();
    var defaultTemplateId = templates.get(0).getId();
    var template = createTemplate(SITE_NAME_PREFIX + "1", defaultTemplateId);

    var wr = r.path(siteServiceRoot);
    var site = new PSSite();
    site.setName(SITE_NAME_PREFIX + "--" + System.currentTimeMillis());
    site.setLabel("My test site");
    site.setHomePageTitle("homePageTitle");
    site.setNavigationTitle("navigationTitle");
    site.setBaseTemplateName(IPSSitemanageConstants.PLAIN_BASE_TEMPLATE_NAME);
    site.setTemplateName(template.getName());

    var sw = new StringWriter();
    JAXB.marshal(site, sw);
    System.out.println("output=" + sw.getBuffer().toString());

    var response =
        getBuilder(wr, c)
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.entity(site, MediaType.APPLICATION_JSON_TYPE));

    Assertions.assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  public List<PSTemplateSummary> findTemplates() {
    var wr = r.path(templateServiceRoot).path("summary/all/readonly");
    var summaries =
        getBuilder(wr, c).accept(MediaType.APPLICATION_JSON_TYPE).get(PSTemplateSummary[].class);
    return Arrays.asList(summaries);
  }

  public PSTemplate createTemplate(String name, String srcId) {
    var wr = r.path(templateServiceRoot).path("create").path(name).path(srcId);
    return getBuilder(wr, c).accept(MediaType.APPLICATION_JSON_TYPE).get(PSTemplate.class);
  }

  protected Builder getBuilder(WebTarget wr, Client client, String userName) {
    var b = wr.request(MediaType.APPLICATION_JSON_TYPE);
    addAuth(b, userName, "demo");
    return wr.request(MediaType.APPLICATION_JSON_TYPE)
        .header(IPSHtmlParameters.SYS_USE_BASIC_AUTH, Boolean.TRUE);
  }

  protected Builder getBuilder(WebTarget wr, String userName) {
    var b = wr.request(MediaType.APPLICATION_JSON_TYPE);
    addAuth(b, userName, "demo");
    return wr.request(MediaType.APPLICATION_JSON_TYPE)
        .header(IPSHtmlParameters.SYS_USE_BASIC_AUTH, Boolean.TRUE);
  }

  protected Builder getBuilder(WebTarget wr) {
    var b = wr.request(MediaType.APPLICATION_JSON_TYPE);
    addAuth(b, "Admin", "demo");
    return b.header(IPSHtmlParameters.SYS_USE_BASIC_AUTH, Boolean.TRUE);
  }

  protected static Builder getBuilder(WebTarget wr, Client client) {
    var b = wr.request(MediaType.APPLICATION_JSON_TYPE);
    addAuth(b, "Admin", "demo");
    return wr.request(MediaType.APPLICATION_JSON_TYPE)
        .header(IPSHtmlParameters.SYS_USE_BASIC_AUTH, Boolean.TRUE);
  }

  private static Builder addAuth(Invocation.Builder b, String username, String password) {
    var usernameAndPassword = username + ":" + password;
    var authorizationHeaderValue =
        "Basic " + java.util.Base64.getEncoder().encodeToString(usernameAndPassword.getBytes());
    b.header("Authorization", authorizationHeaderValue);
    return b;
  }
}
