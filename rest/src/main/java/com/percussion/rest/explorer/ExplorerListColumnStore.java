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
package com.percussion.rest.explorer;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory column overlay for the current server process, keyed by user and folder path. Not
 * durable across restart — the Explorer list session (#4722).
 */
public final class ExplorerListColumnStore {

  private final ConcurrentHashMap<String, List<String>> columnsByUserAndFolder =
      new ConcurrentHashMap<>();

  public void put(String user, String folderPath, List<String> columns) {
    columnsByUserAndFolder.put(key(user, folderPath), List.copyOf(columns));
  }

  public List<String> get(String user, String folderPath) {
    List<String> found = columnsByUserAndFolder.get(key(user, folderPath));
    return found == null ? List.of() : List.copyOf(found);
  }

  static String key(String user, String folderPath) {
    return user.trim().toLowerCase(Locale.ROOT) + "\u0000" + folderPath;
  }
}
