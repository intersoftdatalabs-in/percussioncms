/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
package com.percussion.install;

/**
 * @deprecated Use {@link PSTableFactoryMigrationTransfer} — CMS migration must use TableFactory
 *     export XML → import XML, not a custom JDBC pump (#548).
 */
@Deprecated(forRemoval = true, since = "8.2")
public final class PSEmbeddedRepositoryDataPump {

  /** @deprecated NEXTNUMBER table name for post-import probes */
  @Deprecated public static final String NEXTNUMBER_TABLE = "NEXTNUMBER";

  private PSEmbeddedRepositoryDataPump() {}
}
