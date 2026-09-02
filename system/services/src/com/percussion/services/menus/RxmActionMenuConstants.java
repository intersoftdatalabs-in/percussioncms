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
package com.percussion.services.menus;

/**
 * Shared RXMENUACTION property names used by design-WS JDBC persist and the REST
 * action-menu adaptor. Keep a single spelling so DELETE fail-closed system-menu
 * detection cannot drift from create-time writes.
 */
public final class RxmActionMenuConstants {

  /**
   * Marker on REST/JDBC-created user menus ({@code RXMENUACTIONPROPERTIES}).
   */
  public static final String REST_USER_MENU_PROP = "sys_restUserMenu";

  private RxmActionMenuConstants() {}
}
