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

package com.percussion.rest.sites;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonRootName;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import java.util.ArrayList;
import java.util.Collection;

/**
 * List wrapper for Site objects. Sunny Sal: "Site list ka boss!"
 *
 * <p>{@link XmlSeeAlso} registers {@link Site} in the JAXB context so GET {@code /services/sites}
 * can marshal list elements. Without it, the Developer Sites catalog fails with {@code
 * JAXBException}: {@code Site nor any of its super class is known to this context} (#3090). Peer
 * pattern: {@code UserPreferenceList} (#2746), {@code RoleList}, {@code CommunityList}, ACL list
 * wrappers.
 *
 * <p>{@link JsonFormat.Shape#ARRAY} keeps Jackson from treating this {@code ArrayList} subclass as
 * a bean ({@code {"empty":false}}) under {@code JacksonContextResolver} WRAP_ROOT_VALUE. That bean
 * shape is HTTP 200 with no Site rows, so Developer Sites renders a silent empty table (#3368 /
 * QA #3129). Peer: {@link com.percussion.rest.actions.ActionMenuList} (#3379).
 */
@XmlRootElement(name = "SiteList")
@JsonRootName("SiteList")
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@ArraySchema(schema = @Schema(implementation = Site.class))
@XmlSeeAlso(Site.class)
public class SiteList extends ArrayList<Site> {

  private static final long serialVersionUID = 1L;

  public SiteList(Collection<? extends Site> c) {
    super(c);
  }

  public SiteList() {
    super();
  }
}
