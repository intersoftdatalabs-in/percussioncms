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

package com.percussion.tablefactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

/** Helper that collects constants and small utilities used by the DB import/export tools. */
public class PSJdbcImportExportHelper {

  /** No-op constructor. */
  public PSJdbcImportExportHelper() {
    // no-op
  }

  /** Command line option indicating a database export should be performed. */
  public static String OPTION_DB_EXPORT = "-dbexport";

  /** Command line option indicating a database import should be performed. */
  public static String OPTION_DB_IMPPORT = "-dbimport";

  /** Command line option carrying the path to the JDBC properties file. */
  public static String OPTION_DB_PROPS = "-dbprops";

  /** Command line option carrying the storage path used during import/export. */
  public static String OPTION_STORAGE_PATH = "-storagepath";

  /** Command line option carrying a comma-separated list of tables to skip. */
  public static String OPTION_TABLES_TO_SKIP = "-tablestoskip";

  /** Key under which the selected db option is stored in the parsed options map. */
  public static String DB_OPTION = "dboption";

  /** Default folder name used for storing text data XML files. */
  public static String DEF_DATA_FOLDER = "defData";

  /** Default folder name used for storing binary data files. */
  public static String BINARY_DATA_FOLDER = "binaryData";

  /** Prefix used for binary data bucket directories. */
  public static String BINARY_DATA_BUCKET = "bucket";

  /** Initial binary data bucket directory name (bucket_0). */
  public static String BINARY_DATA_INITIAL_BUCKET = BINARY_DATA_BUCKET + "_0";

  /** The set of option names that must always be supplied to {@link #getOptions(String[])}. */
  public static Set<String> requiredParams =
      new HashSet<>(Arrays.asList(OPTION_DB_PROPS, OPTION_STORAGE_PATH));

  /**
   * Maximum number of files allowed per storage folder before the import/export tooling splits
   * output across folders.
   */
  public static int MAX_FILES_IN_FILDER = 500;

  /**
   * Tables that should be skipped during import/export, parsed from {@link #OPTION_TABLES_TO_SKIP}.
   */
  public static List<String> tablesToSkip = new ArrayList<>();

  /**
   * Import on MySql is choking on import if the limitSizeForIndex value is set to true for
   * DPL_ID_MAPPING mapping. Added this map to check the table while cataloging and update the
   * value. The key is the table name, the value is colon (:) separated list of column names. For
   * example limitSizeForIndexMap.put("DPL_ID_MAPPING", "REPOSITORY_ID"); If there are multiple
   * columns that needs to be set with true value for limit size.
   * limitSizeForIndexMap.put("DPL_ID_MAPPING", "REPOSITORY_ID:ID_MAP");
   */
  public static Map<String, String> limitSizeForIndexMap = new HashMap<>();

  static {
    limitSizeForIndexMap.put("DPL_ID_MAPPING", "REPOSITORY_ID");
  }

  public static Map<String, String> getOptions(String[] options) {
    if (options == null || options.length < 1) {
      throw new IllegalArgumentException("options must not be null or empty");
    }
    String optionType = StringUtils.defaultString(options[0]);
    if (!(OPTION_DB_EXPORT.equals(optionType) || OPTION_DB_IMPPORT.equals(optionType))) {
      throw new IllegalArgumentException(
          "first option must be either " + OPTION_DB_EXPORT + " or " + OPTION_DB_IMPPORT);
    }
    Map<String, String> optionsMap = new HashMap<>();
    optionsMap.put(DB_OPTION, optionType);
    for (int i = 1; i < options.length; i = i + 2) {
      String key = options[i];
      String value = options[i + 1];
      optionsMap.put(key, value);
    }
    if (!CollectionUtils.isSubCollection(requiredParams, optionsMap.keySet())) {
      out("Required parameters are missing");
      usage(optionType);
    }
    // validate props file
    File propsFile = new File(optionsMap.get(OPTION_DB_PROPS));
    if (!propsFile.exists()) {
      out(OPTION_DB_PROPS + " option value must be a valid file.");
      usage(optionType);
    }
    // validate storage path
    File storageFolder = new File(optionsMap.get(OPTION_STORAGE_PATH));
    if (!storageFolder.exists() || !storageFolder.isDirectory()) {
      out(OPTION_STORAGE_PATH + " option value must be a valid directory.");
      usage(optionType);
    }
    // Assign skip tables
    String tablesToSkipOption = optionsMap.get(OPTION_TABLES_TO_SKIP);
    if (StringUtils.isNotBlank(tablesToSkipOption)) {
      tablesToSkip = Arrays.asList(tablesToSkipOption.split(","));
      for (int i = 0; i < tablesToSkip.size() - 1; i++) {
        if (tablesToSkip.get(i) != null) {
          tablesToSkip.set(i, tablesToSkip.get(i).trim());
        }
      }
    }
    return optionsMap;
  }

  /**
   * Recursively find a file by name.
   *
   * @param file the file or directory under which to search, never {@code null}
   * @param search the file name to search for
   * @return the matching file or {@code null} if no match was found
   */
  public static File findFile(File file, String search) {
    if (file.isDirectory()) {
      File[] arr = file.listFiles();
      for (File f : arr) {
        File found = findFile(f, search);
        if (found != null) return found;
      }
    } else {
      if (file.getName().equals(search)) {
        return file;
      }
    }
    return null;
  }

  /**
   * Returns the next numbered sibling bucket directory, creating it if necessary.
   *
   * @param currBucket the current bucket directory, never {@code null}
   * @return a new directory whose name follows the {@code <prefix>_<n+1>} pattern
   */
  public static File getNextBucket(File currBucket) {
    String[] bucketParts = currBucket.getName().split("_");
    int nextBucketNumber = Integer.parseInt(bucketParts[1]) + 1;
    File newBucket = new File(currBucket.getParentFile(), bucketParts[0] + "_" + nextBucketNumber);
    newBucket.mkdirs();
    return newBucket;
  }

  /**
   * Print command line usage for the supplied option.
   *
   * @param dboption assumed to be either -dbexport or -dbimport.
   */
  private static void usage(String dboption) {
    if (OPTION_DB_EXPORT.equals(dboption)) {
      out(
          "Usage: java com.percussion.tablefactory.tools.PSCatalogTableData "
              + OPTION_DB_PROPS
              + " <properties_file_name> "
              + OPTION_STORAGE_PATH
              + " <storage_folder_location>"
              + "["
              + OPTION_TABLES_TO_SKIP
              + " <tables_to_skip_options>]");
    } else {
      out(
          "Usage: java com.percussion.tablefactory.PSJdbcTableFactory "
              + OPTION_DB_PROPS
              + " <properties_file_name> "
              + OPTION_STORAGE_PATH
              + " <storage_folder_location>");
    }
    out("Where:");
    out("properties_file_name - path to the properties file defining the");
    out("    backend database server.  Required.");
    out(
        "storage_folder_location - path to folder under which the data, def and binary files are"
            + " stored");
    out("    Required.");
    if (OPTION_DB_EXPORT.equals(dboption)) {
      out(
          "tables_to_skip_options - pipe (|) seperated list of tables to skip, <tablename>-data.sql"
              + " files are created for these tables.");
      out("Example:");
      out(
          "com.percussion.tablefactory.tools.PSCatalogTableData -dbprops serverProps.properties"
              + " -storagepath e:/Rhythmyx/dataexport -tablestoskip"
              + " PSX_EDITION_TASK_PARAM|PSX_PUBSERVER_PROPERTIES");
    } else {
      out("Example:");
      out(
          "com.percussion.tablefactory.PSJdbcTableFactory "
              + "-dbprops serverProps.properties -storagepath e:/Rhythmyx/dataexport");
    }
    System.exit(1);
  }

  private static void out(String s) {
    System.out.println(s);
  }
}
