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

package com.percussion.services.sitemgr.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.persistence.Column;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

/**
 * Pins the {@code secrets.heuristic} adjudication on {@code PSSite} line 257
 * (issue #6728): {@code RESET_REQUEST_PASSWORD_PAGE} is a JPA {@code @Column}
 * schema identifier for the site's password-reset-request <em>page path</em>,
 * in the same page-reference family as {@code LOGIN_PAGE},
 * {@code REGISTRATION_PAGE} and {@code RESET_PAGE}. It holds no credential
 * material; renaming the column would break the dormant schema mapping, so
 * the mapping is pinned here.
 */
class PSSiteResetPageColumnMappingTest {

  @Test
  void resetRequestPasswordPageColumnIsSchemaIdentifier() throws Exception {
    Field field = PSSite.class.getDeclaredField("resetRequestPasswordPage");
    assertEquals(
        String.class, field.getType(), "resetRequestPasswordPage is a page-path String");
    Column column = field.getAnnotation(Column.class);
    assertNotNull(column, "resetRequestPasswordPage must stay @Column-mapped");
    assertEquals(
        "RESET_REQUEST_PASSWORD_PAGE",
        column.name(),
        "column name is a DB schema identifier, not a hardcoded secret");
  }

  @Test
  void resetRequestPasswordPageRoundTripsAsPagePath() {
    PSSite site = new PSSite();
    site.setResetRequestPasswordPage("/sites/mysite/reset-request.html");
    assertEquals("/sites/mysite/reset-request.html", site.getResetRequestPasswordPage());
  }

  @Test
  void siblingPageColumnsSharePagePathFamily() throws Exception {
    assertEquals("LOGIN_PAGE", columnNameOf("loginPage"));
    assertEquals("REGISTRATION_PAGE", columnNameOf("registrationPage"));
    assertEquals("RESET_PAGE", columnNameOf("resetPage"));
  }

  private static String columnNameOf(String fieldName) throws Exception {
    Field field = PSSite.class.getDeclaredField(fieldName);
    Column column = field.getAnnotation(Column.class);
    assertNotNull(column, fieldName + " must stay @Column-mapped");
    return column.name();
  }
}
