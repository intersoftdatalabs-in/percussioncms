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
package com.percussion.pathmanagement.data;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.pathmanagement.data.PSFolderPermission.Principal;
import com.percussion.pathmanagement.data.PSFolderPermission.PrincipalType;
import com.percussion.sitemanage.json.JacksonContextResolver;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Folder Security GET must serialize ROLE identities and properties without dropping them (#3206).
 */
@Tag("UnitTest")
class PSFolderPropertiesJacksonTest {

  @Test
  void serializesRoleAdminPrincipalAndLocale() {
    ObjectMapper mapper = new JacksonContextResolver().getContext(PSFolderProperties.class);
    PSFolderProperties props = new PSFolderProperties();
    props.setId("16777215-101-703");
    props.setName("Design");
    props.setLocale("en-us");
    props.setCommunityId(-1);
    props.setCommunityName("Default");
    props.setDisplayFormatName("FolderList");
    props.setWorkflowId(-1);

    Principal adminRole = new Principal();
    adminRole.setName("Admin");
    adminRole.setType(PrincipalType.ROLE);
    PSFolderPermission permission = new PSFolderPermission();
    permission.setAccessLevel(PSFolderPermission.Access.ADMIN);
    permission.setAdminPrincipals(new ArrayList<>(List.of(adminRole)));
    props.setPermission(permission);

    String json = mapper.writeValueAsString(props);
    assertTrue(json.contains("\"FolderProperties\""), json);
    assertTrue(json.contains("Admin"), json);
    assertTrue(json.contains("ROLE") || json.contains("Role"), json);
    assertTrue(json.contains("en-us"), json);
    assertTrue(json.contains("FolderList"), json);
  }
}
