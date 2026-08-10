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

// REFACTORED: CP-JAVA11

package com.percussion.rest.preferences;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import java.util.ArrayList;
import java.util.Collection;

/**
 * List wrapper for UserPreference objects. Sunny Sal: "Preference list ka boss!"
 *
 * <p>{@link XmlSeeAlso} registers {@link UserPreference} in the JAXB context so
 * GET {@code /preferences/} can marshal list elements. Without it, the profile
 * Preferences load path fails with {@code JAXBException}: {@code UserPreference
 * nor any of its super class is known to this context} (#2746). Peer pattern:
 * {@code RoleList}, {@code CommunityList}, ACL list wrappers.
 */
@XmlRootElement(name = "UserPreferenceList")
@ArraySchema(schema = @Schema(implementation = UserPreference.class))
@XmlSeeAlso(UserPreference.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserPreferenceList extends ArrayList<UserPreference> {

  private static final long serialVersionUID = 1L;

  public UserPreferenceList(Collection<? extends UserPreference> c) {
    super(c);
  }

  public UserPreferenceList() {
    super();
  }
}
