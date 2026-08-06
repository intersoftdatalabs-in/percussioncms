/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 *
 * See the License for the specific language governing permissions and limitations under the
 * License.
 */
package com.percussion.preinstall;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Resolves CMS repository database targeting for new CLI installs.
 *
 * <p>Supports:
 *
 * <ul>
 *   <li>{@code -Ddbprops=} / {@code --dbprops=} property files in {@code rxrepository.properties}
 *       format (issue #949)
 *   <li>Structured {@code --db.*} CLI / env-file / environment inputs (existing automation surface)
 *   <li>Default embedded H2 when no override is supplied (GitHub #548; Derby retained for
 *       migration)
 * </ul>
 *
 * <p><strong>Security:</strong> never log or print {@code PWD} / password field values in error
 * messages.
 */
public final class DbInstallConfigResolver {

  /** Default embedded engine type after Apache Derby retirement (#548). */
  public static final String DB_TYPE_DEFAULT = "h2";

  /** Default SSL enabled value for new installs. */
  public static final String DB_SSL_ENABLED_DEFAULT = "true";

  /** Default SSL verify value for new installs. */
  public static final String DB_SSL_VERIFY_DEFAULT = "true";

  /** Default SSL allow-self-signed value for new installs. */
  public static final String DB_SSL_ALLOW_SELF_SIGNED_DEFAULT = "false";

  /** Issue #949 system property / CLI key for repository properties file path. */
  public static final String DBPROPS_KEY = "dbprops";

  /** CLI key for silent/non-interactive mode (--silent or --no-tty). */
  public static final String SILENT_KEY = "silent";

  /**
   * CLI key for the optional "install sample sites" flag. Maps to {@code --demo-sites} / {@code
   * --no-demo-sites} and the {@link #DEMO_SITES_SYSTEM_PROPERTY} system property. Default {@code
   * false}. Sample seeding only runs on new installs; upgrades ignore the flag and never overwrite
   * an existing repository with the seed XML.
   */
  public static final String DEMO_SITES_KEY = "demo-sites";

  /** ANT-visible system property propagated by {@code Main.execJar}. */
  public static final String DEMO_SITES_SYSTEM_PROPERTY = "install.demo.sites";

  private static final String MARIADB_DRIVER_CLASS = "org.mariadb.jdbc.Driver";
  private static final String MYSQL_COMPAT_DRIVER_NAME = "mysql";
  private static final String MSSQL_DRIVER_CLASS = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
  private static final String ORACLE_DRIVER_CLASS = "oracle.jdbc.OracleDriver";
  private static final String ORACLE_DRIVER_NAME = "oracle:thin";

  /** PostgreSQL JDBC (#1500). */
  private static final String POSTGRES_DRIVER_CLASS = "org.postgresql.Driver";

  private static final String POSTGRES_DRIVER_NAME = "postgresql";
  private static final String POSTGRES_DEFAULT_SCHEMA = "public";

  private DbInstallConfigResolver() {}

  /**
   * Parse installer CLI arguments: first non-{@code --} token is install path; {@code --key=value}
   * and {@code --key value} options fill the options map.
   *
   * @param args raw CLI arguments
   * @return parsed install path and options (install path may be null if omitted)
   */
  public static ParsedArgs parseArgs(String[] args) {
    Path installPath = null;
    Map<String, String> options = new HashMap<>();

    int index = 0;
    while (index < args.length) {
      String argument = args[index];
      if (!argument.startsWith("--")) {
        if (installPath == null) {
          installPath = Paths.get(argument);
        }
        index++;
        continue;
      }

      String option = argument.substring(2);
      String key;
      String value;
      int equalsPosition = option.indexOf('=');
      if (equalsPosition > -1) {
        key = option.substring(0, equalsPosition).trim();
        value = option.substring(equalsPosition + 1).trim();
      } else {
        key = option.trim();
        if (index + 1 < args.length && !args[index + 1].startsWith("--")) {
          value = args[index + 1].trim();
          index++;
        } else {
          value = "true";
        }
      }

      if (!key.isEmpty()) {
        options.put(key, value);
      }
      index++;
    }

    return new ParsedArgs(installPath, options);
  }

  /**
   * Resolve database configuration into {@code perc.db.*} system properties for the ANT install
   * JVM.
   *
   * @param cliOptions options from {@link #parseArgs(String[])}; may be empty
   * @return resolved config (never null)
   * @throws IllegalArgumentException when validation fails (messages must not contain secrets)
   */
  public static ResolvedDbConfig resolveDbConfig(Map<String, String> cliOptions) {
    Map<String, String> options = cliOptions != null ? cliOptions : Map.of();

    String dbpropsPath =
        firstNonBlank(
            options.get(DBPROPS_KEY),
            options.get("db.props"),
            System.getProperty(DBPROPS_KEY),
            System.getProperty("db.props"));

    if (!isBlank(dbpropsPath)) {
      return resolveFromDbpropsFile(dbpropsPath, options);
    }

    return resolveFromStructuredInput(options);
  }

  private static ResolvedDbConfig resolveFromDbpropsFile(
      String dbpropsPath, Map<String, String> cliOptions) {
    Path path = Paths.get(dbpropsPath);
    if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
      throw new IllegalArgumentException(
          "dbprops file not found or not readable: " + path.toAbsolutePath());
    }

    Properties fileProps = new Properties();
    try (InputStream in = Files.newInputStream(path)) {
      fileProps.load(in);
    } catch (IOException e) {
      throw new IllegalArgumentException(
          "Unable to read dbprops file: " + path.toAbsolutePath(), e);
    }

    String backendRaw = firstNonBlank(fileProps.getProperty("DB_BACKEND"), "");
    String dbType = normalizeBackendToDbType(backendRaw);

    Map<String, String> envFileValues = loadEnvFileIfConfigured(cliOptions);

    String sslEnabled =
        normalizeBoolean(
            firstNonBlank(
                fileProps.getProperty("DB_SSL_ENABLED"),
                getConfigValue(
                    "db.ssl.enabled", cliOptions, envFileValues, DB_SSL_ENABLED_DEFAULT)),
            "db.ssl.enabled");
    String sslVerify =
        normalizeBoolean(
            firstNonBlank(
                fileProps.getProperty("DB_SSL_VERIFY"),
                getConfigValue("db.ssl.verify", cliOptions, envFileValues, DB_SSL_VERIFY_DEFAULT)),
            "db.ssl.verify");
    String sslAllowSelfSigned =
        normalizeBoolean(
            firstNonBlank(
                fileProps.getProperty("DB_SSL_ALLOW_SELF_SIGNED"),
                getConfigValue(
                    "db.ssl.allowSelfSigned",
                    cliOptions,
                    envFileValues,
                    DB_SSL_ALLOW_SELF_SIGNED_DEFAULT)),
            "db.ssl.allowSelfSigned");

    Map<String, String> systemProperties = new HashMap<>();
    systemProperties.put("perc.db.type", dbType);
    systemProperties.put("perc.db.ssl.enabled", sslEnabled);
    systemProperties.put("perc.db.ssl.verify", sslVerify);
    systemProperties.put("perc.db.ssl.allowSelfSigned", sslAllowSelfSigned);

    if (Objects.equals(dbType, "derby") || Objects.equals(dbType, "h2")) {
      // Embedded defaults: product ship props / TableFactory; no enterprise CMS field mapping
      return new ResolvedDbConfig(systemProperties, "dbprops");
    }

    String server = trimToNull(fileProps.getProperty("DB_SERVER"));
    String name = trimToNull(fileProps.getProperty("DB_NAME"));
    String schema = trimToNull(fileProps.getProperty("DB_SCHEMA"));
    String driverName = trimToNull(fileProps.getProperty("DB_DRIVER_NAME"));
    String driverClass = trimToNull(fileProps.getProperty("DB_DRIVER_CLASS_NAME"));
    String user = trimToNull(fileProps.getProperty("UID"));
    String password = trimToNull(fileProps.getProperty("PWD"));

    List<String> missing = new ArrayList<>();
    if (server == null) {
      missing.add("DB_SERVER");
    }
    if (user == null) {
      missing.add("UID");
    }
    if (password == null) {
      missing.add("PWD");
    }
    if (driverName == null) {
      missing.add("DB_DRIVER_NAME");
    }
    if (driverClass == null) {
      missing.add("DB_DRIVER_CLASS_NAME");
    }
    if ((Objects.equals(dbType, "mysql")
            || Objects.equals(dbType, "sqlserver")
            || Objects.equals(dbType, "postgresql"))
        && name == null) {
      missing.add("DB_NAME");
    }
    if (!missing.isEmpty()) {
      throw new IllegalArgumentException(
          "Missing required database properties in dbprops file for DB_BACKEND="
              + backendRaw
              + ": "
              + String.join(", ", missing));
    }

    String resolvedSchema = schema;
    if (resolvedSchema == null) {
      if (Objects.equals(dbType, "sqlserver")) {
        // SQL Server default schema is lowercase "dbo" (product convention)
        resolvedSchema = "dbo";
      } else if (Objects.equals(dbType, "postgresql")) {
        resolvedSchema = POSTGRES_DEFAULT_SCHEMA;
      } else {
        resolvedSchema = "";
      }
    }

    String backendLabel = backendLabelForType(dbType);
    systemProperties.put("perc.db.cms.backend", backendLabel);
    systemProperties.put("perc.db.cms.driverName", driverName);
    systemProperties.put("perc.db.cms.driverClass", driverClass);
    systemProperties.put("perc.db.cms.server", server);
    if (name != null) {
      systemProperties.put("perc.db.cms.name", name);
    } else {
      systemProperties.put("perc.db.cms.name", "");
    }
    systemProperties.put("perc.db.cms.schema", resolvedSchema);
    systemProperties.put("perc.db.user", user);
    systemProperties.put("perc.db.password", password);

    applyDtsHintsFromCms(
        systemProperties, dbType, server, name, resolvedSchema, driverClass, sslEnabled, sslVerify);

    return new ResolvedDbConfig(systemProperties, "dbprops");
  }

  private static ResolvedDbConfig resolveFromStructuredInput(Map<String, String> cliOptions) {
    Map<String, String> environmentFileValues = loadEnvFileIfConfigured(cliOptions);

    String dbType = getConfigValue("db.type", cliOptions, environmentFileValues, null);
    if (isBlank(dbType)) {
      dbType = DB_TYPE_DEFAULT;
    }
    dbType = normalizeStructuredDbType(dbType);

    String sslEnabled =
        normalizeBoolean(
            getConfigValue(
                "db.ssl.enabled", cliOptions, environmentFileValues, DB_SSL_ENABLED_DEFAULT),
            "db.ssl.enabled");
    String sslVerify =
        normalizeBoolean(
            getConfigValue(
                "db.ssl.verify", cliOptions, environmentFileValues, DB_SSL_VERIFY_DEFAULT),
            "db.ssl.verify");
    String sslAllowSelfSigned =
        normalizeBoolean(
            getConfigValue(
                "db.ssl.allowSelfSigned",
                cliOptions,
                environmentFileValues,
                DB_SSL_ALLOW_SELF_SIGNED_DEFAULT),
            "db.ssl.allowSelfSigned");

    String host = getConfigValue("db.host", cliOptions, environmentFileValues, null);
    String port = getConfigValue("db.port", cliOptions, environmentFileValues, null);
    String name = getConfigValue("db.name", cliOptions, environmentFileValues, null);
    String schema = getConfigValue("db.schema", cliOptions, environmentFileValues, null);
    String user = getConfigValue("db.user", cliOptions, environmentFileValues, null);
    String password = getConfigValue("db.password", cliOptions, environmentFileValues, null);

    if (!Objects.equals(dbType, "derby") && !Objects.equals(dbType, "h2")) {
      List<String> missing = new ArrayList<>();
      if (isBlank(host)) {
        missing.add("db.host");
      }
      if (isBlank(port)) {
        missing.add("db.port");
      }
      // Oracle uses db.name as service name / SID in composed DB_SERVER (@host:port:name)
      if (isBlank(name)) {
        missing.add("db.name");
      }
      if (isBlank(user)) {
        missing.add("db.user");
      }
      if (isBlank(password)) {
        missing.add("db.password");
      }
      if (!missing.isEmpty()) {
        throw new IllegalArgumentException(
            "Missing required database parameters for db.type="
                + dbType
                + ": "
                + String.join(", ", missing));
      }
    }

    Map<String, String> systemProperties = new HashMap<>();
    systemProperties.put("perc.db.type", dbType);
    systemProperties.put("perc.db.ssl.enabled", sslEnabled);
    systemProperties.put("perc.db.ssl.verify", sslVerify);
    systemProperties.put("perc.db.ssl.allowSelfSigned", sslAllowSelfSigned);

    // Embedded H2: pull the operator-confirmed password from the structured
    // options map (interactive installs) and surface it as cmdb.password so
    // ANT can use it as the H2 DB password. Persistence to
    // var/config/generated/passwords is the installer's responsibility (see
    // Main.execJar — it has the install root). Silent installs fall through to
    // ANT's PSGenerateRepositoryPassword (random mode), which writes its own
    // cmdb.password ANT property and persists the value.
    String embeddedH2Password = null;
    if ("h2".equals(dbType)) {
      embeddedH2Password =
          getConfigValue(
              InteractiveDbConfigCollector.EMBEDDED_H2_DB_PASSWORD_KEY,
              cliOptions,
              environmentFileValues,
              null);
    }
    if (!isBlank(embeddedH2Password)) {
      systemProperties.put("cmdb.password", embeddedH2Password);
    }

    setIfPresent(systemProperties, "perc.db.host", host);
    setIfPresent(systemProperties, "perc.db.port", port);
    setIfPresent(systemProperties, "perc.db.name", name);
    setIfPresent(systemProperties, "perc.db.schema", schema);
    setIfPresent(systemProperties, "perc.db.user", user);
    setIfPresent(systemProperties, "perc.db.password", password);
    setIfPresent(
        systemProperties,
        "perc.db.ssl.trustStorePath",
        getConfigValue("db.ssl.trustStorePath", cliOptions, environmentFileValues, null));
    setIfPresent(
        systemProperties,
        "perc.db.ssl.trustStorePassword",
        getConfigValue("db.ssl.trustStorePassword", cliOptions, environmentFileValues, null));
    setIfPresent(
        systemProperties,
        "perc.db.ssl.keyStorePath",
        getConfigValue("db.ssl.keyStorePath", cliOptions, environmentFileValues, null));
    setIfPresent(
        systemProperties,
        "perc.db.ssl.keyStorePassword",
        getConfigValue("db.ssl.keyStorePassword", cliOptions, environmentFileValues, null));

    if (Objects.equals(dbType, "mysql")) {
      // Schema may be empty for MySQL; do not use firstNonBlank("", ...) (empty is blank).
      String resolvedSchema = schema == null ? "" : schema.trim();
      // connectionCollation matches compose/server utf8mb4_unicode_ci so UNION views
      // (installRepository sqlMysql) do not mix latin1_swedish_ci connection literals.
      String mysqlParams =
          "?useUnicode=yes&characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci"
              + "&useSSL="
              + sslEnabled
              + "&requireSSL="
              + sslEnabled
              + "&verifyServerCertificate="
              + sslVerify;
      String cmsServer = "//" + host + ":" + port + "/" + name + mysqlParams;
      systemProperties.put("perc.db.cms.backend", "MYSQL");
      systemProperties.put("perc.db.cms.driverName", MYSQL_COMPAT_DRIVER_NAME);
      // Prefer MariaDB client shipped in jetty/base/lib/jdbc (specs/001-fix-jdbc-drivers)
      systemProperties.put("perc.db.cms.driverClass", MARIADB_DRIVER_CLASS);
      systemProperties.put("perc.db.cms.server", cmsServer);
      systemProperties.put("perc.db.cms.name", name);
      systemProperties.put("perc.db.cms.schema", resolvedSchema);
      systemProperties.put(
          "perc.db.dts.jdbcUrl", "jdbc:mysql://" + host + ":" + port + "/" + name + mysqlParams);
      systemProperties.put("perc.db.dts.jdbcDriver", MARIADB_DRIVER_CLASS);
      systemProperties.put(
          "perc.db.dts.hibernateDialect", "org.hibernate.dialect.MySQL5InnoDBDialect");
      systemProperties.put("perc.db.dts.schema", resolvedSchema);
    } else if (Objects.equals(dbType, "sqlserver")) {
      String resolvedSchema = (schema == null || schema.trim().isEmpty()) ? "dbo" : schema.trim();
      String trustServerCertificate =
          Objects.equals(sslAllowSelfSigned, "true") || Objects.equals(sslVerify, "false")
              ? "true"
              : "false";
      String cmsServer =
          "//"
              + host
              + ":"
              + port
              + ";databaseName="
              + name
              + ";encrypt="
              + sslEnabled
              + ";trustServerCertificate="
              + trustServerCertificate;
      systemProperties.put("perc.db.cms.backend", "MSSQL");
      systemProperties.put("perc.db.cms.driverName", "sqlserver");
      systemProperties.put("perc.db.cms.driverClass", MSSQL_DRIVER_CLASS);
      systemProperties.put("perc.db.cms.server", cmsServer);
      systemProperties.put("perc.db.cms.name", name);
      systemProperties.put("perc.db.cms.schema", resolvedSchema);
      systemProperties.put(
          "perc.db.dts.jdbcUrl",
          "jdbc:sqlserver://"
              + host
              + ":"
              + port
              + ";databaseName="
              + name
              + ";encrypt="
              + sslEnabled
              + ";trustServerCertificate="
              + trustServerCertificate);
      systemProperties.put("perc.db.dts.jdbcDriver", MSSQL_DRIVER_CLASS);
      systemProperties.put(
          "perc.db.dts.hibernateDialect",
          "com.percussion.delivery.rdbms.PSUnicodeSQLServerDialect");
      systemProperties.put("perc.db.dts.schema", resolvedSchema);
    } else if (Objects.equals(dbType, "oracle")) {
      String resolvedSchema =
          (schema == null || schema.trim().isEmpty())
              ? (user == null ? "" : user.trim())
              : schema.trim();
      // Service/SID form: @host:port:name (name may be service or SID)
      String serviceOrSid = name == null ? "" : name.trim();
      String cmsServer = "@" + host + ":" + port + ":" + serviceOrSid;
      systemProperties.put("perc.db.cms.backend", "ORACLE");
      systemProperties.put("perc.db.cms.driverName", ORACLE_DRIVER_NAME);
      systemProperties.put("perc.db.cms.driverClass", ORACLE_DRIVER_CLASS);
      systemProperties.put("perc.db.cms.server", cmsServer);
      systemProperties.put("perc.db.cms.name", serviceOrSid);
      systemProperties.put("perc.db.cms.schema", resolvedSchema);
      systemProperties.put(
          "perc.db.dts.jdbcUrl", "jdbc:oracle:thin:@" + host + ":" + port + ":" + serviceOrSid);
      systemProperties.put("perc.db.dts.jdbcDriver", ORACLE_DRIVER_CLASS);
      systemProperties.put(
          "perc.db.dts.hibernateDialect", "org.hibernate.dialect.Oracle12cDialect");
      systemProperties.put("perc.db.dts.schema", resolvedSchema);
    } else if (Objects.equals(dbType, "postgresql")) {
      String resolvedSchema =
          (schema == null || schema.trim().isEmpty()) ? POSTGRES_DEFAULT_SCHEMA : schema.trim();
      // Product DB_SERVER form mirrors MySQL: //host:port/database (jdbc:postgresql: + server)
      String cmsServer = "//" + host + ":" + port + "/" + name;
      systemProperties.put("perc.db.cms.backend", "POSTGRES");
      systemProperties.put("perc.db.cms.driverName", POSTGRES_DRIVER_NAME);
      systemProperties.put("perc.db.cms.driverClass", POSTGRES_DRIVER_CLASS);
      systemProperties.put("perc.db.cms.server", cmsServer);
      systemProperties.put("perc.db.cms.name", name);
      systemProperties.put("perc.db.cms.schema", resolvedSchema);
      systemProperties.put(
          "perc.db.dts.jdbcUrl", "jdbc:postgresql://" + host + ":" + port + "/" + name);
      systemProperties.put("perc.db.dts.jdbcDriver", POSTGRES_DRIVER_CLASS);
      systemProperties.put(
          "perc.db.dts.hibernateDialect", "org.hibernate.dialect.PostgreSQLDialect");
      systemProperties.put("perc.db.dts.schema", resolvedSchema);
    }

    return new ResolvedDbConfig(systemProperties, "structured");
  }

  private static void applyDtsHintsFromCms(
      Map<String, String> systemProperties,
      String dbType,
      String server,
      String name,
      String schema,
      String driverClass,
      String sslEnabled,
      String sslVerify) {
    // Best-effort residual DTS hints; full DTS write-through is out of scope for #949.
    if (Objects.equals(dbType, "mysql")) {
      systemProperties.put("perc.db.dts.jdbcDriver", driverClass);
      systemProperties.put(
          "perc.db.dts.hibernateDialect", "org.hibernate.dialect.MySQL5InnoDBDialect");
      systemProperties.put("perc.db.dts.schema", schema);
      if (server != null && server.startsWith("//")) {
        systemProperties.put("perc.db.dts.jdbcUrl", "jdbc:mysql:" + server);
      }
    } else if (Objects.equals(dbType, "sqlserver")) {
      systemProperties.put("perc.db.dts.jdbcDriver", driverClass);
      systemProperties.put(
          "perc.db.dts.hibernateDialect",
          "com.percussion.delivery.rdbms.PSUnicodeSQLServerDialect");
      systemProperties.put("perc.db.dts.schema", schema);
      if (server != null && server.startsWith("//")) {
        systemProperties.put("perc.db.dts.jdbcUrl", "jdbc:sqlserver:" + server);
      }
    } else if (Objects.equals(dbType, "oracle")) {
      systemProperties.put("perc.db.dts.jdbcDriver", driverClass);
      systemProperties.put(
          "perc.db.dts.hibernateDialect", "org.hibernate.dialect.Oracle12cDialect");
      systemProperties.put("perc.db.dts.schema", schema);
      if (server != null) {
        String thin = server.startsWith("@") ? server : "@" + server;
        systemProperties.put("perc.db.dts.jdbcUrl", "jdbc:oracle:thin:" + thin);
      }
    } else if (Objects.equals(dbType, "postgresql")) {
      systemProperties.put("perc.db.dts.jdbcDriver", driverClass);
      systemProperties.put(
          "perc.db.dts.hibernateDialect", "org.hibernate.dialect.PostgreSQLDialect");
      systemProperties.put(
          "perc.db.dts.schema",
          (schema == null || schema.isEmpty()) ? POSTGRES_DEFAULT_SCHEMA : schema);
      // DB_SERVER is product form "//host:port/database". Prefix with "jdbc:postgresql:"
      // (not "jdbc:postgresql://") so the result is jdbc:postgresql://host:port/database —
      // same pattern as mysql/sqlserver above.
      if (server != null && server.startsWith("//")) {
        systemProperties.put("perc.db.dts.jdbcUrl", "jdbc:postgresql:" + server);
      }
    }
  }

  /**
   * Map {@code DB_BACKEND} values from rxrepository.properties to installer {@code perc.db.type}.
   */
  static String normalizeBackendToDbType(String backendRaw) {
    if (isBlank(backendRaw)) {
      return DB_TYPE_DEFAULT;
    }
    String b = backendRaw.trim().toUpperCase(Locale.ROOT);
    return switch (b) {
      case "DERBY" -> "derby";
      case "H2" -> "h2";
      case "MYSQL" -> "mysql";
      case "MSSQL" -> "sqlserver";
      case "ORACLE", "ORA" -> "oracle";
      case "POSTGRES", "POSTGRESQL" -> "postgresql";
      default ->
          throw new IllegalArgumentException(
              "Unknown DB_BACKEND='"
                  + backendRaw
                  + "'. Allowed values: H2, DERBY, MYSQL, MSSQL, ORACLE, POSTGRES");
    };
  }

  static String normalizeStructuredDbType(String dbTypeRaw) {
    String t = dbTypeRaw.trim().toLowerCase(Locale.ROOT);
    return switch (t) {
      case "h2", "derby", "mysql", "sqlserver", "oracle", "postgresql" -> t;
      case "mssql" -> "sqlserver";
      case "ora" -> "oracle";
      case "postgres" -> "postgresql";
      default ->
          throw new IllegalArgumentException(
              "Unknown db.type='"
                  + dbTypeRaw
                  + "'. Allowed values: h2, derby, mysql, sqlserver, oracle, postgresql");
    };
  }

  /**
   * Map normalized installer {@code db.type} values to {@code DB_BACKEND} labels.
   *
   * <p>Only known types are accepted — fail-fast like {@link #normalizeStructuredDbType(String)}
   * (do not silently default unknown values to H2).
   */
  static String backendLabelForType(String dbType) {
    return switch (dbType) {
      case "h2" -> "H2";
      case "mysql" -> "MYSQL";
      case "sqlserver" -> "MSSQL";
      case "oracle" -> "ORACLE";
      case "derby" -> "DERBY";
      case "postgresql" -> "POSTGRES";
      default ->
          throw new IllegalArgumentException(
              "Unknown db.type='"
                  + dbType
                  + "' for backend label. Allowed values: h2, derby, mysql, sqlserver, oracle,"
                  + " postgresql");
    };
  }

  private static Map<String, String> loadEnvFileIfConfigured(Map<String, String> cliOptions) {
    Map<String, String> environmentFileValues = new HashMap<>();
    String envFilePath =
        firstNonBlank(
            cliOptions.get("db.config.env.file"),
            System.getenv("DB_CONFIG_ENV_FILE"),
            System.getenv("PERC_DB_CONFIG_ENV_FILE"));

    if (envFilePath != null) {
      environmentFileValues.putAll(loadEnvFile(envFilePath));
    }
    return environmentFileValues;
  }

  static Map<String, String> loadEnvFile(String envFilePath) {
    Map<String, String> values = new HashMap<>();
    Path path = Paths.get(envFilePath);
    if (!Files.exists(path)) {
      throw new IllegalArgumentException("db.config.env.file not found: " + envFilePath);
    }
    try {
      List<String> lines = Files.readAllLines(path);
      for (String line : lines) {
        if (line == null) {
          continue;
        }
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
          continue;
        }
        int separator = trimmed.indexOf('=');
        if (separator <= 0) {
          continue;
        }
        String key = trimmed.substring(0, separator).trim();
        String value = trimmed.substring(separator + 1).trim();
        values.put(key, value);
      }
      return values;
    } catch (IOException e) {
      throw new IllegalArgumentException("Unable to read db.config.env.file: " + envFilePath, e);
    }
  }

  static String getConfigValue(
      String logicalKey,
      Map<String, String> cliOptions,
      Map<String, String> envFileValues,
      String defaultValue) {
    String envStyle = logicalToEnvStyle(logicalKey);
    String percEnvStyle = "PERC_" + envStyle;
    return firstNonBlank(
        cliOptions.get(logicalKey),
        cliOptions.get(envStyle),
        cliOptions.get(percEnvStyle),
        envFileValues.get(logicalKey),
        envFileValues.get(envStyle),
        envFileValues.get(percEnvStyle),
        System.getenv(logicalKey),
        System.getenv(envStyle),
        System.getenv(percEnvStyle),
        defaultValue);
  }

  private static String logicalToEnvStyle(String logicalKey) {
    return logicalKey.toUpperCase(Locale.ROOT).replace('.', '_');
  }

  static String firstNonBlank(String... values) {
    for (String value : values) {
      if (!isBlank(value)) {
        return value.trim();
      }
    }
    return null;
  }

  static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  private static String trimToNull(String value) {
    if (isBlank(value)) {
      return null;
    }
    return value.trim();
  }

  static String normalizeBoolean(String value, String key) {
    if (isBlank(value)) {
      throw new IllegalArgumentException("Missing required boolean value for " + key);
    }
    if ("true".equalsIgnoreCase(value)) {
      return "true";
    }
    if ("false".equalsIgnoreCase(value)) {
      return "false";
    }
    throw new IllegalArgumentException("Invalid boolean value for " + key + ": " + value);
  }

  static void setIfPresent(Map<String, String> target, String key, String value) {
    if (!isBlank(value)) {
      target.put(key, value.trim());
    }
  }

  /**
   * Parse the {@code --demo-sites} / {@code --no-demo-sites} CLI flag (also accepted via the {@link
   * #DEMO_SITES_SYSTEM_PROPERTY} system property). Default {@code false}. Mirrors the truthy
   * parsing used by {@link ObsoleteInstallDirCleaner#parseCleanInstallDirFlag}.
   *
   * @param cliOptions parsed CLI options map; may be null
   * @return true when the operator opted in to sample-site seeding
   */
  public static boolean parseDemoSitesFlag(Map<String, String> cliOptions) {
    String fromCli =
        cliOptions == null
            ? null
            : firstNonBlank(cliOptions.get(DEMO_SITES_KEY), cliOptions.get("install.demo.sites"));
    String fromSys = System.getProperty(DEMO_SITES_SYSTEM_PROPERTY);
    String raw = firstNonBlank(fromCli, fromSys);
    if (raw == null || raw.isBlank()) {
      return false;
    }
    String v = raw.trim().toLowerCase(java.util.Locale.ROOT);
    if ("true".equals(v) || "yes".equals(v) || "y".equals(v) || "1".equals(v)) {
      return true;
    }
    if ("false".equals(v) || "no".equals(v) || "n".equals(v) || "0".equals(v)) {
      return false;
    }
    // bare --demo-sites yields "true" from parseArgs; unparseable values fall back to false.
    return Boolean.parseBoolean(raw);
  }

  /**
   * CLI arguments parsed from the preinstall invocation.
   *
   * @param installPath install root path supplied via {@code --install-dir} (or derived from the
   *     installer's CWD); never {@code null}.
   * @param options parsed {@code --key=value} pairs keyed by the long option name; never {@code
   *     null}.
   */
  public record ParsedArgs(Path installPath, Map<String, String> options) {}

  /**
   * Resolved database configuration for the ANT install JVM.
   *
   * @param systemProperties map of {@code perc.db.*} keys for the ANT JVM
   * @param source diagnostic label: {@code default}, {@code dbprops}, {@code structured}
   */
  public record ResolvedDbConfig(Map<String, String> systemProperties, String source) {
    /**
     * Defaults source to {@code "default"}; used by tests and callers that only have properties.
     *
     * @param systemProperties map of {@code perc.db.*} keys for the ANT JVM
     */
    public ResolvedDbConfig(Map<String, String> systemProperties) {
      this(systemProperties, "default");
    }

    /**
     * Canonical constructor: defensively copies {@code systemProperties} (stripping null keys /
     * values) and defaults {@code source} to {@code "default"} when {@code null}.
     */
    public ResolvedDbConfig {
      // Map.copyOf rejects null keys/values; filter defensively.
      Map<String, String> copy = new HashMap<>();
      if (systemProperties != null) {
        for (Map.Entry<String, String> entry : systemProperties.entrySet()) {
          if (entry.getKey() != null && entry.getValue() != null) {
            copy.put(entry.getKey(), entry.getValue());
          }
        }
      }
      systemProperties = Map.copyOf(copy);
      if (source == null) {
        source = "default";
      }
    }
  }
}
